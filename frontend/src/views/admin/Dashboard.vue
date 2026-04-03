<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Users, 
  Calendar, 
  TrendingUp, 
  ArrowUpRight, 
  ArrowDownRight,
  MoreVertical,
  CheckCircle,
  Loader2
} from 'lucide-vue-next'
import request from '../../utils/request'
import * as echarts from 'echarts'

const router = useRouter()
const loading = ref(true)
const userPermissions = ref<string[]>([])
const stats = ref([
  { label: '总活动数', value: '0', change: '+0%', isUp: true, icon: Calendar, color: 'text-primary-600', bg: 'bg-primary-50', key: 'totalActivities' },
  { label: '总报名数', value: '0', change: '+0%', isUp: true, icon: Users, color: 'text-blue-500', bg: 'bg-blue-50', key: 'totalRegistrations' },
  { label: '总签到数', value: '0', change: '+0%', isUp: true, icon: CheckCircle, color: 'text-emerald-500', bg: 'bg-emerald-50', key: 'totalCheckins' },
  { label: '总用户数', value: '0', change: '+0%', isUp: true, icon: TrendingUp, color: 'text-rose-500', bg: 'bg-rose-50', key: 'totalUsers' },
])

const recentActivities = ref<any[]>([])
const trendRef = ref<HTMLDivElement | null>(null)
const categoryRef = ref<HTMLDivElement | null>(null)
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

const renderCharts = async (statsData: any) => {
  await nextTick()
  if (trendRef.value) {
    if (!trendChart) {
      trendChart = echarts.init(trendRef.value)
    }
    const trend = statsData.registrationTrend || { dates: [], counts: [] }
    trendChart.setOption({
      grid: { left: 24, right: 18, top: 20, bottom: 28 },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: trend.dates || [],
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b', fontWeight: 700 }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8', fontWeight: 700 }
      },
      series: [
        {
          name: '报名人数',
          type: 'line',
          data: trend.counts || [],
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 3, color: '#22c55e' },
          itemStyle: { color: '#22c55e' },
          areaStyle: { color: 'rgba(34, 197, 94, 0.12)' }
        }
      ]
    })
  }

  if (categoryRef.value) {
    if (!categoryChart) {
      categoryChart = echarts.init(categoryRef.value)
    }
    const data = statsData.categoryDistribution || []
    categoryChart.setOption({
      tooltip: { trigger: 'item' },
      legend: {
        bottom: 0,
        textStyle: { color: '#64748b', fontWeight: 700 }
      },
      series: [
        {
          name: '分类占比',
          type: 'pie',
          radius: ['45%', '70%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 800 } },
          labelLine: { show: false },
          data
        }
      ]
    })
  }
}

const isSuperAdmin = ref(false)

const fetchData = async () => {
  try {
    const userData: any = await request.get('/api/auth/me')
    userPermissions.value = userData.permissions || []
    isSuperAdmin.value = (userData.roles || []).includes('super_admin')

    const statsData: any = await request.get('/api/statistics/overall')
    stats.value[0].value = statsData.totalActivities || '0'
    stats.value[1].value = statsData.totalRegistrations || '0'
    stats.value[2].value = statsData.totalCheckins || '0'
    stats.value[3].value = statsData.totalUsers || '0'
    
    // 先关闭 loading，再渲染图表
    loading.value = false
    await renderCharts(statsData)

    const activitiesData: any = await request.get('/api/activities', {
      params: { current: 1, size: 5 }
    })
    recentActivities.value = activitiesData.records
  } catch (error) {
    console.error(error)
    loading.value = false
  }
}

const getStatusLabel = (status: number) => {
  const statusMap: any = {
    0: { text: '草稿', color: 'text-slate-500', bg: 'bg-slate-100' },
    1: { text: '待发布', color: 'text-orange-600', bg: 'bg-orange-50' },
    2: { text: '已发布', color: 'text-primary-600', bg: 'bg-primary-50' },
    3: { text: '进行中', color: 'text-blue-600', bg: 'bg-blue-50' },
    4: { text: '已结束', color: 'text-slate-400', bg: 'bg-slate-100' },
    5: { text: '已取消', color: 'text-rose-600', bg: 'bg-rose-50' }
  }
  return statusMap[status] || statusMap[0]
}

const handleResize = () => {
  trendChart?.resize()
  categoryChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
  trendChart = null
  categoryChart = null
})
</script>

