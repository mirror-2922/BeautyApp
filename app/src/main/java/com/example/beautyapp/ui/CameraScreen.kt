package com.example.beautyapp.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.beautyapp.NativeLib
import com.example.beautyapp.viewmodel.AppMode
import com.example.beautyapp.viewmodel.BeautyViewModel
import com.example.beautyapp.viewmodel.FaceResult
import com.example.beautyapp.viewmodel.YoloResultData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController, viewModel: BeautyViewModel) {
    val context = LocalContext.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // 1. Hardware Resolution Detection
    LaunchedEffect(viewModel.lensFacing) {
        withContext(Dispatchers.IO) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraManager.cameraIdList.forEach { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    val target = if (viewModel.lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT
                    
                    if (facing == target) {
                        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
                        if (sizes != null) {
                            val sortedRes = sizes.filter { it.width >= 480 }
                                .sortedByDescending { it.width * it.height }
                                .map { "${it.width}x${it.height}" }.distinct()
                            
                            withContext(Dispatchers.Main) {
                                viewModel.availableResolutions.clear()
                                viewModel.availableResolutions.addAll(sortedRes)
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 2. Model Loading
    LaunchedEffect(viewModel.currentModelId) {
        if (viewModel.currentModelId.isEmpty()) return@LaunchedEffect
        viewModel.isLoading = true
        withContext(Dispatchers.IO) {
            val modelFile = File(context.filesDir, "${viewModel.currentModelId}.onnx")
            var initSuccess = false
            if (modelFile.exists()) {
                initSuccess = NativeLib().initYolo(modelFile.absolutePath)
            }
            withContext(Dispatchers.Main) {
                viewModel.isLoading = false
                if (initSuccess) {
                    Toast.makeText(context, "AI System Ready", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BeautyApp Pro") },
                actions = {
                    IconButton(onClick = {
                        viewModel.lensFacing = if (viewModel.lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    }) { Icon(Icons.Default.Refresh, "Switch") }
                    IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PrecisionManufacturing, null) },
                    label = { Text("YOLO") },
                    selected = viewModel.currentMode == AppMode.AI,
                    onClick = { viewModel.currentMode = AppMode.AI }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Camera, null) },
                    label = { Text("Camera") },
                    selected = viewModel.currentMode == AppMode.Camera,
                    onClick = { viewModel.currentMode = AppMode.Camera }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Face, null) },
                    label = { Text("Face") },
                    selected = viewModel.currentMode == AppMode.FACE,
                    onClick = { viewModel.currentMode = AppMode.FACE }
                )
            }
        },
        floatingActionButton = {
            if (viewModel.currentMode == AppMode.Camera) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showFilterDialog = true },
                    icon = { Icon(Icons.Default.Star, null) },
                    text = { Text("Filters") }
                )
            }
        }
    ) { padding ->
        if (hasPermission) {
            Box(Modifier.padding(padding).fillMaxSize().onGloballyPositioned { containerSize = it.size }) {
                CameraProcessor(viewModel)
                
                // Aspect-Aware UI Overlay
                DetectionOverlay(viewModel, containerSize)

                if (viewModel.showDebugInfo) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("FPS: ${"%.1f".format(viewModel.currentFps)}", color = Color.Green, style = MaterialTheme.typography.labelLarge)
                            Text("Preview: ${viewModel.actualCameraSize}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            
                            if (viewModel.currentMode == AppMode.AI) {
                                val status = if (viewModel.backendResolutionScaling) "Scaled" else "Full"
                                Text("Inference ($status): ${viewModel.actualBackendSize}", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                                Text("Latency: ${viewModel.inferenceTime}ms", color = Color.Cyan, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (viewModel.isLoading) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    if (viewModel.showFilterDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showFilterDialog = false },
            title = { Text("Select Filter") },
            text = {
                Column {
                    viewModel.filters.forEach { filter ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.selectedFilter = filter
                                viewModel.showFilterDialog = false
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (filter == viewModel.selectedFilter), onClick = null)
                            Text(filter, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun DetectionOverlay(viewModel: BeautyViewModel, containerSize: IntSize) {
    val textMeasurer = rememberTextMeasurer()
    if (containerSize.width <= 0) return

    // 1. Logic: Map detection results to the Preview Display Area
    // The source of truth for coordinate system is 'actualBackendSize'
    val backendParts = viewModel.actualBackendSize.split("x")
    if (backendParts.size < 2) return
    val srcW = backendParts[0].toFloat()
    val srcH = backendParts[1].toFloat()

    val containerW = containerSize.width.toFloat()
    val containerH = containerSize.height.toFloat()
    
    val scale = min(containerW / srcW, containerH / srcH)
    val contentW = srcW * scale
    val contentH = srcH * scale
    
    val offsetX = (containerW - contentW) / 2f
    val offsetY = (containerH - contentH) / 2f

    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
        if (viewModel.currentMode == AppMode.AI) {
            viewModel.detectedYoloObjects.forEach { obj ->
                val left = offsetX + obj.box[0] * scale
                val top = offsetY + obj.box[1] * scale
                val width = obj.box[2] * scale
                val height = obj.box[3] * scale

                drawRect(
                    color = Color.Green,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = Stroke(width = 2.dp.toPx())
                )

                val labelText = "${obj.label} ${(obj.confidence * 100).toInt()}%"
                val textLayout = textMeasurer.measure(labelText, style = TextStyle(color = Color.White, fontSize = 12.sp))
                val labelSize = androidx.compose.ui.geometry.Size(textLayout.size.width.toFloat(), textLayout.size.height.toFloat())

                drawRect(
                    color = Color.Green.copy(alpha = 0.7f),
                    topLeft = Offset(left, top - labelSize.height),
                    size = labelSize
                )
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(left, top - labelSize.height),
                    style = TextStyle(color = Color.White, fontSize = 12.sp)
                )
            }
        } else if (viewModel.currentMode == AppMode.FACE) {
            // ML Kit usually uses Preview Size coordinates (actualCameraSize)
            val capParts = viewModel.actualCameraSize.split("x")
            if (capParts.size >= 2) {
                val cw = capParts[0].toFloat()
                val ch = capParts[1].toFloat()
                val fScale = min(containerW / cw, containerH / ch)
                val fOffX = (containerW - cw * fScale) / 2f
                val fOffY = (containerH - ch * fScale) / 2f

                viewModel.detectedFaces.forEach { face ->
                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(fOffX + face.bounds.left * fScale, fOffY + face.bounds.top * fScale),
                        size = androidx.compose.ui.geometry.Size(face.bounds.width() * fScale, face.bounds.height() * fScale),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun CameraProcessor(viewModel: BeautyViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val nativeLib = remember { NativeLib() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build()
        FaceDetection.getClient(options)
    }

    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }
    var lastFrameTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val rgbaMat = remember { Mat() }
    val previewMat = remember { Mat() } // High-res for human eye
    val aiMat = remember { Mat() }      // Low-res for AI logic
    var outputBitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(lifecycleOwner, viewModel.lensFacing, viewModel.cameraResolution) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val selector = CameraSelector.Builder().requireLensFacing(viewModel.lensFacing).build()
            if (!cameraProvider.hasCamera(selector)) return@Runnable
            cameraProvider.unbindAll()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(viewModel.getCameraSize())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                try {
                    val startTime = System.currentTimeMillis()
                    
                    // 1. Hardware to RGBA
                    nativeLib.yuvToRgba(
                        imageProxy.planes[0].buffer, imageProxy.planes[0].rowStride,
                        imageProxy.planes[1].buffer, imageProxy.planes[1].rowStride,
                        imageProxy.planes[2].buffer, imageProxy.planes[2].rowStride,
                        imageProxy.planes[1].pixelStride,
                        imageProxy.width, imageProxy.height, rgbaMat.nativeObjAddr
                    )

                    // 2. Rotate once for High-Res Preview
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    when (rotation) {
                        90 -> Core.rotate(rgbaMat, previewMat, Core.ROTATE_90_CLOCKWISE)
                        180 -> Core.rotate(rgbaMat, previewMat, Core.ROTATE_180)
                        270 -> Core.rotate(rgbaMat, previewMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                        else -> rgbaMat.copyTo(previewMat)
                    }
                    if (viewModel.lensFacing == CameraSelector.LENS_FACING_FRONT) Core.flip(previewMat, previewMat, 1)
                    
                    viewModel.actualCameraSize = "${previewMat.cols()}x${previewMat.rows()}"

                    // 3. Process Filters on HIGH-RES PREVIEW
                    if (viewModel.currentMode == AppMode.Camera && viewModel.selectedFilter != "Normal") {
                        when (viewModel.selectedFilter) {
                            "Beauty" -> nativeLib.applyBeautyFilter(previewMat.nativeObjAddr)
                            "Dehaze" -> nativeLib.applyDehaze(previewMat.nativeObjAddr)
                            "Underwater" -> nativeLib.applyUnderwater(previewMat.nativeObjAddr)
                            "Stage" -> nativeLib.applyStage(previewMat.nativeObjAddr)
                        }
                    }

                    // 4. Branch for AI: Scale previewMat to AI resolution
                    if (viewModel.currentMode == AppMode.AI) {
                        if (viewModel.backendResolutionScaling) {
                            val scale = viewModel.targetBackendWidth.toFloat() / previewMat.cols()
                            val tH = (previewMat.rows() * scale).toInt()
                            Imgproc.resize(previewMat, aiMat, org.opencv.core.Size(viewModel.targetBackendWidth.toDouble(), tH.toDouble()))
                        } else {
                            previewMat.copyTo(aiMat)
                        }
                        
                        viewModel.actualBackendSize = "${aiMat.cols()}x${aiMat.rows()}"
                        
                        val activeIds = viewModel.selectedYoloClasses.map { viewModel.allCOCOClasses.indexOf(it) }.filter { it >= 0 }.toIntArray()
                        val jsonResult = nativeLib.yoloInference(aiMat.nativeObjAddr, viewModel.yoloConfidence, viewModel.yoloIoU, activeIds)
                        
                        val results = mutableListOf<YoloResultData>()
                        val jsonArray = JSONArray(jsonResult)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val boxArr = obj.getJSONArray("box")
                            results.add(YoloResultData(
                                label = obj.getString("label"),
                                confidence = obj.getDouble("conf").toFloat(),
                                box = listOf(boxArr.getInt(0), boxArr.getInt(1), boxArr.getInt(2), boxArr.getInt(3))
                            ))
                        }
                        viewModel.detectedYoloObjects.clear()
                        viewModel.detectedYoloObjects.addAll(results)
                    } else if (viewModel.currentMode == AppMode.FACE) {
                        // ML Kit needs the original InputImage (High res preferred)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, rotation)
                            faceDetector.process(image).addOnSuccessListener { faces ->
                                viewModel.detectedFaces.clear()
                                faces.forEach { viewModel.detectedFaces.add(FaceResult(it.boundingBox, it.trackingId)) }
                            }
                        }
                        viewModel.actualBackendSize = viewModel.actualCameraSize
                    } else {
                        viewModel.actualBackendSize = viewModel.actualCameraSize
                    }

                    // 5. Output: ALWAYS use previewMat (High-Res) for the eye
                    if (outputBitmap == null || outputBitmap!!.width != previewMat.cols() || outputBitmap!!.height != previewMat.rows()) {
                        outputBitmap = Bitmap.createBitmap(previewMat.cols(), previewMat.rows(), Bitmap.Config.ARGB_8888)
                    }
                    Utils.matToBitmap(previewMat, outputBitmap)
                    
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - lastFrameTime
                    lastFrameTime = endTime
                    viewModel.inferenceTime = endTime - startTime
                    if (duration > 0) viewModel.currentFps = 0.9f * viewModel.currentFps + 0.1f * (1000f / duration)

                    bitmapState = outputBitmap
                } catch (e: Exception) { e.printStackTrace() } finally { imageProxy.close() }
            }

            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, imageAnalysis)
            } catch (e: Exception) { e.printStackTrace() }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose { cameraProviderFuture.get().unbindAll() }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            rgbaMat.release()
            previewMat.release()
            aiMat.release()
            faceDetector.close()
        }
    }

    if (bitmapState != null) {
        Image(
            bitmap = bitmapState!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}