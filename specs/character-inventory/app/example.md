# Ejemplos: character-inventory - app

## CharacterCatalogView

**Ruta:** `app/src/views/CharacterCatalogView.vue`
**Para qué sirve:** Muestra el listado del catálogo, la ficha seleccionada y el acceso a la creación de personajes.

```vue
<script setup lang="ts">
import CharacterCatalogView from './views/CharacterCatalogView.vue'
</script>

<template>
  <CharacterCatalogView />
</template>
```

**Notas:** Consume el catálogo REST. Cuando el servidor informa `portraitStatus: "UNKNOWN"`, mantiene la ficha disponible y muestra que el estado del retrato no está disponible.

## CharacterCatalogDetail

**Ruta:** `app/src/components/CharacterCatalogDetail.vue`
**Para qué sirve:** Presenta las descripciones de una ficha, sus indicadores de borrador o revisión pendiente y permite renombrarla.

```vue
<script setup lang="ts">
import CharacterCatalogDetail from './components/CharacterCatalogDetail.vue'
import type { CatalogCharacter } from './api/characters'

const character: CatalogCharacter = {
  id: 1,
  name: 'Pepe',
  narrativeDescription: 'Un conejo alegre.',
  visualDescription: 'Conejo blanco con orejas largas.',
  draft: null,
  portraitStatus: 'UNKNOWN',
  pendingReview: false,
}
</script>

<template>
  <CharacterCatalogDetail :character="character" @renamed="console.log" />
</template>
```

## CharacterFormModal

**Ruta:** `app/src/components/CharacterFormModal.vue`
**Para qué sirve:** Permite solicitar una ficha automática o enviar una descripción manual, y muestra el motivo de rechazo sin añadir una ficha.

```vue
<script setup lang="ts">
import { ref } from 'vue'
import CharacterFormModal from './components/CharacterFormModal.vue'

const open = ref(true)
</script>

<template>
  <CharacterFormModal :open="open" @close="open = false" @created="open = false" />
</template>
```

## CharacterReviewPanel

**Ruta:** `app/src/components/CharacterReviewPanel.vue`
**Para qué sirve:** Solicita una revisión textual, muestra la propuesta pendiente y permite aceptarla o descartarla.

```vue
<script setup lang="ts">
import CharacterReviewPanel from './components/CharacterReviewPanel.vue'
</script>

<template>
  <CharacterReviewPanel :character-id="1" @changed="console.log('revisión actualizada')" />
</template>
```

**Notas:** Solicita confirmación antes de sustituir una revisión pendiente y muestra solo los avisos visuales devueltos por el servidor.
