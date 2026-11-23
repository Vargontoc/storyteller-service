<template>
  <section class="voices">
    <header class="toolbar"><h1>Biblioteca de voces</h1></header>
    <p v-if="loading">Cargando voces...</p>
    <div v-else-if="error" class="load-error"><p>{{ error }}</p><button type="button" class="btn" @click="load">Reintentar</button></div>
    <div v-else class="voices-body">
      <div>
        <p v-if="library && !library.systemVoiceAvailable" class="notice" role="status">
          La voz del sistema no está disponible<template v-if="library.systemVoiceUnavailableReason">: {{ library.systemVoiceUnavailableReason }}</template>.
        </p>
        <p v-if="library?.systemVoiceNameCollision" class="notice" role="status">
          El nombre de la voz del sistema coincide con el de una voz subida.
        </p>
        <p v-if="deleteError" class="load-error" role="alert">{{ deleteError }}</p>
        <p v-if="library && library.voices.length === 0" class="empty">No hay voces todavía.</p>
        <ul aria-label="Voces de la biblioteca">
          <li v-for="voice in library?.voices" :key="voice.system ? 'system' : voice.id" class="voice-row">
            <span class="voice-name">{{ voice.name }}</span>
            <span class="voice-language">{{ languageName(voice.language) }}</span>
            <span v-if="voice.system" class="badge">Voz del sistema</span>
            <button v-else type="button" class="btn danger-text" @click="pendingDelete = voice">Borrar</button>
          </li>
        </ul>
      </div>
      <form class="upload" aria-label="Subir voz" @submit.prevent="submit">
        <h2>Subir voz</h2>
        <label>Nombre<input v-model="name" type="text" name="name" /></label>
        <label>Audio (WAV o MP3, de 5 a 30 s)<input ref="fileInput" type="file" name="file" accept=".wav,.mp3,audio/wav,audio/mpeg" @change="onFile" /></label>
        <label class="check"><input v-model="permission" type="checkbox" name="permission" /> Confirmo que tengo permiso para usar esta voz</label>
        <p v-if="uploadError" class="load-error" role="alert">{{ uploadError }}</p>
        <button type="submit" class="primary" :disabled="uploading">Subir voz</button>
      </form>
    </div>
    <confirm-modal :open="pendingDelete !== null" title="Borrar voz" :message="`¿Borrar la voz «${pendingDelete?.name ?? ''}»?`" confirm-label="Borrar" danger @confirm="confirmDelete" @cancel="pendingDelete = null" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { ApiError } from '../api/httpClient'
import { deleteVoice, listVoices, uploadVoice, type Voice, type VoiceLibrary } from '../api/voices'

const REJECTION_MESSAGES: Record<string, string> = {
  NAME_REQUIRED: 'El nombre es obligatorio.',
  PERMISSION_REQUIRED: 'Debes confirmar que tienes permiso para usar la voz.',
  INVALID_FORMAT: 'Formato no admitido: solo se aceptan ficheros WAV o MP3.',
  INVALID_DURATION: 'La duración del audio debe estar entre 5 y 30 segundos.',
  NAME_DUPLICATED: 'Ya existe una voz con ese nombre.',
}

const library = ref<VoiceLibrary | null>(null)
const loading = ref(false); const error = ref<string | null>(null)
const name = ref(''); const permission = ref(false)
const file = ref<File | null>(null); const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false); const uploadError = ref<string | null>(null)
const pendingDelete = ref<Voice | null>(null); const deleteError = ref<string | null>(null)

const VOICE_LANGUAGE = 'es'
const languageNames = new Intl.DisplayNames(['es'], { type: 'language' })
const languageName = (code: string) => languageNames.of(code) ?? code
const reason = (failure: unknown, fallback: string) => failure instanceof ApiError ? (failure.code && REJECTION_MESSAGES[failure.code]) || failure.message : fallback

async function load() {
  loading.value = true; error.value = null
  try {
    library.value = await listVoices()
  } catch (failure) { error.value = reason(failure, 'No se pudo cargar la biblioteca de voces') } finally { loading.value = false }
}

function onFile(event: Event) { file.value = (event.target as HTMLInputElement).files?.[0] ?? null }

async function submit() {
  uploadError.value = null
  if (!file.value) { uploadError.value = 'Selecciona un fichero de audio WAV o MP3.'; return }
  uploading.value = true
  try {
    await uploadVoice({ name: name.value, language: VOICE_LANGUAGE, permission: permission.value, file: file.value })
    name.value = ''; permission.value = false; file.value = null
    if (fileInput.value) fileInput.value.value = ''
    library.value = await listVoices()
  } catch (failure) { uploadError.value = reason(failure, 'No se pudo subir la voz') } finally { uploading.value = false }
}

async function confirmDelete() {
  const voice = pendingDelete.value
  pendingDelete.value = null
  if (!voice || voice.id === undefined) return
  deleteError.value = null
  try { await deleteVoice(voice.id); library.value = await listVoices() } catch (failure) { deleteError.value = reason(failure, 'No se pudo borrar la voz') }
}

onMounted(load)
</script>

<style scoped>
.voices{padding:0 20px}.toolbar h1{margin:0 0 16px;font-size:24px}.voices-body{display:grid;grid-template-columns:minmax(0,2fr) minmax(220px,1fr);gap:20px}ul{display:grid;gap:8px;padding:0;margin:0;list-style:none}.voice-row{display:flex;flex-wrap:wrap;align-items:center;gap:8px;padding:10px;border:1px solid var(--border);border-radius:6px}.voice-name{color:var(--text-h)}.badge{padding:2px 6px;font-size:12px;color:#1d4ed8;background:#dbeafe;border-radius:999px}.notice{margin:0 0 12px;padding:8px;color:#92400e;background:#fef3c7;border-radius:6px}.upload{display:grid;align-content:start;gap:12px}.upload h2{margin:0;font-size:18px}.upload label{display:grid;gap:4px}.upload .check{display:block}.empty{margin:0}.load-error{margin:0;color:#e5484d}.btn,.primary{padding:8px 16px;font:inherit;cursor:pointer;border-radius:6px}.btn{color:var(--text-h);background:transparent;border:1px solid var(--border)}.danger-text{margin-left:auto;color:#e5484d}.primary{color:#fff;background:#2563eb;border:1px solid #2563eb}.primary:disabled{cursor:not-allowed;opacity:.5}@media (max-width:700px){.voices-body{grid-template-columns:1fr}}
</style>
