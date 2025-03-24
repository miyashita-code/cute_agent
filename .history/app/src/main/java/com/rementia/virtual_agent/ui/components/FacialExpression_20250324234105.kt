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

    // アートボード取得（エラーチェック追加）
    val controller = this.controller ?: run {
        Log.e("lookAt", "RiveFileController が存在しません。")
        return
    }
    val file = controller.file ?: run {
        Log.e("lookAt", "RiveFile が読み込まれていません。")
        return
    }
    val artboard = file.firstArtboard ?: run {
        Log.e("lookAt", "Artboard が見つかりません。")
        return
    }

    val bounds = artboard.bounds
    val artboardWidth = bounds.width()
    val artboardHeight = bounds.height()

    // アートボードのスケールを計算 (Fit=CONTAIN)
    val scale = min(
        this.width.toFloat() / artboardWidth,
        this.height.toFloat() / artboardHeight
    )

    val offsetX = (this.width - artboardWidth * scale) / 2f
    val offsetY = (this.height - artboardHeight * scale) / 2f

    // ビュー座標をアートボード座標に変換（ここ重要）
    val artboardX = (xView - offsetX) / scale + bounds.left
    val artboardY = (yView - offsetY) / scale + bounds.top

    // ログで座標を確認
    Log.d("lookAt", "Artboard XY: ($artboardX, $artboardY)")

    // 正しい順序でイベントを注入
    val ctl = this.controller ?: return
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
