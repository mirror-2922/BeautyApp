package com.mirror2922.ecvl.ui.screens.face

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mirror2922.ecvl.viewmodel.BeautyViewModel
import kotlin.math.min

@Composable
fun FaceOverlay(viewModel: BeautyViewModel, containerSize: IntSize) {
    if (containerSize.width <= 0 || containerSize.height <= 0) return

    val sourceSizeStr = viewModel.actualBackendSize
    val parts = sourceSizeStr.split("x")
    if (parts.size < 2) return
    val srcW = parts[0].toFloat()
    val srcH = parts[1].toFloat()
    if (srcW == 0f || srcH == 0f) return

    val containerW = containerSize.width.toFloat()
    val containerH = containerSize.height.toFloat()
    
    val scale = min(containerW / srcW, containerH / srcH)
    val offsetX = (containerW - srcW * scale) / 2f
    val offsetY = (containerH - srcH * scale) / 2f

    val isFront = viewModel.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT

    Canvas(modifier = Modifier.fillMaxSize()) {
        viewModel.detectedFaces.forEach { face ->
            val left = if (isFront) {
                offsetX + (srcW - face.bounds.right) * scale
            } else {
                offsetX + face.bounds.left * scale
            }
            val top = offsetY + face.bounds.top * scale
            
            drawRect(
                color = Color.Yellow, 
                topLeft = Offset(left, top), 
                size = androidx.compose.ui.geometry.Size(face.bounds.width() * scale, face.bounds.height() * scale), 
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
