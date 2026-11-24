<!--
Origen: specs/definicion-artistica (REQ-009, ALT-001, DEC-006)
Estado del contrato: diseño pendiente de implementación
-->

# Contrato `story.json` — file/json

## 1. Propósito

Define la entrada de un cuento al workflow de producción visual: el texto y las escenas que
se van a ilustrar, la paleta aplicable y el personaje protagonista con su imagen de fondo
transparente.

Este contrato formaliza la estructura lógica de
`docs/visual-production-guidelines.md#contratos-v0` (sección 7.1). No describe todavía una
interfaz contrastada con código: en este repositorio no existe implementación del validador.
El estado pasará a `contrastado con el código` cuando el piloto implemente la validación y se
compare con ella.

## 2. Participantes

| Participante | Rol | Responsabilidad |
|---|---|---|
| StoryTeller | Productor | Genera `story.json` y la imagen del personaje a partir del cuento. |
| Visual Production Agent | Consumidor | Valida la entrada y rechaza el cuento si no cumple este contrato. |
| Producción visual | Consumidor interno | Usa las escenas validadas para generar los assets. |

## 3. Transporte

- Un fichero `story.json` por cuento.
- Codificación UTF-8 sin BOM.
- JSON válido según RFC 8259; sin comentarios, sin comas finales y sin claves duplicadas.
- Ubicación: `stories/<story-id>/input/story.json`.
- La imagen del personaje se referencia desde `input/` con una ruta relativa; el contrato no
  admite rutas absolutas ni `..`.

## 4. Mensaje de entrada

### 4.1 Campos de la raíz

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `schemaVersion` | string | Sí | Debe ser `"0.1"`. |
| `storyId` | string | Sí | Minúsculas ASCII, dígitos y guiones; 3-40 caracteres; estable durante todo el cuento. |
| `title` | string | Sí | Entre 1 y 120 caracteres. |
| `language` | string | Sí | Código BCP 47, por ejemplo `es`. |
| `ageRange` | string | Sí | Debe ser `"3-4"`. |
| `style` | object | Sí | Paleta base o paleta propia; ver 4.2. |
| `characters` | array | Sí | Al menos un personaje y exactamente uno con rol `protagonist`. |
| `scenes` | array | Sí | Al menos una escena `cover` y al menos una `scene`. |

### 4.2 Objeto `style`

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `paletteId` | string | Sí | `base` o el identificador de la paleta propia. |
| `paletteSource` | string | Sí | `base` o `own`. |
| `colors` | array | Condicional | Obligatorio si `paletteSource` es `own`; entre 5 y 9 elementos. |

Cada elemento de `colors`:

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `token` | string | Sí | Único dentro de la paleta. |
| `hex` | string | Sí | `#RRGGBB` en mayúsculas o minúsculas. |
| `role` | string | Sí | Uso previsto: línea, fondo, acento, agua, etc. |

Si `paletteSource` es `base`, `colors` se omite y la paleta se toma de la guía
(`docs/visual-production-guidelines.md#paletas`).

### 4.3 Objeto `characters`

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | string | Sí | Slug único dentro del cuento. |
| `name` | string | Sí | Nombre visible del personaje. |
| `role` | string | Sí | `protagonist` o `supporting`. |
| `image` | string | Sí | Ruta relativa a `input/` que apunta a un PNG con alfa. |
| `immutableTraits` | array de string | Sí | Entre 1 y 5 rasgos que no deben cambiar entre escenas. |

La imagen del personaje se valida con estas reglas:

- Formato real PNG, comprobado en la cabecera del archivo y no solo en la extensión.
- Canal alfa presente y fondo transparente real; no se acepta fondo blanco, negro ni damero
  pintado.
- Resolución mínima 1024 × 1024 px.
- Relación de aspecto 1:1.
- La ruta resuelve dentro de `stories/<story-id>/input/` y el archivo existe.

### 4.4 Objeto `scenes`

