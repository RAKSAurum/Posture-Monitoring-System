// Initialize Chart.js
const ctx = document.getElementById('postureChart').getContext('2d');
const chart = new Chart(ctx, {
    type: 'pie',
    data: {
        labels: ['Sitting', 'Standing', 'Lying'],
        datasets: [{
            data: [0, 0, 0],
            backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56']
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false
    }
});

// Real posture tracking data
let postureTimes = { sitting: 0, standing: 0, lying: 0 };
let lastPosture = null;
let postureStartTime = Date.now();

// Connect to the Python backend via WebSocket
const socket = new WebSocket('ws://192.168.0.7:8000/dashboard');

socket.onopen =  function(event) {
    console.log('Connected to backend server');
    updateConnectionStatus('Connected', 'green');
};

socket.onclose = function(event) {
    console.log('Disconnected from backend server');
    updateConnectionStatus('Disconnected', 'red');
};

socket.onerror = function(error) {
    console.log('WebSocket error:', error);
    updateConnectionStatus('Error', 'red');
};

// Listen for real posture events from backend
socket.onmessage = function(event) {
    try {
        const data = JSON.parse(event.data);
        if (data.type === 'posture_update') {
            const currentPosture = data.posture.toLowerCase();
            const timestamp = data.timestamp;
            
            updatePostureData(currentPosture, timestamp);
        }
    } catch (error) {
        console.error('Error parsing message:', error);
    }
};

function updatePostureData(newPosture, timestamp) {
    const now = Date.now();
    
    // If this is a posture change, add duration to previous posture
    if (lastPosture && lastPosture !== newPosture) {
        const duration = Math.floor((now - postureStartTime) / 1000); // seconds
        if (postureTimes[lastPosture] !== undefined) {
            postureTimes[lastPosture] += duration;
        }
    }
    
    // Update current posture
    lastPosture = newPosture;
    postureStartTime = now;
    
    updateDisplay();
    updateCurrentPosture(newPosture);
}

function updateDisplay() {
    document.getElementById('sitting-time').textContent = `${Math.floor(postureTimes.sitting / 60)} min ${postureTimes.sitting % 60} sec`;
    document.getElementById('standing-time').textContent = `${Math.floor(postureTimes.standing / 60)} min ${postureTimes.standing % 60} sec`;
    document.getElementById('lying-time').textContent = `${Math.floor(postureTimes.lying / 60)} min ${postureTimes.lying % 60} sec`;
    
    chart.data.datasets[0].data = [postureTimes.sitting, postureTimes.standing, postureTimes.lying];
    chart.update();
}

function updateCurrentPosture(posture) {
    const currentPostureElement = document.getElementById('currentPosture');
    if (currentPostureElement) {
        currentPostureElement.textContent = `Current: ${posture.charAt(0).toUpperCase() + posture.slice(1)}`;
    }
}

function updateConnectionStatus(status, color) {
    const statusElement = document.getElementById('connectionStatus');
    if (statusElement) {
        statusElement.textContent = status;
        statusElement.style.color = color;
    }
}

// Add connection status and current posture to the dashboard
window.addEventListener('load', function() {
    const statusDiv = document.createElement('div');
    statusDiv.innerHTML = `
        <div style="padding: 10px; background-color: #f0f0f0; margin-bottom: 10px; display: flex; justify-content: space-between;">
            <strong>Backend: <span id="connectionStatus" style="color: orange;">Connecting...</span></strong>
            <strong><span id="currentPosture">Current: --</span></strong>
        </div>
    `;
    document.body.insertBefore(statusDiv, document.body.firstChild);
});