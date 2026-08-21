import argparse
import time
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import tensorflow as tf

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models" / "yolo-rdd2022-benchmark"
TFLITE_DIR = MODELS_DIR / "tflite_export"
RESULTS_DIR = BASE_DIR / "results"
TEST_IMAGES_DIR = BASE_DIR / "test_images"

CLASS_NAMES = {
    0: "longitudinal_crack",
    1: "transverse_crack",
    2: "alligator_crack",
    3: "pothole",
}

CLASS_COLORS = {
    0: (0, 255, 255),
    1: (0, 200, 255),
    2: (255, 255, 255),
    3: (0, 255, 200),
}

def calculate_iou(box1, box2):
    x1 = max(box1[0], box2[0])
    y1 = max(box1[1], box2[1])
    x2 = min(box1[2], box2[2])
    y2 = min(box1[3], box2[3])

    intersection = max(0.0, x2 - x1) * max(0.0, y2 - y1)
    area1 = max(0.0, box1[2] - box1[0]) * max(0.0, box1[3] - box1[1])
    area2 = max(0.0, box2[2] - box2[0]) * max(0.0, box2[3] - box2[1])
    union = area1 + area2 - intersection

    if union <= 0.0:
        return 0.0
    return intersection / union

def non_max_suppression(detections, iou_threshold=0.45):
    if not detections:
        return []

    detections = sorted(detections, key=lambda x: x["confidence"], reverse=True)
    kept = []

    while detections:
        best = detections.pop(0)
        kept.append(best)
        detections = [
            d for d in detections
            if d["class_id"] != best["class_id"] or calculate_iou(best["bbox"], d["bbox"]) < iou_threshold
        ]

    return kept

def preprocess_image(image_path: Path, target_size=(640, 640)):
    orig_img = Image.open(image_path).convert("RGB")
    orig_w, orig_h = orig_img.size
    resized = orig_img.resize(target_size)
    arr = np.array(resized, dtype=np.float32) / 255.0
    arr = np.expand_dims(arr, axis=0)
    return arr, orig_img, orig_w, orig_h

def run_tflite_inference(model_path: Path, image_path: Path, conf_threshold=0.15, iou_threshold=0.45):
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()[0]
    output_details = interpreter.get_output_details()[0]

    input_data, orig_img, orig_w, orig_h = preprocess_image(image_path)

    interpreter.set_tensor(input_details["index"], input_data)

    start_time = time.perf_counter()
    interpreter.invoke()
    latency_ms = (time.perf_counter() - start_time) * 1000.0

    raw_output = interpreter.get_tensor(output_details["index"])
    predictions = raw_output[0].T

    boxes = predictions[:, :4]
    scores = predictions[:, 4:]

    max_confidences = np.max(scores, axis=1)
    class_ids = np.argmax(scores, axis=1)

    scale_x = orig_w / 640.0
    scale_y = orig_h / 640.0

    raw_detections = []
    for i in range(len(max_confidences)):
        conf = float(max_confidences[i])
        if conf >= conf_threshold:
            cls_id = int(class_ids[i])
            cx, cy, w, h = boxes[i]
            xmin = max(0.0, (cx - w / 2.0) * scale_x)
            ymin = max(0.0, (cy - h / 2.0) * scale_y)
            xmax = min(float(orig_w), (cx + w / 2.0) * scale_x)
            ymax = min(float(orig_h), (cy + h / 2.0) * scale_y)
            raw_detections.append({
                "class_id": cls_id,
                "class_name": CLASS_NAMES.get(cls_id, f"class_{cls_id}"),
                "confidence": conf,
                "bbox": [round(xmin, 2), round(ymin, 2), round(xmax, 2), round(ymax, 2)],
            })

    final_detections = non_max_suppression(raw_detections, iou_threshold)

    return {
        "model_file": model_path.name,
        "input_shape": input_details["shape"].tolist(),
        "input_dtype": str(input_details["dtype"]),
        "output_shape": output_details["shape"].tolist(),
        "output_dtype": str(output_details["dtype"]),
        "latency_ms": latency_ms,
        "orig_size": (orig_w, orig_h),
        "detections": final_detections,
        "orig_img": orig_img,
    }

def annotate_and_save(result, image_name: str, model_tag: str):
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    img = result["orig_img"].copy()
    draw = ImageDraw.Draw(img)

    for det in result["detections"]:
        bbox = det["bbox"]
        cls_name = det["class_name"]
        conf = det["confidence"]
        cls_id = det["class_id"]
        color = CLASS_COLORS.get(cls_id, (0, 255, 0))

        draw.rectangle(bbox, outline=color, width=4)
        label = f"{cls_name} {conf:.2f}"
        draw.text((bbox[0], max(0, bbox[1] - 20)), label, fill=color)

    out_name = f"{model_tag}_{image_name}_annotated.jpg"
    out_path = RESULTS_DIR / out_name
    img.save(str(out_path), quality=95)
    return out_path

def main():
    parser = argparse.ArgumentParser(description="Test and benchmark exported TFLite models")
    parser.add_argument("--conf", type=float, default=0.15, help="Confidence threshold (default: 0.15)")
    parser.add_argument("--iou", type=float, default=0.45, help="NMS IoU threshold (default: 0.45)")
    args = parser.parse_args()

    tflite_models = sorted(list(TFLITE_DIR.glob("*.tflite")))
    if not tflite_models:
        raise FileNotFoundError(f"No .tflite models found in {TFLITE_DIR}. Run export_tflite.py first.")

    test_images = sorted(list(TEST_IMAGES_DIR.glob("*.jpg")) + list(TEST_IMAGES_DIR.glob("*.png")))

    print("=" * 80)
    print("TFLITE INFERENCE & QUANTIZATION EVALUATION")
    print("=" * 80)

    for model_path in tflite_models:
        model_size_mb = model_path.stat().st_size / (1024 * 1024)
        print("\n" + "=" * 80)
        print(f"EVALUATING MODEL: {model_path.name}")
        print(f"File Size: {model_size_mb:.2f} MB ({model_path.stat().st_size:,} bytes)")
        print("=" * 80)

        for img_path in test_images:
            res = run_tflite_inference(model_path, img_path, args.conf, args.iou)
            print(f"\nImage: {img_path.name} ({res['orig_size'][0]}x{res['orig_size'][1]})")
            print(f"Input Tensor: {res['input_shape']} | Dtype: {res['input_dtype']}")
            print(f"Output Tensor: {res['output_shape']} | Dtype: {res['output_dtype']}")
            print(f"Inference Latency: {res['latency_ms']:.2f} ms")
            print(f"Total Detections: {len(res['detections'])}")

            for idx, det in enumerate(res["detections"]):
                print(
                    f"  [{idx + 1}] Class: {det['class_name']} (ID: {det['class_id']}) | "
                    f"Conf: {det['confidence']:.4f} | BBox: {det['bbox']}"
                )

            saved_path = annotate_and_save(res, img_path.stem, model_path.stem)
            print(f"Saved Annotated Image: {saved_path}")

    print("\n" + "=" * 80)
    print("ALL TFLITE MODELS EVALUATED SUCCESSFULLY")
    print("=" * 80)

if __name__ == "__main__":
    main()
