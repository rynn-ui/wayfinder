import argparse
import sys
from pathlib import Path
from PIL import Image
from ultralytics import YOLO

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"
RESULTS_DIR = BASE_DIR / "results"
TEST_IMAGES_DIR = BASE_DIR / "test_images"

DEFAULT_MODEL_PATH = MODELS_DIR / "yolo-rdd2022-benchmark" / "yolo12n_seed0_best.pt"
FALLBACK_MODEL_PATH = MODELS_DIR / "yolo-rdd2022-benchmark" / "yolov8s_seed0_best.pt"

def resolve_model_path(provided_path: str | None) -> Path:
    if provided_path:
        p = Path(provided_path)
        if p.exists():
            return p
        alt_p = MODELS_DIR / provided_path
        if alt_p.exists():
            return alt_p
        alt_p_sub = MODELS_DIR / "yolo-rdd2022-benchmark" / provided_path
        if alt_p_sub.exists():
            return alt_p_sub
        raise FileNotFoundError(f"Model file not found: {provided_path}")

    if DEFAULT_MODEL_PATH.exists():
        return DEFAULT_MODEL_PATH
    if FALLBACK_MODEL_PATH.exists():
        return FALLBACK_MODEL_PATH

    available = list(MODELS_DIR.rglob("*.pt"))
    if available:
        return available[0]

    raise FileNotFoundError(
        f"No model weights found in {MODELS_DIR}. Run download_model.py first."
    )

def resolve_image_paths(provided_path: str | None) -> list[Path]:
    if provided_path:
        p = Path(provided_path)
        if p.is_file():
            return [p]
        if p.is_dir():
            images = list(p.glob("*.jpg")) + list(p.glob("*.png")) + list(p.glob("*.jpeg"))
            if images:
                return sorted(images)
        raise FileNotFoundError(f"Image path not found: {provided_path}")

    default_images = list(TEST_IMAGES_DIR.glob("*.jpg")) + list(TEST_IMAGES_DIR.glob("*.png"))
    if default_images:
        return sorted(default_images)

    raise FileNotFoundError(
        f"No test images found in {TEST_IMAGES_DIR}. Place an image in {TEST_IMAGES_DIR} or pass --image."
    )

def inspect_model_metadata(model: YOLO, model_path: Path):
    print("=" * 70)
    print("MODEL METADATA & SPECIFICATION")
    print("=" * 70)
    print(f"Model file: {model_path.name}")
    print(f"Model full path: {model_path}")
    file_size_mb = model_path.stat().st_size / (1024 * 1024)
    print(f"File size: {file_size_mb:.2f} MB ({model_path.stat().st_size:,} bytes)")
    print(f"Task: {model.task}")
    print(f"Number of classes: {len(model.names)}")
    print(f"Class mapping: {model.names}")

    expected_res = "640x640 (default for RDD2022 benchmark)"
    if hasattr(model, "overrides") and model.overrides.get("imgsz"):
        expected_res = f"{model.overrides.get('imgsz')}"
    print(f"Expected input resolution: {expected_res}")
    print("=" * 70)

def run_evaluation(model_path: Path, image_paths: list[Path], conf_threshold: float, iou_threshold: float):
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    model = YOLO(str(model_path))
    inspect_model_metadata(model, model_path)

    print("\n" + "=" * 70)
    print("INFERENCE EVALUATION RESULTS")
    print("=" * 70)

    for img_path in image_paths:
        print(f"\nEvaluating Image: {img_path.name}")
        print(f"Image Path: {img_path}")

        with Image.open(img_path) as img:
            orig_w, orig_h = img.size
            print(f"Original Image Dimensions: {orig_w}x{orig_h}")

        results = model.predict(
            source=str(img_path),
            conf=conf_threshold,
            iou=iou_threshold,
            save=False,
            verbose=False,
        )

        result = results[0]
        boxes = result.boxes
        total_detections = len(boxes)
        print(f"Total Detections Found: {total_detections} (confidence >= {conf_threshold})")

        if total_detections == 0:
            print("  No road hazards detected above confidence threshold.")
        else:
            for idx, box in enumerate(boxes):
                cls_id = int(box.cls.item())
                cls_name = model.names.get(cls_id, f"unknown_{cls_id}")
                conf_score = float(box.conf.item())
                xyxy = [round(coord, 2) for coord in box.xyxy.tolist()[0]]
                print(
                    f"  [{idx + 1}] Class: {cls_name} (ID: {cls_id}) | "
                    f"Confidence: {conf_score:.4f} | "
                    f"BBox [xmin, ymin, xmax, ymax]: {xyxy}"
                )

        output_filename = f"{model_path.stem}_{img_path.stem}_annotated.jpg"
        output_path = RESULTS_DIR / output_filename
        annotated_bgr = result.plot()
        annotated_img = Image.fromarray(annotated_bgr[..., ::-1])
        annotated_img.save(str(output_path), quality=95)
        print(f"Saved annotated image: {output_path}")

    print("\n" + "=" * 70)
    print("EVALUATION COMPLETED SUCCESSFULLY")
    print("=" * 70)

def main():
    parser = argparse.ArgumentParser(description="Evaluate pretrained YOLO road damage model on test images")
    parser.add_argument(
        "--model",
        type=str,
        default=None,
        help="Path or name of model weights (default: yolo12n_seed0_best.pt)"
    )
    parser.add_argument(
        "--image",
        type=str,
        default=None,
        help="Path to test image or directory of images (default: ml/pretrained-model-test/test_images/)"
    )
    parser.add_argument(
        "--conf",
        type=float,
        default=0.15,
        help="Confidence threshold for detection (default: 0.15)"
    )
    parser.add_argument(
        "--iou",
        type=float,
        default=0.45,
        help="NMS IoU threshold (default: 0.45)"
    )
    args = parser.parse_args()

    model_path = resolve_model_path(args.model)
    image_paths = resolve_image_paths(args.image)
    run_evaluation(model_path, image_paths, args.conf, args.iou)

if __name__ == "__main__":
    main()
