import argparse
import os
import sys
from pathlib import Path
from huggingface_hub import hf_hub_download

REPO_ID = "SreekarAditya/yolo-rdd2022-benchmark"
MODELS_DIR = Path(__file__).resolve().parent / "models"

AVAILABLE_MODELS = {
    "yolo12n": "yolo-rdd2022-benchmark/yolo12n_seed0_best.pt",
    "yolov8s": "yolo-rdd2022-benchmark/yolov8s_seed0_best.pt",
    "yolo11s": "yolo-rdd2022-benchmark/yolo11s_seed0_best.pt",
    "yolo12s": "yolo-rdd2022-benchmark/yolo12s_seed0_best.pt",
    "india_ft_10pct": "yolo-rdd2022-benchmark/india_ft_10pct_seed0_best.pt",
}

def download_file(filename: str, destination_dir: Path) -> Path:
    destination_dir.mkdir(parents=True, exist_ok=True)
    local_path = hf_hub_download(
        repo_id=REPO_ID,
        filename=filename,
        local_dir=str(destination_dir),
    )
    return Path(local_path)

def main():
    parser = argparse.ArgumentParser(description="Download pretrained road damage YOLO models from Hugging Face")
    parser.add_argument(
        "--model",
        type=str,
        default="yolo12n",
        choices=list(AVAILABLE_MODELS.keys()) + ["all"],
        help="Model key to download (default: yolo12n - smallest model)"
    )
    args = parser.parse_args()

    targets = list(AVAILABLE_MODELS.keys()) if args.model == "all" else [args.model]

    print(f"Repository: {REPO_ID}")
    print(f"Target directory: {MODELS_DIR}")

    for key in targets:
        remote_filename = AVAILABLE_MODELS[key]
        print(f"Downloading {key} ({remote_filename})...")
        downloaded_path = download_file(remote_filename, MODELS_DIR)
        file_size_bytes = downloaded_path.stat().st_size
        file_size_mb = file_size_bytes / (1024 * 1024)
        print(f"Saved: {downloaded_path}")
        print(f"Size: {file_size_mb:.2f} MB ({file_size_bytes:,} bytes)")

if __name__ == "__main__":
    main()
