<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Users, Star, ArrowRight, Heart, Sparkles, MapPin, Search } from 'lucide-vue-next'
import request from '../utils/request'

const router = useRouter()
const activities = ref<any[]>([])
const recommendedActivities = ref<any[]>([])
const stats = ref([
  { label: '累计活动', value: '0', icon: Calendar, color: 'text-blue-500', bg: 'bg-blue-50', key: 'totalActivities' },
  { label: '志愿者人数', value: '0', icon: Users, color: 'text-primary-600', bg: 'bg-primary-50', key: 'totalRegistrations' },
  { label: '五星好评', value: '98%', icon: Star, color: 'text-orange-500', bg: 'bg-orange-50', key: 'rating' }
])

// 默认图片映射
const DEFAULT_IMAGES: Record<number, string> = {
  1: 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&q=80&w=800', // 社区关爱
  2: 'https://images.unsplash.com/photo-1518391846015-55a9cc003b25?auto=format&fit=crop&q=80&w=800', // 绿色环保
  3: 'https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&q=80&w=800', // 助学支教
  4: 'https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&q=80&w=800', // 文化传承
  5: 'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?auto=format&fit=crop&q=80&w=800', // 动物保护
}
const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1469571486040-0bd991738221?auto=format&fit=crop&q=80&w=800'

// 获取活动展示图片
const getActivityImage = (activity: any) => {
  return activity.coverImage || DEFAULT_IMAGES[activity.categoryId] || FALLBACK_IMAGE
}

