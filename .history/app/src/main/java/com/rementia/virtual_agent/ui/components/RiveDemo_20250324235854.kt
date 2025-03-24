package com.rementia.virtual_agent.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random
import java.lang.System.gc
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun RiveDemoScreen(
    riveResId: Int,          // 例: R.raw.character_facial_animation
    stateMachineName: String // 例: "State Machine 1"
) {
    // ランダム瞬き制御（状態を保存）
    val isBlinkTwice = rememberSaveable { mutableStateOf(false) }
    
    // 目標座標を示すStateFlow（更新頻度を下げるため）
    val targetPositionFlow = remember { MutableStateFlow(Pair(0f, 0f)) }
    
    // アニメーション実行中フラグ
    val isAnimating = remember { mutableStateOf(true) }
    
    // メモリ使用量監視
    val memoryInfo = remember { mutableStateOf("メモリ監視開始") }
    
    // 瞬き処理
    LaunchedEffect(Unit) {
        while (true) {
            val randomMillis = (500..5000).random()
            delay(randomMillis.toLong())
            isBlinkTwice.value = Random.nextFloat() < 0.25f
        }
    }

    // 定期的なメモリクリーンアップ処理
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000L) // 30秒ごとにGCを促進
            memoryInfo.value = "メモリクリーンアップ実行中..."
            gc() // GCを促進
            
            // 利用可能なヒープメモリを取得
            val runtime = Runtime.getRuntime()
            val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemoryMB = runtime.maxMemory() / 1024 / 1024
            val availableMemoryMB = maxMemoryMB - usedMemoryMB
            
            memoryInfo.value = "メモリ使用: ${usedMemoryMB}MB / 最大: ${maxMemoryMB}MB (空き: ${availableMemoryMB}MB)"
            Log.d("RiveMemory", memoryInfo.value)
        }
    }

    // FacialExpression を使った現在の表情の状態
    val currentExpression = rememberSaveable { mutableStateOf(FacialExpression.normal) }

    // RiveAnimationView の参照を保持するための状態
    val riveViewRef = remember { mutableStateOf<RiveAnimationView?>(null) }

    // 目標座標を更新する処理（頻度を下げた）
    LaunchedEffect(Unit) {
        var xPos = 0f
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        var direction = 1f
        
        // 位置データの更新ジョブ（高頻度）
        launch {
            while (true) {
                delay(300L) // 300msに低下（以前は100ms→250ms）
                if (!isAnimating.value) continue
                
                xPos += direction * 50
                if (xPos > screenWidth || xPos < 0f) direction *= -1f
                
                // StateFlowを更新
                targetPositionFlow.value = Pair(
                    xPos, 
                    Resources.getSystem().displayMetrics.heightPixels / 2f
                )
            }
        }
        
        // lookAt呼び出しジョブ（低頻度）
        launch {
            targetPositionFlow.collectLatest { (x, y) ->
                if (!isAnimating.value) return@collectLatest
                
                riveViewRef.value?.let { riveView ->
                    try {
                        // lookAtを呼び出し
                        riveView.lookAt(x, y, stateMachineName)
                    } catch (e: Exception) {
                        Log.e("RiveDemo", "lookAt実行中にエラー: ${e.message}")
                        // エラー発生時はアニメーションを一時停止
                        isAnimating.value = false
                        delay(5000L) // 5秒待機
                        isAnimating.value = true // 再開
                    }
                }
            }
        }
    }

    // 画面サイズに合わせたレイアウト
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minSize = minOf(maxWidth, maxHeight)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // メモリ情報表示
            Text(
                text = memoryInfo.value,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // アニメーション表示部分
            AndroidView(
                modifier = Modifier.size(minSize),
                factory = { context: Context ->
                    RiveAnimationView(context).apply {
                        // Rive ファイルを読み込む
                        setRiveResource(riveResId)

                        // Artboard変換パラメータを初期化（重要）
                        // ファイル読み込み完了後に実行するためリスナーを使用
                        registerListener(object : RiveFileController.Listener {
                            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                                Handler(Looper.getMainLooper()).post {
                                    Log.d("RiveStateDebug", "State changed: $stateMachineName -> $stateName")
                                }
                            }

                            override fun notifyPlay(animation: PlayableInstance) {
                                // Artboardキャッシュを初期化
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        initArtboardTransform()
                                        Log.d("RiveCache", "アニメーション開始時にArtboardキャッシュを初期化")
                                    } catch (e: Exception) {
                                        Log.e("RiveCache", "キャッシュ初期化エラー: ${e.message}")
                                    }
                                }, 500) // 少し遅延させて初期化
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
                    try {
                        safeSetBooleanState(
                            riveView,
                            stateMachineName,
                            "isBlinkTwice",
                            isBlinkTwice.value
                        )
                    } catch (e: Exception) {
                        Log.e("RiveDemo", "状態更新エラー: ${e.message}")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 現在の選択内容を表示
            Text(
                text = "Selected Expression: ${currentExpression.value.name} " +
                        "(ID = ${currentExpression.value.intValue})"
            )
            Spacer(modifier = Modifier.height(8.dp))

            // アニメーション一時停止/再開ボタン
            Button(
                onClick = { isAnimating.value = !isAnimating.value },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(if (isAnimating.value) "アニメーション停止" else "アニメーション再開")
            }

            // 各表情ボタンを生成（スクロール可能なRowに変更）
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FacialExpression.values().forEach { expr ->
                    Button(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            currentExpression.value = expr
                            riveViewRef.value?.let { riveView ->
                                try {
                                    setFacialExpression(riveView, stateMachineName, expr)
                                } catch (e: Exception) {
                                    Log.e("RiveDemo", "表情変更エラー: ${e.message}")
                                }
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