| Campo | Tipo | Obligatorio | Regla |
|---|---|---|---|
| `id` | string | Sí | Slug único dentro del cuento. |
| `type` | string | Sí | `cover` o `scene`. |
| `text` | string | Sí | Texto que se ilustra; no vacío. |
| `visualDescription` | string | Sí | Descripción visual de la escena; no vacía. |
| `characters` | array de string | Sí | IDs existentes en `characters`; puede estar vacío si no aparece ninguno. |
| `setting` | string | Sí | Lugar o ambiente de la escena. |
| `emotion` | string | Sí | Emoción dominante, por ejemplo `calma`, `curiosidad` o `alegría`. |

La primera escena de tipo `cover` es la portada del cuento. Nada impide que existan más
escenas `cover`, pero cada cuento debe tener al menos una.

### 4.5 Ejemplo válido

```json
{
  "schemaVersion": "0.1",
  "storyId": "la-luna-dormida",
  "title": "La luna dormida",
  "language": "es",
  "ageRange": "3-4",
  "style": {
    "paletteId": "noche-serena",
    "paletteSource": "own",
    "colors": [
      { "token": "ink", "hex": "#1F2A44", "role": "line" },
      { "token": "cream", "hex": "#FFF4D6", "role": "light" },
      { "token": "sky", "hex": "#2E4A7D", "role": "background" },
      { "token": "sun", "hex": "#F6C85F", "role": "accent" },
      { "token": "coral", "hex": "#FF8A65", "role": "accent" },
      { "token": "plum", "hex": "#6A4C93", "role": "shadow" },
      { "token": "water", "hex": "#3D6F8E", "role": "water" }
    ]
  },
  "characters": [
    {
      "id": "nina",
      "name": "Nina",
      "role": "protagonist",
      "image": "characters/nina/character.png",
      "immutableTraits": ["vestido amarillo", "pelo rizado", "ojos grandes"]
    }
  ],
  "scenes": [
    {
      "id": "cover-01",
      "type": "cover",
      "text": "La luna dormida",
      "visualDescription": "Nina mira la luna desde la ventana de su habitación.",
      "characters": ["nina"],
      "setting": "habitación al anochecer",
      "emotion": "calma"
    },
    {
      "id": "scene-01",
      "type": "scene",
      "text": "Nina saluda a la luna.",
      "visualDescription": "Nina está en el jardín, bajo una luna grande y redonda.",
      "characters": ["nina"],
      "setting": "jardín de noche",
      "emotion": "curiosidad"
    }
  ]
}
```

## 5. Validación

El validador comprueba, en este orden:

1. El fichero existe, es UTF-8 sin BOM y es JSON válido.
2. `schemaVersion` es compatible.
3. Los campos obligatorios de la raíz están presentes y son del tipo declarado.
4. La paleta es coherente: `paletteSource` conocido y `colors` válido cuando es propia.
5. Hay personajes y exactamente un `protagonist`.
6. Cada imagen de personaje existe y cumple PNG, alfa, tamaño y proporción.
7. Hay escenas y al menos una portada.
8. Cada escena referencia personajes existentes y tiene texto, descripción y emoción.

No se detiene en el primer error: reporta todos los que pueda comprobar en una sola pasada.

## 6. Respuestas

### 6.1 Entrada aceptada

```json
{
  "valid": true,
  "errors": []
}
```

Con `valid: true`, el cuento puede entrar al workflow. La aceptación no genera todavía ningún
asset ni manifiesto.

### 6.2 Entrada rechazada

```json
{
  "valid": false,
  "errors": [
    {
      "code": "CHARACTER_IMAGE_NO_ALPHA",
      "path": "$.characters[0].image",
      "message": "La imagen del personaje debe ser PNG con canal alfa."
    }
  ]
}
```

Reglas de la respuesta:

- `valid` es `true` solo si `errors` está vacío.
- Cada error declara `code` estable, `path` orientativo en notación JSONPath y `message`
  legible.
- `path` apunta al campo concreto cuando el error es de un elemento de un array.
- Una entrada inválida no produce assets ni `manifest.json`.

