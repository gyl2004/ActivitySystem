<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  LayoutDashboard, 
  Calendar, 
  Users, 
  MessageSquare, 
  Settings, 
  LogOut, 
  Bell,
  Sun,
  Menu,
  X
} from 'lucide-vue-next'
import request from '../utils/request'
import { ElMessage, ElNotification } from 'element-plus'

const router = useRouter()
const isSidebarOpen = ref(true)
const user = ref<any>(null)
let socket: WebSocket | null = null

const initWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//localhost:8080/api/ws/notification`
  
  socket = new WebSocket(wsUrl)
  
  socket.onmessage = (event) => {
    let payload: any = null
    try {
      payload = JSON.parse(event.data)
    } catch (e) {
      payload = null
    }

    if (payload && payload.type === 'new_activity' && payload.activityId) {
      let notification: any
      notification = ElNotification({
        title: '新活动发布',
        message: payload.title || '点击查看详情',
        type: 'success',
        duration: 6000,
        position: 'bottom-right',
        onClick: () => {
          notification?.close?.()
          router.push(`/activities/${payload.activityId}`)
        }
      })
      return
    }

    ElNotification({
      title: '新消息',
      message: event.data,
      type: 'info',
      duration: 5000,
      position: 'bottom-right'
    })
  }
  
  socket.onclose = () => {
    console.log('WebSocket connection closed')
    // 重连逻辑
    setTimeout(() => initWebSocket(), 5000)
  }
}

const menuItems = [
  { label: '控制台', icon: LayoutDashboard, path: '/admin/dashboard' },
  { label: '活动管理', icon: Calendar, path: '/admin/activities' },
  { label: '报名审核', icon: Users, path: '/admin/registrations' },
  { label: '评价管理', icon: MessageSquare, path: '/admin/reviews' },
  { label: '系统设置', icon: Settings, path: '/admin/settings' },
]

const fetchUser = async () => {
  try {
    const data: any = await request.get('/api/auth/me')
    user.value = data.user
  } catch (error) {
    console.error(error)
    router.push('/login')
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('tokenPrefix')
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  fetchUser()
  initWebSocket()
})

onUnmounted(() => {
  if (socket) {
    socket.close()
  }
})
</script>

<template>
  <div class="min-h-screen bg-slate-50 flex">
    <!-- Sidebar -->
    <aside 
      :class="[
        'bg-slate-900 text-white w-72 transition-all duration-300 fixed inset-y-0 z-50 lg:relative',
        isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:w-20 lg:translate-x-0'
      ]"
    >
      <div class="h-full flex flex-col">
        <!-- Logo -->
        <div class="h-20 flex items-center px-6 mb-8">
          <div class="p-2 bg-primary-500 rounded-xl mr-3 shrink-0">
            <Sun class="w-6 h-6 text-white" />
          </div>
          <span v-if="isSidebarOpen" class="text-xl font-bold tracking-tight">阳光公益管理</span>
        </div>

        <!-- Menu -->
        <nav class="flex-grow px-4 space-y-2">
          <button 
            v-for="item in menuItems" 
            :key="item.path"
            @click="router.push(item.path)"
            :class="[
              'w-full flex items-center px-4 py-4 rounded-2xl transition-all group hover:bg-white/10',
              router.currentRoute.value.path === item.path ? 'bg-primary-500 text-white' : 'text-slate-400'
            ]"
          >
            <component :is="item.icon" class="w-6 h-6 shrink-0" />
            <span v-if="isSidebarOpen" class="ml-4 font-bold">{{ item.label }}</span>
          </button>
        </nav>

        <!-- Bottom -->
        <div class="p-4 mt-auto">
          <button 
            @click="handleLogout"
            class="w-full flex items-center px-4 py-4 rounded-2xl text-rose-400 hover:bg-rose-500/10 transition-all"
          >
            <LogOut class="w-6 h-6 shrink-0" />
            <span v-if="isSidebarOpen" class="ml-4 font-bold">退出登录</span>
          </button>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-grow flex flex-col min-w-0">
      <!-- Topbar -->
      <header class="h-20 bg-white border-b border-slate-200 flex items-center justify-between px-8 sticky top-0 z-40">
        <button 
          @click="isSidebarOpen = !isSidebarOpen"
          class="p-2 text-slate-500 hover:bg-slate-50 rounded-xl transition-all"
        >
          <Menu v-if="!isSidebarOpen" class="w-6 h-6" />
          <X v-else class="w-6 h-6 lg:hidden" />
          <Menu v-if="isSidebarOpen" class="w-6 h-6 hidden lg:block" />
        </button>

        <div class="flex items-center space-x-6">
          <button class="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 rounded-xl transition-all relative">
            <Bell class="w-6 h-6" />
            <span class="absolute top-2 right-2 w-2 h-2 bg-rose-500 rounded-full border-2 border-white"></span>
          </button>
          <div class="h-8 w-[1px] bg-slate-200"></div>
          <div class="flex items-center space-x-3 cursor-pointer" v-if="user">
            <img :src="user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.id" class="w-10 h-10 rounded-xl bg-slate-100" />
            <div class="hidden sm:block">
              <div class="text-sm font-bold text-slate-800">{{ user.nickname }}</div>
              <div class="text-[10px] text-slate-400 font-bold uppercase">{{ user.username }}</div>
            </div>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <main class="p-8">
        <router-view />
      </main>
    </div>
  </div>
</template>
