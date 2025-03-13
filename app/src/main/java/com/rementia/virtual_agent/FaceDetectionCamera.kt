package com.rementia.virtual_agent.ui.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

@Composable
fun FaceDetectionCamera(
    modifier: Modifier = Modifier,
    onFaceDetected: (Boolean) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            // 高速モードで ML Kit 顔検出器を初期化
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
            val faceDetector = FaceDetection.getClient(options)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder().build()
                    imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        FaceDetectionAnalyzer(faceDetector) { faceFound ->
                            Log.d("FaceDetectionCamera", "Face detection result: $faceFound")
                            onFaceDetected(faceFound)
                        }
                    )
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        ctx as ComponentActivity,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.d("FaceDetectionCamera", "Camera provider bind success")
                } catch (exc: Exception) {
                    Log.e("FaceDetectionCamera", "カメラバインド失敗", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}


class FaceDetectionAnalyzer(
    private val faceDetector: com.google.mlkit.vision.face.FaceDetector,
    private val onFaceDetection: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees
            )
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    onFaceDetection(faces.isNotEmpty())
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetectionAnalyzer", "顔検出失敗", e)
                    onFaceDetection(false)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
