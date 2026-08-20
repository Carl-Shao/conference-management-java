FROM docker.xuanyuan.run/nvidia/cuda:12.4.0-runtime-ubuntu22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    libsndfile1 \
    git \
    && rm -rf /var/lib/apt/lists/*


# PyTorch CUDA 12.4
RUN pip3 install \
    torch==2.5.1 \
    torchaudio==2.5.1 \
    --index-url https://download.pytorch.org/whl/cu124

# FunASR
RUN pip3 install \
    funasr \
    modelscope \
    fastapi \
    "uvicorn[standard]" \
    python-multipart

WORKDIR /app

COPY server.py /app/server.py

EXPOSE 8000

CMD ["python3", "server.py"]