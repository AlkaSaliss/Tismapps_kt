#pragma once
#include <jni.h>
#include <opencv2/core.hpp>
#include "opencv2/imgproc.hpp"
#include "android/bitmap.h"
#include <vector>
#include <string>
#include <torch/script.h>

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

void drawPredictions(Mat& img, const vector<Bbox>& boxes);

void predToBbox(vector<Bbox>& boxes, torch::Tensor& pred, vector<string>& classNames);

float iOU(const Bbox& a, const Bbox& b);

vector<Bbox> nomMaxSuppression(const vector<Bbox>& boxes, const float& iouThreshold);

void filterLowScoresAndSort(torch::Tensor& pred, const float& confidenceThreshold);