import argparse
import os
import sys
from pathlib import Path
import numpy as np
from PIL import Image

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models" / "yolo-rdd2022-benchmark"
TEST_IMAGES_DIR = BASE_DIR / "test_images"

def get_representative_dataset():
    image_files = list(TEST_IMAGES_DIR.glob("*.jpg")) + list(TEST_IMAGES_DIR.glob("*.png"))
    for img_path in image_files:
        with Image.open(img_path) as img:
            img_resized = img.convert("RGB").resize((640, 640))
            arr = np.array(img_resized, dtype=np.float32) / 255.0
            arr = np.transpose(arr, (2, 0, 1))
            arr = np.expand_dims(arr, axis=0)
            yield [arr]

def convert_to_onnx(pt_path: Path) -> Path:
    from ultralytics import YOLO
    model = YOLO(str(pt_path))
    onnx_path = model.export(format="onnx", imgsz=640)
    return Path(onnx_path)

def convert_onnx_to_tflite(onnx_path: Path, output_dir: Path):
    import onnx2tf
    import onnx2tf.onnx2tf
    import onnx2tf.utils.common_functions as cf

    dummy_sample = np.zeros((1, 3, 640, 640), dtype=np.float32)
    cf.download_test_image_data = lambda: dummy_sample
    onnx2tf.onnx2tf.download_test_image_data = lambda: dummy_sample

    output_dir.mkdir(parents=True, exist_ok=True)
    onnx2tf.convert(
        input_onnx_file_path=str(onnx_path),
        output_folder_path=str(output_dir),
        copy_onnx_input_output_names_to_tflite=True,
        check_onnx_tf_outputs_elementwise_close_full=False,
        output_signaturedefs=True,
        output_integer_quantized_tflite=True,
        output_dynamic_range_quantized_tflite=True,
    )

def quantize_saved_model(saved_model_dir: Path, output_dir: Path):
    import tensorflow as tf

    converter_fp32 = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
    tflite_fp32 = converter_fp32.convert()
    fp32_path = output_dir / "yolo12n_float32.tflite"
    fp32_path.write_bytes(tflite_fp32)
    print(f"Exported FP32 TFLite: {fp32_path} ({len(tflite_fp32):,} bytes)")

    converter_fp16 = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
    converter_fp16.optimizations = [tf.lite.Optimize.DEFAULT]
    converter_fp16.target_spec.supported_types = [tf.float16]
    tflite_fp16 = converter_fp16.convert()
    fp16_path = output_dir / "yolo12n_float16.tflite"
    fp16_path.write_bytes(tflite_fp16)
    print(f"Exported FP16 TFLite: {fp16_path} ({len(tflite_fp16):,} bytes)")

    converter_dyn_int8 = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
    converter_dyn_int8.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_dyn_int8 = converter_dyn_int8.convert()
    dyn_int8_path = output_dir / "yolo12n_dynamic_int8.tflite"
    dyn_int8_path.write_bytes(tflite_dyn_int8)
    print(f"Exported Dynamic INT8 TFLite: {dyn_int8_path} ({len(tflite_dyn_int8):,} bytes)")

    try:
        converter_int8 = tf.lite.TFLiteConverter.from_saved_model(str(saved_model_dir))
        converter_int8.optimizations = [tf.lite.Optimize.DEFAULT]
        converter_int8.representative_dataset = get_representative_dataset
        converter_int8.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
            tf.lite.OpsSet.SELECT_TF_OPS,
        ]
        tflite_int8 = converter_int8.convert()
        int8_path = output_dir / "yolo12n_int8.tflite"
        int8_path.write_bytes(tflite_int8)
        print(f"Exported Full INT8 TFLite: {int8_path} ({len(tflite_int8):,} bytes)")
    except Exception as e:
        print(f"Full integer INT8 quantization with representative dataset failed: {e}")

def main():
    parser = argparse.ArgumentParser(description="Export YOLOv12n model to ONNX, SavedModel, and quantized TFLite")
    parser.add_argument(
        "--pt",
        type=str,
        default=str(MODELS_DIR / "yolo12n_seed0_best.pt"),
        help="Path to PyTorch .pt weights"
    )
    args = parser.parse_args()

    pt_path = Path(args.pt)
    if not pt_path.exists():
        raise FileNotFoundError(f"PyTorch model not found: {pt_path}")

    onnx_path = pt_path.with_suffix(".onnx")
    if not onnx_path.exists():
        print(f"Exporting {pt_path} to ONNX...")
        onnx_path = convert_to_onnx(pt_path)
    else:
        print(f"Existing ONNX model found: {onnx_path}")

    tflite_out_dir = MODELS_DIR / "tflite_export"
    saved_model_dir = tflite_out_dir
    print(f"Converting ONNX to SavedModel and baseline TFLite in {tflite_out_dir}...")
    convert_onnx_to_tflite(onnx_path, tflite_out_dir)

    print("Generating quantized TFLite variants...")
    quantize_saved_model(saved_model_dir, tflite_out_dir)

if __name__ == "__main__":
    main()
