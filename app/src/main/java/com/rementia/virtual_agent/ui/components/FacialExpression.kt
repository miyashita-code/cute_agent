package com.rementia.virtual_agent.ui.components

import android.util.Log
import app.rive.runtime.kotlin.RiveAnimationView

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
 * facialExpressionSelector を指定の [expression] にセットして、
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
    val stateMachine = artboard.stateMachine(machineName)
    return stateMachine?.inputs?.map { it.name } ?: emptyList()
}
