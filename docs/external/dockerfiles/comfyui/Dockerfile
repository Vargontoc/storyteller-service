FROM nvidia/cuda:12.1.1-runtime-ubuntu22.04

RUN apt update && apt install -y \
    python3 python3-pip git

WORKDIR /app

RUN git clone https://github.com/comfyanonymous/ComfyUI .

RUN pip install -r requirements.txt

# Nodo de recorte de fondo usado por las plantillas de personaje
RUN pip install --no-cache-dir rembg onnxruntime

CMD ["python3", "main.py", "--listen", "0.0.0.0", "--port", "8188"]