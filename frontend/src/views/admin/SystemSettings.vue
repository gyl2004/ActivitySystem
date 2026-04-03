<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Save, RefreshCw, Settings, Shield, Bell, AppWindow } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const configs = ref<any[]>([])
const submitting = ref(false)

const fetchConfigs = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/configs')
    configs.value = data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleUpdate = async (config: any) => {
  submitting.value = true
  try {
    await request.put('/api/configs', config)
    ElMessage.success(`配置 ${config.configKey} 更新成功`)
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const getIcon = (key: string) => {
  if (key.includes('app')) return AppWindow
  if (key.includes('security') || key.includes('publish')) return Shield
  if (key.includes('notify')) return Bell
  return Settings
}

onMounted(() => {
  fetchConfigs()
})
</script>

<template>
  <div class="space-y-8 max-w-5xl mx-auto">
    <div class="flex justify-between items-end">
      <div>
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">系统设置</h2>
        <p class="text-slate-500 font-medium mt-1">配置系统全局参数，调整业务运行规则。</p>
      </div>
      <button @click="fetchConfigs" class="p-3 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-2xl transition-all">
        <RefreshCw :class="['w-6 h-6', loading ? 'animate-spin' : '']" />
      </button>
    </div>

    <div v-loading="loading" class="space-y-6">
      <div v-if="configs.length === 0" class="text-center py-20 text-slate-400">
        暂无系统配置项
      </div>
      
      <div 
        v-for="config in configs" 
        :key="config.id"
        class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm hover:shadow-xl hover:shadow-slate-100 transition-all duration-500"
      >
        <div class="flex items-start justify-between gap-8">
          <div class="flex items-start space-x-6 flex-grow">
            <div class="p-4 bg-slate-50 rounded-2xl text-slate-400 group-hover:scale-110 transition-transform">
              <component :is="getIcon(config.configKey)" class="w-7 h-7" />
            </div>
            <div class="space-y-4 flex-grow">
              <div>
                <div class="text-lg font-bold text-slate-800">{{ config.remark || config.configKey }}</div>
                <div class="text-xs text-slate-400 font-bold uppercase tracking-widest mt-1">{{ config.configKey }}</div>
              </div>
              
              <div class="flex items-center space-x-4">
                <el-input 
                  v-model="config.configValue" 
                  placeholder="请输入配置值"
                  class="flex-grow !rounded-2xl"
                />
                <button 
                  @click="handleUpdate(config)"
                  :disabled="submitting"
                  class="flex items-center space-x-2 bg-primary-500 text-white px-6 py-2.5 rounded-xl font-bold shadow-lg shadow-primary-200 hover:bg-primary-600 transition-all active:scale-95 disabled:opacity-70"
                >
                  <Save class="w-4 h-4" />
                  <span>保存</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  @apply !rounded-xl !bg-slate-50 !border-none !shadow-none py-2.5;
}
:deep(.el-input__wrapper.is-focus) {
  @apply !bg-white !ring-2 !ring-primary-100;
}
</style>
