import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
import joblib

data = {
    'hip_y': [0.6, 0.7, 0.8, 0.4, 0.5, 0.3],  # Normalized hip Y position
    'shoulder_y': [0.3, 0.4, 0.5, 0.2, 0.25, 0.1],  # Normalized shoulder Y
    'posture': ['sitting', 'sitting', 'lying', 'standing', 'standing', 'lying']
}

df = pd.DataFrame(data)
X = df[['hip_y', 'shoulder_y']]
y = df['posture']

# Train classifier
clf = RandomForestClassifier(n_estimators=10, random_state=42)
clf.fit(X, y)

# Save model
joblib.dump(clf, '../python-backend/posture_classifier.joblib')
print("Classifier trained and saved!")