#include "OrtEngine.h"
#include <android/log.h>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <onnxruntime_float16.h>
#include <set>

using namespace cv;
using namespace std;

OrtEngine::OrtEngine() : env(ORT_LOGGING_LEVEL_WARNING, "OrtEngine") {}

OrtEngine::~OrtEngine() {
    if (session) delete session;
    if (sessionOptions) delete sessionOptions;
}

bool OrtEngine::loadModel(const std::string& modelPath) {
    try {
        sessionOptions = new Ort::SessionOptions();
        sessionOptions->SetIntraOpNumThreads(4);
        sessionOptions->SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);

        session = new Ort::Session(env, modelPath.c_str(), *sessionOptions);

        Ort::AllocatorWithDefaultOptions allocator;
        inputNames.clear();
        outputNames.clear();
        inputNameStrings.clear();
        outputNameStrings.clear();

        size_t numInputNodes = session->GetInputCount();
        for (size_t i = 0; i < numInputNodes; i++) {
            auto name = session->GetInputNameAllocated(i, allocator);
            inputNameStrings.push_back(name.get());
            inputNames.push_back(inputNameStrings.back().c_str());
        }

        size_t numOutputNodes = session->GetOutputCount();
        for (size_t i = 0; i < numOutputNodes; i++) {
            auto name = session->GetOutputNameAllocated(i, allocator);
            outputNameStrings.push_back(name.get());
            outputNames.push_back(outputNameStrings.back().c_str());
        }

        isLoaded = true;
        __android_log_print(ANDROID_LOG_DEBUG, "OrtEngine", "Model loaded: %s", modelPath.c_str());
    } catch (const Ort::Exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, "OrtEngine", "Load error: %s", e.what());
        isLoaded = false;
    }
    return isLoaded;
}

void OrtEngine::setBackend(const std::string& backend) {
    // Note: To change backend in ORT, we typically need to recreate the session with new options.
    // For simplicity, we just log here. To fully support NNAPI switch at runtime, we'd need to reload the model.
    // However, usually backend is set before loading. 
    // If the user switches backend at runtime, we should reload if we stored the model path.
    // But Engine interface doesn't store path.
    // We'll rely on the caller re-initializing or accept that this only affects *next* load? 
    // Or we assume this is called *before* loadModel in a real app, or we just ignore for now as ORT NNAPI requires build time flags or specific provider options during Session creation.
    // We added NNAPI flags to CMake, so it's available.
    __android_log_print(ANDROID_LOG_INFO, "OrtEngine", "Backend switch request to %s (requires reload for ORT)", backend.c_str());
}

vector<YoloResult> OrtEngine::detect(const Mat& input, float confThreshold, float iouThreshold, const vector<int>& allowedClasses) {
    vector<YoloResult> results;
    if (!isLoaded || input.empty()) return results;

    Mat rgb;
    if (input.channels() == 4) cvtColor(input, rgb, COLOR_RGBA2RGB);
    else rgb = input;

    Mat resized;
    resize(rgb, resized, Size(640, 640));
    resized.convertTo(resized, CV_32FC3, 1.0 / 255.0);

    auto memory_info = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);
    int64_t inputShape[] = {1, 3, 640, 640};

    // Check input type
    auto input_type_info = session->GetInputTypeInfo(0);
    auto input_tensor_info = input_type_info.GetTensorTypeAndShapeInfo();
    ONNXTensorElementDataType expected_type = input_tensor_info.GetElementType();

    Ort::Value inputTensor(nullptr);

    if (expected_type == ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16) {
        vector<uint16_t> fp16_values(1 * 3 * 640 * 640);
        for (int c = 0; c < 3; ++c) {
            for (int h = 0; h < 640; ++h) {
                for (int w = 0; w < 640; ++w) {
                    float val = resized.at<Vec3f>(h, w)[c];
                    fp16_values[c * 640 * 640 + h * 640 + w] = Ort::Float16_t(val).val;
                }
            }
        }
        inputTensor = Ort::Value::CreateTensor<Ort::Float16_t>(memory_info, 
            reinterpret_cast<Ort::Float16_t*>(fp16_values.data()), fp16_values.size(), inputShape, 4);
    } else {
        vector<float> inputTensorValues(1 * 3 * 640 * 640);
        for (int c = 0; c < 3; ++c) {
            for (int h = 0; h < 640; ++h) {
                for (int w = 0; w < 640; ++w) {
                    inputTensorValues[c * 640 * 640 + h * 640 + w] = resized.at<Vec3f>(h, w)[c];
                }
            }
        }
        inputTensor = Ort::Value::CreateTensor<float>(memory_info, inputTensorValues.data(), inputTensorValues.size(), inputShape, 4);
    }

    auto outputTensors = session->Run(Ort::RunOptions{nullptr}, inputNames.data(), &inputTensor, 1, outputNames.data(), 1);
    processResults(outputTensors[0], input.cols, input.rows, confThreshold, iouThreshold, allowedClasses, results);

    return results;
}

void OrtEngine::processResults(Ort::Value& outputTensor, int imgW, int imgH, float confThreshold, float iouThreshold, const std::vector<int>& allowedClasses, std::vector<YoloResult>& results) {
    float* floatData = outputTensor.GetTensorMutableData<float>();
    auto outputShape = outputTensor.GetTensorTypeAndShapeInfo().GetShape();

    // shape: [1, 4+nc, anchors] e.g. [1, 84, 8400]
    int dimensions = (int)outputShape[1]; 
    int rows = (int)outputShape[2];       

    std::set<int> allowedSet(allowedClasses.begin(), allowedClasses.end());
    vector<int> class_ids;
    vector<float> confidences;
    vector<Rect> boxes;

    float x_factor = (float)imgW / 640.0f;
    float y_factor = (float)imgH / 640.0f;

    for (int i = 0; i < rows; ++i) {
        float max_score = 0;
        int class_id = -1;
        // classes start at index 4
        for (int j = 4; j < dimensions; ++j) {
            float score = floatData[j * rows + i]; // Data is transposed [dim, row] access pattern for typical output
            if (score > max_score) {
                max_score = score;
                class_id = j - 4;
            }
        }

        if (max_score > confThreshold) {
            if (allowedSet.empty() || allowedSet.count(class_id)) {
                float cx = floatData[0 * rows + i];
                float cy = floatData[1 * rows + i];
                float w = floatData[2 * rows + i];
                float h = floatData[3 * rows + i];

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
    cv::dnn::NMSBoxes(boxes, confidences, confThreshold, iouThreshold, indices);

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
        res.classId = clsId;
        results.push_back(res);
    }
}
