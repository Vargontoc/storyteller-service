<template>
  <section v-if="character" class="detail" aria-live="polite">
    <div class="heading"><h2>{{ character.name }}</h2><div class="badges"><span v-if="isPortraitUnknown(character)" class="badge unknown">Estado del retrato no disponible</span><span v-else-if="isDraft(character)" class="badge">Borrador</span><span v-if="'pendingReview' in character && character.pendingReview" class="badge pending">Revisión pendiente</span></div></div>
    <img v-if="hasApprovedPortrait(character)" class="approved-portrait" :src="approvedUrlFor(character)" :alt="`Retrato aprobado de ${character.name}`" @error="portraitUnavailable = true" />
    <dl><div><dt>Descripción narrativa</dt><dd>{{ character.narrativeDescription }}</dd></div><div><dt>Descripción visual</dt><dd>{{ character.visualDescription }}</dd></div></dl>
    <p v-if="locked" class="locked">Generando retratos. La ficha sigue disponible, pero no se puede modificar hasta que termine.</p>
    <form v-if="'id' in character" class="rename" @submit.prevent="rename"><label for="character-name">Cambiar nombre</label><div class="rename-row"><input id="character-name" v-model.trim="newName" :disabled="locked || renaming" maxlength="100" required /><button type="submit" class="btn" :disabled="locked || renaming || !newName || newName === character.name">{{ renaming ? 'Guardando...' : 'Guardar' }}</button></div><p v-if="renameError" class="error">{{ renameError }}</p></form>
    <character-review-panel v-if="'id' in character" :character-id="character.id" :locked="locked" @changed="forkCatalogCharacterId => emit('reviewed', forkCatalogCharacterId)" />
    <p v-else class="notice">La ficha se creó correctamente. Se actualizará al recuperar el catálogo.</p>
  </section>
  <section v-else class="placeholder">Selecciona un personaje para ver su ficha.</section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { approvedPortraitAssetUrl, renameCatalogCharacter, type CatalogCharacter } from '../api/characters'
import { ApiError } from '../api/httpClient'
import CharacterReviewPanel from './CharacterReviewPanel.vue'
interface CreatedCharacter { name: string; narrativeDescription: string; visualDescription: string }
const props = defineProps<{ character: CatalogCharacter | CreatedCharacter | null; locked?: boolean; portraitAssetVersion?: number }>()
const emit = defineEmits<{ renamed: [character: CatalogCharacter]; reviewed: [forkCatalogCharacterId?: number] }>()
const newName = ref(''); const renaming = ref(false); const renameError = ref<string | null>(null); const portraitUnavailable = ref(false)
function isPortraitUnknown(character: CatalogCharacter | CreatedCharacter) { return 'id' in character && (character.portraitStatus === 'UNKNOWN' || (character.portraitStatus === undefined && character.draft == null)) }
function isDraft(character: CatalogCharacter | CreatedCharacter) { return 'id' in character && (character.portraitStatus === 'DRAFT' || (character.portraitStatus === undefined && character.draft === true)) }
function hasApprovedPortrait(character: CatalogCharacter | CreatedCharacter) { return 'id' in character && character.portraitStatus === 'APPROVED' && !portraitUnavailable.value }
function approvedUrlFor(character: CatalogCharacter | CreatedCharacter) { return 'id' in character ? approvedPortraitAssetUrl(character.id, props.portraitAssetVersion ?? 0) : '' }
watch(() => props.character, (character) => { newName.value = character?.name ?? ''; renameError.value = null; portraitUnavailable.value = false }, { immediate: true })
watch(() => props.portraitAssetVersion, () => { portraitUnavailable.value = false })
async function rename() {
  if (!props.character || !('id' in props.character) || !newName.value || renaming.value) return
  renaming.value = true; renameError.value = null
  try { emit('renamed', await renameCatalogCharacter(props.character.id, newName.value)) } catch (error) { renameError.value = error instanceof ApiError ? error.message : 'No se pudo cambiar el nombre' } finally { renaming.value = false }
}
</script>

<style scoped>
.detail,.placeholder{padding:20px;border:1px solid var(--border);border-radius:10px}.heading,.badges,.rename-row{display:flex;align-items:center;gap:8px}.heading{justify-content:space-between}h2{margin:0;font-size:22px}.approved-portrait{display:block;width:min(260px,100%);aspect-ratio:1;object-fit:cover;margin:16px 0;border:1px solid var(--border);border-radius:8px;background:var(--border)}dl{display:grid;gap:16px;margin:20px 0}dt,label{font-size:14px;font-weight:600;color:var(--text-h)}dd{margin:4px 0 0;color:var(--text-h);white-space:pre-wrap}.badge{padding:3px 8px;font-size:12px;color:#92400e;background:#fef3c7;border-radius:999px}.unknown{color:var(--text);background:var(--border)}.pending{color:#1d4ed8;background:#dbeafe}.rename{display:grid;gap:8px}.rename-row input{flex:1;min-width:0;padding:8px;font:inherit;color:var(--text-h);background:var(--bg);border:1px solid var(--border);border-radius:6px}.btn{padding:8px 16px;font:inherit;color:var(--text-h);cursor:pointer;background:transparent;border:1px solid var(--border);border-radius:6px}.btn:disabled{cursor:not-allowed;opacity:.5}.error{margin:0;color:#e5484d}.notice{margin:0;color:var(--text)}.locked{margin:16px 0 0;padding:8px;color:#92400e;background:#fef3c7;border-radius:6px}
</style>
