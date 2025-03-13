package com.rementia.virtual_agent

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class MyApplication : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig {
        // Camera2 を使った CameraX のデフォルト設定を返します
        return Camera2Config.defaultConfig()
    }
}
