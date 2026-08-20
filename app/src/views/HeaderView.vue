<template>
  <header class="header">
    <p class="title">Agente Cuentacuentos</p>

    <div class="services">
      <div
        class="service"
        role="button"
        tabindex="0"
        :aria-expanded="expanded"
        @click="expanded = !expanded"
        @keydown.enter="expanded = !expanded"
        @keydown.space.prevent="expanded = !expanded"
      >
        <span class="dot" :class="`dot-${ollamaStatus}`"></span>
        <span>Servicio Agentes</span>

        <div v-if="expanded && api" class="agents">
          <div class="agent">
            <span class="dot" :class="`dot-${api.ollama.topic ? 'ok' : 'down'}`"></span>
            <span>Agente temas</span>
          </div>
          <div class="agent">
            <span class="dot" :class="`dot-${api.ollama.director ? 'ok' : 'down'}`"></span>
            <span>Agente director</span>
          </div>
          <div class="agent">
            <span class="dot" :class="`dot-${api.ollama.scriptwriter ? 'ok' : 'down'}`"></span>
            <span>Agente escritor</span>
        </div>
        </div>
    </div>

      <div class="service">
        <span class="dot" :class="`dot-${chatterboxStatus}`"></span>
        <span>Servicio Audio</span>
      </div>

      <div class="service">
        <span class="dot" :class="`dot-${comfyStatus}`"></span>
        <span>Servicio Imagenes</span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ApiStateResponse } from '../api/services'

interface Props {
    api: ApiStateResponse | null
}

const props = defineProps<Props>()
const expanded = ref(false)

type Status = 'ok' | 'down' | 'partial' | 'unknown'

const ollamaStatus = computed<Status>(() => {
    if (!props.api) return 'unknown'
    const { topic, director, scriptwriter } = props.api.ollama
    if (topic && director && scriptwriter) return 'ok'
    if (!topic && !director && !scriptwriter) return 'down'
    return 'partial'
})

const chatterboxStatus = computed<Status>(() => (!props.api ? 'unknown' : props.api.chatterbox ? 'ok' : 'down'))
const comfyStatus = computed<Status>(() => (!props.api ? 'unknown' : props.api.comfy ? 'ok' : 'down'))
</script>

<style scoped>
.header {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 12px 20px;
    border-bottom: 1px solid var(--border);
}

.title {
    margin: 0;
    margin-right: auto;
    font-weight: 600;
    color: var(--text-h);
}

.services {
    display: flex;
    align-items: center;
    gap: 20px;
}

.service {
    position: relative;
    display: flex;
    align-items: center;
    gap: 8px;
}

.service[role='button'] {
    cursor: pointer;
}

.dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #9ca3af;
}

.dot-ok {
    background: #2ecc71;
}

.dot-down {
    background: #e5484d;
}

.dot-partial {
    background: #f5a623;
}

.dot-unknown {
    background: #9ca3af;
}

.agents {
    position: absolute;
    top: calc(100% + 8px);
    left: 0;
    z-index: 10;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 10px 12px;
    white-space: nowrap;
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 6px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.agent {
    display: flex;
    align-items: center;
    gap: 8px;
}
</style>
