#pragma once

#include <camera/NdkCameraManager.h>
#include <camera/NdkCameraDevice.h>
#include <camera/NdkCameraCaptureSession.h>
#include <media/NdkImageReader.h>
#include <android/native_window_jni.h>
#include <opencv2/opencv.hpp>
#include <string>
#include <vector>
#include <mutex>
#include <chrono>
#include <atomic>

class NativeCamera {
public:
    NativeCamera();
    ~NativeCamera();

    bool start(int facing, int width, int height, jobject viewfinderSurface, jobject mlKitSurface);
    void stop();

private:
    ACameraManager* cameraManager = nullptr;
    ACameraDevice* cameraDevice = nullptr;
    
    ANativeWindow* viewfinderWindow = nullptr;
    ANativeWindow* mlKitWindow = nullptr;
    AImageReader* viewfinderReader = nullptr;
    AImageReader* aiReader = nullptr;

    ACaptureRequest* captureRequest = nullptr;
    ACameraCaptureSession* captureSession = nullptr;
    ACaptureSessionOutputContainer* outputContainer = nullptr;
    
    ACameraOutputTarget* viewfinderTarget = nullptr;
    ACameraOutputTarget* mlKitTarget = nullptr;
    ACameraOutputTarget* aiTarget = nullptr;

    ACaptureSessionOutput* sessionOutputViewfinder = nullptr;
    ACaptureSessionOutput* sessionOutputMlKit = nullptr;
    ACaptureSessionOutput* sessionOutputAi = nullptr;

    // Callbacks
    static void onDeviceDisconnected(void* context, ACameraDevice* device);
    static void onDeviceError(void* context, ACameraDevice* device, int error);
    static void onSessionClosed(void* context, ACameraCaptureSession* session);
    static void onSessionReady(void* context, ACameraCaptureSession* session);
    static void onSessionActive(void* context, ACameraCaptureSession* session);
    
    static void onViewfinderImage(void* context, AImageReader* reader);
    static void onAiImage(void* context, AImageReader* reader);
    
    void processViewfinderFrame(AImageReader* reader);
    void processAiFrame(AImageReader* reader);

    ACameraDevice_StateCallbacks deviceCallbacks;
    ACameraCaptureSession_stateCallbacks sessionCallbacks;

    int currentWidth = 0;
    int currentHeight = 0;
    int sensorOrientation = 0;
    int lensFacing = 0;

    std::mutex windowMutex;
    std::atomic<bool> cameraActive{false};

    // Perf metrics
    std::chrono::steady_clock::time_point lastFrameTime;
    float currentFps = 0;
    int frameCount = 0;
    std::chrono::steady_clock::time_point lastFpsUpdateTime;
};
