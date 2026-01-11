#include "DNNEngine.h"
#include <android/log.h>
#include <opencv2/imgproc.hpp>
#include <set>

using namespace cv;
using namespace cv::dnn;
using namespace std;

DNNEngine::DNNEngine() {}

bool DNNEngine::loadModel(const string& modelPath) {
    try {
        net = readNet(modelPath);
        net.setPreferableBackend(DNN_BACKEND_OPENCV);
        net.setPreferableTarget(DNN_TARGET_CPU);
        isLoaded = true;
        __android_log_print(ANDROID_LOG_DEBUG, "DNNEngine", "Model loaded: %s", modelPath.c_str());
    } catch (const cv::Exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, "DNNEngine", "Load error: %s", e.what());
        isLoaded = false;
    }
    return isLoaded;
}

void DNNEngine::setBackend(const string& backend) {
    if (!isLoaded) return;
    __android_log_print(ANDROID_LOG_INFO, "DNNEngine", "Setting backend to: %s", backend.c_str());
    
    if (backend == "OpenCL" || backend == "GPU") {
        net.setPreferableBackend(DNN_BACKEND_OPENCV);
        net.setPreferableTarget(DNN_TARGET_OPENCL);
    } else if (backend == "NNAPI") {
        net.setPreferableBackend(DNN_BACKEND_DEFAULT);
        net.setPreferableTarget(DNN_TARGET_NPU);
    } else {
        net.setPreferableBackend(DNN_BACKEND_OPENCV);
        net.setPreferableTarget(DNN_TARGET_CPU);
    }
}

vector<YoloResult> DNNEngine::detect(const Mat& input, float confThreshold, float iouThreshold, const vector<int>& allowedClasses) {
    vector<YoloResult> results;
    if (!isLoaded || input.empty()) return results;

    std::set<int> allowedSet(allowedClasses.begin(), allowedClasses.end());
    
    Mat rgb;
    if (input.channels() == 4) cvtColor(input, rgb, COLOR_RGBA2RGB);
    else if (input.channels() == 3) rgb = input;
    else return results; // Handle other cases?

    int imgW = input.cols;
    int imgH = input.rows;

    Mat blob;
    blobFromImage(rgb, blob, 1.0/255.0, Size(netInputWidth, netInputHeight), Scalar(), false, false);
    net.setInput(blob);

    vector<Mat> outputs;
    net.forward(outputs, net.getUnconnectedOutLayersNames());

    Mat output = outputs[0];
    // Check output dimensions. YOLOv8/v5 typically: [1, 84, 8400] (for 80 classes + 4 box)
    // 4 + 80 = 84
    
    int dimensions = output.size[1];
    int rows = output.size[2];
    
    // If shapes are transposed (e.g. [1, 8400, 84])
    bool transposed = false;
    if (dimensions > rows) {
        // likely [1, 8400, 84] ? No, typically [1, channels, anchors]
        // But if dimensions is 8400 and rows is 84, that's weird for standard YOLOv8 export.
        // YOLOv8 export is [1, 4+nc, anchors].
        // Let's assume [1, dimensions, rows] where dimensions = 4+nc, rows = anchors.
    }

    Mat out2D = output.reshape(1, dimensions); 
    Mat t_output;
    transpose(out2D, t_output); // [rows, dimensions]
    
    float* data = (float*)t_output.data;
    
    vector<int> class_ids;
    vector<float> confidences;
    vector<Rect> boxes;

    float x_factor = (float)imgW / netInputWidth;
    float y_factor = (float)imgH / netInputHeight;

    for (int i = 0; i < rows; ++i) {
        float* row_ptr = data + (i * dimensions);
        float* scores_ptr = row_ptr + 4;
        
        // Find max score
        float max_score = 0;
        int class_id = -1;
        
        // Optimization: loop manually or use minMaxLoc? Manual is often faster for small arrays?
        // dimensions = 4 + 80 = 84.
        for (int j = 0; j < dimensions - 4; ++j) {
            if (scores_ptr[j] > max_score) {
                max_score = scores_ptr[j];
                class_id = j;
            }
        }

        if (max_score > confThreshold) {
            if (allowedSet.empty() || allowedSet.count(class_id)) {
                float cx = row_ptr[0];
                float cy = row_ptr[1];
                float w = row_ptr[2];
                float h = row_ptr[3];

                int left = int((cx - 0.5 * w) * x_factor);
                int top = int((cy - 0.5 * h) * y_factor);
                int width = int(w * x_factor);
                int height = int(h * y_factor);

                boxes.push_back(Rect(left, top, width, height));
                confidences.push_back(max_score);
                class_ids.push_back(class_id);
            }
        }
    }

    vector<int> indices;
    NMSBoxes(boxes, confidences, confThreshold, iouThreshold, indices);

    for (int idx : indices) {
        YoloResult res;
        int clsId = class_ids[idx];
        if (clsId >= 0 && clsId < classNames.size()) res.label = classNames[clsId];
        else res.label = "unknown";
        
        res.confidence = confidences[idx];
        res.x = boxes[idx].x;
        res.y = boxes[idx].y;
        res.width = boxes[idx].width;
        res.height = boxes[idx].height;
        res.classId = class_ids[idx];
        results.push_back(res);
    }
    return results;
}
