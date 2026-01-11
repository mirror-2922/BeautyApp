package com.mirror2922.ecvl.ui.components.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.mirror2922.ecvl.viewmodel.BeautyViewModel
import com.mirror2922.ecvl.viewmodel.FaceResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@Composable
fun MlKitCameraView(viewModel: BeautyViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        FaceDetection.getClient(options)
    }

    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner, viewModel.lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val selector = CameraSelector.Builder().requireLensFacing(viewModel.lensFacing).build()
            
            if (!cameraProvider.hasCamera(selector)) return@Runnable
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                    
                    // Update actual backend size for overlay scaling
                    // ML Kit uses the unrotated image dimensions but coordinates are relative to that.
                    // However, we need to handle rotation for the overlay.
                    // For PreviewView, the display is handled by CameraX.
                    // The Overlay needs to know the "source" coordinate system.
                    val width = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                    val height = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
                    
                    viewModel.actualBackendSize = "${width}x${height}"

                    faceDetector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            viewModel.detectedFaces.clear()
                            faces.forEach { viewModel.detectedFaces.add(FaceResult(it.boundingBox, it.trackingId)) }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            faceDetector.close()
        }
    }

    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}
