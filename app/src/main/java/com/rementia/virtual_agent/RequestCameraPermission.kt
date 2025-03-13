package com.rementia.virtual_agent.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun RequestCameraPermission() {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 必要に応じて、許可されなかった場合の処理を追加
    }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }
}
