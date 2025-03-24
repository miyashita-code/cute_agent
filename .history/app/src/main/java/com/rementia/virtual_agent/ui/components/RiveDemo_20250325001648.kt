package com.rementia.virtual_agent.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

/**
 * Riveアニメーションを表示するためのComposable関数
 *
 * 指定されたRiveリソースIDとステートマシン名を使用して、
 * インタラクティブなRiveアニメーションを表示します。
 * 表情選択、視線追従、瞬きなどの機能を提供します。
 *
 * @param riveResId 表示するRiveアニメーションのリソースID (例: R.raw.character_facial_animation)
 * @param stateMachineName 制御対象のステートマシン名 (例: "State Machine 1")
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun RiveDemoScreen(
    riveResId: Int,
    stateMachineName: String
) {
    // 状態の定義
    val isArtboardInitialized = remember { mutableStateOf(false) }
    val isBlinkTwice = rememberSaveable { mutableStateOf(false) }
    val targetPositionFlow = remember { MutableStateFlow(Pair(0f, 0f)) }
    val isAnimating = remember { mutableStateOf(true) }
    val memoryInfo = remember { mutableStateOf("メモリ監視開始") }
    val currentExpression = rememberSaveable { mutableStateOf(FacialExpression.NORMAL) }
    val riveViewRef = remember { mutableStateOf<RiveAnimationView?>(null) }

    // 瞬き処理の実装
    LaunchedEffect(Unit) {
        startBlinkingEffect(isBlinkTwice)
    }

    // メモリ監視の実装
    LaunchedEffect(Unit) {
        startMemoryMonitoring(memoryInfo)
    }

    // 視線追従の実装
    LaunchedEffect(Unit) {
        startGazeFollowingEffect(targetPositionFlow, isAnimating, riveViewRef, stateMachineName)
    }

    // 画面レイアウト
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
            
            // Riveアニメーション表示
            RiveAnimationView(
                modifier = Modifier.size(minSize),
                riveResId = riveResId,
                stateMachineName = stateMachineName,
                isArtboardInitialized = isArtboardInitialized,
                isBlinkTwice = isBlinkTwice,
                riveViewRef = riveViewRef
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 現在の表情を表示
            Text(
                text = "Selected Expression: ${currentExpression.value.name} " +
                        "(ID = ${currentExpression.value.intValue})"
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // アニメーション制御ボタン
            Button(
                onClick = { isAnimating.value = !isAnimating.value },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(if (isAnimating.value) "アニメーション停止" else "アニメーション再開")
            }

            // 表情選択ボタン
            ExpressionSelectorButtons(
                currentExpression = currentExpression,
                riveViewRef = riveViewRef,
                stateMachineName = stateMachineName
            )
        }
    }
}

/**
 * 瞬き効果を開始する関数
 *
 * @param isBlinkTwice 二重瞬きフラグを格納するMutableState
 */
private suspend fun startBlinkingEffect(
    isBlinkTwice: MutableState<Boolean>
) {
    while (true) {
        val randomMillis = (500..5000).random()
        delay(randomMillis.toLong())
        isBlinkTwice.value = Random.nextFloat() < 0.25f
    }
}

/**
 * メモリ監視を開始する関数
 *
 * @param memoryInfo メモリ情報を格納するMutableState
 */
private suspend fun startMemoryMonitoring(
    memoryInfo: MutableState<String>
) {
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

/**
 * 視線追従効果を開始する関数
 *
 * @param targetPositionFlow 目標位置を格納するStateFlow
 * @param isAnimating アニメーション実行フラグ
 * @param riveViewRef RiveAnimationViewの参照
 * @param stateMachineName 制御対象のステートマシン名
 */
private suspend fun startGazeFollowingEffect(
    targetPositionFlow: MutableStateFlow<Pair<Float, Float>>,
    isAnimating: MutableState<Boolean>,
    riveViewRef: MutableState<RiveAnimationView?>,
    stateMachineName: String
) {
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
                    delay(50L) // 50ms待機
                    isAnimating.value = true // 再開
                }
            }
        }
    }
}

