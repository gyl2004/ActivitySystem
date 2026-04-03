<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sun, LogIn, User, Search, LayoutDashboard, LogOut } from 'lucide-vue-next'
import request from '../utils/request'
import { ElMessage, ElNotification } from 'element-plus'

const router = useRouter()
const userInfo = ref<any>(null)
const isAdmin = ref(false)
let socket: WebSocket | null = null
const wsIdentity = ref('public')
let shouldReconnect = true

const initWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//localhost:8080/api/ws/notification/${encodeURIComponent(wsIdentity.value)}`
  
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

    if (payload && payload.type === 'checkin') {
      ElNotification({
        title: '新签到',
        message: `${payload.nickname} 刚刚完成了签到！`,
        type: 'success',
        duration: 4000,
        position: 'bottom-right'
      })
      return
    }

    ElNotification({
      title: '新消息',
      message: payload && payload.message ? payload.message : (typeof event.data === 'string' && event.data.startsWith('{') ? '您有一条新消息' : event.data),
      type: 'info',
      duration: 5000,
      position: 'bottom-right'
    })
  }
  
  socket.onclose = () => {
    console.log('WebSocket connection closed')
    if (shouldReconnect) {
      setTimeout(() => initWebSocket(), 5000)
    }
  }
}

const fetchUserInfo = async () => {
  const token = localStorage.getItem('token')
  if (!token) return
  
  try {
    const data: any = await request.get('/api/auth/me')
    userInfo.value = data.user
    isAdmin.value = data.roles.includes('admin') || data.roles.includes('super_admin')
    wsIdentity.value = String(data.user?.id || 'public')
    if (socket) {
      socket.close()
    } else {
      initWebSocket()
    }
  } catch (error) {
    console.error(error)
    localStorage.removeItem('token')
    localStorage.removeItem('tokenPrefix')
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('tokenPrefix')
  userInfo.value = null
  isAdmin.value = false
  wsIdentity.value = 'public'
  if (socket) {
    socket.close()
  }
  ElMessage.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  fetchUserInfo()
  initWebSocket()
})

onUnmounted(() => {
  if (socket) {
    shouldReconnect = false
    socket.close()
  }
})
</script>

<template>
  <div class="min-h-screen flex flex-col bg-slate-50">
    <!-- Navbar -->
    <header class="bg-white/80 backdrop-blur-md sticky top-0 z-50 border-b border-slate-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo -->
          <div class="flex items-center space-x-2 cursor-pointer" @click="router.push('/')">
            <div class="p-2 bg-primary-500 rounded-xl shadow-lg shadow-primary-200">
              <Sun class="w-6 h-6 text-white" />
            </div>
            <span class="text-xl font-bold bg-gradient-to-r from-primary-600 to-primary-400 bg-clip-text text-transparent">
              阳光公益
            </span>
          </div>

          <!-- Desktop Menu -->
          <nav class="hidden md:flex items-center space-x-8">
            <router-link to="/" class="text-slate-600 hover:text-primary-600 font-medium transition-colors">首页</router-link>
            <router-link to="/activities" class="text-slate-600 hover:text-primary-600 font-medium transition-colors">活动广场</router-link>
            <router-link to="/profile" class="text-slate-600 hover:text-primary-600 font-medium transition-colors">个人中心</router-link>
            <router-link v-if="isAdmin" to="/admin/dashboard" class="flex items-center space-x-1 text-primary-600 hover:text-primary-700 font-bold transition-colors">
              <LayoutDashboard class="w-4 h-4" />
              <span>管理后台</span>
            </router-link>
          </nav>

          <!-- Action Buttons -->
          <div class="flex items-center space-x-4">
            <button class="p-2 text-slate-400 hover:text-primary-500 hover:bg-primary-50 rounded-full transition-all">
              <Search class="w-5 h-5" />
            </button>
            <div class="h-6 w-[1px] bg-slate-200"></div>
            
            <template v-if="!userInfo">
              <button 
                @click="router.push('/login')"
                class="flex items-center space-x-2 bg-primary-500 text-white px-5 py-2 rounded-full font-semibold shadow-md shadow-primary-200 hover:bg-primary-600 hover:shadow-lg transition-all active:scale-95"
              >
                <LogIn class="w-4 h-4" />
                <span>登录</span>
              </button>
            </template>
            <template v-else>
              <el-dropdown trigger="click">
                <div class="flex items-center space-x-3 cursor-pointer group">
                  <img :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + userInfo.id" class="w-9 h-9 rounded-xl bg-slate-100 ring-2 ring-transparent group-hover:ring-primary-100 transition-all" />
                  <span class="hidden sm:block font-bold text-slate-700 text-sm group-hover:text-primary-600 transition-colors">{{ userInfo.nickname }}</span>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="router.push('/profile')">
                      <div class="flex items-center space-x-2">
                        <User class="w-4 h-4" />
                        <span>个人资料</span>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item v-if="isAdmin" @click="router.push('/admin/dashboard')">
                      <div class="flex items-center space-x-2 text-primary-600">
                        <LayoutDashboard class="w-4 h-4" />
                        <span>进入管理后台</span>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleLogout">
                      <div class="flex items-center space-x-2 text-rose-500">
                        <LogOut class="w-4 h-4" />
                        <span>退出登录</span>
                      </div>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <router-view v-slot="{ Component }">
          <transition 
            name="fade-transform" 
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- Footer -->
    <footer class="bg-white border-t border-slate-200 py-12 mt-auto">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-12">
          <div class="col-span-1 md:col-span-4 text-center flex flex-col items-center">
            <div class="flex items-center justify-center space-x-2 mb-6">
              <div class="p-2 bg-primary-500 rounded-lg">
                <Sun class="w-5 h-5 text-white" />
              </div>
              <span class="text-xl font-bold text-slate-800">阳光公益</span>
            </div>
            <p class="text-slate-500 max-w-sm leading-relaxed">
              汇聚微光，点亮世界。我们致力于为每一位公益参与者提供最纯粹、最高效的活动管理平台。
            </p>
          </div>
        </div>
        <div class="border-t border-slate-100 mt-12 pt-8 text-center text-slate-400 text-sm">
          &copy; 2026 阳光公益管理系统.
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
