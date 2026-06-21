import tensorflow as tf
import numpy as np
import json
import os

NUM_FEATURES = 50

def build_model(input_dim):
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(64, activation='relu', input_shape=(input_dim,)),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])
    model.compile(
        optimizer='adam',
        loss='binary_crossentropy',
        metrics=['accuracy']
    )
    return model

def generate_synthetic_data(num_samples=5000):
    X = np.random.random((num_samples, NUM_FEATURES)).astype(np.float32)
    weights = np.random.random(NUM_FEATURES).astype(np.float32)
    y = np.clip(X @ weights + np.random.normal(0, 0.1, num_samples), 0, 1)
    y = (y > 0.5).astype(np.float32)
    return X, y

model = build_model(NUM_FEATURES)

X_train, y_train = generate_synthetic_data(5000)
X_val, y_val = generate_synthetic_data(1000)

print("Entrenando modelo...")
history = model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=20,
    batch_size=32,
    verbose=1
)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

output_dir = "../app/src/main/assets"
os.makedirs(output_dir, exist_ok=True)
output_path = os.path.join(output_dir, "recommendation_model.tflite")
with open(output_path, 'wb') as f:
    f.write(tflite_model)

print(f"Modelo guardado en: {output_path}")
print(f"Tamaño: {len(tflite_model) / 1024:.1f} KB")
