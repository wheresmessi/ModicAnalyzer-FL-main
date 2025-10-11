from pathlib import Path
import time
import uuid
import shutil
import os
from typing import List
import logging
import io
import numpy as np
from PIL import Image

from fastapi import FastAPI, UploadFile, File, Form, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware

# import server_aggregate as sa  # Temporarily disabled due to TensorFlow issue

# Configure logging first
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# TensorFlow for prediction endpoint
try:
    import tensorflow as tf
    tf_available = True
    logger.info("✅ TensorFlow loaded successfully")
except ImportError:
    tf_available = False
    logger.warning("⚠️ TensorFlow not available - prediction endpoint will be disabled")

ROOT = Path(__file__).parent
UPLOADS = ROOT / "storage" / "uploads"
GLOBAL = ROOT / "storage" / "global"
ARCHIVE = ROOT / "storage" / "archive"

for d in (UPLOADS, GLOBAL, ARCHIVE):
    d.mkdir(parents=True, exist_ok=True)

# Ensure required models exist
KERAS_MODEL = ROOT / "final_model.keras"
TFLITE_MODEL = ROOT / "global_model.tflite"

if not KERAS_MODEL.exists():
    logger.error(f"❌ Keras model not found: {KERAS_MODEL}")
    raise FileNotFoundError(f"Required Keras model not found: {KERAS_MODEL}")
else:
    logger.info(f"✅ Keras model found: {KERAS_MODEL}")

# Load Keras model for prediction endpoint
prediction_model = None
if tf_available:
    try:
        logger.info(f"🔄 Loading Keras model from: {KERAS_MODEL}")
        prediction_model = tf.keras.models.load_model(str(KERAS_MODEL))
        logger.info(f"✅ Prediction model loaded successfully")
        logger.info(f"   Input shape: {prediction_model.input_shape}")
        logger.info(f"   Output shape: {prediction_model.output_shape}")
    except Exception as e:
        logger.error(f"❌ Failed to load prediction model: {e}")
        logger.error(f"   Model path: {KERAS_MODEL}")
        logger.error(f"   File exists: {KERAS_MODEL.exists()}")
        prediction_model = None
else:
    logger.warning("⚠️ TensorFlow not available - skipping model loading")

app = FastAPI(
    title="ModicAnalyzer Federated Learning Server - Production", 
    version="2.1",
    description="Production-ready privacy-preserving federated learning for medical image analysis"
)

# Production CORS configuration
allowed_origins = ["*"]  # In production, specify exact domains
if os.getenv("PYTHON_ENV") == "production":
    allowed_origins = [
        "https://your-app-domain.com",  # Replace with your Android app's domain
    ]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Statistics tracking
stats = {
    "total_uploads": 0,
    "total_aggregations": 0,
    "unique_clients": set(),
    "last_aggregation": None,
    "server_start_time": time.time(),
    "successful_aggregations": 0,
    "failed_aggregations": 0
}

# Production configuration
MAX_CLIENTS_PER_ROUND = int(os.getenv("MAX_CLIENTS_PER_ROUND", "10"))
MIN_CLIENTS_FOR_AGGREGATION = int(os.getenv("MIN_CLIENTS_FOR_AGGREGATION", "2"))
AUTO_AGGREGATION_ENABLED = os.getenv("AUTO_AGGREGATION", "false").lower() == "true"


async def trigger_auto_aggregation():
    """Background task to trigger automatic aggregation"""
    try:
        logger.info("🤖 Auto-aggregation triggered")
        # Call the aggregation function
        result = aggregate()
        logger.info(f"✅ Auto-aggregation completed: {result}")
    except Exception as e:
        logger.error(f"❌ Auto-aggregation failed: {e}")
        stats["failed_aggregations"] += 1


