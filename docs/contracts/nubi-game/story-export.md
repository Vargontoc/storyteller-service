<!--
Origen: specs/export-import-feature (REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, DEC-014, DEC-015)
Estado del contrato: contrastado con el código
-->

# Contrato del zip de exportación para Nubi Game — file/json

## 1. Propósito

Define el formato del archivo `.zip` que `storyteller-server` genera bajo demanda y sin persistir para la aplicación externa **Nubi Game** (`GET /api/v1/storyteller/{storyId}/export/nubi-game`).

Sustituye por completo a la exportación heredada basada en la convención posicional `page_N`: ahora el consumidor resuelve cada recurso exclusivamente mediante las rutas explícitas declaradas en `story.json` y recibe las imágenes originales sin convertir.

## 2. Participantes

| Participante | Rol | Responsabilidad |
|---|---|---|
| StoryTeller (`storyteller-server`) | Productor | Valida disponibilidad completa, compone `story.json` y empaqueta la imagen de portada y la imagen y audio de cada página interior en memoria. |
| Nubi Game | Consumidor | Lee `story.json` desde la raíz del zip, resuelve las rutas relativas declaradas y gestiona los formatos de imagen recibidos. |

## 3. Estructura del ZIP y transporte

- Entrega síncrona bajo demanda como descarga binaria (`application/octet-stream`, `story-{storyId}.zip`), generada en memoria sin persistirse en el servidor.
- Contenido del zip para un cuento de `N` páginas interiores:
  - `story.json` en la raíz (UTF-8, JSON válido).
  - `cover<ext>`: 1 fichero con la imagen importada de la portada (la portada **no** lleva audio).
  - Por cada página interior `n` (`1..N`, en orden de lectura):
    - `pages/<nnn>/image<ext>`: imagen importada de la página `n` (por ejemplo `pages/001/image.jpg`).
    - `pages/<nnn>/audio.wav`: audio narrado publicado de la página `n` (por ejemplo `pages/001/audio.wav`).

## 4. Mensaje `story.json`

### 4.1 Campos de la raíz

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `title` | string | Sí | Título del cuento. |
| `cover` | string | Sí | Ruta relativa a la raíz del zip de la imagen de portada. |
| `pages` | array | Sí | Lista ordenada (`1..N`) de páginas interiores; ver 4.2. |

### 4.2 Cada elemento de `pages`

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `page` | integer | Sí | Número de página interior (desde `1`, en orden `1..N`). |
| `text` | string | Sí | Texto narrativo aceptado de la página. |
| `image` | string | Sí | Ruta relativa a la raíz del zip de la imagen de la página. |
| `audio` | string | Sí | Ruta relativa a la raíz del zip del audio de la página. |

`story.json` **no** contiene ningún campo que declare el formato de las imágenes.

### 4.3 Reglas sobre rutas e imágenes

1. **Rutas explícitas y resolubles:** Todas las rutas (`cover`, `pages[].image`, `pages[].audio`) son relativas a la raíz del zip y apuntan a un fichero existente dentro del mismo zip. Nubi Game nunca deduce ficheros por nombre ni por posición: si se renombran los ficheros de imagen o audio y se actualizan sus rutas en `story.json`, la asociación sigue intacta.
2. **Imágenes originales sin modificar:** Cada imagen (portada y páginas interiores) se copia byte a byte exactamente tal como fue subida por el adulto, sin convertir su formato, redimensionar, reorientar, recomprimir ni eliminar metadatos.
3. **Extensión por formato real:** La extensión de cada fichero de imagen en el zip se determina inspeccionando su contenido real al exportar (por ejemplo `.png` para PNG, `.jpg` para JPEG, `.bmp` para BMP), con independencia del nombre o extensión con que se subió originalmente. El formato de imagen se identifica únicamente por esa extensión de fichero.

## 5. Ejemplo válido

```json
{
  "title": "La luna dormida",
  "cover": "cover.png",
  "pages": [
    {
      "page": 1,
      "text": "Nina mira la luna desde su ventana.",
      "image": "pages/001/image.jpg",
      "audio": "pages/001/audio.wav"
    },
    {
      "page": 2,
      "text": "La luna brilla sobre el jardín.",
      "image": "pages/002/image.png",
      "audio": "pages/002/audio.wav"
    }
  ]
}
```

## 6. Errores y rechazo de exportación

Si falta algún requisito de disponibilidad (portada sin aceptar o sin planteamiento en el guion, texto aceptado de alguna página interior, imagen de portada, o imagen o audio de alguna página interior), no se genera ni se entrega ningún zip. El endpoint REST `GET /api/v1/storyteller/{storyId}/export/nubi-game` responde HTTP `409` con la lista completa de faltas (`COVER_NOT_ACCEPTED`, `ACCEPTED_TEXT`, `IMAGE`, `AUDIO`) e indicando la portada (`page: 0`) o página afectada. El formato de las imágenes importadas nunca es motivo de rechazo.

## 7. Trazabilidad

| Requisito o decisión | Dónde se cumple |
|---|---|
| REQ-011 | Contenido del zip (`story.json`, 1 imagen de portada sin audio, 1 imagen y 1 audio por página) y extensión por formato real. |
| REQ-012 | Cabecera de `story.json` (`title` y `cover`). |
| REQ-013 | Array `pages` en orden de lectura con `page`, `text`, `image` y `audio`. |
| REQ-014, DEC-014 | Rutas relativas a la raíz explícitas y resolubles sin convención posicional. |
| REQ-015, DEC-015 | Copia byte a byte de las imágenes originales, extensión por contenido real y ausencia de campo de formato en `story.json`. |
