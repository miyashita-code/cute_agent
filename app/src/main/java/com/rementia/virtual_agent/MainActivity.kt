package com.rementia.virtual_agent

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.rementia.virtual_agent.ui.components.MainScreen
import com.rementia.virtual_agent.ui.theme.VirtualAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // システムデコレーション（ステータスバー等）のレイアウト調整を無効にする
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // ステータスバーを非表示にする
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        setContent {
            VirtualAgentTheme {
                MainScreen()
            }
        }
    }
}