const fetchData = async () => {
  try {
    // 1. 获取统计数据
    const statsData: any = await request.get('/api/statistics/overall')
    stats.value[0].value = statsData.totalActivities || '0'
    stats.value[1].value = statsData.totalRegistrations || '0'

    // 2. 获取热门活动
    const activitiesData: any = await request.get('/api/activities', {
      params: { current: 1, size: 3, status: 2 }
    })
    activities.value = activitiesData.records

    // 3. 获取个性化推荐
    const recData: any = await request.get('/api/recommendations/user', {
      params: { limit: 4 }
    })
    recommendedActivities.value = recData
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="space-y-24 pb-24">
    <!-- Hero Section -->
    <section class="relative overflow-hidden rounded-[3rem] bg-slate-900 py-24 sm:py-32 shadow-2xl">
      <div class="absolute inset-0 opacity-20">
        <img 
          src="https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&q=80&w=2000" 
          class="h-full w-full object-cover"
          alt="Charity"
        />
        <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-slate-900/40 to-transparent"></div>
      </div>
      
      <div class="relative max-w-7xl mx-auto px-6 lg:px-8 text-center">
        <div class="inline-flex items-center space-x-2 bg-white/10 backdrop-blur-md px-4 py-2 rounded-full mb-8 border border-white/20 animate-fade-in">
          <Sparkles class="w-4 h-4 text-primary-400" />
          <span class="text-sm font-medium text-white/80 tracking-wide">汇聚爱心，温暖每一个角落</span>
        </div>
        
        <h1 class="text-5xl sm:text-7xl font-extrabold text-white tracking-tight mb-8 leading-[1.1]">
          让公益成为一种 <br />
          <span class="text-primary-400">阳光时尚</span> 的生活方式
        </h1>
        
        <p class="max-w-2xl mx-auto text-xl text-slate-300 leading-relaxed mb-12">
          加入阳光公益，与万千志愿者一起参与有意义的活动。发现您身边的美好，为社会贡献一份温暖。
        </p>
        
        <div class="flex flex-wrap justify-center gap-6">
          <button 
            @click="router.push('/activities')"
            class="px-8 py-4 bg-primary-500 text-white rounded-2xl font-bold text-lg shadow-xl shadow-primary-500/30 hover:bg-primary-600 hover:shadow-2xl transition-all active:scale-95 flex items-center space-x-2"
          >
            <span>立即探索活动</span>
            <ArrowRight class="w-5 h-5" />
          </button>
          <button class="px-8 py-4 bg-white/10 backdrop-blur-md text-white border border-white/20 rounded-2xl font-bold text-lg hover:bg-white/20 transition-all active:scale-95">
            了解我们
          </button>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div 
          v-for="stat in stats" 
          :key="stat.label"
          class="bg-white p-8 rounded-3xl shadow-sm border border-slate-100 flex items-center space-x-6 hover:shadow-md transition-shadow"
        >
          <div :class="['p-4 rounded-2xl', stat.bg]">
            <component :is="stat.icon" :class="['w-8 h-8', stat.color]" />
          </div>
          <div>
            <div class="text-3xl font-bold text-slate-900 mb-1">{{ stat.value }}</div>
            <div class="text-slate-500 font-medium">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Personalized Recommendations -->
    <section v-if="recommendedActivities.length > 0" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between mb-12">
        <div>
          <h2 class="text-4xl font-bold text-slate-900 mb-4 flex items-center space-x-3">
            <Sparkles class="w-10 h-10 text-primary-500" />
            <span>为您推荐</span>
          </h2>
          <p class="text-slate-500 text-lg">基于您的兴趣，为您匹配最适合的公益项目</p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
        <div 
          v-for="activity in recommendedActivities" 
          :key="activity.id"
          class="group bg-white rounded-3xl border border-slate-100 shadow-sm hover:shadow-xl transition-all duration-500 overflow-hidden cursor-pointer flex flex-col"
          @click="router.push(`/activities/${activity.id}`)"
        >
          <div class="relative h-48 overflow-hidden">
            <img 
              :src="getActivityImage(activity)" 
              class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" 
              alt="Cover"
            />
            <div class="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
            <div class="absolute bottom-4 left-4 right-4 translate-y-4 group-hover:translate-y-0 opacity-0 group-hover:opacity-100 transition-all">
              <span class="bg-primary-500 text-white px-4 py-1.5 rounded-full font-bold text-xs shadow-lg">立即参与</span>
            </div>
          </div>
          <div class="p-6 flex flex-col flex-grow">
            <div class="flex items-center text-primary-600 text-[10px] font-bold uppercase tracking-widest mb-2">
              {{ activity.locationName }}
            </div>
            <h3 class="text-lg font-bold text-slate-800 line-clamp-1 group-hover:text-primary-600 transition-colors mb-2">
              {{ activity.title }}
            </h3>
            <div class="flex items-center justify-between mt-auto pt-4 border-t border-slate-50">
              <div class="flex items-center text-slate-400 text-xs font-medium">
                <Users class="w-3.5 h-3.5 mr-1" />
                {{ activity.registeredCount }}人已报
              </div>
              <div class="text-orange-500 font-bold text-xs">+{{ activity.points }} 积分</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Popular Activities -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-end mb-12">
        <div>
          <h2 class="text-4xl font-bold text-slate-900 mb-4 flex items-center space-x-3">
            <Heart class="w-10 h-10 text-rose-500 fill-rose-500" />
            <span>热门公益活动</span>
          </h2>
          <p class="text-slate-500 text-lg">大家都在参与的暖心行动</p>
        </div>
        <button 
          @click="router.push('/activities')"
          class="text-primary-600 font-bold hover:text-primary-700 transition-colors flex items-center space-x-1 group"
        >
          <span>查看全部活动</span>
          <ArrowRight class="w-5 h-5 group-hover:translate-x-1 transition-transform" />
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
        <div 
          v-for="activity in activities" 
          :key="activity.id"
          class="group bg-white rounded-[2.5rem] border border-slate-100 shadow-sm hover:shadow-2xl hover:shadow-slate-200 transition-all duration-500 overflow-hidden cursor-pointer flex flex-col h-full"
          @click="router.push(`/activities/${activity.id}`)"
        >
          <div class="relative h-64 overflow-hidden">
            <img 
              :src="getActivityImage(activity)" 
              class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" 
              alt="Cover"
            />
            <div class="absolute top-6 right-6">
              <span class="bg-white/90 backdrop-blur-md px-4 py-2 rounded-2xl text-primary-600 font-bold text-sm shadow-lg">
                {{ activity.status === 2 ? '招募中' : '进行中' }}
              </span>
            </div>
          </div>

          <div class="p-8 flex flex-col flex-grow">
            <div class="flex items-center text-slate-400 text-sm mb-4 space-x-4">
              <div class="flex items-center">
                <Calendar class="w-4 h-4 mr-1.5 text-primary-500" />
                {{ activity.startTime?.split('T')[0] }}
              </div>
              <div class="flex items-center">
                <MapPin class="w-4 h-4 mr-1.5 text-primary-500" />
                {{ activity.locationName }}
              </div>
            </div>

            <h3 class="text-2xl font-bold text-slate-800 mb-4 group-hover:text-primary-600 transition-colors">
              {{ activity.title }}
            </h3>
            
            <p class="text-slate-500 line-clamp-2 leading-relaxed mb-6 flex-grow">
              {{ activity.summary }}
            </p>

            <div class="pt-6 border-t border-slate-50" v-if="activity.maxParticipants > 0">
              <div class="flex justify-between items-center mb-4">
                <span class="text-slate-500 font-medium">招募进度</span>
                <span class="text-primary-600 font-bold">{{ Math.round((activity.registeredCount / activity.maxParticipants) * 100) }}%</span>
              </div>
              <div class="h-2.5 w-full bg-slate-100 rounded-full overflow-hidden">
                <div 
                  class="h-full bg-gradient-to-r from-primary-400 to-primary-600 rounded-full transition-all duration-1000"
                  :style="{ width: `${(activity.registeredCount / activity.maxParticipants) * 100}%` }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Search Bar Section -->
    <section class="max-w-4xl mx-auto px-4">
      <div class="bg-white p-6 rounded-[2.5rem] shadow-2xl shadow-slate-200 border border-slate-100 flex flex-wrap md:flex-nowrap items-center gap-4">
        <div class="flex-grow flex items-center bg-slate-50 px-6 py-4 rounded-3xl border border-transparent focus-within:border-primary-400 transition-all">
          <Search class="w-6 h-6 text-slate-400 mr-4" />
          <input 
            type="text" 
            placeholder="搜索您感兴趣的活动或关键词..." 
            class="bg-transparent border-none outline-none text-slate-700 placeholder-slate-400 w-full text-lg"
          />
        </div>
        <button class="bg-primary-500 text-white px-10 py-4 rounded-3xl font-bold text-lg hover:bg-primary-600 transition-all shadow-lg shadow-primary-200 active:scale-95 shrink-0">
          搜索
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
@keyframes fade-in {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-fade-in {
  animation: fade-in 0.8s ease-out;
}
</style>
