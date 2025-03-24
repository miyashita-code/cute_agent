package com.rementia.virtual_agent.ui.components

import android.util.Log
import android.view.MotionEvent
import android.graphics.PointF

import kotlin.math.max
import kotlin.math.min

// Riveランタイム関連のimport
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.StateMachineInstance

// ここが重要：stateMachine(...) 拡張関数のインポート
import app.rive.runtime.kotlin.renderers.PointerEvents

/**
 * 0～6 の顔の表情をまとめた Enum です。
 */
enum class FacialExpression(val intValue: Int) {
    superAngry(0),
    littleAngry(1),
    sliteLittleAngry(2),
    normal(3),
    sliteLittleHappy(4),
    littleHappy(5),
    happy(6)
}

/**
 * facialExpressionSelector を指定の [expression] にセットし、
 * ついでに facialExpressionTrigger を発火するヘルパー関数です。
 */
fun setFacialExpression(
    riveView: RiveAnimationView,
    machineName: String,
    expression: FacialExpression
) {
    Log.d("RiveDebug", "Setting expression to: ${expression.name}")
    safeSetNumberState(riveView, machineName, "facialExpressionSelector", expression.intValue.toFloat())

    Log.d("RiveDebug", "Firing facialExpressionTrigger")
    riveView.fireState(machineName, "facialExpressionTrigger")
}

/**
 * 指定した Boolean 入力を設定する。
 */
fun safeSetBooleanState(
    riveView: RiveAnimationView,
    machineName: String,
    inputName: String,
    value: Boolean
) {
    try {
        Log.d("RiveDebug", "Setting $inputName to: $value")
        riveView.setBooleanState(machineName, inputName, value)
    } catch (e: Exception) {
        val available = getAvailableInputs(riveView, machineName)
        throw IllegalArgumentException(
            "Boolean Input '$inputName' not found in '$machineName'. " +
                    "Available: ${available.joinToString(", ")}",
            e
        )
    }
}

/**
 * 指定した Number 入力を設定する。
 */
fun safeSetNumberState(
    riveView: RiveAnimationView,
    machineName: String,
    inputName: String,
    value: Float
) {
    try {
        Log.d("RiveDebug", "Setting $inputName to $value")
        riveView.setNumberState(machineName, inputName, value)
    } catch (e: Exception) {
        val available = getAvailableInputs(riveView, machineName)
        throw IllegalArgumentException(
            "Number Input '$inputName' not found in '$machineName'. " +
                    "Available: ${available.joinToString(", ")}",
            e
        )
    }
}

/**
 * 指定された State Machine 内に存在する入力の名前一覧を返す。
 */
fun getAvailableInputs(riveView: RiveAnimationView, machineName: String): List<String> {
    val file = riveView.file
    if (file == null) {
        Log.e("RiveDebug", "RiveFile not loaded.")
        return emptyList()
    }
    val artboard = file.firstArtboard
    if (artboard == null) {
        Log.e("RiveDebug", "Artboard not found.")
        return emptyList()
    }

    // Artboard の拡張関数 stateMachine(name: String) を使ってステートマシンを取得
    val smDefinition = artboard.stateMachine(machineName)
    // ここからステートマシンの inputs (List<StateMachineInput>) が取れる
    val inputs = smDefinition?.inputs ?: emptyList()

    return inputs.map { it.name }
}

/**
 * RiveAnimationView に「画面上の (xScreen, yScreen) を向かせる」拡張関数の例。
 *
 * [stateMachineName] が PointerMove を受け取る仕組みを Rive ファイルで
 * 設定済み（ヒットボックスやリスナーなど）であることを想定。
 */
fun RiveAnimationView.lookAt(
    xScreen: Float,
    yScreen: Float,
    stateMachineName: String
) {
    // Viewの位置をスクリーン座標から取得
    val viewCoords = IntArray(2)
    this.getLocationOnScreen(viewCoords)
    val xView = xScreen - viewCoords[0]
    val yView = yScreen - viewCoords[1]

    // コントローラー取得（エラーチェック追加）
    val ctl = this.controller ?: run {
        Log.e("lookAt", "RiveFileController が存在しません。")
        return
    }
    
    // 座標変換処理は入力イベント送信のみが重要なため簡略化
    // ログも減らして処理を軽量化
    
    // 正しい順序でイベントを注入（デバッグログを削除）
    ctl.pointerEvent(PointerEvents.POINTER_DOWN, xView, yView)
    ctl.pointerEvent(PointerEvents.POINTER_MOVE, xView, yView)
    ctl.pointerEvent(PointerEvents.POINTER_UP, xView, yView)
}


/**
 * 例: RiveAnimationView にタッチリスナーをつけてポインタ操作を一括で送る方法
 * （pointerDown / pointerMove / pointerUp など。必要に応じてどうぞ）
 */
fun RiveAnimationView.enablePointerEvents() {
    val ctl = this.controller ?: return
    this.setOnTouchListener { _, event ->
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> ctl.pointerEvent(PointerEvents.POINTER_DOWN, x, y)
            MotionEvent.ACTION_MOVE -> ctl.pointerEvent(PointerEvents.POINTER_MOVE, x, y)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> ctl.pointerEvent(PointerEvents.POINTER_UP, x, y)
        }
        true
    }
}
