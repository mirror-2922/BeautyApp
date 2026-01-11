#pragma once
#include "Engine.h"
#include <opencv2/dnn.hpp>

class DNNEngine : public Engine {
public:
    DNNEngine();
    bool loadModel(const std::string& modelPath) override;
    void setBackend(const std::string& backend) override;
    std::vector<YoloResult> detect(const cv::Mat& input, float confThreshold, float iouThreshold, const std::vector<int>& allowedClasses) override;

private:
    cv::dnn::Net net;
    bool isLoaded = false;
    const int netInputWidth = 640;
    const int netInputHeight = 640;
};
