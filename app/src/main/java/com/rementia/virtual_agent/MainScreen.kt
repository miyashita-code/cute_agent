package com.rementia.virtual_agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rementia.virtual_agent.R

@Composable
fun MainScreen() {

    RequestCameraPermission()

    var isFaceDetected by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 上半分：顔検出カメラと検出結果表示
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FaceDetectionCamera(
                modifier = Modifier.fillMaxSize(),
                onFaceDetected = { detected ->
                    isFaceDetected = detected
                }
            )
            Text(
                text = if (isFaceDetected) "Face Detected" else "No Face Detected",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .zIndex(1f),  // これで重なり順を上げる
                color = androidx.compose.ui.graphics.Color.White  // 背景に映える色に設定
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // 下半分：Rive アニメーション（RiveDemoScreen）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            RiveDemoScreen(
                riveResId = R.raw.character_facial_animation,
                stateMachineName = "State Machine 1"
            )
        }
    }
}
