#include "cvutils.h"


void bitmapToMat(JNIEnv * env, jobject bitmap, Mat& dst, jboolean needUnPremultiplyAlpha)
{
    AndroidBitmapInfo  info;
    void*              pixels = nullptr;

    try {
        CV_Assert( AndroidBitmap_getInfo( env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        dst.create(info.height, info.width, CV_8UC4);
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(needUnPremultiplyAlpha) cvtColor(tmp, dst, COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}


void matToBitmap(JNIEnv * env, const Mat& src, jobject bitmap, jboolean needPremultiplyAlpha)
{
    AndroidBitmapInfo  info;
    void*              pixels = nullptr;

    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( src.dims == 2 && info.height == (uint32_t)src.rows && info.width == (uint32_t)src.cols );
        CV_Assert( src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(src.type() == CV_8UC1)
            {
                cvtColor(src, tmp, COLOR_GRAY2RGBA);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, COLOR_RGB2RGBA);
            } else if(src.type() == CV_8UC4){
                if(needPremultiplyAlpha) cvtColor(src, tmp, COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if(src.type() == CV_8UC1)
            {
                cvtColor(src, tmp, COLOR_GRAY2BGR565);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, COLOR_RGB2BGR565);
            } else if(src.type() == CV_8UC4){
                cvtColor(src, tmp, COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}



Bbox::Bbox(int left, int top, int width, int height, float confidenceScore, string className="") :
        left(left), top(top), width(width), height(height), confidenceScore(confidenceScore),
        className(std::move(className)) {
}

int Bbox::bottom() const {
    return this->top + this->height;
}

int Bbox::right() const {
    return this->left + this->width;
}

int Bbox::area() const {
    return this->width * this->height;
}

string Bbox::toString() const{
    return "left: " + to_string(left) + ", "
           + "top: " + to_string(top) + ", "
           + "width: " + to_string(width) + ", "
           + "height: " + to_string(height) + ", "
           + "score: " + to_string(confidenceScore) + ", "
           + "class: " + className
            ;
}

void drawPredictions(Mat& img, const vector<Bbox>& boxes) {
    for (const Bbox& box : boxes) {
        rectangle(
                img,
                Rect(box.left, box.top, box.width, box.height),
                Scalar(0, 255, 0),
                2
        );
        putText(
                img,
                box.className,
                Point(box.left, max(box.top-5, 0)),
                FONT_HERSHEY_COMPLEX_SMALL,
                1,
                Scalar(0, 255, 0),
                1
        );
    }
}


float iOU(const Bbox& a, const Bbox& b) {

    if (a.area() <= 0 || b.area() <= 0)
        return 0.0;

    const int intersectionMinX = max(a.left, b.left);
    const int intersectionMinY = max(a.top, b.top);
    const int intersectionMaxX = min(a.right(), b.right());
    const int intersectionMaxY = min(a.bottom(), b.bottom());
    const int intersectionArea = max(intersectionMaxY - intersectionMinY, 0) * max(intersectionMaxX - intersectionMinX, 0);

    return float(intersectionArea) / (1e-7 + float(a.area() + b.area() - intersectionArea));
}

vector<Bbox> nomMaxSuppression(const vector<Bbox>& boxes, const float& iouThreshold) {
    vector<Bbox> selected;
    vector<bool> active(boxes.size(), true);
    auto numActive = active.size();
    bool done = false;
    int i = 0;

    while (i < boxes.size() && !done) {
        if (active[i]){
            const auto& boxA = boxes[i];
            selected.push_back(boxA);
            for (int j = i + 1; j < boxes.size(); ++j) {
                if (active[j]) {
                    const auto& boxB = boxes[j];
                    if (iOU(boxA, boxB) > iouThreshold) {
                        active[j] = false;
                        numActive--;
                        if (numActive <= 0) {
                            done = true;
                            break;
                        }
                    }
                }
            }
        }
        i++;
    }
    return selected;
}


std::vector<float> preprocessImg(JNIEnv * env, Mat image)
{
    try {
    // reshape to 1D
    //cv::resize(image, image, cv::Size(640, 640));
    image = image.reshape(1, 1);


    //__android_log_print(ANDROID_LOG_VERBOSE, "ONNX_CPP", "ONNX_CPP 6 %d %d %d", dim1, dim2, dim3);
    // uint_8, [0, 255] -> float, [0, 1]
    // Normalize number to between 0 and 1
    // Convert to vector<float> from cv::Mat.

    std::vector<float> vec;
    image.convertTo(vec, CV_32FC1, 1. / 255);

    // Transpose (Height, Width, Channel) to (Channel, Height, Width)
    std::vector<float> output;
    for (size_t ch = 0; ch < 3; ++ch) {
        for (size_t i = ch; i < vec.size(); i += 3) {
            output.emplace_back(vec[i]);
        }
    }
    return output;
    } catch(const cv::Exception& e) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        std::vector<float> vec;
        return vec;
    }
}

void filterBoxes(std::unique_ptr<std::array<float, 25200*85>>& outputPtr, int numBoxes, float confidenceThreshold, int boxSize) {
    //boxSize: shape of each box prediction by yolov5

    // Iterate through the boxes and remove those with a confidence score lower than the threshold
    for (int i = 0; i < numBoxes; ++i) {
        float objectnessScore = (*outputPtr)[i * boxSize + 4];
        if (objectnessScore < confidenceThreshold) {
            // Set the coordinates and objectness score to zero to mark for removal
            for (int j = 0; j < boxSize; ++j) {
                (*outputPtr)[i * boxSize + j] = 0.0f;
            }
        }
    }
}


// Function to create Bbox objects from filtered output and sort them by descending objectness score
void predictionsToBboxes(std::unique_ptr<std::array<float, 25200*85>>& outputPtr,
                              vector<Bbox>& result,
                               int numBoxes, const std::vector<std::string>& classNames) {

    int boxSize = 85;  // Size of each box in the flattened output

    // Iterate through the filtered boxes and create Bbox objects
    for (int i = 0; i < numBoxes; ++i) {
        float objectnessScore = (*outputPtr)[i * boxSize + 4];
        if (objectnessScore != 0.0f) {  // Check if the box is not marked for removal
            float centerX = (*outputPtr)[i * boxSize];       // Center X-coordinate
            float centerY = (*outputPtr)[i * boxSize + 1];   // Center Y-coordinate
            float boxWidth = (*outputPtr)[i * boxSize + 2];  // Width of the bounding box
            float boxHeight = (*outputPtr)[i * boxSize + 3]; // Height of the bounding box

            // Calculate top-left coordinates
            int x = static_cast<int>((centerX - boxWidth / 2.0f));
            int y = static_cast<int>((centerY - boxHeight / 2.0f));



            // Find the index of the class with the highest probability
            int maxClassIdx = -1;
            float maxClassProb = 0.0f;
            for (int j = 6; j < boxSize; ++j) {
                if ((*outputPtr)[i * boxSize + j] > maxClassProb) {
                    maxClassIdx = j - 6;
                    maxClassProb = (*outputPtr)[i * boxSize + j];
                }
            }

            // Get the class name based on the index
            std::string className = (maxClassIdx >= 0 && maxClassIdx < classNames.size())
                                    ? classNames[maxClassIdx]
                                    : "Unknown";

            // Create a Bbox object and add it to the result vector
            Bbox bbox(x, y, static_cast<int>(boxWidth), static_cast<int>(boxHeight), objectnessScore, className);
            result.push_back(bbox);
        }
    }

    // Sort the result vector by descending objectness score
    std::sort(result.begin(), result.end(), [](const Bbox& a, const Bbox& b) {
        return a.confidenceScore > b.confidenceScore;
    });

}