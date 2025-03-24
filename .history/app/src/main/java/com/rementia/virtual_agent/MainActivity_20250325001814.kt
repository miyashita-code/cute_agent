package com.rementia.virtual_agent

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.rementia.virtual_agent.ui.components.RiveDemoScreen
import com.rementia.virtual_agent.ui.theme.VirtualAgentTheme

/**
 * アプリケーションのメインアクティビティ
 * 
 * このアクティビティは、アプリケーションの起動時に最初に表示される画面を管理します。
 * Rive アニメーションを表示するためのフルスクリーンインターフェースを設定します。
 */
class MainActivity : ComponentActivity() {
    
    /**
     * アクティビティの作成時に呼び出されるメソッド
     * 
     * システムUIの設定やComposeのコンテンツを初期化します。
     * 
     * @param savedInstanceState 保存された状態情報
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // システムのデコレーション（ステータスバーなど）のレイアウト調整を無効にする
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // ステータスバーを非表示にしてフルスクリーン表示を実現
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        // Jetpack Composeを使用してUIを構築
        setContent {
            // アプリケーションのテーマを適用
            VirtualAgentTheme {
                // Riveデモ画面を表示
                // res/raw 内の指定されたRiveファイルとステートマシンを使用
                RiveDemoScreen(
                    riveResId = R.raw.character_facial_animation,
                    stateMachineName = "State Machine 1"
                )
            }
        }
    }
}
