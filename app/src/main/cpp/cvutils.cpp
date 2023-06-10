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

void filterLowScoresAndSort(torch::Tensor& pred, const float& confidenceThreshold) {

    c10::InferenceMode guard; // disable gradients ops tracking

    pred = pred.index({pred.select(1, 4) > confidenceThreshold});
    pred = pred.index_select(0, pred.select(1, 4).argsort());

    pred = torch::cat(
{
            pred.index({//x,y,w,h,objectness
                torch::indexing::Slice(),
                torch::indexing::Slice(torch::indexing::None, 5)
            }),
            pred.index({//index of class with max softmax score
               torch::indexing::Slice(),
               torch::indexing::Slice(5, torch::indexing::None)
            }).argmax(1, true)
        },
    1
    );
    // Now pred is of shape [X, 6] where 6 is x,y,w,h,objectness, index of class with max softmax score
}

void predToBbox(vector<Bbox>& boxes, torch::Tensor& pred, vector<string>& classNames) {

    c10::InferenceMode guard; // disable gradients ops tracking

    for (int i = 0; i < pred.sizes()[0]; ++i) {
        const int left = (pred.index({i, 0}).item().toInt() - pred.index({i, 2}).item().toInt() / 2);
        const int top = (pred.index({i, 1}).item().toInt() - pred.index({i, 3}).item().toInt() / 2);
        const int width = pred.index({i, 2}).item().toInt();
        const int height = pred.index({i, 3}).item().toInt();
        const float score = pred.index({i, 4}).item().toFloat();
        const string className =  classNames[pred.index({i, 5}).item().toInt()];
        const auto box = Bbox(left, top, width, height, score, className);
        boxes.push_back(box);
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
