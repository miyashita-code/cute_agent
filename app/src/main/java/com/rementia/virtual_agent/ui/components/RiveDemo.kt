package com.rementia.virtual_agent.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView

import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.PlayableInstance
import app.rive.runtime.kotlin.core.RiveEvent
import com.rementia.virtual_agent.ui.components.FacialExpression
import com.rementia.virtual_agent.ui.components.getAvailableInputs
import com.rementia.virtual_agent.ui.components.safeSetBooleanState
import com.rementia.virtual_agent.ui.components.safeSetNumberState
import com.rementia.virtual_agent.ui.components.setFacialExpression
import kotlinx.coroutines.delay
import kotlin.random.Random

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun RiveDemoScreen(
    riveResId: Int,          // 例: R.raw.character_facial_animation
    stateMachineName: String // 例: "State Machine 1"
) {
    // ランダム瞬き制御（状態を保存）
    val isBlinkTwice = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val randomMillis = (500..5000).random()
            delay(randomMillis.toLong())
            isBlinkTwice.value = Random.nextFloat() < 0.25f
        }
    }

    // FacialExpression を使った現在の表情の状態
    val currentExpression = rememberSaveable { mutableStateOf(FacialExpression.normal) }

    // ログ出力用のタイムスタンプ
    var lastLogTime by rememberSaveable { mutableStateOf(0L) }

    // RiveAnimationView の参照を保持するための状態
    val riveViewRef = remember { mutableStateOf<RiveAnimationView?>(null) }

    // 画面サイズに合わせたレイアウト
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minSize = minOf(maxWidth, maxHeight)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // アニメーション表示部分
            AndroidView(
                modifier = Modifier.size(minSize),
                factory = { context: Context ->
                    RiveAnimationView(context).apply {
                        // Rive ファイルを読み込む
                        setRiveResource(riveResId)

                        // 状態変更イベントのリスナーを登録
                        registerListener(object : RiveFileController.Listener {
                            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                                Handler(Looper.getMainLooper()).post {
                                    Log.d("RiveStateDebug", "State changed: $stateMachineName -> $stateName")
                                }
                            }

                            override fun notifyPlay(animation: PlayableInstance) {
                                // pass
                            }

                            override fun notifyPause(animation: PlayableInstance) {
                                // pass
                            }

                            override fun notifyLoop(animation: PlayableInstance) {
                                // pass
                            }

                            override fun notifyStop(animation: PlayableInstance) {
                                // pass
                            }
                        })

                        // 参照を保持
                        riveViewRef.value = this
                    }
                },
                update = { riveView ->

                    // 入力の更新処理：blink の状態を毎回更新
                    safeSetBooleanState(
                        riveView,
                        stateMachineName,
                        "isBlinkTwice",
                        isBlinkTwice.value
                    )


                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 現在の選択内容を表示
            Text(
                text = "Selected Expression: ${currentExpression.value.name} " +
                        "(ID = ${currentExpression.value.intValue})"
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 各表情ボタンを生成
            Row {
                FacialExpression.values().forEach { expr ->
                    Button(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            currentExpression.value = expr
                            riveViewRef.value?.let { riveView ->

                                setFacialExpression(riveView, stateMachineName, expr)
                            }
                        }
                    ) {
                        Text(expr.name)
                    }
                }
            }
        }
    }
}