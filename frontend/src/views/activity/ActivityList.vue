<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, MapPin, Calendar, Star, Users, ArrowRight, LayoutGrid, LayoutList } from 'lucide-vue-next'
import request from '../../utils/request'

const router = useRouter()
const viewMode = ref<'grid' | 'list'>('grid')
const categories = ref<any[]>([{ id: 0, name: '全部' }])
const activeCategoryId = ref(0)
const searchQuery = ref('')
const activities = ref<any[]>([])
const loading = ref(false)
const pagination = ref({
  current: 1,
  size: 8,
  total: 0
})

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

const fetchCategories = async () => {
  try {
    const data: any = await request.get('/api/activity-categories')
    categories.value = [{ id: 0, name: '全部' }, ...data]
  } catch (error) {
    console.error(error)
  }
}

const fetchActivities = async () => {
  loading.value = true
  try {
    const params: any = {
      current: pagination.value.current,
      size: pagination.value.size,
      status: 2 // 只看已发布的
    }
    if (activeCategoryId.value !== 0) {
      params.categoryId = activeCategoryId.value
    }
    if (searchQuery.value) {
      params.title = searchQuery.value
    }

    const data: any = await request.get('/api/activities', { params })
    activities.value = data.records
    pagination.value.total = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.value.current = page
  fetchActivities()
}

const handleSearch = () => {
  pagination.value.current = 1
  fetchActivities()
}

watch(activeCategoryId, () => {
  pagination.value.current = 1
  fetchActivities()
})

onMounted(() => {
  fetchCategories()
  fetchActivities()
})
</script>

<template>
  <div class="space-y-12 pb-24">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-8">
      <div>
        <h1 class="text-4xl font-extrabold text-slate-900 tracking-tight mb-4">活动广场</h1>
        <p class="text-slate-500 text-lg font-medium">发现身边的公益力量，参与让改变发生</p>
      </div>

      <!-- Filters -->
      <div class="flex items-center space-x-3 bg-white p-2 rounded-[2rem] shadow-sm border border-slate-100 shrink-0 overflow-x-auto max-w-full">
        <button 
          v-for="cat in categories" 
          :key="cat.id"
          @click="activeCategoryId = cat.id"
          :class="[
            'px-6 py-2.5 rounded-full font-bold text-sm transition-all duration-300 shrink-0',
            activeCategoryId === cat.id ? 'bg-primary-500 text-white shadow-lg shadow-primary-200' : 'text-slate-500 hover:text-primary-500'
          ]"
        >
          {{ cat.name }}
        </button>
      </div>
    </div>

    <!-- Search and Layout Control -->
    <div class="flex flex-col md:flex-row items-center gap-6">
      <div class="relative flex-grow w-full group">
        <div class="absolute inset-y-0 left-6 flex items-center text-slate-400 group-focus-within:text-primary-500 transition-colors">
          <Search class="w-6 h-6" />
        </div>
        <input 
          v-model="searchQuery"
          type="text" 
          placeholder="搜索活动名称、关键词或地点..." 
          @keyup.enter="handleSearch"
          class="w-full bg-white border border-slate-100 shadow-sm focus:border-primary-400 focus:shadow-xl focus:shadow-primary-100 rounded-[2.5rem] py-5 pl-16 pr-8 outline-none text-slate-800 transition-all font-medium text-lg"
        />
      </div>

      <div class="flex items-center space-x-2 bg-white p-2 rounded-[2rem] shadow-sm border border-slate-100 shrink-0">
        <button 
          @click="viewMode = 'grid'"
          :class="['p-3 rounded-2xl transition-all', viewMode === 'grid' ? 'bg-primary-50 text-primary-600' : 'text-slate-400 hover:bg-slate-50']"
        >
          <LayoutGrid class="w-6 h-6" />
        </button>
        <button 
          @click="viewMode = 'list'"
          :class="['p-3 rounded-2xl transition-all', viewMode === 'list' ? 'bg-primary-50 text-primary-600' : 'text-slate-400 hover:bg-slate-50']"
        >
          <LayoutList class="w-6 h-6" />
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent"></div>
    </div>

    <!-- Empty State -->
    <div v-else-if="activities.length === 0" class="text-center py-20 bg-white rounded-[3rem] border border-slate-100 shadow-sm">
      <div class="mb-6 inline-block p-6 bg-slate-50 rounded-full text-slate-300">
        <Search class="w-12 h-12" />
      </div>
      <h3 class="text-xl font-bold text-slate-800 mb-2">暂无匹配活动</h3>
      <p class="text-slate-400">换个关键词试试，或者看看其他分类吧</p>
    </div>

    <!-- Grid View -->
    <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-10">
      <div 
        v-for="activity in activities" 
        :key="activity.id"
        class="group bg-white rounded-[2.5rem] border border-slate-100 shadow-sm hover:shadow-2xl hover:shadow-slate-200 transition-all duration-500 overflow-hidden cursor-pointer flex flex-col h-full"
        @click="router.push(`/activities/${activity.id}`)"
      >
        <div class="relative h-56 overflow-hidden">
          <img 
            :src="getActivityImage(activity)" 
            class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" 
            alt="Cover"
          />
          <div class="absolute top-4 right-4">
            <span class="bg-white/90 backdrop-blur-md px-3 py-1.5 rounded-2xl text-primary-600 font-bold text-xs shadow-lg">
              招募中
            </span>
          </div>
        </div>

        <div class="p-6 flex flex-col flex-grow">
          <div class="flex items-center text-slate-400 text-xs mb-3 space-x-3">
            <div class="flex items-center">
              <Calendar class="w-3.5 h-3.5 mr-1 text-primary-500" />
              {{ activity.startTime?.split('T')[0] }}
            </div>
            <div class="flex items-center">
              <MapPin class="w-3.5 h-3.5 mr-1 text-primary-500" />
              {{ activity.locationName }}
            </div>
          </div>

          <h3 class="text-xl font-bold text-slate-800 mb-3 group-hover:text-primary-600 transition-colors line-clamp-1">
            {{ activity.title }}
          </h3>
          
          <p class="text-slate-500 text-sm line-clamp-2 leading-relaxed mb-6 flex-grow">
            {{ activity.summary }}
          </p>

          <div class="pt-5 border-t border-slate-50" v-if="activity.maxParticipants > 0">
            <div class="flex justify-between items-center mb-3">
              <div class="flex items-center space-x-1">
                <Users class="w-4 h-4 text-slate-400" />
                <span class="text-sm font-bold text-slate-600">{{ activity.registeredCount }} / {{ activity.maxParticipants }}</span>
              </div>
              <div class="flex items-center space-x-1">
                <Star class="w-4 h-4 text-yellow-400 fill-yellow-400" />
                <span class="text-sm font-bold text-slate-800">4.8</span>
              </div>
            </div>
            <div class="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
              <div 
                class="h-full bg-gradient-to-r from-primary-400 to-primary-600 rounded-full transition-all duration-1000"
                :style="{ width: `${(activity.registeredCount / activity.maxParticipants) * 100}%` }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- List View -->
    <div v-else class="space-y-6">
      <div 
        v-for="activity in activities" 
        :key="activity.id"
        class="group bg-white p-6 rounded-[2.5rem] border border-slate-100 shadow-sm hover:shadow-2xl hover:shadow-slate-200 transition-all duration-500 flex items-center gap-8 cursor-pointer"
        @click="router.push(`/activities/${activity.id}`)"
      >
        <div class="w-64 h-44 rounded-[2rem] overflow-hidden shrink-0">
        <img 
          :src="getActivityImage(activity)" 
          class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" 
          alt="Cover"
        />
      </div>

        <div class="flex-grow">
          <div class="flex items-center text-slate-400 text-sm mb-3 space-x-6">
            <div class="flex items-center">
              <Calendar class="w-4 h-4 mr-1.5 text-primary-500" />
              {{ activity.startTime?.replace('T', ' ') }}
            </div>
            <div class="flex items-center">
              <MapPin class="w-4 h-4 mr-1.5 text-primary-500" />
              {{ activity.locationName }}
            </div>
            <div class="flex items-center">
              <Star class="w-4 h-4 mr-1.5 text-yellow-400 fill-yellow-400" />
              <span class="font-bold text-slate-800">4.8</span>
            </div>
          </div>

          <h3 class="text-2xl font-bold text-slate-800 mb-3 group-hover:text-primary-600 transition-colors">
            {{ activity.title }}
          </h3>
          
          <p class="text-slate-500 line-clamp-2 leading-relaxed mb-6 max-w-2xl">
            {{ activity.summary }}
          </p>

          <div class="flex items-center space-x-8">
            <div class="w-64" v-if="activity.maxParticipants > 0">
              <div class="flex justify-between items-center mb-2 text-xs">
                <span class="text-slate-500 font-medium">招募人数: {{ activity.registeredCount }} / {{ activity.maxParticipants }}</span>
                <span class="text-primary-600 font-bold">{{ Math.round((activity.registeredCount / activity.maxParticipants) * 100) }}%</span>
              </div>
              <div class="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                <div 
                  class="h-full bg-gradient-to-r from-primary-400 to-primary-600 rounded-full"
                  :style="{ width: `${(activity.registeredCount / activity.maxParticipants) * 100}%` }"
                ></div>
              </div>
            </div>
            <button class="ml-auto flex items-center space-x-2 text-primary-600 font-bold hover:text-primary-700 transition-all">
              <span>查看详情</span>
              <ArrowRight class="w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="flex justify-center pt-12" v-if="pagination.total > pagination.size">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
        class="sunny-pagination"
      />
    </div>
  </div>
</template>

<style scoped>
.sunny-pagination :deep(.el-pager li) {
  @apply w-12 h-12 rounded-2xl font-bold text-slate-600 bg-white border border-slate-100 mx-1 flex items-center justify-center transition-all hover:bg-slate-50;
}
.sunny-pagination :deep(.el-pager li.is-active) {
  @apply bg-primary-500 text-white border-primary-500 shadow-lg shadow-primary-200;
}
.sunny-pagination :deep(button) {
  @apply w-12 h-12 rounded-2xl bg-white border border-slate-100 mx-1 flex items-center justify-center transition-all hover:bg-slate-50;
}
</style>
