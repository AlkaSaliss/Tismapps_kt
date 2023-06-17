#include <android/log.h>
#include <fstream>
#include <iostream>
#include "cvutils.h"
#include <ctime>
#include <stdio.h>

using namespace std;
using namespace cv;


// CONSTANTS
torch::jit::Module MODULE;
float CONFIDENCE_THRESHOLD, IOU_THRESHOLD;
vector<string> CLASS_NAMES;

#define GET_VARIABLE_NAME(Variable) (#Variable)

extern "C" JNIEXPORT void JNICALL
Java_com_example_tismapps_ui_data_DetectorViewModel_loadModuleNative(
        JNIEnv* env,
        jobject,
        jstring jmodulePath,
        jstring jclassNamesPath,
        jfloat confidenceThreshold,
        jfloat iouThreshold
){
    c10::InferenceMode guard; // disable gradients ops tracking

    const char* modelPath = env->GetStringUTFChars(jmodulePath, nullptr);
    const char* classNamesPath = env->GetStringUTFChars(jclassNamesPath, nullptr);

    // Model loading
    MODULE = torch::jit::load(modelPath);
    MODULE.eval();

    // Class names loading
    fstream classNameFile(classNamesPath);
    string cls;
    while(getline(classNameFile, cls)){
        CLASS_NAMES.push_back(cls);
    }
    classNameFile.close();

    // Thresholds loading
    CONFIDENCE_THRESHOLD = confidenceThreshold;
    IOU_THRESHOLD = iouThreshold;

    // Free some unused memory
    env->ReleaseStringUTFChars(jmodulePath, modelPath);
    env->ReleaseStringUTFChars(jclassNamesPath, classNamesPath);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_tismapps_ui_data_DetectorViewModel_predictNative(
        JNIEnv* env,
        jobject,
        jobject imgBitmap
){

    clock_t start_time = clock();

    c10::InferenceMode guard; // disable gradients ops tracking

    // Initialize different containers
    Mat img;
    vector<Bbox> boxes;
    // Prepare input
    bitmapToMat(env, imgBitmap, img, false);

    clock_t time_bitmapToMat = clock();
    std::vector<torch::jit::IValue> inputs;
    torch::Tensor imgTensor =
        torch::from_blob(img.data, {img.rows, img.cols, img.channels()}, torch::kByte)
        .index({
            torch::indexing::Slice(), // take all rows
            torch::indexing::Slice(), // take all cols
            torch::indexing::Slice(torch::indexing::None, 3) // take only 3 RGB channels in case there's an extra alpha channel
        })
        .permute({2, 0, 1}) // HWC -> CHW
        .toType(torch::kFloat)
        .div(255)
        .unsqueeze(0);
    inputs.emplace_back(imgTensor);
    clock_t time_inputCreation = clock();

    // make predictions
    torch::Tensor output = MODULE.forward(inputs).toTuple()->elements()[0].toTensor()[0];
    clock_t time_forward = clock();
    //torch::Tensor output = torch::randn({25000, 85});
    filterLowScoresAndSort(output, CONFIDENCE_THRESHOLD);

    clock_t time_filterLowScoresAndSort = clock();
    predToBbox(boxes, output, CLASS_NAMES);
    clock_t time_predToBbox = clock();
    boxes = nomMaxSuppression(boxes, IOU_THRESHOLD);
    clock_t time_nomMaxSuppression = clock();

    // Draw bboxes on image
    drawPredictions(img, boxes);
    clock_t time_drawPredictions = clock();
    matToBitmap(env, img, imgBitmap, false);
    clock_t time_matToBitmap = clock();

    auto compute_duration = [](clock_t& start, clock_t& end) {
        return ((float)(end - start)) / CLOCKS_PER_SEC;
    };
    auto duration_bitmapToMat = compute_duration(start_time, time_bitmapToMat);
    auto duration_inputCreation = compute_duration(time_bitmapToMat, time_inputCreation);
    auto duration_forward = compute_duration(time_inputCreation, time_forward);
    auto duration_filterLowScoresAndSort = compute_duration(time_forward, time_filterLowScoresAndSort);
    auto duration_predToBbox = compute_duration(time_filterLowScoresAndSort, time_predToBbox);
    auto duration_nomMaxSuppression = compute_duration(time_predToBbox, time_nomMaxSuppression);
    auto duration_drawPredictions = compute_duration(time_nomMaxSuppression, time_drawPredictions);
    auto duration_matToBitmap = compute_duration(time_drawPredictions, time_matToBitmap);

    auto duration_bitmapToMatVal = (string)GET_VARIABLE_NAME(duration_matToBitmap);

    auto format_duration = [](float& var){
        return (string)GET_VARIABLE_NAME(var) + "="+to_string(var);
    };

    auto durations_formatted = format_duration(duration_bitmapToMat) + ", "
        + format_duration(duration_bitmapToMat) + ", "
        + format_duration(duration_inputCreation) + ", "
        + format_duration(duration_forward) + ", "
        + format_duration(duration_filterLowScoresAndSort) + ", "
        + format_duration(duration_predToBbox) + ", "
        + format_duration(duration_nomMaxSuppression) + ", "
        + format_duration(duration_drawPredictions) + ", "
        + format_duration(duration_matToBitmap)
    ;


    float duration = ((float)(clock() - start_time)) / CLOCKS_PER_SEC;
    float fps = 1 / duration;

    __android_log_print(ANDROID_LOG_VERBOSE, "YOLO_FPS", "DURATION: %s", durations_formatted.c_str());

    return imgBitmap;
}