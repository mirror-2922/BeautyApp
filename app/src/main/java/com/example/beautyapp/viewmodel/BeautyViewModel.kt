package com.example.beautyapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import android.util.Size

enum class AppMode {
    Camera, AI
}

class BeautyViewModel : ViewModel() {
    // 基础设置
    var isDarkTheme by mutableStateOf(false)
    var useDynamicColor by mutableStateOf(true)
    
    // 分辨率设置
    var cameraResolution by mutableStateOf("1280x720")
    var backendResolutionScaling by mutableStateOf(false) // "保留前端分辨率" 开关
    var targetBackendWidth by mutableStateOf(640)
    
    // 性能监控
    var showDebugInfo by mutableStateOf(true)
    var currentFps by mutableStateOf(0f)
    var inferenceTime by mutableStateOf(0L)
    var hardwareBackend by mutableStateOf("CPU")
    var actualCameraSize by mutableStateOf("0x0")
    var actualBackendSize by mutableStateOf("0x0")

    // 状态
    var currentMode by mutableStateOf(AppMode.Camera)
    var selectedFilter by mutableStateOf("Normal")
    var showFilterDialog by mutableStateOf(false)
    var showResolutionDialog by mutableStateOf(false)
    var lensFacing by mutableStateOf(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
    var isLoading by mutableStateOf(false)

    // YOLO 配置
    var yoloConfidence by mutableStateOf(0.5f)
    var yoloIoU by mutableStateOf(0.45f)
    val allCOCOClasses = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    )
    val selectedYoloClasses = mutableStateListOf<String>().apply { addAll(allCOCOClasses) }

    val availableResolutions = mutableStateListOf<String>()
    val filters = listOf("Normal", "Beauty", "Dehaze", "Underwater", "Stage")

    fun toggleYoloClass(className: String) {
        if (selectedYoloClasses.contains(className)) selectedYoloClasses.remove(className)
        else selectedYoloClasses.add(className)
    }
    
    fun getCameraSize(): Size {
        val parts = cameraResolution.split("x")
        return Size(parts[0].toInt(), parts[1].toInt())
    }
}
