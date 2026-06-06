<template>
  <div class="font-size-control">
    <el-popover
      ref="popoverRef"
      placement="bottom-end"
      :width="'min(92vw, 32rem)'"
      trigger="click"
      :show-arrow="false"
      :popper-class="'font-size-popover-popper'"
      :teleported="true"
    >
      <template #reference>
        <el-button class="font-size-btn" circle>
          <el-icon :size="24"><ZoomIn /></el-icon>
        </el-button>
      </template>
      
      <div class="font-size-popover">
        <div class="popover-title">
          <el-icon :size="20"><ZoomIn /></el-icon>
          <span>字体大小调节</span>
          <span class="recommend-tip">推荐大号字体</span>
        </div>
        
        <div class="font-size-slider">
          <span class="size-label">小</span>
          <el-slider
            v-model="currentFontSize"
            :min="14"
            :max="28"
            :step="1"
            :marks="fontSizeMarks"
            @change="handleFontSizeChange"
          />
          <span class="size-label">大</span>
        </div>
        
        <div class="font-size-preview">
          <div class="preview-text" :style="{ fontSize: currentFontSize + 'px' }">
            预览文字：社区服务暖人心
          </div>
          <div class="preview-sub" :style="{ fontSize: (currentFontSize - 4) + 'px' }">
            这是辅助文字效果
          </div>
        </div>
        
        <div class="font-size-actions">
          <el-button size="default" @click="resetFontSize">恢复默认</el-button>
          <el-button size="default" type="primary" @click="confirmAndClose">确定</el-button>
        </div>
        
        <div class="font-size-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>设置将自动保存，下次登录生效</span>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ZoomIn, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  DEFAULT_FONT_SIZE,
  applyAppFontSize,
  currentAppFontSize,
  getSavedFontSize
} from '@/composables/useFontSize'

const currentFontSize = currentAppFontSize
const popoverRef = ref()

const fontSizeMarks = {
  14: '14',
  16: '16',
  18: '18',
  20: '20',
  22: '22',
  24: '24',
  26: '26',
  28: '28'
}

function applyFontSize(size: number) {
  applyAppFontSize(size)
}

function handleFontSizeChange(value: number) {
  applyFontSize(value)
  currentFontSize.value = value
}

function resetFontSize() {
  currentFontSize.value = DEFAULT_FONT_SIZE
  applyFontSize(DEFAULT_FONT_SIZE)
  ElMessage.success('已恢复默认字体大小')
  if (popoverRef.value) {
    popoverRef.value.hide()
  }
}

function confirmAndClose() {
  ElMessage.success(`字体大小已调整为 ${currentFontSize.value}px`)
  if (popoverRef.value) {
    popoverRef.value.hide()
  }
}

function loadFontSize() {
  currentFontSize.value = applyAppFontSize(getSavedFontSize())
}

onMounted(() => {
  loadFontSize()
})
</script>

<style scoped>
.font-size-control {
  display: flex;
  align-items: center;
}

.font-size-btn {
  border: none;
  background: var(--card-bg); /* 使用主题卡片背景，支持深色模式 */
  width: 2.5rem;                          /* 稍小一点，40px */
  height: 2.5rem;
  border-radius: 50%;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: var(--shadow-sm);
}

.font-size-btn:hover {
  background: var(--bg-tertiary);
  transform: scale(1.05);
}

.font-size-btn .el-icon {
  font-size: 1.5rem;
  color: var(--text-primary);
}
</style>

<style>
/* 全局样式，不加 scoped */
.font-size-popover-popper {
  padding: 0 !important;
  border-radius: 1rem !important;
  box-shadow: 0 0.5rem 2rem rgba(0, 0, 0, 0.12) !important;
  background: var(--card-bg) !important;
  border-color: var(--border-color) !important;
  max-width: min(92vw, 32rem) !important;
  width: auto !important;
  min-width: min(28rem, 92vw) !important;
}

.font-size-popover-popper .el-popover__title {
  display: none;
}

.font-size-popover-popper .el-popover__content {
  padding: 0 !important;
}

/* 滑块标记文字样式 - 防止溢出 */
.font-size-popover-popper .el-slider__marks-text {
  font-size: 0.6875rem !important;
  white-space: nowrap !important;
  transform: translateX(-50%) !important;
}

/* 小屏幕适配 */
@media (max-width: 768px) {
  .font-size-popover-popper {
    min-width: 20rem !important;
    max-width: 90vw !important;
  }
}
</style>

<style scoped>
.font-size-popover {
  padding: 1.25rem;
  min-width: min(26rem, 86vw);
  max-width: 100%;
  background: var(--card-bg);
  color: var(--text-primary);
  box-sizing: border-box;
}

.popover-title {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 0.0625rem solid var(--border-color);
  flex-wrap: wrap;
}

.popover-title .el-icon {
  font-size: 1.25rem;
  color: var(--gold);
  flex-shrink: 0;
}

.recommend-tip {
  font-size: 0.6875rem;
  font-weight: normal;
  color: var(--jade);
  background: rgba(39, 174, 96, 0.1);
  padding: 0.125rem 0.5rem;
  border-radius: 0.75rem;
  margin-left: auto;
  white-space: nowrap;
}

.font-size-slider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.size-label {
  font-size: 0.8125rem;
  color: var(--text-muted);
  width: 1.5rem;
  flex-shrink: 0;
}

.font-size-slider :deep(.el-slider) {
  flex: 1;
  min-width: 0;
}

.font-size-slider :deep(.el-slider__runway) {
  background-color: var(--border-soft);
  height: 0.25rem;
  margin: 1rem 0;
}

.font-size-slider :deep(.el-slider__bar) {
  background-color: var(--gold);
  height: 0.25rem;
}

.font-size-slider :deep(.el-slider__button) {
  width: 1.125rem;
  height: 1.125rem;
  border: 0.1875rem solid var(--gold);
  background-color: var(--card-bg);
}

.font-size-slider :deep(.el-slider__marks) {
  top: 0.5rem;
}

.font-size-slider :deep(.el-slider__marks-text) {
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 0.5rem;
  white-space: nowrap;
}

.font-size-preview {
  background: var(--bg-tertiary);
  border: 0.0625rem solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  text-align: center;
  margin-bottom: 1.25rem;
  overflow-x: auto;
}

.preview-text {
  color: var(--text-primary);
  font-weight: 600;
  transition: font-size 0.1s ease;
  margin-bottom: 0.5rem;
  white-space: normal;
  overflow-wrap: anywhere;
}

.preview-sub {
  color: var(--text-muted);
  transition: font-size 0.1s ease;
  white-space: normal;
  overflow-wrap: anywhere;
}

.font-size-actions {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding-top: 0.5rem;
}

.font-size-tip {
  display: flex;
  align-items: flex-start;
  gap: 0.375rem;
  font-size: 0.6875rem;
  color: var(--text-muted);
  padding-top: 0.75rem;
  border-top: 0.0625rem solid var(--border-color);
}

.preview-text {
  color: var(--text-primary);
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .font-size-popover {
    min-width: auto;
    padding: 1rem;
  }
  
  .font-size-slider {
    flex-wrap: wrap;
  }
  
  .size-label {
    width: auto;
  }
  
  .font-size-slider :deep(.el-slider) {
    min-width: 10rem;
  }
  
  .preview-text,
  .preview-sub {
    white-space: normal;
    word-break: keep-all;
  }
}
</style>
