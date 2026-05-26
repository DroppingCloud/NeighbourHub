<template>
  <div ref="chartRef" style="width:100%;height:100%"></div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{ options: any }>()
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

onMounted(() => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    chart.setOption(props.options)
  }
  window.addEventListener('resize', handleResize)
})

watch(() => props.options, (opts) => {
  chart?.setOption(opts, true)
}, { deep: true })

const handleResize = () => chart?.resize()

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>
