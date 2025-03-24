# Riveアニメーション使用ガイド

このドキュメントでは、Virtual Agentプロジェクトにおける[Rive](https://rive.app/)アニメーションの実装方法と使用方法について説明します。特に目の動き（視線制御）、表情変更、アニメーションの配置方法に焦点を当てています。

## バージョン情報と互換性

現在のプロジェクトでは以下のバージョンを使用しています：

- **Riveライブラリ**: `app.rive:rive-android:9.12.2`
- **Kotlin**: 1.9.22以上推奨（kotlinCompilerExtensionVersion: 1.5.8と互換）
- **Android最小SDK**: 33 (Android 13)
- **Android対象SDK**: 34 (Android 14)
- **Jetpack Compose**: 2024.10.01 BOM

### 互換性に関する注意事項

- Riveライブラリは頻繁に更新されるため、バージョンアップデート時には細心の注意が必要です
- アニメーションファイルはRiveエディター（Web/デスクトップアプリ）から書き出したものを使用
- アニメーションファイルのバージョンとライブラリのバージョンに互換性があることを確認してください

## 1. 目の動かし方（視線制御）

Virtual Agentの目の動きは、画面上の座標に対して視線を追従させる機能を実装しています。

### 実装方法

1. **視線追従のための関数**:

```kotlin
fun RiveAnimationView.lookAt(
    xScreen: Float,
    yScreen: Float,
    stateMachineName: String
)
```

2. **使用例**:

```kotlin
// 画面上の座標(x, y)に視線を向ける
riveView.lookAt(x, y, "State Machine 1")
```

### メモリ最適化

目の動きを実装する際、メモリ使用量を最適化するために以下の方法を採用しています：

- `pointerEvent`を直接使用してArtboardのインスタンス生成を回避
- 座標変換のためのパラメータをキャッシュ（`ArtboardCache`）
- 更新頻度の調整（300ms間隔）

### 使用上の注意点

- 視線追従機能を使用するには、Riveファイル内に適切に設定されたステートマシンが必要です
- Riveファイル内に`lookAt`に対応する入力が定義されている必要があります

## 2. 表情の変え方

表情変更は`FacialExpression`列挙型と関連ユーティリティ関数を使って実装されています。

### 使用可能な表情

```kotlin
enum class FacialExpression(val intValue: Int) {
    SUPER_ANGRY(0),    // 非常に怒った表情
    LITTLE_ANGRY(1),   // 少し怒った表情
    SLIGHT_LITTLE_ANGRY(2), // わずかに怒った表情
    NORMAL(3),         // 通常の表情
    SLIGHT_LITTLE_HAPPY(4), // わずかに嬉しい表情
    LITTLE_HAPPY(5),   // 少し嬉しい表情
    HAPPY(6)           // 非常に嬉しい表情
}
```

### 表情変更の関数

```kotlin
fun setFacialExpression(
    riveView: RiveAnimationView,
    machineName: String,
    expression: FacialExpression
)
```

### 表情変更の実装方法

表情を変更するには以下の2つのステップが必要です：

1. `facialExpressionSelector`に表情の数値を設定
2. `facialExpressionTrigger`をトリガーして表情の変更を適用

```kotlin
// 例：表情を「HAPPY」に変更
setFacialExpression(riveView, "State Machine 1", FacialExpression.HAPPY)
```

### Riveファイルの要件

表情変更機能を使用するには、Riveファイル内に以下の定義が必要です：

- 数値入力: `facialExpressionSelector` (0～6の値を受け取る)
- トリガー: `facialExpressionTrigger` (表情変更を適用するためのトリガー)

## 3. アニメーションの配置方法

### リソースとしてのアニメーションファイル配置

1. Riveエディターからエクスポートした`.riv`ファイルを`res/raw`ディレクトリに配置します

```
app/
  src/
    main/
      res/
        raw/
          character_facial_animation.riv  // Riveアニメーションファイル
```

2. リソースIDを使用してRiveAnimationViewを初期化します

```kotlin
// res/raw内のRiveファイルを読み込む
riveView.setRiveResource(R.raw.character_facial_animation)
```

### Compose UIへの統合

```kotlin
@Composable
fun RiveDemoScreen(
    riveResId: Int,          // 例: R.raw.character_facial_animation
    stateMachineName: String // 例: "State Machine 1"
) {
    // ... 状態の定義 ...
    
    // RiveアニメーションをCompose UIに統合
    AndroidView(
        modifier = Modifier.size(minSize),
        factory = { context: Context ->
            // Riveアニメーションビューの作成と初期化
            RiveAnimationView(context).apply {
                setRiveResource(riveResId)
                // ... リスナー登録など ...
            }
        },
        update = { riveView ->
            // 更新処理
        }
    )
    
    // ... UI要素の配置 ...
}
```

### コンポーネントの分離

コードの再利用性と可読性を高めるために、Riveアニメーション関連のコンポーネントを以下のように分離しています：

- `RiveDemoScreen`: メインのComposeコンポーネント
- `RiveAnimationView`: Androidビューを含むCompose関数
- `createRiveAnimationView`: RiveAnimationViewの作成と初期化
- `updateRiveAnimation`: アニメーションの状態更新
- `ExpressionSelectorButtons`: 表情選択UI

## パフォーマンス最適化とメモリ管理

### メモリ使用量の監視

```kotlin
private suspend fun startMemoryMonitoring(
    memoryInfo: MutableState<String>
) {
    while (true) {
        delay(30000L) // 30秒ごと
        // ガベージコレクションを促進
        gc()
        
        // メモリ情報を取得して表示
        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemoryMB = runtime.maxMemory() / 1024 / 1024
        val availableMemoryMB = maxMemoryMB - usedMemoryMB
        
        memoryInfo.value = "メモリ使用: ${usedMemoryMB}MB / 最大: ${maxMemoryMB}MB (空き: ${availableMemoryMB}MB)"
    }
}
```

### リソース解放のベストプラクティス

- `RiveAnimationView`の参照を適切に管理する
- 不要な座標変換計算を避けるためにキャッシングを使用
- 視線追従などの高頻度更新処理の間隔を調整（300ms）

## トラブルシューティング

### よくある問題と解決策

1. **メモリリーク**
   - 原因: Artboardのインスタンス化が多すぎる
   - 解決策: `lookAt`の最適化版を使用する

2. **表情が変わらない**
   - 原因: Riveファイル内の入力名が一致していない
   - 解決策: `getAvailableInputs`関数で利用可能な入力を確認

3. **視線が動かない**
   - 原因: ステートマシン名の不一致またはポインターイベントが正しく設定されていない
   - 解決策: ステートマシン名を確認し、Riveファイルのポインターイベント設定を確認

## 設計上の考慮事項

1. **リソース効率**
   - 高解像度のアニメーションはメモリ消費が大きいため適切なサイズに調整
   
2. **エラー処理**
   - すべての操作が例外をキャッチして適切に処理するようにする
   
3. **Composeとの統合**
   - Androidビューとの橋渡しには`AndroidView`コンポーネントを使用
   - 状態管理には`remember`と`mutableStateOf`を使用

## 参考リンク

- [Rive公式ドキュメント](https://help.rive.app/runtimes/overview)
- [Rive Android Runtime GitHub](https://github.com/rive-app/rive-android)
- [Jetpack Compose AndroidView](https://developer.android.com/jetpack/compose/interop/interop-apis#views-in-compose)
