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

app = FastAPI(
    title="ModicAnalyzer Federated Learning Server", 
    version="1.0",
    description="Privacy-preserving federated learning for medical image analysis"
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
    "last_aggregation": None
}


@app.post("/upload_weights")
async def upload_weights(client_id: str = Form(...), file: UploadFile = File(...)):
    """Accept a client-uploaded .npz file containing model weights.

    Expects a multipart form with `client_id` and a file field (binary .npz).
    Saves the file into `backend/storage/uploads/` for later aggregation.
    """
    # Track statistics
    stats["total_uploads"] += 1
    stats["unique_clients"].add(client_id)
    
    # accept anything as binary; clients should send an .npz
    filename = f"{client_id}_{int(time.time())}_{uuid.uuid4().hex}.npz"
    dest = UPLOADS / filename
    try:
        content = await file.read()
        with open(dest, "wb") as f:
            f.write(content)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to save upload: {e}")

    return {
        "status": "received", 
        "filename": dest.name,
        "total_clients": len(stats["unique_clients"]),
        "total_uploads": stats["total_uploads"]
    }


@app.get("/latest_weights")
def latest_weights():
    """Download the latest aggregated weights (.npz)."""
    path = GLOBAL / "latest_weights.npz"
    if path.exists():
        return FileResponse(str(path), media_type="application/octet-stream", filename="latest_weights.npz")
    raise HTTPException(status_code=404, detail="No global model available yet")


@app.post("/aggregate")
def aggregate():
    """Trigger aggregation across all uploads currently in the uploads folder.

    This performs a simple Federated Averaging on arrays stored in each client's .npz file.
    After aggregation, the output is saved to `storage/global/latest_weights.npz` and
    processed uploads are moved to `storage/archive/`.
    """
    files = sorted(UPLOADS.glob("*.npz"))
    if not files:
        raise HTTPException(status_code=400, detail="No uploads to aggregate")

    input_paths = [str(p) for p in files]
    output = GLOBAL / "latest_weights.npz"
    try:
        sa.aggregate_npz_files(input_paths, str(output))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Aggregation failed: {e}")

    # archive processed uploads
    for p in files:
        try:
            shutil.move(str(p), ARCHIVE / p.name)
        except Exception:
            # non-fatal: continue
            pass

    # Update statistics
    stats["total_aggregations"] += 1
    stats["last_aggregation"] = time.time()

    return {
        "status": "aggregated", 
        "clients": len(input_paths), 
        "output": output.name,
        "aggregation_number": stats["total_aggregations"],
        "timestamp": stats["last_aggregation"]
    }


@app.get("/status")
def status():
    return {
        "uploads": len(list(UPLOADS.glob("*.npz"))),
        "global_exists": (GLOBAL / "latest_weights.npz").exists(),
        "total_uploads": stats["total_uploads"],
        "total_aggregations": stats["total_aggregations"],
        "unique_clients": len(stats["unique_clients"]),
        "last_aggregation": stats["last_aggregation"],
        "archived_files": len(list(ARCHIVE.glob("*.npz")))
    }


@app.get("/")
def root():
    return {
        "message": "ModicAnalyzer Federated Learning Server",
        "version": "1.0",
        "status": "operational",
        "endpoints": {
            "upload": "POST /upload_weights",
            "aggregate": "POST /aggregate", 
            "download": "GET /latest_weights",
            "status": "GET /status"
        }
    }


if __name__ == "__main__":
    # Run with: uvicorn main:app --host 0.0.0.0 --port 8000
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
