<template>
  <div class="rules-container">
    <div class="rules-header">
      <h2>敏感检测规则管理</h2>
      <div class="tabs">
        <button 
          :class="['tab-btn', { active: activeTab === 'add' }]" 
          @click="activeTab = 'add'"
        >
          新增规则
        </button>
        <button 
          :class="['tab-btn', { active: activeTab === 'manage' }]" 
          @click="fetchRules(); activeTab = 'manage'"
        >
          管理规则
        </button>
      </div>
    </div>

    <div v-if="activeTab === 'add'" class="tab-content">
      <h3>新增自定义敏感检测规则</h3>
      <div class="form-group">
        <label>规则名称 (Pattern Name)</label>
        <input v-model="newRule.patternName" type="text" placeholder="例如: MY_ID_CARD" />
      </div>
      <div class="form-group">
        <label>正则表达式 (Regex)</label>
        <textarea v-model="newRule.regex" placeholder="例如: \d{18}" rows="3"></textarea>
      </div>
      <div class="form-group">
        <label>描述 (Description)</label>
        <input v-model="newRule.description" type="text" placeholder="规则描述（可选）" />
      </div>
      <div class="form-group checkbox-group">
        <label>
          <input type="checkbox" v-model="newRule.isEnabled" />
          启用规则
        </label>
      </div>
      <div class="actions">
        <button class="btn btn-primary" @click="addRule" :disabled="loading">
          {{ loading ? '添加中...' : '添加规则' }}
        </button>
      </div>
      <div v-if="message" :class="['message', messageType]">{{ message }}</div>
    </div>

    <div v-if="activeTab === 'manage'" class="tab-content">
      <h3>现有自定义规则列表</h3>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="rules.length === 0" class="empty-state">
        暂无自定义规则
      </div>
      <div v-else class="rules-list">
        <div v-for="rule in rules" :key="rule.patternName" class="rule-item">
          <div class="rule-info">
            <span class="rule-name">
              {{ rule.patternName }}
              <span v-if="!rule.isEnabled" class="badge-disabled">已禁用</span>
            </span>
            <code class="rule-regex">{{ rule.regex }}</code>
            <span class="rule-desc" v-if="rule.description">{{ rule.description }}</span>
          </div>
          <div class="rule-actions">
            <button 
              class="btn btn-small" 
              :class="rule.isEnabled ? 'btn-warning' : 'btn-success'"
              @click="toggleRuleStatus(rule)"
            >
              {{ rule.isEnabled ? '禁用' : '启用' }}
            </button>
            <button class="btn btn-danger btn-small" @click="deleteRule(rule.patternName)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { API_BASE_URL } from '../config'

const props = defineProps({
  initialTab: { type: String, default: 'add' }
})

const activeTab = ref(props.initialTab)
const loading = ref(false)
const message = ref('')
const messageType = ref('success')
const rules = ref([]) // 改为数组

const newRule = reactive({
  patternName: '',
  regex: '',
  description: '',
  isEnabled: true
})

async function addRule() {
  if (!newRule.patternName || !newRule.regex) {
    showMessage('请填写规则名称和正则表达式', 'error')
    return
  }

  loading.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/rules`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newRule)
    })

    if (res.ok) {
      showMessage('规则添加成功', 'success')
      // 重置表单
      newRule.patternName = ''
      newRule.regex = ''
      newRule.description = ''
      newRule.isEnabled = true
    } else {
      throw new Error(await res.text())
    }
  } catch (e) {
    showMessage(`添加失败: ${e.message}`, 'error')
  } finally {
    loading.value = false
  }
}

async function fetchRules() {
  loading.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/rules`)
    if (res.ok) {
      rules.value = await res.json()
    } else {
      throw new Error('获取规则失败')
    }
  } catch (e) {
    showMessage(`加载失败: ${e.message}`, 'error')
  } finally {
    loading.value = false
  }
}

async function deleteRule(name) {
  if (!confirm(`确定要删除规则 "${name}" 吗?`)) return

  try {
    const res = await fetch(`${API_BASE_URL}/rules/${encodeURIComponent(name)}`, {
      method: 'DELETE'
    })

    if (res.ok) {
      showMessage('删除成功', 'success')
      fetchRules()
    } else {
      throw new Error(await res.text())
    }
  } catch (e) {
    showMessage(`删除失败: ${e.message}`, 'error')
  }
}

async function toggleRuleStatus(rule) {
  try {
    const newStatus = !rule.isEnabled
    const res = await fetch(`${API_BASE_URL}/rules/${encodeURIComponent(rule.patternName)}/status?enabled=${newStatus}`, {
      method: 'PATCH'
    })

    if (res.ok) {
      showMessage(`规则已${newStatus ? '启用' : '禁用'}`, 'success')
      // 更新本地状态，无需重新获取列表
      rule.isEnabled = newStatus
    } else {
      throw new Error(await res.text())
    }
  } catch (e) {
    showMessage(`操作失败: ${e.message}`, 'error')
  }
}

function showMessage(text, type) {
  message.value = text
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

watch(() => props.initialTab, (val) => {
  if (val) activeTab.value = val
  if (val === 'manage') fetchRules()
})

onMounted(() => {
  if (activeTab.value === 'manage') fetchRules()
})
</script>

<style scoped>
.rules-container {
  padding: 20px;
}
.rules-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 10px;
}
.tabs {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}
.tab-btn {
  padding: 8px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  font-weight: 600;
  color: #64748b;
}
.tab-btn.active {
  color: #3b82f6;
  border-bottom-color: #3b82f6;
}
.tab-content {
  animation: fadeIn 0.3s ease;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
}
.form-group input[type="text"], 
.form-group textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
}
.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.message {
  margin-top: 10px;
  padding: 10px;
  border-radius: 6px;
}
.message.success {
  background: #dcfce7;
  color: #166534;
}
.message.error {
  background: #fee2e2;
  color: #991b1b;
}
.rules-list {
  display: grid;
  gap: 10px;
}
.rule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}
.rule-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.rule-name {
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 8px;
}
.rule-regex {
  font-family: monospace;
  color: #64748b;
  font-size: 0.9em;
  background: #e2e8f0;
  padding: 2px 4px;
  border-radius: 4px;
  align-self: flex-start;
}
.rule-desc {
  font-size: 0.85em;
  color: #94a3b8;
}
.badge-disabled {
  font-size: 0.75em;
  background: #cbd5e1;
  color: #475569;
  padding: 2px 6px;
  border-radius: 4px;
}
.rule-actions {
  display: flex;
  gap: 8px;
}
.btn-small {
  padding: 4px 8px;
  font-size: 0.85em;
}
.btn-warning {
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.btn-success {
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.btn-danger {
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>