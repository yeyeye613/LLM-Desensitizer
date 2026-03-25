<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  histories: { type: Array, default: () => [] },
  activeId: { type: String, default: null }
})
const emit = defineEmits(['clear', 'select'])

const selectedId = ref(null)
const sorted = computed(() => [...props.histories].sort((a, b) => b.timestamp - a.timestamp))
const effectiveSelectedId = computed(() => selectedId.value ?? props.activeId ?? null)
const selected = computed(() => sorted.value.find(x => x.id === effectiveSelectedId.value) || null)

function onSelect(id) {
  selectedId.value = id
  const item = sorted.value.find(x => x.id === id)
  if (item) emit('select', item)
}

function clearAll() {
  selectedId.value = null
  emit('clear')
}
</script>

<template>
  <div class="card">
    <h2>🕘 历史对话</h2>
    <div class="history-layout">
      <div class="history-list">
        <div class="toolbar">
          <span class="count">共 {{ sorted.length }} 条</span>
          <button class="btn btn-danger btn-small" @click="clearAll">清空</button>
        </div>
        <div v-if="sorted.length === 0" class="empty">暂无历史</div>
        <ul v-else class="items">
          <li v-for="item in sorted" :key="item.id" :class="['item', { active: item.id === selectedId }]" @click="onSelect(item.id)">
            <div class="title">{{ new Date(item.timestamp).toLocaleString() }}</div>
            <div class="meta">{{ item.provider }}</div>
            <div class="preview">{{ (item.desensitizedPrompt || item.originalPrompt || '').slice(0, 60) }}</div>
          </li>
        </ul>
      </div>
      <div class="history-detail">
        <div v-if="!selected" class="empty">选择左侧一条记录查看详情</div>
        <div v-else class="detail">
          <div class="detail-section">
            <h3>原始提示词</h3>
            <pre>{{ selected.originalPrompt }}</pre>
          </div>
          <div class="detail-section">
            <h3>脱敏后提示词</h3>
            <pre>{{ selected.desensitizedPrompt }}</pre>
          </div>
          <div class="detail-section">
            <h3>模型响应</h3>
            <pre>{{ selected.responseText }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
  
</template>

<style scoped>
.history-layout { display: grid; grid-template-columns: 320px 1fr; gap: 20px; }
.history-list { border: 1px solid var(--border); border-radius: 12px; padding: 12px; background: var(--card-bg); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.count { color: var(--text-light); font-size: 0.9rem; }
.items { list-style: none; padding: 0; margin: 0; max-height: 420px; overflow-y: auto; }
.item { padding: 10px; border: 1px solid var(--border); border-radius: 8px; margin-bottom: 8px; cursor: pointer; background: var(--card-bg); }
.item:hover { background: var(--input-bg); }
.item.active { border-color: var(--primary); box-shadow: var(--shadow); }
.title { font-weight: 600; color: var(--text); }
.meta { font-size: 0.85rem; color: var(--text-light); }
.preview { font-size: 0.9rem; color: var(--text-light); margin-top: 4px; }
.history-detail { border: 1px solid var(--border); border-radius: 12px; padding: 12px; background: var(--card-bg); }
.empty { color: var(--text-light); text-align: center; padding: 20px; }
.detail-section { margin-bottom: 16px; }
.detail-section h3 { margin: 0 0 8px 0; color: var(--primary); }
.detail-section pre { background: var(--card-bg); color: var(--text); border: 1px solid var(--border); border-radius: 8px; padding: 12px; white-space: pre-wrap; word-break: break-word; }
@media (max-width: 980px) { .history-layout { grid-template-columns: 1fr; } }
</style>
