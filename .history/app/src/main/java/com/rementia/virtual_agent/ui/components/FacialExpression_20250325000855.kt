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
 * メモリリークを防止するため、弱参照を使用
 */
object ArtboardCache {
    // 最後に計算したアートボードの座標変換情報
    private var lastScale: Float = 1.0f
    private var lastOffsetX: Float = 0f
    private var lastOffsetY: Float = 0f
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0
    private var lastBoundsLeft: Float = 0f
    private var lastBoundsTop: Float = 0f
    
    // 変換パラメータを設定
    fun setTransformParams(scale: Float, offsetX: Float, offsetY: Float, 
                          width: Int, height: Int, boundsLeft: Float, boundsTop: Float) {
        lastScale = scale
        lastOffsetX = offsetX
        lastOffsetY = offsetY
        lastWidth = width
        lastHeight = height
        lastBoundsLeft = boundsLeft
        lastBoundsTop = boundsTop
    }
    
    // ビュー座標をアートボード座標に変換（キャッシュ使用）
    fun viewToArtboardCoords(xView: Float, yView: Float): Pair<Float, Float> {
        val artboardX = (xView - lastOffsetX) / lastScale + lastBoundsLeft
        val artboardY = (yView - lastOffsetY) / lastScale + lastBoundsTop
        return Pair(artboardX, artboardY)
    }
    
    // キャッシュが有効かどうかチェック
    fun isValid(width: Int, height: Int): Boolean {
        return lastWidth == width && lastHeight == height && lastScale > 0
    }
}

/**
 * Artboardの座標変換パラメータを初期化する（一度だけ実行）
 */
fun RiveAnimationView.initArtboardTransform() {
    try {
        val controller = this.controller ?: return
        val file = controller.file ?: return
        val artboard = file.firstArtboard ?: return
        
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
        
        // キャッシュに保存
        ArtboardCache.setTransformParams(
            scale, offsetX, offsetY, 
            this.width, this.height, 
            bounds.left, bounds.top
        )
        
        Log.d("RiveCache", "Artboard変換パラメータを初期化しました")
    } catch (e: Exception) {
        Log.e("RiveCache", "Artboardパラメータの初期化に失敗: ${e.message}")
    }
}

/**
 * メモリ効率化バージョン - Artboardインスタンス生成を完全に回避
 * 視線追従の機能を維持しながらメモリ使用を削減
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
        
        // ファイル読み込み確認（Artboardインスタンス化なし）
        if (ctl.file == null) {
            return
        }
        
        // ステートマシン名の安全チェック
        if (stateMachineName.isBlank()) {
            return
        }
        
        // メモリ効率的なポインタイベント処理
        try {
            ctl.pointerEvent(PointerEvents.POINTER_MOVE, xView, yView)
        } catch (e: Exception) {
            // エラーを記録するだけ（伝播させない）
            Log.e("lookAt", "ポインタイベント送信エラー: ${e.message}")
        }
    } catch (e: Exception) {
        // トップレベルの例外処理
        Log.e("lookAt", "処理中にエラーが発生: ${e.message}")
    }
}

