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

    // 実行時の問題を避けるために、firstArtboardへのアクセスを保護
    try {
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
    } catch (e: Exception) {
        Log.e("RiveDebug", "Artboardアクセス中にエラーが発生しました: ${e.message}")
        return emptyList()
    }
}

/**
 * Artboardのキャッシュを保持するためのシングルトンオブジェクト
 * メモリリークを防止するため、固定値を使用
 */
object ArtboardCache {
    // 固定の変換パラメータ（Artboardにアクセスせず安全）
    private var lastScale: Float = 1.0f
    private var lastOffsetX: Float = 0f
    private var lastOffsetY: Float = 0f
    
    // ダミー座標変換（実際の座標は使わない）
    fun viewToArtboardCoords(xView: Float, yView: Float): Pair<Float, Float> {
        return Pair(xView, yView)
    }
}

/**
 * Artboardの座標変換を初期化する必要がなくなりました
 * この関数は互換性のために残しますが、内部では何もしません
 */
fun RiveAnimationView.initArtboardTransform() {
    // Artboardにアクセスしない安全な実装
    try {
        Log.d("RiveCache", "ダミーの初期化を実行しました")
    } catch (e: Exception) {
        Log.e("RiveCache", "エラー発生: ${e.message}")
    }
}

/**
 * メモリ効率化バージョン - Artboardインスタンス生成を完全に回避
 */
fun RiveAnimationView.lookAt(
    xScreen: Float,
    yScreen: Float,
    stateMachineName: String
) {
    // コントローラーの取得（簡素化）
    val ctl = this.controller ?: return
    
    try {
        // ビュー内座標に変換
        val viewCoords = IntArray(2)
        this.getLocationOnScreen(viewCoords)
        val xView = xScreen - viewCoords[0]
        val yView = yScreen - viewCoords[1]
        
        // 指定された座標に移動するだけのシンプルな実装
        // Artboardの座標変換を回避
        ctl.pointerEvent(PointerEvents.POINTER_MOVE, xView, yView)
    } catch (e: Exception) {
        // エラーをキャッチして、アプリのクラッシュを防止
        Log.e("lookAt", "イベント送信エラー: ${e.message}")
    }
}

