package com.example.posturemonitoring

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.atomic.AtomicLong

@androidx.camera.core.ExperimentalGetImage
class PoseAnalyzer(
    private val onPoseDetected: (landmarks: FloatArray) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val poseDetector: PoseDetector
    private val lastAnalyzedTimestamp = AtomicLong(0)
    
    init {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)
    }
    
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        
        // Throttle to ~2 FPS to avoid overwhelming the backend
        if (currentTimestamp - lastAnalyzedTimestamp.get() >= 500) {
            lastAnalyzedTimestamp.set(currentTimestamp)
            
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                poseDetector.process(image)
                    .addOnSuccessListener { pose ->
                        val landmarks = pose.allPoseLandmarks
                        if (landmarks.isNotEmpty()) {
                            // Convert landmarks to simple float array
                            val landmarkArray = FloatArray(landmarks.size * 2)
                            landmarks.forEachIndexed { index, landmark ->
                                landmarkArray[index * 2] = landmark.position.x
                                landmarkArray[index * 2 + 1] = landmark.position.y
                            }
                            onPoseDetected(landmarkArray)
                        }
                    }
                    .addOnFailureListener {
                        // Handle error silently for now
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}