## 7. Errores

| Código | Condición | Ruta orientativa |
|---|---|---|
| `STORY_SCHEMA_VERSION_UNSUPPORTED` | `schemaVersion` ausente o distinto de `"0.1"`. | `$.schemaVersion` |
| `STORY_ID_INVALID` | `storyId` no cumple el patrón o no coincide con el directorio del cuento. | `$.storyId` |
| `STORY_TITLE_MISSING` | `title` vacío o ausente. | `$.title` |
| `STORY_LANGUAGE_INVALID` | `language` no es un código BCP 47 válido. | `$.language` |
| `STORY_AGE_RANGE_INVALID` | `ageRange` no es `"3-4"`. | `$.ageRange` |
| `STORY_STYLE_INVALID` | `style` ausente o con campos incoherentes. | `$.style` |
| `PALETTE_COLORS_INVALID` | Paleta propia sin `colors`, con menos de 5 o más de 9, o con hex inválido. | `$.style.colors` |
| `STORY_CHARACTERS_MISSING` | No hay personajes o no hay exactamente un `protagonist`. | `$.characters` |
| `CHARACTER_ID_DUPLICATE` | Dos personajes comparten `id`. | `$.characters[i].id` |
| `CHARACTER_ROLE_INVALID` | `role` distinto de `protagonist` o `supporting`. | `$.characters[i].role` |
| `CHARACTER_IMAGE_MISSING` | La ruta de la imagen no existe o sale de `input/`. | `$.characters[i].image` |
| `CHARACTER_IMAGE_NOT_PNG` | El archivo no es PNG real. | `$.characters[i].image` |
| `CHARACTER_IMAGE_NO_ALPHA` | El PNG no tiene canal alfa o el fondo no es transparente. | `$.characters[i].image` |
| `CHARACTER_IMAGE_TOO_SMALL` | Mide menos de 1024 × 1024 px. | `$.characters[i].image` |
| `CHARACTER_IMAGE_NOT_SQUARE` | La relación de aspecto no es 1:1. | `$.characters[i].image` |
| `STORY_SCENES_MISSING` | No hay escenas o falta la portada. | `$.scenes` |
| `SCENE_ID_DUPLICATE` | Dos escenas comparten `id`. | `$.scenes[i].id` |
| `SCENE_TYPE_INVALID` | `type` distinto de `cover` o `scene`. | `$.scenes[i].type` |
| `SCENE_TEXT_MISSING` | `text` vacío o ausente. | `$.scenes[i].text` |
| `SCENE_DESCRIPTION_MISSING` | `visualDescription` vacío o ausente. | `$.scenes[i].visualDescription` |
| `SCENE_CHARACTER_UNKNOWN` | La escena referencia un `id` que no existe. | `$.scenes[i].characters` |
| `SCENE_EMOTION_MISSING` | `emotion` vacío o ausente. | `$.scenes[i].emotion` |

Cualquiera de estos errores rechaza la entrada y bloquea el cuento hasta que se corrija.

## 8. Compatibilidad y cambios

- `schemaVersion` identifica el contrato; un cambio incompatible incrementa la versión y
  exige actualizar este documento y la sección 7.1 de la guía.
- Añadir un campo opcional es compatible dentro de la misma versión.
- Cambiar un campo obligatorio, su tipo o una regla de validación es incompatible.
- El validador ignora campos desconocidos solo si son opcionales y no alteran una regla
  vigente; en caso contrario, se rechaza la entrada.

## 9. Trazabilidad

| Requisito o decisión | Dónde se cumple |
|---|---|
| REQ-009 | Campos obligatorios, escenas, texto y personaje con fondo transparente. |
| ALT-001 | Rechazo de la entrada con el detalle de campos y validaciones fallidas. |
| DEC-006 | Contrato de entrada en JSON v0. |
| DEC-002, DEC-003, DEC-005 | Paleta en `style`, formato del personaje y ruta en `input/`. |