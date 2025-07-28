// Initialize the pie chart using Chart.js to display posture durations
const ctx = document.getElementById('postureChart').getContext('2d');
const chart = new Chart(ctx, {
    type: 'pie',
    data: {
        labels: ['Sitting', 'Standing', 'Lying'], // Labels for each posture
        datasets: [{
            data: [0, 0, 0], // Initial posture time values
            backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56'] // Colors for each slice
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false // Allows flexible chart sizing
    }
});

// Track posture duration data
let postureTimes = { sitting: 0, standing: 0, lying: 0 };
let lastPosture = null;
let postureStartTime = Date.now(); // Time when the current posture started

// Set up WebSocket connection to backend server
const socket = new WebSocket('ws://192.168.0.7:8000/dashboard');

socket.onopen = function(event) {
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

// Handle incoming posture data from backend
socket.onmessage = function(event) {
    try {
        const data = JSON.parse(event.data);
        if (data.type === 'posture_update') {
            const currentPosture = data.posture.toLowerCase(); // posture: "Sitting" → "sitting"
            const timestamp = data.timestamp;

            updatePostureData(currentPosture, timestamp);
        }
    } catch (error) {
        console.error('Error parsing message:', error);
    }
};

// Update posture tracking data
function updatePostureData(newPosture, timestamp) {
    const now = Date.now();

    // If the posture changed, record how long the previous posture lasted
    if (lastPosture && lastPosture !== newPosture) {
        const duration = Math.floor((now - postureStartTime) / 1000); // Convert to seconds
        if (postureTimes[lastPosture] !== undefined) {
            postureTimes[lastPosture] += duration;
        }
    }

    // Update the tracking state
    lastPosture = newPosture;
    postureStartTime = now;

    updateDisplay(); // Update chart and time text
    updateCurrentPosture(newPosture); // Update the "Current: posture" text
}

// Refresh the chart and time display in the UI
function updateDisplay() {
    document.getElementById('sitting-time').textContent = `${Math.floor(postureTimes.sitting / 60)} min ${postureTimes.sitting % 60} sec`;
    document.getElementById('standing-time').textContent = `${Math.floor(postureTimes.standing / 60)} min ${postureTimes.standing % 60} sec`;
    document.getElementById('lying-time').textContent = `${Math.floor(postureTimes.lying / 60)} min ${postureTimes.lying % 60} sec`;

    chart.data.datasets[0].data = [postureTimes.sitting, postureTimes.standing, postureTimes.lying];
    chart.update();
}

// Update the label showing the current posture
function updateCurrentPosture(posture) {
    const currentPostureElement = document.getElementById('currentPosture');
    if (currentPostureElement) {
        currentPostureElement.textContent = `Current: ${posture.charAt(0).toUpperCase() + posture.slice(1)}`;
    }
}

// Update the UI's connection status text and color
function updateConnectionStatus(status, color) {
    const statusElement = document.getElementById('connectionStatus');
    if (statusElement) {
        statusElement.textContent = status;
        statusElement.style.color = color;
    }
}

// On page load, insert a top bar showing connection and posture status
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