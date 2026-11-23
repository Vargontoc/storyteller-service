# Storyteller Agent App

## Description

Este proyecto es una aplicación web para generar cuentos infantiles ilustrados y narrados para niños de 3 a 4 años mediante agentes de IA locales (Ollama), generación de imágenes (ComfyUI) y narración de audio (Chatterbox). El adulto elige un tema del catálogo y un protagonista reutilizable, revisa cada etapa (personajes, guion con escaleta, portada y páginas) y acepta antes de continuar.

## Features

* **Catálogo de protagonistas reutilizables**: alta automática o manual desde una frase libre, ficha narrativa y visual, edición conversacional (con fork automático si el personaje ya se usa en cuentos), renombrado y generación asíncrona de 4 retratos candidatos sobre fondo transparente en ComfyUI con progreso por WebSocket.
* **Catálogo de temas**: catálogo estático en el frontend (`Aventuras`, `Rutinas`, `Animales`, `Naturaleza`, `Familia y amigos` y `Juegos`) orientado a 3-4 años y sin moraleja.
* **Guion con escaleta beat a beat**: el agente director genera título, sinopsis, reparto episódico ligero y una escaleta con un beat por página interior; el adulto puede revisarlo con un hint global o editar beats individuales antes de aprobar el guion.
* **Escritura secuencial por etapas**: con el guion aprobado, el agente escritor genera portada y páginas interiores de forma asíncrona (con eventos en tiempo real por WebSocket), admitiendo versiones, hints de revisión, avisos de longitud y aceptación paso a paso.
* **Generación de imágenes y audios**: generación mediante servicios externos para los retratos de personaje, escenas e imágenes importadas por página, así como audio narrado con Chatterbox.

## Arquitectura y documentación

El repositorio está organizado en tres módulos:

* `api/` (`storyteller-server`): Backend en Java 21 y Spring Boot 4.1.0 (Spring AI 2.0.0, arquitectura hexagonal, PostgreSQL con `pgvector`, Liquibase, API REST `/api/v1` y WebSocket `/storyteller`).
* `app/` (`storyteller-app`): Aplicación web SPA en Vue 3, Vite, TypeScript y Pinia.
* `agents/`: Modelfiles de referencia y script `load_model.ps1` para Ollama.

Documentación de referencia:

