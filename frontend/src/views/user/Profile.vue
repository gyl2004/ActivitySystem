<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Mail, Phone, Calendar, Heart, Shield, Settings, LogOut, ChevronRight, Clock, Loader2, LayoutDashboard } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(true)
const user = ref<any>(null)
const isAdmin = ref(false)
const registrations = ref<any[]>([])

const stats = ref([
  { label: '参与活动', value: 0, icon: Heart, color: 'text-rose-500', bg: 'bg-rose-50' },
  { label: '志愿时长', value: '0h', icon: Clock, color: 'text-blue-500', bg: 'bg-blue-50' },
  { label: '获得积分', value: 0, icon: Shield, color: 'text-primary-600', bg: 'bg-primary-50' }
])

const fetchData = async () => {
  try {
    // 1. 获取用户信息
    const data: any = await request.get('/api/auth/me')
    user.value = data.user
    isAdmin.value = data.roles.includes('admin')
    
    stats.value[1].value = (user.value.volunteerDuration || 0) + 'h'
    stats.value[2].value = user.value.points || 0

    // 2. 获取报名记录
    const regData: any = await request.get('/api/registrations/my')
    registrations.value = regData
    stats.value[0].value = regData.length
  } catch (error) {
    console.error(error)
    // router.push('/login') // 不要在这里强制跳转，让拦截器处理
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('tokenPrefix')
  ElMessage.success('已退出登录')
  router.push('/login')
}

const getStatusLabel = (status: number) => {
  switch (status) {
    case 0: return { text: '待审核', class: 'bg-orange-50 text-orange-600' }
    case 1: return { text: '已通过', class: 'bg-primary-50 text-primary-600' }
    case 2: return { text: '已驳回', class: 'bg-rose-50 text-rose-600' }
    case 3: return { text: '已取消', class: 'bg-slate-50 text-slate-500' }
    default: return { text: '未知', class: 'bg-slate-50 text-slate-500' }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div v-if="loading" class="flex justify-center py-40">
    <Loader2 class="w-12 h-12 text-primary-500 animate-spin" />
  </div>
  <div v-else-if="user" class="grid grid-cols-1 lg:grid-cols-3 gap-12 pb-24">
    <!-- Left: Profile Info -->
    <div class="lg:col-span-1 space-y-8">
      <div class="bg-white rounded-[3rem] border border-slate-100 shadow-xl p-10 text-center relative overflow-hidden">
        <div class="absolute top-0 left-0 right-0 h-32 bg-gradient-to-r from-primary-400 to-primary-600"></div>
        <div class="relative pt-8">
          <div class="inline-block p-2 bg-white rounded-[2.5rem] shadow-lg mb-6">
            <img :src="user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.id" class="w-32 h-32 rounded-[2rem] bg-slate-50" alt="Avatar" />
          </div>
          <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">{{ user.nickname }}</h2>
          <p class="text-slate-400 font-medium mt-1">@{{ user.username }}</p>
          <div class="inline-flex items-center space-x-2 bg-primary-50 text-primary-600 px-4 py-1.5 rounded-full font-bold text-sm mt-6">
            <Shield class="w-4 h-4" />
            <span>阳光志愿者</span>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-6 mt-12 text-left">
          <div class="flex items-center space-x-4 p-4 bg-slate-50 rounded-2xl border border-slate-100 group hover:border-primary-200 transition-all">
            <div class="w-10 h-10 bg-white rounded-xl flex items-center justify-center shadow-sm group-hover:text-primary-500 transition-colors">
              <Mail class="w-5 h-5" />
            </div>
            <div>
              <div class="text-[10px] text-slate-400 font-bold uppercase tracking-widest">电子邮箱</div>
              <div class="text-sm font-bold text-slate-700">{{ user.email || '未设置' }}</div>
            </div>
          </div>
          <div class="flex items-center space-x-4 p-4 bg-slate-50 rounded-2xl border border-slate-100 group hover:border-primary-200 transition-all">
            <div class="w-10 h-10 bg-white rounded-xl flex items-center justify-center shadow-sm group-hover:text-primary-500 transition-colors">
              <Phone class="w-5 h-5" />
            </div>
            <div>
              <div class="text-[10px] text-slate-400 font-bold uppercase tracking-widest">联系电话</div>
              <div class="text-sm font-bold text-slate-700">{{ user.phone || '未设置' }}</div>
            </div>
          </div>
        </div>

        <div class="space-y-4 mt-10">
          <button v-if="isAdmin" @click="router.push('/admin/dashboard')" class="w-full py-4 bg-primary-500 text-white rounded-[1.5rem] font-bold text-lg hover:bg-primary-600 transition-all active:scale-95 flex items-center justify-center space-x-2 shadow-xl shadow-primary-100">
            <LayoutDashboard class="w-5 h-5" />
            <span>进入管理后台</span>
          </button>

          <button class="w-full py-4 bg-slate-900 text-white rounded-[1.5rem] font-bold text-lg hover:bg-slate-800 transition-all active:scale-95 flex items-center justify-center space-x-2 shadow-xl shadow-slate-200">
            <Settings class="w-5 h-5" />
            <span>编辑个人资料</span>
          </button>
        </div>
      </div>

      <!-- Menu Card -->
      <div class="bg-white rounded-[2.5rem] border border-slate-100 shadow-sm overflow-hidden">
        <div class="p-4 space-y-2">
          <button @click="handleLogout" class="w-full flex items-center justify-between p-4 rounded-2xl transition-all text-rose-500 hover:bg-rose-50">
            <div class="flex items-center space-x-4">
              <LogOut class="w-5 h-5" />
              <span class="font-bold">退出登录</span>
            </div>
            <ChevronRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- Right: Stats & Activities -->
    <div class="lg:col-span-2 space-y-10">
      <!-- Stats Grid -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div v-for="stat in stats" :key="stat.label" class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm flex flex-col items-center text-center group hover:shadow-xl hover:shadow-slate-100 transition-all duration-500">
          <div :class="['p-4 rounded-2xl mb-4', stat.bg]">
            <component :is="stat.icon" :class="['w-8 h-8', stat.color]" />
          </div>
          <div class="text-3xl font-extrabold text-slate-900 mb-1 tracking-tight group-hover:scale-110 transition-transform">{{ stat.value }}</div>
          <div class="text-slate-400 font-bold text-sm tracking-wide">{{ stat.label }}</div>
        </div>
      </div>

      <!-- Registration List -->
      <div class="bg-white rounded-[3rem] border border-slate-100 shadow-sm overflow-hidden">
        <div class="p-10 border-b border-slate-50 flex justify-between items-center">
          <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight">我的活动记录</h3>
          <button class="text-primary-600 font-bold hover:text-primary-700 text-sm">查看全部</button>
        </div>
        <div class="p-6 space-y-4">
          <div v-if="registrations.length === 0" class="text-center py-10 text-slate-400 font-medium">
            您还没有参加过任何活动哦
          </div>
          <div v-for="reg in registrations" :key="reg.id" class="group flex items-center justify-between p-6 bg-slate-50 hover:bg-white rounded-[2rem] border border-transparent hover:border-slate-100 hover:shadow-xl hover:shadow-slate-100 transition-all duration-500 cursor-pointer" @click="router.push('/activities/' + reg.activityId)">
            <div class="flex items-center space-x-6">
              <div class="w-14 h-14 bg-white rounded-2xl flex items-center justify-center shadow-sm overflow-hidden shrink-0">
                <img v-if="reg.activityCover" :src="reg.activityCover" class="w-full h-full object-cover" />
                <Calendar v-else class="w-7 h-7 text-primary-500" />
              </div>
              <div>
                <h4 class="text-lg font-bold text-slate-800 group-hover:text-primary-600 transition-colors line-clamp-1">{{ reg.activityTitle || '活动 ID: ' + reg.activityId }}</h4>
                <div class="flex items-center space-x-4 mt-1">
                  <p class="text-xs text-slate-400 font-medium">报名时间: {{ reg.createTime?.split('T')[0] }}</p>
                  <!-- Rewards display -->
                  <div v-if="reg.earnedPoints || reg.earnedDuration" class="flex items-center space-x-3">
                    <div v-if="reg.earnedPoints" class="flex items-center text-orange-500 font-bold text-xs">
                      <Shield class="w-3 h-3 mr-1" />
                      +{{ reg.earnedPoints }}
                    </div>
                    <div v-if="reg.earnedDuration" class="flex items-center text-blue-500 font-bold text-xs">
                      <Clock class="w-3 h-3 mr-1" />
                      {{ reg.earnedDuration }}h
                    </div>
                  </div>
                  <div v-else-if="reg.status === 1" class="flex items-center text-slate-300 font-bold text-xs">
                    待签到以获取奖励
                  </div>
                </div>
              </div>
            </div>
            <div class="flex items-center space-x-6">
              <span :class="['px-5 py-2 rounded-full font-bold text-xs shadow-sm', getStatusLabel(reg.status).class]">
                {{ getStatusLabel(reg.status).text }}
              </span>
              <ChevronRight class="w-5 h-5 text-slate-300 group-hover:text-primary-500 transition-colors" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
