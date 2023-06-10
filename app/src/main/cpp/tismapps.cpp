#include <android/log.h>
#include <fstream>
#include <iostream>
#include "cvutils.h"

using namespace std;
using namespace cv;


// CONSTANTS
torch::jit::Module MODULE;
float CONFIDENCE_THRESHOLD, IOU_THRESHOLD;
vector<string> CLASS_NAMES;


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

    c10::InferenceMode guard; // disable gradients ops tracking

    // Initialize different containers
    Mat img;
    vector<Bbox> boxes;
    // Prepare input
    bitmapToMat(env, imgBitmap, img, false);
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

    // make predictions
    torch::Tensor output = MODULE.forward(inputs).toTuple()->elements()[0].toTensor()[0];
    filterLowScoresAndSort(output, CONFIDENCE_THRESHOLD);
    predToBbox(boxes, output, CLASS_NAMES);
    boxes = nomMaxSuppression(boxes, IOU_THRESHOLD);

    // Draw bboxes on image
    drawPredictions(img, boxes);
    matToBitmap(env, img, imgBitmap, false);

    return imgBitmap;
}