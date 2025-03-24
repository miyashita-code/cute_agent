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
 * 顔の表情を表す列挙型クラス
 *
 * Riveアニメーションの顔の表情をコントロールするために使用される値を定義します。
 * 各表情は0から6までの整数値に対応しています。
 * 
 * @property intValue Riveアニメーションに送信される表情の整数値
 */
enum class FacialExpression(val intValue: Int) {
    /** 非常に怒った表情 (値: 0) */
    SUPER_ANGRY(0),
    
    /** 少し怒った表情 (値: 1) */
    LITTLE_ANGRY(1),
    
    /** わずかに怒った表情 (値: 2) */
    SLIGHT_LITTLE_ANGRY(2),
    
    /** 通常の表情 (値: 3) */
    NORMAL(3),
    
    /** わずかに嬉しい表情 (値: 4) */
    SLIGHT_LITTLE_HAPPY(4),
    
    /** 少し嬉しい表情 (値: 5) */
    LITTLE_HAPPY(5),
    
    /** 非常に嬉しい表情 (値: 6) */
    HAPPY(6)
}

/**
 * 指定した表情をRiveアニメーションに適用します。
 *
 * この関数は以下の2つの操作を実行します：
 * 1. facialExpressionSelector に表情の数値を設定
 * 2. facialExpressionTrigger をトリガーして表情の変更を適用
 *
 * @param riveView 操作対象のRiveAnimationViewインスタンス
 * @param machineName 操作対象のステートマシン名
 * @param expression 設定する表情
 * @throws IllegalArgumentException 指定した入力が見つからない場合
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
 * 指定したブール値の入力をRiveステートマシンに安全に設定します。
 *
 * 入力が存在しない場合は詳細なエラーメッセージを提供します。
 *
 * @param riveView 操作対象のRiveAnimationViewインスタンス
 * @param machineName 操作対象のステートマシン名
 * @param inputName 設定する入力の名前
 * @param value 設定するブール値
 * @throws IllegalArgumentException 指定した入力が見つからない場合
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
 * 指定した数値の入力をRiveステートマシンに安全に設定します。
 *
 * 入力が存在しない場合は詳細なエラーメッセージを提供します。
 *
 * @param riveView 操作対象のRiveAnimationViewインスタンス
 * @param machineName 操作対象のステートマシン名
 * @param inputName 設定する入力の名前
 * @param value 設定する数値
 * @throws IllegalArgumentException 指定した入力が見つからない場合
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
 * 指定されたRiveステートマシン内に存在する入力の名前一覧を取得します。
 *
 * このメソッドはデバッグとエラーメッセージの生成に利用されます。
 *
 * @param riveView 検査対象のRiveAnimationViewインスタンス
 * @param machineName 検査対象のステートマシン名
 * @return ステートマシン内に存在する入力名のリスト、取得できない場合は空リスト
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
 * Artboardの座標変換に関するキャッシュを管理するシングルトンオブジェクト
 *
 * パフォーマンス最適化のため、Artboardの座標変換パラメータをキャッシュします。
 * これにより、毎回の座標変換の際にArtboardオブジェクトを作成する必要がなくなります。
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
    
    /**
     * 変換パラメータを設定します。
     *
     * @param scale スケーリング係数
     * @param offsetX X軸オフセット
     * @param offsetY Y軸オフセット
     * @param width ビューの幅
     * @param height ビューの高さ
     * @param boundsLeft アートボード境界の左座標
     * @param boundsTop アートボード境界の上座標
     */
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
    
    /**
     * ビュー座標をアートボード座標に変換します。
     *
     * @param xView ビュー上のX座標
     * @param yView ビュー上のY座標
     * @return アートボード上の座標ペア (X, Y)
     */
    fun viewToArtboardCoords(xView: Float, yView: Float): Pair<Float, Float> {
        val artboardX = (xView - lastOffsetX) / lastScale + lastBoundsLeft
        val artboardY = (yView - lastOffsetY) / lastScale + lastBoundsTop
        return Pair(artboardX, artboardY)
    }
    
    /**
     * キャッシュが現在のビューサイズに対して有効かどうかを確認します。
     *
     * @param width 現在のビュー幅
     * @param height 現在のビュー高さ
     * @return キャッシュが有効な場合true、それ以外はfalse
     */
    fun isValid(width: Int, height: Int): Boolean {
        return lastWidth == width && lastHeight == height && lastScale > 0
    }
}

/**
 * Artboardの座標変換パラメータを初期化します。
 *
 * この関数は一度だけ実行する必要があり、Artboardの座標変換の初期設定を行います。
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
 * 指定した画面座標に対してRiveアニメーションの視線を向けます。
 * 
 * メモリ効率化されたバージョンで、Artboardインスタンス生成を完全に回避しています。
 * ポインターイベントを直接使用することで、視線追従機能を維持しながらメモリ使用を削減します。
 *
 * @param xScreen 画面上のX座標
 * @param yScreen 画面上のY座標
 * @param stateMachineName 操作対象のステートマシン名
 */
fun RiveAnimationView.lookAt(
    xScreen: Float,
    yScreen: Float,
    stateMachineName: String
) {
    // コントローラーの取得
    val ctl = this.controller ?: return
    
    try {
        // ビュー内座標に変換
        val viewCoords = IntArray(2)
        this.getLocationOnScreen(viewCoords)
        val xView = xScreen - viewCoords[0]
        val yView = yScreen - viewCoords[1]
        
        // ファイル読み込み確認
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