/**
 * RiveAnimationViewを表示するComposable関数
 *
 * @param modifier 適用するModifier
 * @param riveResId 表示するRiveアニメーションのリソースID
 * @param stateMachineName 制御対象のステートマシン名
 * @param isArtboardInitialized Artboard初期化フラグ
 * @param isBlinkTwice 二重瞬きフラグ
 * @param riveViewRef RiveAnimationViewの参照を格納するMutableState
 */
@Composable
private fun RiveAnimationView(
    modifier: Modifier,
    riveResId: Int,
    stateMachineName: String,
    isArtboardInitialized: MutableState<Boolean>,
    isBlinkTwice: MutableState<Boolean>,
    riveViewRef: MutableState<RiveAnimationView?>
) {
    AndroidView(
        modifier = modifier,
        factory = { context: Context ->
            createRiveAnimationView(
                context, 
                riveResId, 
                stateMachineName, 
                isArtboardInitialized, 
                riveViewRef
            )
        },
        update = { riveView ->
            updateRiveAnimation(riveView, stateMachineName, isBlinkTwice)
        }
    )
}

/**
 * RiveAnimationViewを作成する関数
 *
 * @param context Androidコンテキスト
 * @param riveResId 表示するRiveアニメーションのリソースID
 * @param stateMachineName 制御対象のステートマシン名
 * @param isArtboardInitialized Artboard初期化フラグ
 * @param riveViewRef RiveAnimationViewの参照を格納するMutableState
 * @return 新しく作成されたRiveAnimationView
 */
private fun createRiveAnimationView(
    context: Context,
    riveResId: Int,
    stateMachineName: String,
    isArtboardInitialized: MutableState<Boolean>,
    riveViewRef: MutableState<RiveAnimationView?>
): RiveAnimationView {
    return RiveAnimationView(context).apply {
        // Riveファイルを読み込む
        setRiveResource(riveResId)

        // リスナー登録
        registerListener(object : RiveFileController.Listener {
            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                Handler(Looper.getMainLooper()).post {
                    Log.d("RiveStateDebug", "State changed: $stateMachineName -> $stateName")
                }
            }

            override fun notifyPlay(animation: PlayableInstance) {
                // 初期化は一度だけ実行する
                if (!isArtboardInitialized.value) {
                    isArtboardInitialized.value = true
                    Log.d("RiveCache", "初回アニメーション開始 - 一度だけ初期化を実行")
                }
            }

            override fun notifyPause(animation: PlayableInstance) {
                // アニメーション一時停止時の処理（現在は不要）
            }

            override fun notifyLoop(animation: PlayableInstance) {
                // アニメーションループ時の処理（現在は不要）
            }

            override fun notifyStop(animation: PlayableInstance) {
                // アニメーション停止時の処理（現在は不要）
            }
        })

        // 参照を保持
        riveViewRef.value = this
    }
}

/**
 * RiveAnimationViewを更新する関数
 *
 * @param riveView 更新対象のRiveAnimationView
 * @param stateMachineName 制御対象のステートマシン名
 * @param isBlinkTwice 二重瞬きフラグ
 */
private fun updateRiveAnimation(
    riveView: RiveAnimationView,
    stateMachineName: String,
    isBlinkTwice: MutableState<Boolean>
) {
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

/**
 * 表情選択ボタンを表示するComposable関数
 *
 * @param currentExpression 現在の表情を格納するMutableState
 * @param riveViewRef RiveAnimationViewの参照
 * @param stateMachineName 制御対象のステートマシン名
 */
@Composable
private fun ExpressionSelectorButtons(
    currentExpression: MutableState<FacialExpression>,
    riveViewRef: MutableState<RiveAnimationView?>,
    stateMachineName: String
) {
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
