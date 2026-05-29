<template>
  <AuthLayout>
    <div class="register-box">
      <div class="form-header">
        <h2 class="form-title">注册账号</h2>
        <p class="form-hint">请填写真实信息完成居民账号注册</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入 18 位身份证号" size="large" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="6-20 位密码"
            size="large"
            :prefix-icon="Lock"
          >
            <template #suffix>
              <el-icon class="pwd-eye" @click="showPwd = !showPwd">
                <View v-if="showPwd" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="submitRegister">
          完成注册
        </el-button>
      </el-form>

      <div class="form-footer">
        已有账号？
        <router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Hide, Lock, Phone, User, View } from '@element-plus/icons-vue'
import AuthLayout from '@/layout/AuthLayout.vue'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const showPwd = ref(false)

const form = ref({
  username: '',
  realName: '',
  idCard: '',
  phone: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需要在 3-20 位之间', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需要在 6-20 位之间', trigger: 'blur' }
  ]
}

async function submitRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-box {
  width: 100%;
}

.form-header {
  margin-bottom: 1.5rem;
}

.form-title {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.form-hint,
.form-footer {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.pwd-eye {
  cursor: pointer;
  color: var(--text-muted);
}

.submit-btn {
  width: 100%;
  min-height: 2.75rem;
  margin-top: 0.5rem;
}

.form-footer {
  text-align: center;
  margin-top: 1.5rem;
}

.link {
  color: var(--gold);
  text-decoration: none;
}
</style>
