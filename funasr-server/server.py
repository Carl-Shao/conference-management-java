import os
import tempfile

from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from funasr import AutoModel

app = FastAPI(title="FunASR Server", version="1.0")

ASR_MODEL_DIR = "/app/models/paraformer-zh"
PUNC_MODEL_DIR = "/app/models/ct-punc"

print("[INFO] Loading Paraformer ASR model...")

asr_model = AutoModel(
    model=ASR_MODEL_DIR,
    device="cuda:0",
    disable_update=True
)

print("[INFO] Paraformer ASR model loaded successfully.")

print("[INFO] Loading CT-Punc model...")

punc_model = AutoModel(
    model=PUNC_MODEL_DIR,
    device="cuda:0",
    disable_update=True
)

print("[INFO] CT-Punc model loaded successfully.")


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "asr_model": "paraformer-zh",
        "punctuation": True,
        "timestamp": True
    }


@app.post("/v1/audio/transcriptions")
async def transcribe(
    file: UploadFile = File(...)
):

    suffix = os.path.splitext(file.filename or ".wav")[1] or ".wav"

    tmp_path = None

    try:
        # 保存音频
        with tempfile.NamedTemporaryFile(
            delete=False,
            suffix=suffix
        ) as tmp:

            content = await file.read()
            tmp.write(content)
            tmp_path = tmp.name

        print(
            f"[INFO] ASR request: "
            f"file={file.filename}"
        )

        # ==========================================
        # 1. Paraformer ASR + 时间戳
        # ==========================================

        results = asr_model.generate(
            input=tmp_path,
            batch_size_s=300,
            is_final=True,
            pred_timestamp=True
        )

        print("========== ASR RAW RESULT ==========")
        print(results)
        print("====================================")

        if not results:
            return JSONResponse({
                "code": 0,
                "text": "",
                "raw_text": "",
                "timestamp": [],
                "sentence_info": []
            })

        result = results[0]

        raw_text = result.get("text", "")

        timestamp = result.get(
            "timestamp",
            []
        )

        print("========== TIMESTAMP ==========")
        print(timestamp)
        print("================================")

        # ==========================================
        # 2. CT-Punc
        # ==========================================

        punc_results = punc_model.generate(
            input=raw_text
        )

        print("========== PUNC RESULT ==========")
        print(punc_results)
        print("=================================")

        text_with_punc = raw_text

        if punc_results:

            punc_result = punc_results[0]

            if isinstance(punc_result, dict):
                text_with_punc = punc_result.get(
                    "text",
                    raw_text
                )

            elif isinstance(punc_result, str):
                text_with_punc = punc_result

        # ==========================================
        # 3. 最终返回
        # ==========================================

        response = {
            "code": 0,
            "text": text_with_punc,
            "raw_text": raw_text,
            "timestamp": timestamp,
            "sentence_info": [],
            "language": "zh"
        }

        print("========== FINAL RESULT ==========")
        print(response)
        print("==================================")

        return JSONResponse(response)

    except Exception as e:

        print(
            "[ERROR] ASR ERROR:",
            repr(e)
        )

        return JSONResponse(
            status_code=500,
            content={
                "code": 1,
                "msg": str(e),
                "text": "",
                "raw_text": "",
                "timestamp": [],
                "sentence_info": []
            }
        )

    finally:

        if tmp_path and os.path.exists(tmp_path):
            os.remove(tmp_path)


if __name__ == "__main__":

    import uvicorn

    print(
        "[INFO] FunASR HTTP server starting "
        "on 0.0.0.0:8898"
    )

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8898,
        workers=1
    )