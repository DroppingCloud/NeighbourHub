<template>
  <div class="upload-panel">
    <el-alert title="请上传所需材料（支持 PDF、JPG、PNG，单文件不超过 10MB）" type="info" :closable="false" style="margin-bottom:16px" />
    <el-upload
      v-model:file-list="fileList"
      action="#"
      :auto-upload="false"
      :on-change="handleChange"
      :before-upload="beforeUpload"
      multiple
      drag
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">支持 PDF、JPG、PNG 格式，单文件不超过 10MB</div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UploadFile } from 'element-plus'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ uploaded: [files: UploadFile[]] }>()
const fileList = ref<UploadFile[]>([])

const beforeUpload = (file: File) => {
  const allowed = ['application/pdf', 'image/jpeg', 'image/png']
  const maxSize = 10 * 1024 * 1024
  if (!allowed.includes(file.type)) {
    ElMessage.error('只支持 PDF、JPG、PNG 格式')
    return false
  }
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return false // 阻止自动上传
}

const handleChange = (_: UploadFile, files: UploadFile[]) => {
  emit('uploaded', files)
}
</script>
