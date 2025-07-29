package com.example.posturemonitoring

/**
 * Data class to represent pose landmarks data sent to the backend
 * @param timestamp The time when the pose was detected (milliseconds since epoch)
 * @param landmarks List of landmark coordinates (x, y pairs flattened into a single list)
 * @param sessionId Optional session identifier for tracking user sessions
 */
data class PoseData(
    val timestamp: Long,
    val landmarks: List<Float>,
    val sessionId: String? = null
)

/**
 * Data class to represent the response from the backend after posture classification
 * @param posture The classified posture (e.g., "sitting", "standing", "lying")
 * @param timestamp The timestamp when the classification was made
 * @param confidence Confidence level of the classification ("high", "medium", "low")
 * @param sessionId Session identifier matching the request
 */
data class PostureResponse(
    val posture: String?,
    val timestamp: Long?,
    val confidence: String?,
    val sessionId: String? = null
)

/**
 * Data class to represent individual pose landmarks
 * @param x X coordinate (normalized 0-1)
 * @param y Y coordinate (normalized 0-1)
 * @param z Z coordinate (depth, if available)
 * @param visibility Visibility score (0-1, how visible the landmark is)
 */
data class PoseLandmark(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f
)

/**
 * Data class for storing posture session information
 * @param sessionId Unique identifier for the session
 * @param startTime When the monitoring session started
 * @param endTime When the monitoring session ended (null if ongoing)
 * @param totalPostures Map of posture types to duration in seconds
 */
data class PostureSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalPostures: MutableMap<String, Int> = mutableMapOf(
        "sitting" to 0,
        "standing" to 0,
        "lying" to 0
    )
)

/**
 * Data class for real-time posture statistics
 * @param currentPosture The currently detected posture
 * @param duration How long the current posture has been maintained (seconds)
 * @param sessionStats Overall session statistics
 */
data class PostureStats(
    val currentPosture: String,
    val duration: Int,
    val sessionStats: Map<String, Int>
)

/**
 * Enum class for posture types with display names
 */
enum class PostureType(val displayName: String) {
    SITTING("Sitting"),
    STANDING("Standing"),
    LYING("Lying Down"),
    UNKNOWN("Unknown")
}

/**
 * Data class for websocket connection status
 * @param isConnected Whether the websocket is currently connected
 * @param lastPingTime Last successful ping to the server
 * @param errorMessage Any connection error message
 */
data class ConnectionStatus(
    val isConnected: Boolean,
    val lastPingTime: Long? = null,
    val errorMessage: String? = null
)

/**
 * Helper extension functions for data processing
 */
fun PoseData.toJson(): String {
    return """
        {
            "timestamp": $timestamp,
            "landmarks": [${landmarks.joinToString(",")}],
            "sessionId": ${sessionId?.let { "\"$it\"" } ?: "null"}
        }
    """.trimIndent()
}

/**
 * Convert flat landmark array to structured PoseLandmark objects
 * @param landmarkArray Flattened array of x,y coordinates
 * @return List of PoseLandmark objects
 */
fun convertToLandmarks(landmarkArray: FloatArray): List<PoseLandmark> {
    val landmarks = mutableListOf<PoseLandmark>()
    for (i in landmarkArray.indices step 2) {
        if (i + 1 < landmarkArray.size) {
            landmarks.add(
                PoseLandmark(
                    x = landmarkArray[i],
                    y = landmarkArray[i + 1]
                )
            )
        }
    }
    return landmarks
}

/**
 * Generate a unique session ID
 */
fun generateSessionId(): String {
    return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
}