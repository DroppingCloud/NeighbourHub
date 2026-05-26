<template>
  <el-form label-width="100px">
    <el-steps :active="currentStep" finish-status="success" style="margin-bottom:24px">
      <el-step v-for="(step, i) in steps" :key="i" :title="step.title" />
    </el-steps>
    <div v-if="currentStep < steps.length">
      <el-form-item
        v-for="field in steps[currentStep]?.fields"
        :key="field.key"
        :label="field.label"
      >
        <el-input v-if="field.type === 'text'" v-model="formData[field.key]" :placeholder="field.placeholder" />
        <el-select v-else-if="field.type === 'select'" v-model="formData[field.key]" style="width:100%">
          <el-option v-for="opt in field.options" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <div style="margin-top:16px;display:flex;gap:8px">
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button v-if="currentStep < steps.length - 1" type="primary" @click="currentStep++">下一步</el-button>
        <el-button v-else type="primary" @click="$emit('submit', formData)">提交</el-button>
      </div>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

interface StepField { key: string; label: string; type: 'text' | 'select'; placeholder?: string; options?: any[] }
interface Step { title: string; fields: StepField[] }

defineProps<{ steps: Step[] }>()
defineEmits<{ submit: [data: Record<string, any>] }>()

const currentStep = ref(0)
const formData = reactive<Record<string, any>>({})
</script>
