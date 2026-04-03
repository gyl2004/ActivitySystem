<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Sun, User, Lock, Mail, Phone, Smile, ArrowRight, Loader2 } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: ''
})

const handleRegister = async () => {
  loading.value = true
  try {
    await request.post('/api/auth/register', form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">加入阳光公益</h2>
        <p class="text-slate-500 mt-2 font-medium">开启您的温暖志愿之旅</p>
      </div>

      <!-- Card -->
      <div class="bg-white p-10 rounded-[3rem] shadow-2xl shadow-slate-200 border border-slate-100">
        <form @submit.prevent="handleRegister" class="space-y-5">
          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-400 ml-4 uppercase tracking-widest">用户名</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <User class="w-5 h-5" />
              </div>
              <input 
                v-model="form.username"
                type="text" 
                placeholder="设置您的登录账号" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-3.5 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
                required
              />
            </div>
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-400 ml-4 uppercase tracking-widest">昵称</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <Smile class="w-5 h-5" />
              </div>
              <input 
                v-model="form.nickname"
                type="text" 
                placeholder="您的展示称呼" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-3.5 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
                required
              />
            </div>
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-400 ml-4 uppercase tracking-widest">密码</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <Lock class="w-5 h-5" />
              </div>
              <input 
                v-model="form.password"
                type="password" 
                placeholder="设置您的登录密码" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-3.5 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
                required
              />
            </div>
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-400 ml-4 uppercase tracking-widest">邮箱</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <Mail class="w-5 h-5" />
              </div>
              <input 
                v-model="form.email"
                type="email" 
                placeholder="您的电子邮箱" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-3.5 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
              />
            </div>
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-400 ml-4 uppercase tracking-widest">手机号</label>
            <div class="relative group">
              <div class="absolute inset-y-0 left-5 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
                <Phone class="w-5 h-5" />
              </div>
              <input 
                v-model="form.phone"
                type="tel" 
                placeholder="您的手机号码" 
                class="w-full bg-slate-50 border-2 border-transparent focus:border-primary-400 focus:bg-white rounded-3xl py-3.5 pl-14 pr-6 outline-none text-slate-800 transition-all font-medium"
              />
            </div>
          </div>

          <div class="pt-4">
            <button 
              type="submit" 
              :disabled="loading"
              class="w-full bg-primary-500 text-white rounded-3xl py-4 font-bold text-lg shadow-xl shadow-primary-200 hover:bg-primary-600 hover:shadow-2xl transition-all active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
            >
              <Loader2 v-if="loading" class="w-5 h-5 animate-spin" />
              <span v-else>立即注册</span>
              <ArrowRight v-if="!loading" class="w-5 h-5" />
            </button>
          </div>
        </form>
      </div>

      <!-- Footer -->
      <p class="text-center text-slate-500 mt-8 font-medium">
        已有账号？ 
        <router-link to="/login" class="text-primary-600 font-bold hover:text-primary-700 transition-colors">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
input:focus {
  box-shadow: 0 10px 15px -3px rgba(34, 197, 94, 0.1), 0 4px 6px -4px rgba(34, 197, 94, 0.1);
}
</style>
