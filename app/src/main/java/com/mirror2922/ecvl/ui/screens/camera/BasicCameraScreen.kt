package com.mirror2922.ecvl.ui.screens.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mirror2922.ecvl.ui.components.AppHud
import com.mirror2922.ecvl.ui.components.FilterPanel
import com.mirror2922.ecvl.ui.components.camera.CameraView
import com.mirror2922.ecvl.viewmodel.AppMode
import com.mirror2922.ecvl.viewmodel.BeautyViewModel

@Composable
fun BasicCameraScreen(viewModel: BeautyViewModel) {
    LaunchedEffect(Unit) {
        viewModel.currentMode = AppMode.Camera
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraView(viewModel)
        
        AppHud(viewModel, Modifier.align(Alignment.TopStart))

        // Filter FAB
        FloatingActionButton(
            onClick = { viewModel.showFilterPanel = !viewModel.showFilterPanel },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = if (viewModel.showFilterPanel) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                imageVector = if (viewModel.showFilterPanel) Icons.Default.Close else Icons.Default.AutoFixHigh,
                contentDescription = "Effects",
                modifier = Modifier.size(28.dp)
            )
        }

        FilterPanel(
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
