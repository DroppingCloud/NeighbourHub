<template>
  <div class="settings-container">
    <div class="page-header">
      <h2>系统设置</h2>
      <p>个性化您的使用体验</p>
    </div>

    <div class="settings-content">
      <!-- 外观设置 -->
      <div class="setting-group">
        <div class="group-header">
          <el-icon><Monitor /></el-icon>
          <span>外观设置</span>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>深色模式</span>
            <span class="setting-desc">开启后界面将切换为深色主题</span>
          </div>
          <el-switch 
            v-model="settings.darkMode" 
            size="large"
            @change="toggleDarkMode" 
          />
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>字体大小</span>
            <span class="setting-desc">可点击右上角🔍按钮精细调节</span>
          </div>
          <div class="font-quick-actions">
            <el-button size="small" @click="decreaseFontQuick">A-</el-button>
            <span class="current-font">{{ currentFontSize }}px</span>
            <el-button size="small" @click="increaseFontQuick">A+</el-button>
            <el-button size="small" @click="resetFontQuick">默认</el-button>
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="setting-group">
        <div class="group-header">
          <el-icon><Bell /></el-icon>
          <span>通知设置</span>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>消息推送</span>
            <span class="setting-desc">接收服务进度和提醒消息</span>
          </div>
          <el-switch v-model="settings.notification" size="large" />
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>声音提醒</span>
            <span class="setting-desc">新消息时播放提示音</span>
          </div>
          <el-switch v-model="settings.sound" size="large" :disabled="!settings.notification" />
        </div>
      </div>

      <!-- 安全设置 -->
      <div class="setting-group">
        <div class="group-header">
          <el-icon><Lock /></el-icon>
          <span>安全设置</span>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>修改密码</span>
            <span class="setting-desc">定期更换密码保护账号安全</span>
          </div>
          <el-button size="small" @click="showChangePwdDialog = true">修改</el-button>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>账号注销</span>
            <span class="setting-desc warning">注销后数据将清空，不可恢复</span>
          </div>
          <el-button size="small" type="danger" plain @click="showLogoutConfirm">注销账号</el-button>
        </div>
      </div>

      <!-- 关于 -->
      <div class="setting-group">
        <div class="group-header">
          <el-icon><InfoFilled /></el-icon>
          <span>关于</span>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>版本号</span>
            <span class="setting-desc">智慧社区服务平台</span>
          </div>
          <span>v2.0.0</span>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>用户协议</span>
          </div>
          <el-button link @click="showAgreement">查看</el-button>
        </div>
        <div class="setting-item">
          <div class="setting-label">
            <span>隐私政策</span>
          </div>
          <el-button link @click="showPrivacy">查看</el-button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog 
      v-model="showChangePwdDialog" 
      title="修改密码" 
      width="450px" 
      class="change-pwd-dialog"
    >
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePwdDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 用户协议弹窗 -->
    <el-dialog v-model="showAgreementDialog" title="用户协议" width="700px">
      <div style="max-height:60vh; overflow:auto;">
        <h3>智慧社区服务平台 用户协议</h3>
        <p>欢迎使用智慧社区服务平台。请在使用本平台前仔细阅读本用户协议。您使用本平台即表示同意本协议的全部条款。</p>
        <p>1. 服务内容：本平台为社区居民和工作人员提供事项申报、预约、通知等在线服务。</p>
        <p>2. 用户义务：用户应保证提供的信息真实、准确，不得利用本平台从事违法活动。</p>
        <p>3. 权利与义务、免责声明等条款请在部署到生产环境时替换为法律部门审核的正式文本。</p>
      </div>
      <template #footer>
        <el-button @click="showAgreementDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 隐私政策弹窗 -->
    <el-dialog v-model="showPrivacyDialog" title="隐私政策" width="700px">
      <div style="max-height:60vh; overflow:auto;">
        <h3>隐私政策</h3>
        <p>我们重视您的隐私。平台会在获得您的授权后处理必要的个人信息，以便提供服务。</p>
        <p>1. 我们收集的信息类型：注册信息（姓名、手机号等）、头像、申请材料等。</p>
        <p>2. 使用目的：为提供申请、预约、通知等功能并进行安全审计。</p>
        <p>3. 数据保留与删除：用户可通过“注销账号”删除其个人数据（不可恢复）。</p>
        <p>4. 本政策为示例文本，生产环境请替换为合规的隐私条款。</p>
      </div>
      <template #footer>
        <el-button @click="showPrivacyDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Monitor, Bell, Lock, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import {
  DEFAULT_FONT_SIZE,
  MAX_FONT_SIZE,
  MIN_FONT_SIZE,
  applyAppFontSize,
  currentAppFontSize
} from '@/composables/useFontSize'
import { useSettingsStore } from '@/stores/settings'
import { changePassword as apiChangePassword, deleteAccount as apiDeleteAccount } from '@/api/auth'

const authStore = useAuthStore()
const router = useRouter()

const showChangePwdDialog = ref(false)
const showAgreementDialog = ref(false)
const showPrivacyDialog = ref(false)
const pwdFormRef = ref()
const currentFontSize = currentAppFontSize