<template>
  <div v-if="loading" class="flex justify-center py-40">
    <Loader2 class="w-12 h-12 text-primary-500 animate-spin" />
  </div>
  <div v-else class="space-y-10 pb-12">
    <!-- Header -->
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-6">
      <div>
        <h1 class="text-3xl font-extrabold text-slate-900 tracking-tight mb-2">管理控制台</h1>
        <p class="text-slate-400 font-medium">欢迎回来，这是今天的公益数据概览。</p>
      </div>
      <div class="flex items-center space-x-3 bg-white p-2 rounded-2xl shadow-sm border border-slate-100">
        <button 
          v-if="isSuperAdmin || userPermissions.includes('activity:create')"
          @click="router.push('/admin/activities')"
          class="px-6 py-2.5 bg-primary-500 text-white rounded-xl font-bold text-sm shadow-lg shadow-primary-200 hover:bg-primary-600 transition-all active:scale-95"
        >
          发布新活动
        </button>
        <button 
          v-if="isSuperAdmin || userPermissions.includes('activity:export')"
          class="px-6 py-2.5 bg-slate-50 text-slate-600 rounded-xl font-bold text-sm hover:bg-slate-100 transition-all"
        >
          导出报表
        </button>
      </div>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-8">
      <div v-for="stat in stats" :key="stat.label" class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm hover:shadow-xl hover:shadow-slate-100 transition-all duration-500 group">
        <div class="flex justify-between items-start mb-6">
          <div :class="['p-4 rounded-2xl group-hover:scale-110 transition-transform', stat.bg]">
            <component :is="stat.icon" :class="['w-7 h-7', stat.color]" />
          </div>
          <button class="p-2 text-slate-300 hover:text-slate-500 hover:bg-slate-50 rounded-xl transition-all">
            <MoreVertical class="w-5 h-5" />
          </button>
        </div>
        <div>
          <div class="text-slate-400 font-bold text-xs uppercase tracking-widest mb-1">{{ stat.label }}</div>
          <div class="flex items-baseline space-x-3">
            <span class="text-3xl font-extrabold text-slate-900 tracking-tight">{{ stat.value }}</span>
            <span :class="['text-xs font-bold flex items-center', stat.isUp ? 'text-emerald-500' : 'text-rose-500']">
              <ArrowUpRight v-if="stat.isUp" class="w-3.5 h-3.5 mr-1" />
              <ArrowDownRight v-else class="w-3.5 h-3.5 mr-1" />
              {{ stat.change }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Placeholder & Recent List -->
    <div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
      <!-- Activity Trend -->
      <div class="xl:col-span-2 bg-white rounded-[3rem] border border-slate-100 shadow-sm p-10">
        <div class="flex justify-between items-center mb-10">
          <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight">近 7 天报名趋势</h3>
        </div>
        <div ref="trendRef" class="h-72 w-full"></div>
      </div>

      <!-- Recent Activities -->
      <div class="bg-white rounded-[3rem] border border-slate-100 shadow-sm p-10">
        <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight mb-8">分类占比</h3>
        <div ref="categoryRef" class="h-56 w-full"></div>
        <h3 class="text-2xl font-extrabold text-slate-900 tracking-tight mb-8 mt-10">最近更新</h3>
        <div class="space-y-6">
          <div v-if="recentActivities.length === 0" class="text-center py-10 text-slate-400">
            暂无活动数据
          </div>
          <div v-for="act in recentActivities" :key="act.id" class="flex items-center justify-between p-4 bg-slate-50 hover:bg-white rounded-2xl border border-transparent hover:border-slate-100 hover:shadow-lg transition-all cursor-pointer group">
            <div class="flex items-center space-x-4">
              <div class="w-12 h-12 bg-white rounded-xl flex items-center justify-center shadow-sm">
                <Calendar class="w-6 h-6 text-primary-500" />
              </div>
              <div class="min-w-0">
                <div class="text-sm font-bold text-slate-800 group-hover:text-primary-600 transition-colors truncate max-w-[120px]">{{ act.title }}</div>
                <div class="text-[10px] text-slate-400 font-bold uppercase">{{ act.createTime?.split('T')[0] }}</div>
              </div>
            </div>
            <span :class="['px-3 py-1 rounded-full font-bold text-[10px] shadow-sm shrink-0', getStatusLabel(act.status).bg, getStatusLabel(act.status).color]">
              {{ getStatusLabel(act.status).text }}
            </span>
          </div>
        </div>
        <button 
          v-if="isSuperAdmin || userPermissions.includes('activity:manage')"
          @click="router.push('/admin/activities')"
          class="w-full mt-10 py-4 border-2 border-slate-50 text-slate-500 rounded-2xl font-bold text-sm hover:bg-slate-50 transition-all"
        >
          查看所有活动
        </button>
      </div>
    </div>
  </div>
</template>