@app.post("/predict")
async def predict(
    file_t1: UploadFile = File(..., description="T1-weighted MRI image"),
    file_t2: UploadFile = File(..., description="T2-weighted MRI image")
):
    """
    Predict Modic changes from T1 and T2 MRI images using the server-side Keras model.
    
    Returns:
        JSON with prediction score, label, and processing metadata
    """
    logger.info(f"📥 Prediction request received")
    logger.info(f"   TensorFlow available: {tf_available}")
    logger.info(f"   Model loaded: {prediction_model is not None}")
    
    if not tf_available:
        logger.error("❌ TensorFlow not available")
        raise HTTPException(
            status_code=503, 
            detail="TensorFlow not available on server"
        )
    
    if prediction_model is None:
        logger.error("❌ Prediction model not loaded")
        raise HTTPException(
            status_code=503, 
            detail="Prediction model not loaded on server"
        )
    
    try:
        start_time = time.time()
        
        # Validate file types
        if not file_t1.content_type.startswith('image/'):
            raise HTTPException(status_code=400, detail="T1 file must be an image")
        if not file_t2.content_type.startswith('image/'):
            raise HTTPException(status_code=400, detail="T2 file must be an image")
        
        # Read and process images
        t1_bytes = await file_t1.read()
        t2_bytes = await file_t2.read()
        
        # Convert to PIL Images
        t1_image = Image.open(io.BytesIO(t1_bytes)).convert('RGB')
        t2_image = Image.open(io.BytesIO(t2_bytes)).convert('RGB')
        
        # Resize to model input size (224x224)
        t1_resized = t1_image.resize((224, 224))
        t2_resized = t2_image.resize((224, 224))
        
        # Convert to numpy arrays and normalize
        t1_array = np.array(t1_resized, dtype=np.float32) / 255.0
        t2_array = np.array(t2_resized, dtype=np.float32) / 255.0
        
        # Prepare input for model (batch of 1, dual inputs)
        input_batch = np.expand_dims(np.stack([t1_array, t2_array], axis=0), axis=0)
        
        logger.info(f"🔍 Processing prediction: T1={file_t1.filename}, T2={file_t2.filename}")
        logger.info(f"📊 Input shape: {input_batch.shape}")
        
        # Run prediction
        if len(prediction_model.input_shape) > 1:
            # Dual input model - separate T1 and T2
            t1_batch = np.expand_dims(t1_array, axis=0)
            t2_batch = np.expand_dims(t2_array, axis=0)
            prediction = prediction_model.predict([t1_batch, t2_batch], verbose=0)
        else:
            # Single input model - use combined input
            prediction = prediction_model.predict(input_batch, verbose=0)
        
        # Extract results
        output = prediction[0]  # Remove batch dimension
        no_modic_score = float(output[0])
        modic_score = float(output[1])
        
        # Determine label and confidence
        if modic_score > no_modic_score:
            label = "Modic"
            confidence = modic_score
        else:
            label = "No Modic"
            confidence = no_modic_score
        
        processing_time = int((time.time() - start_time) * 1000)  # ms
        
        result = {
            "prediction": confidence,
            "label": label,
            "confidence": confidence,
            "detailed_scores": {
                "no_modic": no_modic_score,
                "modic": modic_score
            },
            "processing_time_ms": processing_time,
            "model_version": str(stats.get("total_aggregations", 0)),
            "input_files": {
                "t1": file_t1.filename,
                "t2": file_t2.filename
            }
        }
        
        logger.info(f"✅ Prediction completed: {label} ({confidence:.3f}) in {processing_time}ms")
        return result
        
    except Exception as e:
        logger.error(f"❌ Prediction failed: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.post("/upload_weights")
async def upload_weights(
    background_tasks: BackgroundTasks,
    client_id: str = Form(...), 
    file: UploadFile = File(...)
):
    """Accept a client-uploaded .npz file containing model weights.

    Expects a multipart form with `client_id` and a file field (binary .npz).
    Saves the file into `backend/storage/uploads/` for later aggregation.
    """
    # Validate input
    if not client_id or len(client_id) > 64:
        raise HTTPException(status_code=400, detail="Invalid client_id")
    
    if not file.filename or not file.filename.endswith(('.npz', '.npy')):
        raise HTTPException(status_code=400, detail="File must be .npz or .npy format")
    
    # Check if we have too many pending uploads
    pending_uploads = len(list(UPLOADS.glob("*.npz")))
    if pending_uploads >= MAX_CLIENTS_PER_ROUND:
        raise HTTPException(
            status_code=429, 
            detail=f"Too many pending uploads. Max: {MAX_CLIENTS_PER_ROUND}"
        )
    
    # Track statistics
    stats["total_uploads"] += 1
    stats["unique_clients"].add(client_id)
    
    # Save file with timestamp and unique ID
    filename = f"{client_id}_{int(time.time())}_{uuid.uuid4().hex[:8]}.npz"
    dest = UPLOADS / filename
    
    try:
        content = await file.read()
        
        # Basic validation - check if it's a valid .npz file
        if len(content) < 100:  # Minimum reasonable size
            raise HTTPException(status_code=400, detail="File too small to be valid weights")
        
        with open(dest, "wb") as f:
            f.write(content)
        
        logger.info(f"📤 Received weights from client {client_id}: {len(content)} bytes")
        
        # Auto-aggregation check
        if AUTO_AGGREGATION_ENABLED and pending_uploads + 1 >= MIN_CLIENTS_FOR_AGGREGATION:
            background_tasks.add_task(trigger_auto_aggregation)
        
    except Exception as e:
        logger.error(f"❌ Failed to save upload from {client_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to save upload: {e}")

    return {
        "status": "received", 
        "filename": dest.name,
        "total_clients": len(stats["unique_clients"]),
        "total_uploads": stats["total_uploads"],
        "pending_uploads": pending_uploads + 1,
        "auto_aggregation": AUTO_AGGREGATION_ENABLED
    }


@app.get("/latest_weights")
def latest_weights():
    """Download the latest aggregated weights (.npz) - Legacy endpoint."""
    path = GLOBAL / "latest_weights.npz"
    if path.exists():
        return FileResponse(str(path), media_type="application/octet-stream", filename="latest_weights.npz")
    raise HTTPException(status_code=404, detail="No global model available yet")


@app.get("/get_global_model")
def get_global_model():
    """Download the latest global .tflite model for client use."""
    if not TFLITE_MODEL.exists():
        raise HTTPException(status_code=404, detail="No global .tflite model available yet. Run aggregation first.")
    
    # Log download
    logger.info(f"📥 Global model downloaded: {TFLITE_MODEL.stat().st_size} bytes")
    
    return FileResponse(
        str(TFLITE_MODEL), 
        media_type="application/octet-stream", 
        filename="global_model.tflite",
        headers={"Model-Version": str(stats["total_aggregations"])}
    )


# Aggregate endpoint temporarily disabled due to TensorFlow dependency issue

@app.get("/status")
def status():
    """Comprehensive server status for production monitoring"""
    current_time = time.time()
    uptime_hours = round((current_time - stats["server_start_time"]) / 3600, 2)
    
    return {
        # Basic status
        "status": "operational",
        "architecture": "hybrid_keras_tflite",
        "version": "2.1",
        "uptime_hours": uptime_hours,
        
        # File status
        "uploads": len(list(UPLOADS.glob("*.npz"))),
        "global_exists": (GLOBAL / "latest_weights.npz").exists(),
        "keras_model_exists": KERAS_MODEL.exists(),
        "tflite_model_exists": TFLITE_MODEL.exists(),
        "archived_files": len(list(ARCHIVE.glob("*.npz"))),
        
        # Statistics
        "total_uploads": stats["total_uploads"],
        "total_aggregations": stats["total_aggregations"],
        "successful_aggregations": stats["successful_aggregations"],
        "failed_aggregations": stats["failed_aggregations"],
        "unique_clients": len(stats["unique_clients"]),
        "last_aggregation": stats["last_aggregation"],
        
        # Model info
        "keras_model_size_mb": round(KERAS_MODEL.stat().st_size / (1024*1024), 2) if KERAS_MODEL.exists() else 0,
        "tflite_model_size_mb": round(TFLITE_MODEL.stat().st_size / (1024*1024), 2) if TFLITE_MODEL.exists() else 0,
        
        # Configuration
        "max_clients_per_round": MAX_CLIENTS_PER_ROUND,
        "min_clients_for_aggregation": MIN_CLIENTS_FOR_AGGREGATION,
        "auto_aggregation_enabled": AUTO_AGGREGATION_ENABLED,
        
        # Health indicators
        "health": {
            "can_aggregate": len(list(UPLOADS.glob("*.npz"))) >= MIN_CLIENTS_FOR_AGGREGATION,
            "models_ready": KERAS_MODEL.exists() and TFLITE_MODEL.exists(),
            "error_rate": round(stats["failed_aggregations"] / max(stats["total_aggregations"], 1) * 100, 2)
        }
    }


@app.get("/")
def root():
    """Production API information"""
    return {
        "message": "ModicAnalyzer Federated Learning Server - Production Ready",
        "version": "2.1",
        "architecture": "hybrid_keras_tflite",
        "status": "operational",
        "endpoints": {
            "predict": "POST /predict",
            "upload": "POST /upload_weights",
            "aggregate": "POST /aggregate", 
            "download_tflite": "GET /get_global_model",
            "download_legacy": "GET /latest_weights",
            "status": "GET /status",
            "health": "GET /health"
        },
        "workflow": {
            "1": "Clients send T1/T2 images via /predict for server-side inference",
            "2": "Clients send weight updates (.npz) via /upload_weights",
            "3": "Server aggregates using .keras model via /aggregate",
            "4": "Server converts to .tflite and serves via /get_global_model",
            "5": "Clients download and update local .tflite models for offline use"
        },
        "features": {
            "online_inference": tf_available and prediction_model is not None,
            "federated_learning": True,
            "offline_model_distribution": True
        },
        "production_features": {
            "auto_aggregation": AUTO_AGGREGATION_ENABLED,
            "max_clients_per_round": MAX_CLIENTS_PER_ROUND,
            "min_clients_for_aggregation": MIN_CLIENTS_FOR_AGGREGATION,
            "comprehensive_logging": True,
            "error_tracking": True,
            "model_validation": True
        }
    }


@app.get("/health")
def health_check():
    """Health check endpoint for load balancers and debugging"""
    try:
        # Check critical components
        keras_ok = KERAS_MODEL.exists()
        storage_ok = all(d.exists() for d in [UPLOADS, GLOBAL, ARCHIVE])
        model_loaded = prediction_model is not None
        
        status_info = {
            "status": "healthy" if (keras_ok and storage_ok and tf_available and model_loaded) else "unhealthy",
            "timestamp": time.time(),
            "components": {
                "tensorflow_available": tf_available,
                "keras_model_file": keras_ok,
                "prediction_model_loaded": model_loaded,
                "storage_dirs": storage_ok
            }
        }
        
        if tf_available and prediction_model:
            status_info["model_info"] = {
                "input_shape": str(prediction_model.input_shape),
                "output_shape": str(prediction_model.output_shape)
            }
        
        return status_info
    except Exception as e:
        return {"status": "error", "error": str(e)}, 500


if __name__ == "__main__":
    # Run with: uvicorn main:app --host 0.0.0.0 --port 8000
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
