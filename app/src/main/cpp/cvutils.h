#pragma once
#include <array>
#include <string>
#include <jni.h>
#include "android/bitmap.h"
#include <opencv2/core.hpp>
#include <opencv2/dnn/dnn.hpp>
#include "opencv2/imgproc.hpp"
#include <onnxruntime_cxx_api.h>
#include <android/log.h>


using namespace cv;
using namespace std;


void bitmapToMat(JNIEnv * env, jobject bitmap, Mat& dst, jboolean needUnPremultiplyAlpha);

void matToBitmap(JNIEnv * env, const Mat& src, jobject bitmap, jboolean needPremultiplyAlpha);

typedef struct Bbox {
    int left, top, width, height;
    float confidenceScore;
    string className;
    Bbox(int left, int top, int width, int height, float confidenceScore, string className);

    int bottom() const;

    int right() const;

    int area() const;

    string toString() const;

} Bbox ;

std::vector<float> preprocessImg(JNIEnv * env, Mat image);

void filterBoxes(std::unique_ptr<std::array<float, 25200*85>>& outputPtr, int numBoxes, float confidenceThreshold, int boxSize = 85);

void predictionsToBboxes(std::unique_ptr<std::array<float, 25200*85>>& outputPtr,
                              vector<Bbox>& boxes,
                               int numBoxes, const std::vector<std::string>& classNames);


void drawPredictions(Mat& img, const vector<Bbox>& boxes);

float iOU(const Bbox& a, const Bbox& b);

vector<Bbox> nomMaxSuppression(const vector<Bbox>& boxes, const float& iouThreshold);
