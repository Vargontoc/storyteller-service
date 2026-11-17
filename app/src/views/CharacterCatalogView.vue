<template>
  <section class="catalog">
    <header class="toolbar"><h1>Catálogo de personajes</h1><button type="button" class="primary" @click="formOpen = true">Crear personaje</button></header>
    <p v-if="loading">Cargando personajes...</p>
    <div v-else-if="error" class="load-error"><p>{{ error }}</p><button type="button" class="btn" @click="loadCatalog">Reintentar</button></div>
    <div v-else class="catalog-body">
      <aside aria-label="Personajes del catálogo"><p v-if="characters.length === 0" class="empty">No hay personajes todavía.</p><button v-for="character in characters" :key="character.id" type="button" class="character-row" :class="{ selected: selectedId === character.id }" @click="select(character.id)"><span>{{ character.name }}</span><span v-if="isPortraitUnknown(character)" class="badge unknown">Retrato no disponible</span><span v-else-if="isDraft(character)" class="badge">Borrador</span><span v-if="character.pendingReview" class="badge pending">Revisión pendiente</span></button></aside>
      <div>
        <p v-if="detailError" class="load-error">{{ detailError }} <button type="button" class="link" @click="retryDetail">Reintentar</button></p>
        <character-catalog-detail :character="selected" :locked="portraitRunning" :portrait-asset-version="portraitAssetVersion" @renamed="onRenamed" @reviewed="refreshAfterReview" />
        <section v-if="selected && 'id' in selected" class="portrait-actions">
          <p v-if="portraitRunning" class="running">Generando cuatro retratos. El estado se actualizará cuando el servidor termine.</p>
          <p v-else-if="portraitError" class="load-error">{{ portraitError }}</p>
          <button type="button" class="primary" :disabled="portraitBusy || portraitRunning" @click="generate">{{ candidates.length === 4 ? 'Regenerar retratos' : 'Generar retratos' }}</button>
          <portrait-candidate-gallery v-if="candidates.length === 4 || galleryInvalidated" :character-id="selected.id" :name="selected.name" :candidates="candidates" :locked="portraitRunning" :busy="portraitBusy" :invalidated="galleryInvalidated" @generate="generate" @select="selectCandidate" />
        </section>
      </div>
    </div>
    <character-form-modal :open="formOpen" @close="formOpen = false" @created="onCreated" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import CharacterCatalogDetail from '../components/CharacterCatalogDetail.vue'
import CharacterFormModal from '../components/CharacterFormModal.vue'
import PortraitCandidateGallery from '../components/PortraitCandidateGallery.vue'
import { generateCatalogCharacterPortraits, getCatalogCharacter, getCatalogCharacters, getPortraitCandidates, isPortraitGenerationRunning, selectPortraitCandidate, type CatalogCharacter, type CatalogCharacterCreation, type PortraitCandidate } from '../api/characters'
import { ApiError } from '../api/httpClient'
import { useToastStore } from '../stores/toast'
import { useWebsocketStore } from '../stores/websocket'

