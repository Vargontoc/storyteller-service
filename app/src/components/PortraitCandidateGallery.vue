<template>
  <section class="gallery" aria-live="polite">
    <div class="gallery-heading">
      <div><h3>Retratos propuestos</h3><p>Elige uno para convertirlo en el retrato aprobado.</p></div>
      <button type="button" class="btn" :disabled="locked || busy" @click="emit('generate')">Regenerar</button>
    </div>
    <p v-if="invalidated" class="notice">Las propuestas cambiaron. Se están recuperando las opciones vigentes.</p>
    <div v-else class="candidates">
      <article v-for="candidate in candidates" :key="candidate.id" class="candidate">
        <img :src="assetUrl(characterId, candidate.id)" :alt="`Retrato propuesto de ${name}`" />
        <button type="button" class="btn primary" :disabled="locked || busy" @click="emit('select', candidate.id)">Elegir este retrato</button>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { portraitCandidateAssetUrl, type PortraitCandidate } from '../api/characters'

const props = defineProps<{ characterId: number; name: string; candidates: PortraitCandidate[]; locked: boolean; busy: boolean; invalidated?: boolean }>()
const emit = defineEmits<{ generate: []; select: [candidateId: number] }>()
function assetUrl(characterId: number, candidateId: number) { return portraitCandidateAssetUrl(characterId, candidateId) }
</script>

<style scoped>
.gallery{display:grid;gap:12px;margin-top:20px;padding-top:16px;border-top:1px solid var(--border)}.gallery-heading{display:flex;align-items:start;justify-content:space-between;gap:12px}.gallery-heading h3,.gallery-heading p{margin:0}.gallery-heading p,.notice{color:var(--text);font-size:14px}.candidates{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px}.candidate{display:grid;gap:8px;padding:8px;border:1px solid var(--border);border-radius:8px}.candidate img{width:100%;aspect-ratio:1;object-fit:cover;border-radius:5px;background:var(--border)}.btn{padding:8px 12px;font:inherit;color:var(--text-h);cursor:pointer;background:transparent;border:1px solid var(--border);border-radius:6px}.primary{color:#fff;background:#2563eb;border-color:#2563eb}.btn:disabled{cursor:not-allowed;opacity:.5}@media(max-width:500px){.gallery-heading{display:grid}}
</style>
