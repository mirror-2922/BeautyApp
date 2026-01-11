package com.mirror2922.ecvl.ui.screens.face

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.mirror2922.ecvl.ui.components.AppHud
import com.mirror2922.ecvl.ui.components.camera.MlKitCameraView
import com.mirror2922.ecvl.viewmodel.AppMode
import com.mirror2922.ecvl.viewmodel.BeautyViewModel

@Composable
fun FaceCameraScreen(viewModel: BeautyViewModel) {
    LaunchedEffect(Unit) {
        viewModel.currentMode = AppMode.FACE
        viewModel.showFilterPanel = false
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { containerSize = it.size }) {
        MlKitCameraView(viewModel)
        FaceOverlay(viewModel, containerSize)
        AppHud(viewModel, Modifier.align(Alignment.TopStart))
    }
}
