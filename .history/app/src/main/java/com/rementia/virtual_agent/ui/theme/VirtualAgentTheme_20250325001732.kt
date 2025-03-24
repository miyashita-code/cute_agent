package com.rementia.virtual_agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * アプリケーションのカラーパレット定義
 * 
 * Material Design 3に準拠したカラーシステムのための色定義です。
 * 各色は、ダークモードとライトモードの両方で使用されます。
 */

/** 紫色 - プライマリカラー (ライトモード用) */
val Purple40 = Color(0xFF7E57C2)

/** 明るい紫色 - プライマリカラー (ダークモード用) */
val Purple80 = Color(0xFFD1C4E9)

/** グレーがかった紫色 - セカンダリカラー (ライトモード用) */
val PurpleGrey40 = Color(0xFF90A4AE)

/** 明るいグレーがかった紫色 - セカンダリカラー (ダークモード用) */
val PurpleGrey80 = Color(0xFFCFD8DC)

/** ピンク色 - アクセントカラー (ライトモード用) */
val Pink40 = Color(0xFFFF4081)

/** 明るいピンク色 - アクセントカラー (ダークモード用) */
val Pink80 = Color(0xFFFF80AB)

/**
 * ダークモード用のカラースキーム
 * 
 * 背景とサーフェスの色を黒に設定して、Riveアニメーションを目立たせます。
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,      // プライマリカラー
    secondary = PurpleGrey80,// セカンダリカラー
    tertiary = Pink80,       // アクセントカラー
    background = Color.Black,// 背景色を黒に設定
    surface = Color.Black    // サーフェスも黒に設定
)

/**
 * ライトモード用のカラースキーム
 * 
 * ダークモードと同様に、背景とサーフェスの色を黒に設定しています。
 * これは、Riveアニメーションを目立たせるための意図的な設計です。
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,      // プライマリカラー
    secondary = PurpleGrey40,// セカンダリカラー
    tertiary = Pink40,       // アクセントカラー
    background = Color.Black,// 背景色を黒に設定
    surface = Color.Black    // サーフェスも黒に設定
)

/**
 * アプリケーションのタイポグラフィ設定
 * 
 * 現在はデフォルト設定を使用しています。
 * 必要に応じて、フォントファミリーやウェイト、サイズを変更できます。
 */
val Typography = Typography()

/**
 * アプリケーションの形状設定
 * 
 * コンポーネントの角の丸みなどの形状を定義します。
 */
val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),  // 小さいコンポーネント用
    medium = RoundedCornerShape(4.dp), // 中サイズコンポーネント用
    large = RoundedCornerShape(0.dp)   // 大きいコンポーネント用
)

/**
 * VirtualAgentのテーマ
 *
 * システムのダークモード設定に基づいて、適切なカラースキームを適用します。
 * MaterialThemeを使用して、アプリ全体のデザインシステムを統一します。
 *
 * @param darkTheme ダークモードを使用するかどうか。デフォルトはシステム設定に従います。
 * @param content 表示するコンテンツ
 */
@Composable
fun VirtualAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // システム設定に基づいてカラースキームを選択
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // MaterialThemeを適用
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
