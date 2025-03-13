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

// 色の定義例
val Purple40 = Color(0xFF7E57C2)
val Purple80 = Color(0xFFD1C4E9)
val PurpleGrey40 = Color(0xFF90A4AE)
val PurpleGrey80 = Color(0xFFCFD8DC)
val Pink40 = Color(0xFFFF4081)
val Pink80 = Color(0xFFFF80AB)

// カラーパレットの定義（ライト・ダーク両方で背景とサーフェスを黒に設定）
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color.Black,  // 背景色を黒に設定
    surface = Color.Black      // サーフェスも黒に設定
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.Black,  // 背景色を黒に設定
    surface = Color.Black      // サーフェスも黒に設定
)

// Typography と Shapes の定義
val Typography = Typography()
val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(0.dp)
)

/**
 * VirtualAgentTheme
 *
 * darkTheme によって、ダークテーマまたはライトテーマのカラースキームを適用し、
 * MaterialTheme を利用してコンテンツをラップします。
 */
@Composable
fun VirtualAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
