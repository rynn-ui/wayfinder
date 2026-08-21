# Pretrained Road Damage YOLO Model & TFLite Evaluation

Isolated evaluation and quantization workspace for pretrained RDD2022 road hazard detection models.

## 1. Overview

This workspace evaluates pretrained road damage detection models from Hugging Face (`SreekarAditya/yolo-rdd2022-benchmark`), exports them to ONNX and TFLite formats, tests INT8 / FP16 quantization, and compares detection performance against the original PyTorch model.

This workspace is completely isolated from the Android application (`app/`).

## 2. Model Architecture & Specifications

### Backbone & Detection Head
- **Base Architecture**: YOLOv12 Nano (`YOLOv12n`)
- **Key Modules**:
  - Area Attention (`A2`) with Positional Encoding (`attn.pe.conv`)
  - Depthwise Separable Convolutions (`DWConv`)
  - Multi-scale Feature Pyramid (P3/8, P4/16, P5/32: grid dimensions 80x80, 40x40, 20x20)
  - Distribution Focal Loss (`DFL`) bounding box regression head
- **Parameters**: 2,557,508 (2.56M parameters)
- **Computational Complexity**: 7.3 GFLOPs (3.7G MACs)
- **Number of Classes**: 4 (`0: longitudinal_crack`, `1: transverse_crack`, `2: alligator_crack`, `3: pothole`)

### Tensor Formats & Shapes
- **PyTorch (.pt) / ONNX (.onnx)**:
  - Input: `[1, 3, 640, 640]` (NCHW float32)
  - Output: `[1, 8, 8400]` (8 channels: 4 bbox coordinates `[cx, cy, w, h]` + 4 class confidences across 8,400 anchor points)
- **TFLite (.tflite)**:
  - Input: `[1, 640, 640, 3]` (NHWC float32, normalized to range [0.0, 1.0])
  - Output: `[1, 8, 8400]` (float32)

## 3. Exported Models & Quantization Comparison

| Format / Quantization | File Name | File Size | Size Reduction | Inference Latency | Detection Integrity |
|---|---|---|---|---|---|
| **PyTorch FP32** | `yolo12n_seed0_best.pt` | 5.28 MB | Baseline | ~138 ms | Baseline (100%) |
| **ONNX FP32** | `yolo12n_seed0_best.onnx` | 10.08 MB | - | ~95 ms | Identical to .pt |
| **TFLite Float32** | `yolo12n_seed0_best_float32.tflite` | 9.97 MB | - | ~132 ms | Identical to .pt |
| **TFLite Float16** | `yolo12n_seed0_best_float16.tflite` | 5.08 MB | **49%** vs FP32 | ~133 ms | Identical to .pt |
| **TFLite Dynamic INT8** | `yolo12n_seed0_best_dynamic_range_quant.tflite` | 2.83 MB | **72%** vs FP32 | ~194 ms | High (Pothole 0.35, Alligator 0.83) |
| **TFLite Full INT8** | `yolo12n_seed0_best_full_int8.tflite` | 2.90 MB | **71%** vs FP32 | ~241 ms | Needs large calibration set |

## 4. Detection Results Comparison (PyTorch vs TFLite)

### Test Image 1: `sample_pothole.jpg` (1024x1024)

| Model Variant | Detected Class | Confidence | Bounding Box [xmin, ymin, xmax, ymax] |
|---|---|---|---|
| **PyTorch .pt** | `pothole` | 0.2530 | `[633.83, 543.20, 728.48, 574.35]` |
| | `pothole` | 0.2470 | `[387.72, 566.33, 651.09, 643.56]` |
| **TFLite Float32** | `pothole` | 0.3663 | `[383.01, 566.20, 657.04, 645.50]` |
| | `pothole` | 0.1979 | `[633.90, 542.44, 726.49, 575.00]` |
| **TFLite Float16** | `pothole` | 0.3666 | `[382.98, 566.20, 657.13, 645.51]` |
| | `pothole` | 0.1976 | `[633.89, 542.43, 726.50, 575.00]` |
| **TFLite Dynamic INT8** | `pothole` | 0.3502 | `[382.16, 565.62, 657.38, 645.41]` |
| | `pothole` | 0.1790 | `[633.14, 542.20, 726.57, 574.97]` |

### Test Image 2: `sample_cracks.jpg` (1024x1024)

| Model Variant | Detected Class | Confidence | Bounding Box [xmin, ymin, xmax, ymax] |
|---|---|---|---|
| **PyTorch .pt** | `alligator_crack` | 0.8329 | `[175.57, 168.12, 1009.98, 970.92]` |
| | `alligator_crack` | 0.4134 | `[0.72, 119.04, 457.05, 354.71]` |
| **TFLite Float32** | `alligator_crack` | 0.8295 | `[171.70, 169.01, 1009.76, 971.07]` |
| | `alligator_crack` | 0.4202 | `[0.27, 121.16, 443.73, 358.52]` |
| **TFLite Float16** | `alligator_crack` | 0.8295 | `[171.69, 169.04, 1009.75, 971.05]` |
| | `alligator_crack` | 0.4200 | `[0.27, 121.14, 443.85, 358.51]` |
| **TFLite Dynamic INT8** | `alligator_crack` | 0.8274 | `[171.85, 168.96, 1010.05, 970.13]` |
| | `alligator_crack` | 0.4187 | `[0.51, 120.62, 446.12, 358.89]` |

## 5. Workspace Directory Structure

```text
ml/pretrained-model-test/
├── .gitignore
├── README.md
├── requirements.txt
├── download_model.py
├── export_tflite.py
├── test_model.py
├── test_tflite.py
├── test_images/
│   ├── sample_pothole.jpg
│   └── sample_cracks.jpg
├── models/
│   └── yolo-rdd2022-benchmark/
│       ├── yolo12n_seed0_best.pt
│       ├── yolo12n_seed0_best.onnx
│       ├── yolov8s_seed0_best.pt
│       └── tflite_export/
│           ├── yolo12n_seed0_best_float32.tflite
│           ├── yolo12n_seed0_best_float16.tflite
│           ├── yolo12n_seed0_best_dynamic_range_quant.tflite
│           └── yolo12n_seed0_best_full_int8.tflite
└── results/
    ├── yolo12n_seed0_best_dynamic_range_quant_sample_pothole_annotated.jpg
    ├── yolo12n_seed0_best_dynamic_range_quant_sample_cracks_annotated.jpg
    ├── yolo12n_seed0_best_float16_sample_pothole_annotated.jpg
    ├── yolo12n_seed0_best_float16_sample_cracks_annotated.jpg
    ├── yolo12n_seed0_best_float32_sample_pothole_annotated.jpg
    └── yolo12n_seed0_best_float32_sample_cracks_annotated.jpg
```

## 6. Execution Scripts

### 1. Download Model
```bash
python download_model.py --model yolo12n
```

### 2. Export ONNX & TFLite Quantized Models
```bash
python export_tflite.py
```

### 3. Run PyTorch Evaluation
```bash
python test_model.py --model yolo12n_seed0_best.pt --conf 0.15
```

### 4. Run TFLite Benchmarking & Evaluation
```bash
python test_tflite.py --conf 0.15
```
