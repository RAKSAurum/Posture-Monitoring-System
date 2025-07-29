package com.example.posturemonitoring

import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

class ApiService(private val serverUrl: String) {
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    
    fun connect() {
        val request = Request.Builder()
            .url(serverUrl)
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("WebSocket connected to posture monitoring backend")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val response = gson.fromJson(text, PostureResponse::class.java)
                    onPostureReceived?.invoke(response.posture ?: "unknown")
                } catch (e: Exception) {
                    println("Error parsing posture response: $e")
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocket connection failed: ${t.message}")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket connection closed: $reason")
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Monitoring session ended")
        webSocket = null
    }
    
    private var onPostureReceived: ((String) -> Unit)? = null
    
    fun sendPoseData(landmarks: FloatArray, callback: (String) -> Unit) {
        onPostureReceived = callback
        
        val poseData = PoseData(
            timestamp = System.currentTimeMillis(),
            landmarks = landmarks.toList(),
            sessionId = generateSessionId()
        )
        
        val json = gson.toJson(poseData)
        webSocket?.send(json)
    }
}