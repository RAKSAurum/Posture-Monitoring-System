# Posture Monitoring System

## Overview

The **Posture Monitoring System** is an end-to-end solution to monitor and classify human posture in real-time using a mobile Android application, a Python backend for posture classification, and a web dashboard for data visualization. This system leverages Google ML Kit for pose detection, streams pose landmarks over WebSocket, and uses a machine learning classifier to recognize postures such as sitting, standing, and lying down.

## Features

- Real-time pose detection using **ML Kit Pose Detection API**
- Android app with **camera preview** and posture monitoring controls
- **WebSocket communication** between the Android app and Python backend
- Python backend implements posture classification and streams results back
- Live **web dashboard** displaying posture durations and current posture
- Supports **session tracking** for monitoring posture over time
- Modular and extensible architecture for further improvements

## Architecture

```
Android App (Kotlin) 
    ↓ WebSocket (pose landmarks)
Python Backend (FastAPI)
    ↓ WebSocket (posture classification)
Web Dashboard (JavaScript + Chart.js)
```

## Setup and Installation

### Prerequisites

- Android device or emulator with camera support
- Python 3.10+
- Node.js and npm (for the web dashboard)
- Android Studio (for building Android app)

### Android App

1. **Configure backend IP:**  
   Edit `ApiService.kt` and set the backend WebSocket URL to your machine IP (e.g., `ws://{machine_ip_address}/stream`).

2. **Build and install:**  
   Use Android Studio or command line:
   ```bash
   cd android-app
   ./gradlew clean build
   ```
   Deploy the APK to your device.

3. **Permissions:**  
   The app requests camera and internet permissions at runtime.

### Python Backend

1. **Setup virtual environment:**  
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

2. **Install dependencies:**  
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the backend server:**  
   ```bash
   uvicorn app:app --reload --host 0.0.0.0 --port 8000
   ```

4. **Model:**  
   Ensure `posture_classifier.joblib` is present or use the built-in rule-based classifier logic.

### Web Dashboard

1. **Install dependencies:**  
   ```bash
   cd web-dashboard
   npm install
   ```

2. **Run the dashboard:**  
   Use a simple HTTP server or your preferred development server:
   ```bash
   python3 -m http.server 8080
   ```
   
3. **Open dashboard:**  
   Visit `http://localhost:8080` in browser to view posture statistics.

## Usage

1. Start the Python backend server
2. Open the web dashboard in your browser
3. Launch the Android app and tap **Start Monitoring**
4. Observe live posture classifications on the app and dashboard
5. Change posture to see detection updates in real-time

## Code Overview

- **Android App:**  
  - `MainActivity.kt` - app logic and camera handling  
  - `PoseAnalyzer.kt` - ML Kit pose detection analyzer  
  - `ApiService.kt` - WebSocket client for streaming pose data  
  - `PostureData.kt` - data models  

- **Python Backend:**  
  - `app.py` - FastAPI server and WebSocket handling  
  - `posture_classifier.joblib` - trained posture classification model  

- **Web Dashboard:**  
  - `index.html` - main dashboard UI  
  - `dashboard.js` - Chart.js setup and WebSocket integration  

## Troubleshooting

- **No posture detected:**  
  - Verify camera permissions granted on Android  
  - Confirm backend IP matches your machine IP in `ApiService.kt` and `dashboard.js`  
  - Make sure the backend server is running and accessible on port 8000  
  - Check WebSocket connections in app logs and dashboard console  

- **Build errors:**  
  - Ensure Java version <= 19 for Android Studio compatibility  
  - Sync Gradle project after changes  

## Future Improvements

- Enhance classifier with more features or retrain with larger datasets  
- Implement session persistence and historical playback in dashboard  
- Add notifications for poor posture alerts  
- Expand posture categories  
- Improve app UI/UX and error handling  