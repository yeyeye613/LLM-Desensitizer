<script setup>
import { computed } from 'vue'
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js'
import { Pie } from 'vue-chartjs'

ChartJS.register(ArcElement, Tooltip, Legend)

const props = defineProps({
  detectedEntities: {
    type: Array,
    default: () => []
  }
})

const totalIntercepted = computed(() => props.detectedEntities.length)

const chartData = computed(() => {
  const counts = {}
  props.detectedEntities.forEach(entity => {
    const type = entity.type
    counts[type] = (counts[type] || 0) + 1
  })

  const labels = Object.keys(counts)
  const data = Object.values(counts)

  // 预定义一些颜色，对应常见的敏感类型
  const colorMap = {
    'PHONE': '#fbbf24',
    'MOBILE_PHONE': '#fbbf24',
    'PHONE_NUMBER': '#fbbf24',
    'LANDLINE': '#f59e0b',
    'EMAIL': '#3b82f6',
    'ID_CARD': '#ef4444',
    'BANK_CARD': '#10b981',
    'ADDRESS': '#8b5cf6',
    'LOCATION': '#8b5cf6',
    'PLATE_NUMBER': '#ec4899',
    'LICENSE_PLATE': '#ec4899',
    'NAME': '#f97316',
    'ORGANIZATION': '#06b6d4',
    'COMPANY': '#06b6d4',
    'DATE': '#64748b',
    'PASSPORT': '#6366f1'
  }
  
  const backgroundColors = labels.map(label => colorMap[label] || '#94a3b8')

  return {
    labels: labels,
    datasets: [
      {
        backgroundColor: backgroundColors,
        data: data
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'right',
      labels: {
        usePointStyle: true,
        boxWidth: 10
      }
    }
  }
}
</script>

<template>
  <div class="dashboard-stats" v-if="totalIntercepted > 0">
    <div class="stat-content">
      <div class="stat-info">
        <h3>🛡️ 安全拦截统计</h3>
        <div class="big-number">{{ totalIntercepted }}</div>
        <p class="stat-desc">本次会话拦截敏感信息</p>
      </div>
      <div class="chart-container">
        <Pie :data="chartData" :options="chartOptions" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-stats {
  width: 100%;
  height: 100%;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.stat-card {
  /* Removed old card styles as parent is now the card */
}

.stat-info {
  flex: 1;
  text-align: center;
  padding-right: 20px;
  border-right: 1px solid var(--border, #e2e8f0);
}

.stat-info h3 {
  margin: 0;
  font-size: 1rem;
  color: #64748b;
  font-weight: 600;
}

.big-number {
  font-size: 3rem;
  font-weight: 800;
  color: #3b82f6;
  margin: 10px 0;
  line-height: 1;
}

.stat-desc {
  margin: 0;
  color: #94a3b8;
  font-size: 0.9rem;
}

.chart-container {
  flex: 1.5;
  height: 160px;
  position: relative;
  padding-left: 10px;
  display: flex;
  justify-content: center;
}

@media (max-width: 640px) {
  .stat-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .stat-info {
    border-right: none;
    border-bottom: 1px solid var(--border, #e2e8f0);
    padding-right: 0;
    padding-bottom: 20px;
    width: 100%;
  }
}

:global(.dark) .stat-info h3 { color: #94a3b8; }
:global(.dark) .big-number { color: #60a5fa; }
</style>
