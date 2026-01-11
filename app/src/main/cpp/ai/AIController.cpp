#include "AIController.h"
#include "engine/DNNEngine.h"
#include "engine/OrtEngine.h"
#include <opencv2/imgproc.hpp>
#include <android/log.h>

using namespace cv;
using namespace std;

std::unique_ptr<AIController> aiController;

AIController::AIController() {}

bool AIController::init(const std::string& modelPath, const std::string& engineType) {
    currentModelPath = modelPath;
    setEngine(engineType);
    return engine != nullptr && engine->loadModel(currentModelPath);
}

void AIController::setEngine(const std::string& engineType) {
    __android_log_print(ANDROID_LOG_INFO, "AIController", "Switching engine to: %s", engineType.c_str());
    if (engineType == "ONNXRuntime") {
        engine = make_unique<OrtEngine>();
    } else {
        engine = make_unique<DNNEngine>();
    }
    
    if (!currentModelPath.empty()) {
        engine->loadModel(currentModelPath);
    }
}

void AIController::setBackend(const std::string& backend) {
    if (engine) {
        engine->setBackend(backend);
    }
}

vector<YoloResult> AIController::processFrame(Mat& frame, float confThreshold, float iouThreshold, const vector<int>& allowedClasses) {
    if (!engine) return {};

    // Detect
    auto results = engine->detect(frame, confThreshold, iouThreshold, allowedClasses);
    
    // Draw (OpenCV composition)
    drawResults(frame, results);
    
    return results;
}

void AIController::drawResults(Mat& frame, const vector<YoloResult>& results) {
    for (const auto& res : results) {
        Scalar color(0, 255, 0, 255); // Green RGBA
        
        // Draw box
        rectangle(frame, Rect(res.x, res.y, res.width, res.height), color, 2);
        
        // Draw label
        string labelText = res.label + " " + to_string(int(res.confidence * 100)) + "%";
        int baseLine;
        Size labelSize = getTextSize(labelText, FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);
        int top = max(res.y, labelSize.height);
        
        rectangle(frame, Point(res.x, top - labelSize.height),
                      Point(res.x + labelSize.width, top + baseLine), color, FILLED);
        putText(frame, labelText, Point(res.x, top), FONT_HERSHEY_SIMPLEX, 0.5, Scalar(0, 0, 0, 255), 1);
    }
}

// Wrappers
bool initAI(const char* modelPath) {
    if (!aiController) {
        aiController = make_unique<AIController>();
    }
    // Default to OpenCV initially if not specified, but init() requires engine type. 
    // We'll default to OpenCV.
    return aiController->init(modelPath, "OpenCV");
}

void setAIEngine(const string& engineName) {
    if (!aiController) aiController = make_unique<AIController>();
    aiController->setEngine(engineName);
}

void setAIBackend(const string& backendName) {
    if (aiController) aiController->setBackend(backendName);
}

vector<YoloResult> runAIInference(Mat& frame, float conf, float iou, const vector<int>& classes) {
    if (aiController) {
        return aiController->processFrame(frame, conf, iou, classes);
    }
    return {};
}