type CreatedCharacter = Pick<CatalogCharacter, 'name' | 'narrativeDescription' | 'visualDescription'>
const toastStore = useToastStore(); const websocketStore = useWebsocketStore()
const characters = ref<CatalogCharacter[]>([]); const selected = ref<CatalogCharacter | CreatedCharacter | null>(null); const selectedId = ref<number | null>(null)
const loading = ref(false); const error = ref<string | null>(null); const detailError = ref<string | null>(null); const formOpen = ref(false)
const candidates = ref<PortraitCandidate[]>([]); const portraitRunning = ref(false); const portraitBusy = ref(false); const portraitError = ref<string | null>(null); const galleryInvalidated = ref(false); const portraitAssetVersion = ref(0)
let portraitRequest = 0
const message = (reason: unknown, fallback: string) => reason instanceof ApiError ? reason.message : fallback
function isPortraitUnknown(character: CatalogCharacter) { return character.portraitStatus === 'UNKNOWN' || (character.portraitStatus === undefined && character.draft == null) }
function isDraft(character: CatalogCharacter) { return character.portraitStatus === 'DRAFT' || (character.portraitStatus === undefined && character.draft === true) }
async function loadCatalog(): Promise<CatalogCharacter[] | null> { loading.value = true; error.value = null; try { const result = await getCatalogCharacters(); characters.value = result; return result } catch (reason) { error.value = message(reason, 'No se pudo cargar el catálogo'); return null } finally { loading.value = false } }
async function refreshPortrait(id: number) { const request = ++portraitRequest; try { const [running, currentCandidates] = await Promise.all([isPortraitGenerationRunning(id), getPortraitCandidates(id)]); if (request !== portraitRequest || selectedId.value !== id) return; portraitRunning.value = running; candidates.value = currentCandidates.length === 4 ? currentCandidates : []; galleryInvalidated.value = false; portraitError.value = null } catch (reason) { if (request === portraitRequest && selectedId.value === id) portraitError.value = message(reason, 'No se pudo actualizar el estado de retratos') } }
async function select(id: number) { selectedId.value = id; detailError.value = null; candidates.value = []; portraitError.value = null; galleryInvalidated.value = false; try { selected.value = await getCatalogCharacter(id); portraitAssetVersion.value += 1; await refreshPortrait(id) } catch (reason) { detailError.value = message(reason, 'No se pudo cargar la ficha') } }
async function retryDetail() { if (selectedId.value !== null) await select(selectedId.value) }
async function generate() { if (selectedId.value === null || portraitBusy.value || portraitRunning.value) return; portraitBusy.value = true; portraitError.value = null; galleryInvalidated.value = true; candidates.value = []; try { await generateCatalogCharacterPortraits(selectedId.value); await refreshPortrait(selectedId.value) } catch (reason) { portraitError.value = message(reason, 'No se pudo iniciar la generación de retratos'); galleryInvalidated.value = false } finally { portraitBusy.value = false } }
async function selectCandidate(candidateId: number) { if (selectedId.value === null || portraitBusy.value || portraitRunning.value) return; portraitBusy.value = true; portraitError.value = null; try { await selectPortraitCandidate(selectedId.value, candidateId); candidates.value = []; await refreshSelected(); toastStore.show('Retrato aprobado') } catch (reason) { portraitError.value = message(reason, 'No se pudo aprobar el retrato') } finally { portraitBusy.value = false } }
async function refreshSelected() { if (selectedId.value === null) return; const id = selectedId.value; const [detail] = await Promise.all([getCatalogCharacter(id), loadCatalog()]); if (selectedId.value === id) { selected.value = detail; portraitAssetVersion.value += 1; await refreshPortrait(id) } }
async function onCreated(creation: CatalogCharacterCreation) { if (!creation.name || !creation.narrativeDescription || !creation.visualDescription) { toastStore.show('La creación se confirmó, pero la ficha recibida está incompleta.', 'error'); return }; formOpen.value = false; selected.value = { name: creation.name, narrativeDescription: creation.narrativeDescription, visualDescription: creation.visualDescription }; selectedId.value = null; detailError.value = null; toastStore.show('Personaje creado'); const previousIds = new Set(characters.value.map((character) => character.id)); const refreshed = await loadCatalog(); if (!refreshed) { detailError.value = 'No se pudo actualizar el catálogo. La ficha creada se conserva arriba.'; return }; const newCharacters = refreshed.filter((character) => !previousIds.has(character.id)); if (newCharacters.length === 1 && newCharacters[0].name === creation.name) await select(newCharacters[0].id) }
async function onRenamed(character: CatalogCharacter) { selected.value = character; selectedId.value = character.id; const index = characters.value.findIndex((item) => item.id === character.id); if (index !== -1) characters.value.splice(index, 1, character); toastStore.show('Nombre actualizado') }
async function refreshAfterReview(forkCatalogCharacterId?: number) { await loadCatalog(); if (forkCatalogCharacterId !== undefined) await select(forkCatalogCharacterId); else await refreshSelected() }
watch(() => websocketStore.lastPortraitEvent?.id, async () => { const event = websocketStore.lastPortraitEvent; if (!event || event.catalogCharacterId !== selectedId.value) return; galleryInvalidated.value = true; candidates.value = []; await refreshSelected() })
watch(() => websocketStore.connectionId, async (connectionId) => { if (connectionId > 0 && selectedId.value !== null) await refreshSelected() })
onMounted(loadCatalog)
</script>

<style scoped>
.catalog{padding:0 20px}.toolbar{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:16px}.toolbar h1{margin:0;font-size:24px}.catalog-body{display:grid;grid-template-columns:minmax(220px,1fr) minmax(0,2fr);gap:20px}aside{display:grid;align-content:start;gap:8px}.character-row{display:flex;flex-wrap:wrap;align-items:center;gap:6px;width:100%;padding:10px;font:inherit;text-align:left;color:var(--text-h);cursor:pointer;background:transparent;border:1px solid var(--border);border-radius:6px}.character-row.selected{border-color:#2563eb}.badge{padding:2px 6px;font-size:12px;color:#92400e;background:#fef3c7;border-radius:999px}.unknown{color:var(--text);background:var(--border)}.pending{color:#1d4ed8;background:#dbeafe}.empty,.load-error{margin:0}.load-error{color:#e5484d}.btn,.primary{padding:8px 16px;font:inherit;cursor:pointer;border-radius:6px}.btn{color:var(--text-h);background:transparent;border:1px solid var(--border)}.primary{color:#fff;background:#2563eb;border:1px solid #2563eb}.primary:disabled{cursor:not-allowed;opacity:.5}.link{padding:0;font:inherit;color:inherit;cursor:pointer;text-decoration:underline;background:transparent;border:0}.portrait-actions{display:grid;gap:12px;margin-top:16px}.running{margin:0;padding:8px;color:#92400e;background:#fef3c7;border-radius:6px}@media (max-width:700px){.catalog-body{grid-template-columns:1fr}}
</style>