* [Contexto del proyecto](.sdd/project.md)
* [Mapa de arquitectura](.sdd/architecture.md)
* [Contrato OpenAPI (`/api/v1`)](docs/contracts/openapi/openapi.yaml)
* [Contrato WebSocket (`/storyteller`)](docs/contracts/websocket/websocket.md)

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
docker exec -it ollama ollama pull qwen3:14b
```

### Contenedor para ComfyUI

Crear un contenedor con `docs\external\dockerfiles\comfyui\Dockerfile`, que clona ComfyUI. El `CMD` del Dockerfile ya fija `--listen 0.0.0.0 --port 8188`, así que no hace falta pasar `CLI_ARGS`. Los volúmenes montan las subcarpetas de `core/` para persistir modelos, custom_nodes, input/output y los directorios de proyecto:

Hay que tener los siguientes modelos:

- `core\models\checkpoints\sd_xl_base_1.0.safetensors`
- `core\models\clip_vision\CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors`
- `core\models\ipadapter\ip-adapter-plus_sdxl_vit-h.safetensors`
- `core\models\loras\StorybookRedmondV2-KidsBook-KidsRedmAF.safetensors`
- `core\models\loras\CuteCartoonRedmond-CuteCartoon-CuteCartoonAF.safetensors`

Para generar personajes con fondo transparente, el entorno de ComfyUI necesita además:

- El nodo `Image Remove Background (rembg)` con selector de modelo (por ejemplo, `rembg-comfyui-node-better`). El nodo original sin selector también funciona, pero usa siempre `u2net` y no permite elegir `isnet-anime`.
- Los paquetes `rembg` y `onnxruntime` instalados en el mismo Python que ejecuta ComfyUI.
- El modelo `isnet-anime` en `~/.u2net` o en la ruta indicada por `U2NET_HOME`; se descarga automáticamente en el primer uso.

Si el contenedor ya está creado, se puede corregir sin reconstruirlo:

```bash
docker exec -it <contenedor-comfyui> python3 -m pip install rembg onnxruntime
docker restart <contenedor-comfyui>
```

Para que la dependencia sobreviva a una reconstrucción, añadir al Dockerfile:

```dockerfile
RUN pip install --no-cache-dir rembg onnxruntime
```

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

Capa de agentes en Ollama encargados de las distintas fases de generación y revisión.

### Requisitos

* Contenedor de [Ollama](#contenedor-ollama) levantado con los modelos base `qwen3:14b` (director, escritor y personaje) y `qwen2.5:14b-instruct` (narrador), además de `mxbai-embed-large` para embeddings.

### Aprovisionamiento automático desde el backend (`api/`)

Al arrancar `api/`, los provisioners comprueban si cada modelo existe en Ollama y, si falta, lo crean automáticamente desde el modelo base que corresponda: `qwen3:14b` para director, escritor y personaje, y `qwen2.5:14b-instruct` para el narrador (salvo que se desactive por variable de entorno):

* **Agente de personajes (`storyteller-character`)**: aprovisionado por `CharacterModelProvisioner` (`APP_AGENT_CHARACTER_PROVISIONING_ENABLED`, por defecto `true`), usando `APP_AGENT_CHARACTER_BASE_MODEL` (`qwen3:14b`) y las plantillas `api/src/main/resources/prompts/character_generate.st` y `character_review.st`.
* **Agente director (`storyteller-director`)**: aprovisionado por `DirectorModelProvisioner` (`APP_AGENT_DIRECTOR_PROVISIONING_ENABLED`, por defecto `true`) desde `api/src/main/resources/prompts/director.modelfile`. Genera el guion (título, sinopsis, reparto episódico y escaleta de beats), revisa la escaleta por hint global y valida ediciones directas de beats.
* **Agente escritor (`storyteller-writer`)**: aprovisionado por `WriterModelProvisioner` (`APP_AGENT_WRITER_PROVISIONING_ENABLED`, por defecto `true`) desde `api/src/main/resources/prompts/writer.modelfile`. Genera la escena de portada y el texto y escena de cada página siguiendo el beat aprobado.

### Carga manual en Ollama (`agents/`)

Para el agente narrador (o para cargar manualmente los Modelfiles de `agents/`):

#### Agente narrador

Agente generativo que recibe el cuento completo y establece un ritmo narrativo.

```bash
cd agents
./load_model.ps1 "storyteller-narrator" ".\narrator-story-agent"
```

También permanecen en `agents/` los Modelfiles de referencia `director-story-agent` y `writer-story-agent`, cargables de la misma forma con `load_model.ps1` si se desactiva el aprovisionamiento automático del backend.

## API

Capa backend (Java 21 / Spring Boot 4.1.0 / Spring AI 2.0.0) donde reside la lógica de negocio, la orquestación de agentes, la persistencia y la API REST/WebSocket. Crear `api/.env` a partir de `api/.env.example` y ajustar las variables necesarias (`SERVER_PORT`, `SPRING_DATASOURCE_URL`, `SPRING_AI_OLLAMA_BASE_URL`, `APP_CORS_*`, `CHATTERBOX_URL`, `COMFY_URL`, `APP_AGENT_*`).

> Nota de configuración: en `api/src/main/resources/application.yaml`, `CHATTERBOX_URL` toma por defecto `http://localhost:4123` (ajústalo a `http://localhost:5123` en `api/.env` si usas el mapeo `5123:5123` de Chatterbox), y las credenciales por defecto del datasource son `postgres` / `postgres`.

### Requisitos

* Java 21 y Maven (en `api/pom.xml` se referencia el repositorio GitHub Packages de `es.vargontoc.framework` y por defecto la dependencia `jave-nativebin-win64` para Windows).
* PostgreSQL con la extensión `pgvector` (migraciones gestionadas con Liquibase y vector store inicializado al arrancar).
* Los contenedores de [ComfyUI](#contenedor-para-comfyui), [Chatterbox](#contenedor-para-chatterbox) y [Ollama](#contenedor-ollama) levantados.

### Run

Para levantarla:

```bash
cd api
mvn spring-boot:run
```

La API REST queda expuesta bajo `/api/v1` (con documentación OpenAPI vía `springdoc-openapi-starter-webmvc-ui`) y los eventos en tiempo real bajo el endpoint WebSocket `/storyteller`.

## App

Aplicación web (Vue 3 + Vite + TypeScript + Pinia) donde reside la interfaz de usuario. Crear `app/.env` a partir de `app/.env.example` y configurar `VITE_API_BASE_URL`, `VITE_API_TIMEOUT_MS` y `APP_PORT` (por defecto `80` con `strictPort: true` en `app/vite.config.ts`).

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

Otros scripts disponibles en `app/package.json`: `npm run build` y `npm run test`.
