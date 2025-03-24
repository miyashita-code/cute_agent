package com.rementia.virtual_agent

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.rementia.virtual_agent.ui.components.RiveDemoScreen
import com.rementia.virtual_agent.ui.theme.VirtualAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // システムのデコレーション（ステータスバーなど）のレイアウト調整を無効にする
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // ステータスバーを非表示にする
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContent {
            VirtualAgentTheme {
                // res/raw 内の Rive ファイルと対象の State Machine 名を指定
                RiveDemoScreen(
                    riveResId = R.raw.character_facial_animation,
                    stateMachineName = "State Machine 1"
                )
            }
        }
    }
}
