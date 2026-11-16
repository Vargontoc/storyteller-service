<template>
  <Teleport to="body"><Transition name="modal-fade"><div v-if="open" class="overlay" @click.self="close" @keydown.esc="close"><form class="modal" role="dialog" aria-modal="true" aria-label="Crear personaje" @submit.prevent="submit"><header><h2>Crear personaje</h2><button type="button" class="close" aria-label="Cerrar" :disabled="submitting" @click="close">×</button></header><fieldset :disabled="submitting"><label><input v-model="mode" type="radio" value="automatic" /> Automático</label><label><input v-model="mode" type="radio" value="manual" /> Manual</label></fieldset><label v-if="mode === 'manual'" for="character-hint">Describe el personaje</label><textarea v-if="mode === 'manual'" id="character-hint" v-model.trim="hint" rows="4" required /><p v-if="rejection" class="rejection">{{ rejection }}</p><p v-if="error" class="error">{{ error }}</p><button type="submit" class="primary" :disabled="submitting || (mode === 'manual' && !hint)">{{ submitting ? 'Creando...' : mode === 'automatic' ? 'Crear automáticamente' : 'Crear personaje' }}</button></form></div></Transition></Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { createCatalogCharacter, type CatalogCharacterCreation } from '../api/characters'
import { ApiError } from '../api/httpClient'
const props = defineProps<{ open: boolean }>(); const emit = defineEmits<{ close: []; created: [creation: CatalogCharacterCreation] }>()
const mode = ref<'automatic' | 'manual'>('automatic'); const hint = ref(''); const submitting = ref(false); const rejection = ref<string | null>(null); const error = ref<string | null>(null)
watch(() => props.open, (open) => { if (open) { mode.value = 'automatic'; hint.value = ''; rejection.value = null; error.value = null } })
async function submit() { if (submitting.value || (mode.value === 'manual' && !hint.value)) return; submitting.value = true; rejection.value = null; error.value = null; try { const creation = await createCatalogCharacter(mode.value === 'manual' ? hint.value : undefined); if (!creation.accepted) { rejection.value = creation.rejectionReason || 'El personaje no cumple las reglas del catálogo.'; return }; emit('created', creation) } catch (reason) { error.value = reason instanceof ApiError ? reason.message : 'No se pudo crear el personaje' } finally { submitting.value = false } }
function close() { if (!submitting.value) emit('close') }
</script>

<style scoped>
.overlay{position:fixed;inset:0;z-index:100;display:grid;place-items:center;padding:20px;background:rgba(0,0,0,.45)}.modal{display:grid;gap:16px;width:100%;max-width:480px;padding:24px;background:var(--bg);border:1px solid var(--border);border-radius:10px}header{display:flex;justify-content:space-between;align-items:center}h2{margin:0;font-size:20px}.close{font:inherit;font-size:24px;color:var(--text-h);cursor:pointer;background:transparent;border:0}fieldset{display:flex;gap:16px;padding:0;border:0}label{color:var(--text-h)}textarea{width:100%;padding:8px;font:inherit;color:var(--text-h);background:var(--bg);border:1px solid var(--border);border-radius:6px;resize:vertical}.primary{padding:8px 16px;font:inherit;color:#fff;cursor:pointer;background:#2563eb;border:1px solid #2563eb;border-radius:6px}.primary:disabled,.close:disabled{cursor:not-allowed;opacity:.5}.rejection,.error{margin:0;color:#e5484d}.modal-fade-enter-active,.modal-fade-leave-active{transition:opacity .15s ease}.modal-fade-enter-from,.modal-fade-leave-to{opacity:0}
</style>
