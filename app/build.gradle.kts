plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rementia.virtual_agent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rementia.virtual_agent"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"  // Kotlin 1.9.22 に対応したバージョンに更新
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Rive
    implementation("app.rive:rive-android:9.12.2")
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Compose
    // Compose BOM: Compose 関連ライブラリのバージョン管理用
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    // material3: Compose Material Design のライブラリ
    implementation("androidx.compose.material3:material3")

    // 最低限必要な Compose UI の基本ライブラリ
    implementation("androidx.compose.ui:ui")

    // 基本的なレイアウトやジェスチャーなど、Compose Foundation の機能を使いたい場合
    implementation("androidx.compose.foundation:foundation")

    // Compose を Activity で使う場合は連携用ラブラリ（Activity Compose）
    implementation("androidx.activity:activity-compose:1.9.2")
}
