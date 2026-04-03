<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Sun, User, Lock, ArrowRight, Loader2 } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

const handleLogin = async () => {
  if (!username.value || !password.value) return
  loading.value = true
  
  try {
    const data: any = await request.post('/api/auth/login', {
      username: username.value,
      password: password.value
    })
    
    localStorage.setItem('token', data.token)
    localStorage.setItem('tokenPrefix', data.prefix)
    
    ElMessage.success('登录成功')
    
    if (data.roles && data.roles.includes('admin')) {
      router.push('/admin/dashboard')
    } else {
      router.push('/')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 p-6">
    <div class="max-w-md w-full">
      <!-- Logo -->
      <div class="text-center mb-12">
        <div class="inline-flex p-4 bg-primary-500 rounded-[2rem] shadow-2xl shadow-primary-200 mb-6 cursor-pointer" @click="router.push('/')">
          <Sun class="w-10 h-10 text-white" />
        </div>
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">欢迎回来</h2>
        <p class="text-slate-500 mt-2 font-medium">继续您的阳光公益之旅</p>
      </div>

      <!-- Card -->
      <div class="bg-white p-10 rounded-[3rem] shadow-2xl shadow-slate-200 border border-slate-100">
        <form @submit.prevent="handleLogin" class="space-y-6">
          <div class="space-y-2">
            <label class="text-sm font-bold text-slate-700 ml-4">用户名</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <User class="w-5 h-5" />
              </div>
              <input 
                v-model="username"
                type="text" 
                placeholder="请输入您的用户名" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-4 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
                required
              />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-sm font-bold text-slate-700 ml-4">密码</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <Lock class="w-5 h-5" />
              </div>
              <input 
                v-model="password"
                type="password" 
                placeholder="请输入您的密码" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-4 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
                required
              />
            </div>
          </div>

          <div class="flex items-center justify-between ml-4 mr-4">
            <label class="flex items-center space-x-2 cursor-pointer group">
              <input type="checkbox" class="w-4 h-4 rounded-md border-slate-300 text-primary-500 focus:ring-primary-400" />
              <span class="text-sm text-slate-500 group-hover:text-slate-700 transition-colors">记住我</span>
            </label>
            <a href="#" class="text-sm text-primary-600 font-bold hover:text-primary-700 transition-colors">忘记密码？</a>
          </div>

          <button 
            type="submit" 
            :disabled="loading"
            class="w-full bg-primary-500 text-white rounded-3xl py-4 font-bold text-lg shadow-xl shadow-primary-200 hover:bg-primary-600 hover:shadow-2xl transition-all active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
          >
            <Loader2 v-if="loading" class="w-5 h-5 animate-spin" />
            <span v-else>立即登录</span>
            <ArrowRight v-if="!loading" class="w-5 h-5" />
          </button>
        </form>
      </div>

      <!-- Footer -->
      <p class="text-center text-slate-500 mt-12 font-medium">
        还没有账号？ 
        <router-link to="/register" class="text-primary-600 font-bold hover:text-primary-700 transition-colors">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
input:focus {
  box-shadow: 0 10px 15px -3px rgba(34, 197, 94, 0.1), 0 4px 6px -4px rgba(34, 197, 94, 0.1);
}
</style>
