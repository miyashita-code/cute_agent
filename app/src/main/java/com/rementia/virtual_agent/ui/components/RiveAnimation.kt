package com.rementia.virtual_agent.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView

@Composable
fun RiveAnimation(
    modifier: Modifier = Modifier,
    riveResId: Int,
    stateMachineName: String = "State Machine 1"
) {
    AndroidView(
        modifier = modifier,
        factory = { context: Context ->
            RiveAnimationView(context).apply {
                // Rive ファイルを読み込む
                setRiveResource(riveResId)
            }
        },
        update = { riveView ->
            // ここで内部の hitbox（＝リスナー）機能が有効なら
            // 手動のタッチ操作は不要。
            // 例として "isActive" 入力を true に設定する（必要な場合のみ）
            riveView.setBooleanState(stateMachineName, "isActive", true)
        }
    )
}