// 深色模式切换函数
function applyTheme(isDark: boolean) {
  const html = document.documentElement
  if (isDark) {
    html.setAttribute('data-theme', 'dark')
  } else {
    html.setAttribute('data-theme', 'light')
  }
}

const settingsStore = useSettingsStore()
const settings = reactive({
  get darkMode() { return settingsStore.darkMode },
  set darkMode(v: boolean) { settingsStore.darkMode = v },
  get notification() { return settingsStore.notification },
  set notification(v: boolean) { settingsStore.notification = v },
  get sound() { return settingsStore.sound },
  set sound(v: boolean) { settingsStore.sound = v }
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_rule: any, value: string, callback: any) => {
      if (value !== pwdForm.newPassword) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ]
}

function loadSettings() {
  // settings loaded via settingsStore on import
  applyTheme(settings.darkMode)
  const fontSize = localStorage.getItem('app-font-size')
  if (fontSize) {
    currentFontSize.value = applyAppFontSize(parseInt(fontSize))
  }
}

function saveSettings() {
  settingsStore.save()
}

// 切换深色模式
function toggleDarkMode() {
  applyTheme(settings.darkMode)
  saveSettings()
  ElMessage.success(settings.darkMode ? '已切换至深色模式' : '已切换至浅色模式')
}

function decreaseFontQuick() {
  if (currentFontSize.value > MIN_FONT_SIZE) {
    applyAppFontSize(currentFontSize.value - 1)
  }
}

function increaseFontQuick() {
  if (currentFontSize.value < MAX_FONT_SIZE) {
    applyAppFontSize(currentFontSize.value + 1)
  }
}

function resetFontQuick() {
  applyAppFontSize(DEFAULT_FONT_SIZE)
  ElMessage.success('字体已恢复默认')
}

function changePassword() {
  pwdFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      await apiChangePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword, confirmPassword: pwdForm.confirmPassword })
      ElMessage.success('密码修改成功，请重新登录')
      showChangePwdDialog.value = false
      // logout and redirect
      await authStore.logout()
      router.push('/login')
    } catch (err: any) {
      ElMessage.error(err?.message || '修改密码失败')
    }
  })
}

function showLogoutConfirm() {
  ElMessageBox.confirm('确定要注销账号吗？此操作不可恢复，所有数据将被清空。', '警告', {
    confirmButtonText: '确定注销',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    // call backend to delete account
    apiDeleteAccount().then(async () => {
      ElMessage.success('账号已注销')
      await authStore.logout()
      router.push('/login')
    }).catch((err: any) => {
      ElMessage.error(err?.message || '注销失败')
    })
  }).catch(() => {})
}

function showAgreement() {
  showAgreementDialog.value = true
}

function showPrivacy() {
  showPrivacyDialog.value = true
}

onMounted(() => {
  loadSettings()
  // watch settings and persist when changed
  watch(() => settings.darkMode, () => saveSettings())
  watch(() => settings.notification, () => saveSettings())
  watch(() => settings.sound, () => saveSettings())
})
</script>

<style scoped>
.settings-container {
  max-width: 56.25rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.page-header p {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.settings-content {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  box-shadow: var(--shadow-sm);
}

.setting-group {
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 0.0625rem solid var(--border-color);
}

.setting-group:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 1rem;
}

.group-header .el-icon {
  font-size: 1.125rem;
  color: var(--gold);
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0.75rem 0;
}

.setting-label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: min(18rem, 100%);
  flex: 1 1 18rem;
}

.setting-label span:first-child {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-primary);
}

.setting-desc {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.setting-desc.warning {
  color: var(--vermilion);
}

.font-quick-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.current-font {
  font-size: 0.875rem;
  min-width: 2.5rem;
  text-align: center;
  color: var(--text-primary);
}

/* 修改密码弹窗样式 */
:deep(.change-pwd-dialog) {
  border-radius: 0.75rem;
}

:deep(.change-pwd-dialog .el-dialog__header) {
  padding: 1rem 1.5rem;
  border-bottom: 0.0625rem solid var(--border-color);
}

:deep(.change-pwd-dialog .el-dialog__title) {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.change-pwd-dialog .el-dialog__body) {
  padding: 1.5rem;
}

:deep(.change-pwd-dialog .el-dialog__footer) {
  padding: 1rem 1.5rem;
  border-top: 0.0625rem solid var(--border-color);
}

:deep(.change-pwd-dialog .el-form-item__label) {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

:deep(.change-pwd-dialog .el-input__wrapper) {
  padding: 0.5rem 0.75rem;
}
/* 修改密码弹窗输入框深色模式适配 */
:deep(.change-pwd-dialog .el-input__wrapper) {
  padding: 0.5rem 0.75rem;
  background: var(--bg-tertiary);
  border-color: var(--border-color);
}

:deep(.change-pwd-dialog .el-input__inner) {
  color: var(--text-primary);
}

:deep(.change-pwd-dialog .el-input__inner::placeholder) {
  color: var(--text-muted);
}
</style>
