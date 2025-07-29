package com.example.posturemonitoring

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var startButton: Button
    private lateinit var statusText: TextView
    private lateinit var postureText: TextView
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseAnalyzer: PoseAnalyzer
    private lateinit var apiService: ApiService
    
    private var isMonitoring = false  // Changed from val to var
    private var currentSession: PostureSession? = null  // Changed from val to var
    
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupServices()
        setupButtonClickListener()
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            setupCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }
    
    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)
        postureText = findViewById(R.id.postureText)
    }
    
    private fun setupServices() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize API service with your backend IP
        apiService = ApiService("ws://192.168.0.7:8000/stream")
        
        // Initialize pose analyzer with callback
        poseAnalyzer = PoseAnalyzer { landmarks ->
            if (isMonitoring) {
                sendPoseDataToBackend(landmarks)
            }
        }
    }
    
    private fun setupButtonClickListener() {
        startButton.setOnClickListener {
            if (!isMonitoring) {
                startMonitoring()
            } else {
                stopMonitoring()
            }
        }
    }
    
    private fun startMonitoring() {
        // Create new session
        currentSession = PostureSession(
            sessionId = generateSessionId(),
            startTime = System.currentTimeMillis()
        )
        
        // Connect to backend
        apiService.connect()
        
        // Update UI
        isMonitoring = true
        startButton.text = "Stop Monitoring"
        statusText.text = "Status: Monitoring Active"
        
        Toast.makeText(this, "Posture monitoring started!", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopMonitoring() {
        // End current session
        currentSession?.let { session ->  // FIXED: Changed from { _ -> to { session ->
            val duration = (System.currentTimeMillis() - session.startTime) / 1000
            Toast.makeText(this, "Session ended. Duration: ${duration}s", Toast.LENGTH_LONG).show()
        }
        
        // Disconnect from backend
        apiService.disconnect()
        
        // Update UI
        isMonitoring = false
        startButton.text = "Start Monitoring"
        statusText.text = "Status: Stopped"
        postureText.text = "Current Posture: --"
        
        currentSession = null
    }
    
    private fun sendPoseDataToBackend(landmarks: FloatArray) {
        currentSession?.let { _ ->  // This is OK since we don't use the parameter
            apiService.sendPoseData(landmarks) { posture ->
                // Update UI on main thread
                runOnUiThread {
                    updatePostureDisplay(posture)
                    updateSessionStats(posture)
                }
            }
        }
    }
    
    private fun updatePostureDisplay(posture: String) {
        val postureType = when (posture.lowercase()) {
            "sitting" -> PostureType.SITTING
            "standing" -> PostureType.STANDING
            "lying" -> PostureType.LYING
            else -> PostureType.UNKNOWN
        }
        
        postureText.text = "Current Posture: ${postureType.displayName}"
    }
    
    private fun updateSessionStats(posture: String) {
        currentSession?.let { session ->  // FIXED: Changed from { _ -> to { session ->
            val normalizedPosture = posture.lowercase()
            if (session.totalPostures.containsKey(normalizedPosture)) {
                session.totalPostures[normalizedPosture] = 
                    session.totalPostures[normalizedPosture]!! + 1
            }
        }
    }
    
    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, poseAnalyzer)
                }
            
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupCamera()
            } else {
                Toast.makeText(this, "Camera permissions are required for posture detection", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        apiService.disconnect()
    }
}