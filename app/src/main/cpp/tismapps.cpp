#include "cvutils.h"
#include <fstream>
#include <iostream>


using namespace std;


// GLOBAL VARS
Ort::RunOptions RUN_OPTIONS;
Ort::Env ONNX_ENV;
Ort::Session ONNX_SESSION(nullptr);
constexpr int64_t numChannels = 3;
constexpr int64_t width = 640;
constexpr int64_t height = 640;
constexpr int64_t numInputElements = numChannels * height * width;
constexpr int64_t numBoxes = 25200;
constexpr int64_t numOutputElements = numBoxes * 85;
const std::array<int64_t, 3> outputShape = { 1, numBoxes, 85 };

float CONFIDENCE_THRESHOLD, IOU_THRESHOLD;
vector<string> CLASS_NAMES;


extern "C" JNIEXPORT void JNICALL
Java_com_example_tismapps_ui_data_DetectorViewModel_loadOnnxModuleNative(
        JNIEnv* env,
        jobject,
        jstring jmodulePath,
        jstring jclassNamesPath,
        jfloat confidenceThreshold,
        jfloat iouThreshold
){

    const char* modelPath = env->GetStringUTFChars(jmodulePath, nullptr);
    const char* classNamesPath = env->GetStringUTFChars(jclassNamesPath, nullptr);

    ONNX_SESSION = Ort::Session(ONNX_ENV, modelPath, Ort::SessionOptions{nullptr});
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

    //__android_log_print(ANDROID_LOG_VERBOSE, "ONNX_CPP", "ONNX_CPP: LOADING MODEL");
}


extern "C" JNIEXPORT jobject JNICALL
Java_com_example_tismapps_ui_data_DetectorViewModel_predictNativeOnnx(
        JNIEnv* env,
        jobject,
        jobject imgBitmap
){
    // Initialize different containers
    Mat img;
    vector<Bbox> boxes;
    // Prepare input
    bitmapToMat(env, imgBitmap, img, false);
    std::vector<float> imgVec = preprocessImg(env, img);

    // define shape
    const std::array<int64_t, 4> inputShape = { 1, numChannels, height, width };

    // define array
    std::unique_ptr<std::array<float, numInputElements>> inputPtr =
        std::make_unique<std::array<float, numInputElements>>();
    std::unique_ptr<std::array<float, numOutputElements>> outputPtr =
        std::make_unique<std::array<float, numOutputElements>>();

    // define Tensor
    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtDeviceAllocator, OrtMemTypeCPU);
    auto inputTensor = Ort::Value::CreateTensor<float>(memory_info, (*inputPtr).data(), (*inputPtr).size(), inputShape.data(), inputShape.size());
    auto outputTensor = Ort::Value::CreateTensor<float>(memory_info, (*outputPtr).data(), (*outputPtr).size(), outputShape.data(), outputShape.size());


    //__android_log_print(ANDROID_LOG_VERBOSE, "ONNX_CPP", "ONNX_CPP 6 %d", imgVec.size());

    // copy image data to input array
    std::copy(imgVec.begin(), imgVec.end(), (*inputPtr).begin());


    // define names
    Ort::AllocatorWithDefaultOptions ort_alloc;
    Ort::AllocatedStringPtr inputName = ONNX_SESSION.GetInputNameAllocated(0, ort_alloc);
    Ort::AllocatedStringPtr outputName = ONNX_SESSION.GetOutputNameAllocated(0, ort_alloc);
    const std::array<const char*, 1> inputNames = { inputName.get()};
    const std::array<const char*, 1> outputNames = { outputName.get()};
    inputName.release();
    outputName.release();

    ONNX_SESSION.Run(
        RUN_OPTIONS,
        inputNames.data(),
        &inputTensor,
        1,
        outputNames.data(),
        &outputTensor,
        1
    );


    filterBoxes(outputPtr, numBoxes, CONFIDENCE_THRESHOLD);

    predictionsToBboxes(outputPtr, boxes, numBoxes, CLASS_NAMES);

    boxes = nomMaxSuppression(boxes, IOU_THRESHOLD);

    // Draw bboxes on image
    drawPredictions(img, boxes);
    matToBitmap(env, img, imgBitmap, false);

    //__android_log_print(ANDROID_LOG_VERBOSE, "ONNX_CPP", "ONNX_CPP: %f %d", output[0], output.size());

    return imgBitmap;
}
