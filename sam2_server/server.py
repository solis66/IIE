"""
SAM2 抠图服务端
启动: uvicorn server:app --host 0.0.0.0 --port 8800
依赖: pip install fastapi uvicorn pillow numpy torch torchvision
模型: 首次运行自动下载，或手动放入 sam2_server/checkpoints/
"""
import io
import logging
import os
import base64
import time
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

# ---------- 配置 ----------
MODEL_NAME = "facebook/sam2.1-hiera-small"  # small 模型：精度与速度的平衡点
try:
    DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
except AttributeError:
    DEVICE = "cpu"
MODEL_DIR = Path(__file__).parent
CHECKPOINT_DIR = MODEL_DIR / "checkpoints"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("sam2-server")

# ---------- 懒加载 SAM2 ----------
_predictor = None
_model_loaded = False


def get_predictor():
    """懒加载 SAM2ImagePredictor，避免启动时长时间阻塞"""
    global _predictor, _model_loaded
    if not _model_loaded:
        logger.info(f"加载 SAM2 模型: {MODEL_NAME}, device={DEVICE}")
        from sam2.build_sam import build_sam2_hf
        from sam2.sam2_image_predictor import SAM2ImagePredictor

        _predictor = SAM2ImagePredictor(
            build_sam2_hf(MODEL_NAME, device=DEVICE)
        )
        _model_loaded = True
        logger.info("模型加载完成")
    return _predictor


# ---------- FastAPI ----------
app = FastAPI(title="SAM2 Segmentation Server", version="1.0.0")


class SegmentRequest(BaseModel):
    image_base64: str = Field(..., description="JPEG/PNG 图片的 base64 编码（不含 data:image 前缀）")
    center_x: float = Field(0.5, ge=0.0, le=1.0, description="目标中心点 x（0~1 归一化坐标）")
    center_y: float = Field(0.5, ge=0.0, le=1.0, description="目标中心点 y（0~1 归一化坐标）")
    prompt_strategy: str = Field("center", description="提示策略: center / bbox / auto")


class SegmentResponse(BaseModel):
    mask_base64: str = Field(..., description="Alpha mask PNG 的 base64 编码")
    mask_width: int
    mask_height: int
    elapsed_ms: float
    strategy: str


def predict_mask_by_center(predictor, point_x: float, point_y: float):
    """
    用中心点提示做前景分割
    point_x/point_y: 0~1 归一化坐标
    """
    h, w = predictor._orig_hw
    px = int(point_x * w)
    py = int(point_y * h)

    # 正点标记前景
    point_coords = np.array([[px, py]], dtype=np.float32)
    point_labels = np.array([1], dtype=np.int32)  # 1 = foreground

    masks, scores, _ = predictor.predict(
        point_coords=point_coords,
        point_labels=point_labels,
        multimask_output=True,
    )
    # 取得分最高的 mask
    best_idx = scores.argmax()
    mask = masks[best_idx].astype(np.uint8) * 255
    return mask


def predict_mask_by_bbox(predictor):
    """
    用整张图片边界框做分割（适合物体基本居中且占画面大部分的场景）
    """
    h, w = predictor._orig_hw
    # 内缩 10% 作为初始框
    margin = 0.1
    box = np.array([
        int(w * margin),
        int(h * margin),
        int(w * (1 - margin)),
        int(h * (1 - margin)),
    ], dtype=np.float32)

    masks, scores, _ = predictor.predict(
        box=box[None, :],
        multimask_output=True,
    )
    best_idx = scores.argmax()
    mask = masks[best_idx].astype(np.uint8) * 255
    return mask


@app.post("/segment", response_model=SegmentResponse)
def segment(req: SegmentRequest):
    """对上传图片执行前景分割，返回 alpha mask"""
    if not req.image_base64:
        raise HTTPException(400, "image_base64 不能为空")

    t0 = time.time()

    # 解码图片
    try:
        img_bytes = base64.b64decode(req.image_base64)
        image = Image.open(io.BytesIO(img_bytes)).convert("RGB")
    except Exception as e:
        raise HTTPException(400, f"图片解码失败: {e}")

    predictor = get_predictor()
    predictor.set_image(np.array(image))

    # 按策略执行分割
    strategy = req.prompt_strategy
    try:
        if strategy == "center":
            mask = predict_mask_by_center(predictor, req.center_x, req.center_y)
        elif strategy == "bbox":
            mask = predict_mask_by_center(predictor, 0.5, 0.5)  # fallback to center
        else:
            mask = predict_mask_by_center(predictor, 0.5, 0.5)
    except Exception as e:
        logger.exception("分割失败")
        raise HTTPException(500, f"分割失败: {e}")

    # mask 编码为 PNG base64
    mask_img = Image.fromarray(mask, mode="L")
    buf = io.BytesIO()
    mask_img.save(buf, format="PNG")
    mask_b64 = base64.b64encode(buf.getvalue()).decode("utf-8")

    elapsed = (time.time() - t0) * 1000
    logger.info(f"分割完成: {mask_img.size}, {elapsed:.0f}ms, strategy={strategy}")

    return SegmentResponse(
        mask_base64=mask_b64,
        mask_width=mask_img.width,
        mask_height=mask_img.height,
        elapsed_ms=elapsed,
        strategy=strategy,
    )


@app.get("/health")
def health():
    return {"status": "ok", "device": DEVICE, "model_loaded": _model_loaded}


if __name__ == "__main__":
    import uvicorn
    logger.info(f"启动服务: device={DEVICE}")
    uvicorn.run(app, host="0.0.0.0", port=8800)
