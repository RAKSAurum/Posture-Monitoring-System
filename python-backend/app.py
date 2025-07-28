from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import json
import joblib
import numpy as np
import asyncio
from typing import List

# Create FastAPI app instance
app = FastAPI()

# Enable FastAPI's CORS to allow frontend to communicate with backend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load posture_classifier model
try:
    classifier = joblib.load('posture_classifier.joblib')
    print("Classifier loaded successfully!")
except:
    classifier = None
    print("Warning: Classifier not found. Train it first.")

# Connection manager for tracking WebSocket clients (Android & dashboard)
class ConnectionManager:
    def __init__(self):
        self.android_connections: List[WebSocket] = []
        self.dashboard_connections: List[WebSocket] = []

    async def connect_android(self, websocket: WebSocket):
        await websocket.accept()
        self.android_connections.append(websocket)

    async def connect_dashboard(self, websocket: WebSocket):
        await websocket.accept()
        self.dashboard_connections.append(websocket)

    def disconnect_android(self, websocket: WebSocket):
        if websocket in self.android_connections:
            self.android_connections.remove(websocket)

    def disconnect_dashboard(self, websocket: WebSocket):
        if websocket in self.dashboard_connections:
            self.dashboard_connections.remove(websocket)

    # Broadcast posture updates to all connected dashboard clients
    async def broadcast_to_dashboards(self, message: dict):
        for connection in self.dashboard_connections:
            try:
                await connection.send_text(json.dumps(message))
            except:
                pass

# Instantiate the connection manager
manager = ConnectionManager()

# Simple test to verify server is running
@app.get("/")
async def root():
    return {"message": "Posture Monitoring Backend", "classifier_loaded": classifier is not None}

# Android WebSocket endpoint for sending posture data
@app.websocket("/stream")
async def android_websocket(websocket: WebSocket):
    await manager.connect_android(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            pose_data = json.loads(data)

            if 'landmarks' in pose_data:
                landmarks = pose_data['landmarks']

                # Rebuild coordinate pairs from flat list
                coords = []
                for i in range(0, len(landmarks), 2):
                    if i + 1 < len(landmarks):
                        coords.append([landmarks[i], landmarks[i+1]])

                print(f"Received {len(coords)} coordinate pairs")

                if len(coords) >= 25:
                    # Default image dimensions
                    image_width = 1080.0
                    image_height = 1920.0

                    # Normalize selected landmark Y-values for the basic classifier model to work
                    nose_y = coords[0][1] / image_height if len(coords) > 0 else 0.5
                    left_shoulder_y = coords[11][1] / image_height if len(coords) > 11 else 0.5
                    right_shoulder_y = coords[12][1] / image_height if len(coords) > 12 else 0.5
                    left_hip_y = coords[23][1] / image_height if len(coords) > 23 else 0.6
                    right_hip_y = coords[24][1] / image_height if len(coords) > 24 else 0.6

                    # Calculate posture metrics from key points
                    shoulder_y = (left_shoulder_y + right_shoulder_y) / 2
                    hip_y = (left_hip_y + right_hip_y) / 2
                    torso_length = abs(shoulder_y - hip_y)

                    print(f"Debug - normalized nose_y: {nose_y:.3f}, shoulder_y: {shoulder_y:.3f}, hip_y: {hip_y:.3f}, torso_length: {torso_length:.3f}")

                    # Basic rule-based classification logic
                    if torso_length < 0.05:
                        posture = "lying"
                    elif hip_y > 0.6:
                        posture = "sitting"
                    elif hip_y < 0.4:
                        posture = "standing"
                    else:
                        posture = "sitting"  # Default fallback (I can't make the model smarter than this right now)

                    print(f"Classified as: {posture}")

                    # Send posture result back to Android client
                    response = {
                        "timestamp": pose_data.get('timestamp'),
                        "posture": posture,
                        "confidence": "high"
                    }
                    await websocket.send_text(json.dumps(response))

                    # Send update to dashboard
                    await manager.broadcast_to_dashboards({
                        "type": "posture_update",
                        "posture": posture,
                        "timestamp": pose_data.get('timestamp')
                    })

                else:
                    # Oops 1 Not enough landmark data
                    response = {"posture": "unknown", "error": "Insufficient landmarks"}
                    await websocket.send_text(json.dumps(response))
            else:
                # Oops 2 No landmark key in incoming data
                response = {"error": "No landmarks in data"}
                await websocket.send_text(json.dumps(response))

    except WebSocketDisconnect:
        manager.disconnect_android(websocket)

# Dashboard WebSocket endpoint (only receives updates)
@app.websocket("/dashboard")
async def dashboard_websocket(websocket: WebSocket):
    await manager.connect_dashboard(websocket)
    try:
        while True:
            # Keep connection alive with a simple sleep loop
            await asyncio.sleep(1)
    except WebSocketDisconnect:
        manager.disconnect_dashboard(websocket)
