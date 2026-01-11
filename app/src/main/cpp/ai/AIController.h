#pragma once
#include "engine/Engine.h"
#include <memory>
#include <string>
#include <vector>
#include <opencv2/core.hpp>

class AIController {
public:
    AIController();
    bool init(const std::string& modelPath, const std::string& engineType);
    void setEngine(const std::string& engineType);
    void setBackend(const std::string& backend);
    
    // Process frame: Detect and Draw
    std::vector<YoloResult> processFrame(cv::Mat& frame, float confThreshold, float iouThreshold, const std::vector<int>& allowedClasses);

private:
    std::unique_ptr<Engine> engine;
    std::string currentModelPath;
    
    void drawResults(cv::Mat& frame, const std::vector<YoloResult>& results);
};

// Global instance
extern std::unique_ptr<AIController> aiController;

// C-style wrappers for JNI
bool initAI(const char* modelPath);
void setAIEngine(const std::string& engineName);
void setAIBackend(const std::string& backendName);
std::vector<YoloResult> runAIInference(cv::Mat& frame, float conf, float iou, const std::vector<int>& classes);
