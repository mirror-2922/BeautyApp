#pragma once
#include "Engine.h"
#include <onnxruntime_cxx_api.h>

class OrtEngine : public Engine {
public:
    OrtEngine();
    ~OrtEngine() override;
    bool loadModel(const std::string& modelPath) override;
    void setBackend(const std::string& backend) override;
    std::vector<YoloResult> detect(const cv::Mat& input, float confThreshold, float iouThreshold, const std::vector<int>& allowedClasses) override;

private:
    Ort::Env env;
    Ort::Session* session = nullptr;
    Ort::SessionOptions* sessionOptions = nullptr;
    std::vector<std::string> inputNameStrings;
    std::vector<std::string> outputNameStrings;
    std::vector<const char*> inputNames;
    std::vector<const char*> outputNames;
    bool isLoaded = false;

    void processResults(Ort::Value& outputTensor, int imgW, int imgH, float confThreshold, float iouThreshold, const std::vector<int>& allowedClasses, std::vector<YoloResult>& results);
};
