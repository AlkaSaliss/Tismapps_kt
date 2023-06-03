#include "cvutils.h"
#include "opencv2/imgproc.hpp"


void myFlip(cv::Mat src) {
    cv::flip(src, src, 0);
}
void myBlur(cv::Mat src, float sigma) {
    cv::GaussianBlur(src, src, cv::Size(), sigma);
}