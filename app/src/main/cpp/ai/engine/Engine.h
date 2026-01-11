#pragma once
#include "../types.h"
#include <opencv2/core.hpp>
#include <vector>
#include <string>

class Engine {
public:
    virtual ~Engine() = default;
    virtual bool loadModel(const std::string& modelPath) = 0;
    virtual std::vector<YoloResult> detect(const cv::Mat& input, float confThreshold, float iouThreshold, const std::vector<int>& allowedClasses) = 0;
    virtual void setBackend(const std::string& backend) = 0;
protected:
    std::vector<std::string> classNames = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    };
};
