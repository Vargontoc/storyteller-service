FROM nvidia/cuda:12.1.1-runtime-ubuntu22.04

RUN apt update && apt install -y \
    python3 python3-pip git

WORKDIR /app

RUN git clone https://github.com/comfyanonymous/ComfyUI .

RUN pip install -r requirements.txt

CMD ["python3", "main.py", "--listen", "0.0.0.0", "--port", "8188"]