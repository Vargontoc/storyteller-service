# Storyteller Agent App

## Description

Este proyecto es una aplicación web para generar cuentos mediante agentes de IA para niños de 3 a 4 años.

## Features

* **Generación de cuentos** : generacion de un cuento de manera secuencial y en tiempo real.
* **Gestión de cuentos** : gestión de los cuentos generados permitiendo al usuario revisar guión, descripción de personaejes y las escenas con previo aceptación del agente.
* **Generación de Imágenes** : generacion de imágenes mediante servicio externo para los personajes y cada una de las escenas.
* **Generación de Audios** : generacion de audios mediante servicio externo para la narración cada una de las escenas.

## Prerequisites

El sistema debe tener una RTX 4070 SUPER o superior con 12 GB de VRAM

Para la ejecución del proyecto es necesario tener instalado el siguiente software:

* Docker
* Docker Compose

### Contenedor Ollama

Crear un contenedor con el siguiente formato de servicio, usando la imagen oficial:

```yaml
  ollama:
    image: ollama/ollama
    container_name: ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [ gpu ]
```

Con el contenedor levantado, descargar los modelos necesarios:

```bash
docker exec -it ollama ollama pull mxbai-embed-large
docker exec -it ollama ollama pull qwen2.5:14b-instruct
```

### Contenedor para ComfyUI

Crear un contenedor con `docs\external\dockerfiles\comfyui\Dockerfile`, que clona ComfyUI. El `CMD` del Dockerfile ya fija `--listen 0.0.0.0 --port 8188`, así que no hace falta pasar `CLI_ARGS`. Los volúmenes montan las subcarpetas de `core/` para persistir modelos, custom_nodes, input/output y los directorios de proyecto:

Hay que tener los siguientes modelos:

- `core\models\checkpoints\sd_xl_base_1.0.safetensors`
- `core\models\clip_vision\CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors`
- `core\models\ipadapter\ip-adapter-plus_sdxl_vit-h.safetensors`
- `core\models\loras\StorybookRedmondV2-KidsBook-KidsRedmAF.safetensors`
- `core\models\loras\CuteCartoonRedmond-CuteCartoon-CuteCartoonAF.safetensors`

Crear el contenedor con el siguiente formato de servicio:

```yaml
  comfyui:
    build:
      context: <directorio del Dockerfile>
      dockerfile: Dockerfile
    container_name: <nombre contenedor>
    volumes:
      - <directorio del Dockerfile>/core/models:/app/models
      - <directorio del Dockerfile>/core/input:/app/input
      - <directorio del Dockerfile>/core/output:/app/output
      - <directorio del Dockerfile>/core/custom_nodes:/app/custom_nodes
      - <directorio del Dockerfile>/projects:/app/projects
      - <directorio del Dockerfile>/shared:/app/shared
```

### Contenedor para Chatterbox

Crear el contenedor con el siguiente formato de servicio:

```yaml
  chatterbox:
    image: travisvn/chatterbox-tts-api
    container_name: chatterbox
    restart: unless-stopped
    ports:
      - "5123:5123"
    environment:
      - DEVICE=cuda
      - EXAGGERATION=0.5
      - CFG_WEIGHT=0.5
      - TEMPERATURE=0.8
      - MAX_CHUNK_LENGTH=280
    volumes:
      - chatterbox-models:/cache
      - chatterbox-voices:/voices
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [ gpu ]
```

## Agents

Capa de agentes donde reside la configuración de los distintos agentes encargados de la generación de los cuentos.

### Requisitos

* Contendor de [Ollama](#contenedor-ollama) levantado.

#### Agente Topics

Agente generativo que genera distintos temas apropiados para los cuentos

Para instalarlo en ollama

```bash
cd agents
load_model.ps1 "storyteller-topics" ".\topic-story-agent"
```

#### Agente director

Agente generativo que genera el guión, personajes del cuento y tambien evalua la revisión del usuario

Para instalarlo en ollama

```bash
cd agents
load_model.ps1 "storyteller-director" ".\director-story-agent"
```

#### Agente writer

Agente generativo que genera las distintas paginas con su escena y texto

Para instalarlo en ollama

```bash
cd agents
load_model.ps1 "storyteller-writer" ".\writer-story-agent"
```

#### Agente narrador

Agente generativo que recibe el cuento completo y establece un ritmo narrativo

Para instalarlo en ollama

```bash
cd agents
load_model.ps1 "storyteller-narrator" ".\narrator-story-agent"
```


## API

Capa backend (Spring Boot) donde reside la lógica de negocio y la comunicación con los agentes de IA para la generación de cuentos. Crear `api/.env` (ver `api/.env.example` para la configuracion de las distintas variables, aplicar los cambios convenientes).

### Requisitos

* Java (Spring Boot)
* PostgreSQL con la extensión `pgvector` (usada como vector store para embeddings)
* Los contenedores de [ComfyUI](#contenedor-para-comfyui), [Chatterbox](#contenedor-para-chatterbox) y [Ollama](#contenedor-ollama) levantados


### Run
Para levantarla:

```bash
cd api
mvn spring-boot:run
```


## App

Aplicación web (Vue 3 + Vite + Pinia) donde reside la lógica de la aplicación visual. Crear `app/.env` (ver `app/.env.example` para la configuración de las distintas variables, aplicar los cambios convenientes).

### Requisitos

* Node.js
* [API](#api) levantada

### Run

Para levantarla en modo desarrollo:

```bash
cd app
npm install
npm run dev
```
