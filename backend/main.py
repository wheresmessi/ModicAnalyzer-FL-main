from pathlib import Path
import time
import uuid
import shutil
import os
from typing import List

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware

import server_aggregate as sa

ROOT = Path(__file__).parent
UPLOADS = ROOT / "storage" / "uploads"
GLOBAL = ROOT / "storage" / "global"
ARCHIVE = ROOT / "storage" / "archive"

for d in (UPLOADS, GLOBAL, ARCHIVE):
    d.mkdir(parents=True, exist_ok=True)

from pathlib import Path
import time
import uuid
import shutil
import os
from typing import List
import logging

from fastapi import FastAPI, UploadFile, File, Form, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware

import server_aggregate as sa

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

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


@app.post("/aggregate")
def aggregate():
    """Trigger aggregation across all uploads using hybrid .keras + .tflite approach.

    This performs Federated Averaging using the .keras model for aggregation,
    then converts the result to .tflite for client download.
    After aggregation, processed uploads are moved to `storage/archive/`.
    """
    files = sorted(UPLOADS.glob("*.npz"))
    if not files:
        raise HTTPException(status_code=400, detail="No uploads to aggregate")
    
    if len(files) < MIN_CLIENTS_FOR_AGGREGATION:
        raise HTTPException(
            status_code=400, 
            detail=f"Need at least {MIN_CLIENTS_FOR_AGGREGATION} clients for aggregation. Got {len(files)}"
        )

    input_paths = [str(p) for p in files]
    
    try:
        logger.info(f"🔄 Starting aggregation with {len(files)} clients")
        
        # Use hybrid FL aggregation
        keras_path, tflite_path = sa.hybrid_fl_aggregation(
            input_paths, 
            keras_output_path=str(KERAS_MODEL),
            tflite_output_path=str(TFLITE_MODEL)
        )
        
        # Also create legacy .npz for backward compatibility
        legacy_output = GLOBAL / "latest_weights.npz"
        sa.aggregate_npz_files(input_paths, str(legacy_output))
        
        # Update statistics
        stats["total_aggregations"] += 1
        stats["successful_aggregations"] += 1
        stats["last_aggregation"] = time.time()
        
        logger.info(f"✅ Aggregation successful: {len(files)} clients processed")
        
    except Exception as e:
        stats["failed_aggregations"] += 1
        logger.error(f"❌ Aggregation failed: {e}")
        raise HTTPException(status_code=500, detail=f"Aggregation failed: {e}")

    # Archive processed uploads
    for p in files:
        try:
            shutil.move(str(p), ARCHIVE / p.name)
        except Exception as e:
            logger.warning(f"⚠️ Failed to archive {p.name}: {e}")

    return {
        "status": "aggregated", 
        "clients": len(input_paths), 
        "keras_model": KERAS_MODEL.name,
        "tflite_model": TFLITE_MODEL.name,
        "legacy_weights": legacy_output.name,
        "aggregation_number": stats["total_aggregations"],
        "timestamp": stats["last_aggregation"],
        "tflite_size_mb": round(TFLITE_MODEL.stat().st_size / (1024*1024), 2) if TFLITE_MODEL.exists() else 0
    }


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
            "upload": "POST /upload_weights",
            "aggregate": "POST /aggregate", 
            "download_tflite": "GET /get_global_model",
            "download_legacy": "GET /latest_weights",
            "status": "GET /status",
            "health": "GET /health"
        },
        "workflow": {
            "1": "Clients send weight updates (.npz) via /upload_weights",
            "2": "Server aggregates using .keras model via /aggregate",
            "3": "Server converts to .tflite and serves via /get_global_model",
            "4": "Clients download and update local .tflite models"
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
    """Health check endpoint for load balancers"""
    try:
        # Check critical components
        keras_ok = KERAS_MODEL.exists()
        storage_ok = all(d.exists() for d in [UPLOADS, GLOBAL, ARCHIVE])
        
        if keras_ok and storage_ok:
            return {"status": "healthy", "timestamp": time.time()}
        else:
            return {"status": "unhealthy", "issues": {
                "keras_model": keras_ok,
                "storage_dirs": storage_ok
            }}, 503
    except Exception as e:
        return {"status": "error", "error": str(e)}, 500


if __name__ == "__main__":
    # Run with: uvicorn main:app --host 0.0.0.0 --port 8000
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
