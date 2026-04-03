<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, X, MoreVertical, Shield, Clock, AlertCircle, Download, Users } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadBlob } from '../../utils/download'

const loading = ref(true)
const registrations = ref<any[]>([])
const statsMap = ref<any>({})
const pagination = ref({ current: 1, size: 10, total: 0 })
const activeStatus = ref<number | null>(null) // null 表示全部
const exporting = ref(false)

const fetchStats = async () => {
  try {
    const data: any = await request.get('/api/registrations/statistics')
    statsMap.value = data
  } catch (error) {
    console.error(error)
  }
}

const fetchRegistrations = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/registrations', {
      params: {
        current: pagination.value.current,
        size: pagination.value.size,
        status: activeStatus.value
      }
    })
    registrations.value = data.records
    pagination.value.total = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const blob: any = await request.get('/api/registrations/export', {
      params: {
        status: activeStatus.value
      },
      responseType: 'blob'
    })
    downloadBlob(blob, `报名数据_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error(error)
  } finally {
    exporting.value = false
  }
}

const handleStatusFilter = (status: number | null) => {
  activeStatus.value = status
  pagination.value.current = 1
  fetchRegistrations()
}

const handleAudit = (id: number, status: number) => {
  const action = status === 1 ? '通过' : '驳回'
  ElMessageBox.confirm(`确定要${action}该报名申请吗？`, '审核确认', { type: status === 1 ? 'success' : 'warning' })
    .then(async () => {
      await request.put(`/api/registrations/${id}/audit`, {
        status: status,
        auditRemark: action + '申请'
      })
      ElMessage.success(`已${action}`)
      fetchStats()
      fetchRegistrations()
    })
}

const getStatusLabel = (status: number) => {
  const statusMap: any = {
    0: { text: '待审核', icon: Clock, color: 'text-orange-500', bg: 'bg-orange-50', border: 'border-orange-100' },
    1: { text: '已通过', icon: Shield, color: 'text-primary-600', bg: 'bg-primary-50', border: 'border-primary-100' },
    2: { text: '已驳回', icon: X, color: 'text-rose-500', bg: 'bg-rose-50', border: 'border-rose-100' },
    3: { text: '已取消', icon: AlertCircle, color: 'text-slate-400', bg: 'bg-slate-50', border: 'border-slate-100' },
    4: { text: '候补中', icon: Users, color: 'text-indigo-600', bg: 'bg-indigo-50', border: 'border-indigo-100' }
  }
  return statusMap[status] || statusMap[0]
}

onMounted(() => {
  fetchStats()
  fetchRegistrations()
})
</script>

<template>
  <div class="space-y-8">
    <div>
      <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">报名审核</h2>
      <p class="text-slate-500 font-medium mt-1">审核志愿者的活动申请，确保活动质量。</p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div 
        @click="handleStatusFilter(null)"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeStatus === null ? 'bg-slate-900 border-slate-900 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeStatus === null ? 'bg-white/10' : 'bg-slate-50']"><AlertCircle class="w-6 h-6" /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeStatus === null ? 'text-slate-300' : 'text-slate-400']">全部报名</div>
          <div class="text-2xl font-extrabold">{{ Object.values(statsMap).reduce((a: any, b: any) => a + b, 0) }}</div>
        </div>
      </div>
      <div 
        @click="handleStatusFilter(0)"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeStatus === 0 ? 'bg-orange-500 border-orange-500 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeStatus === 0 ? 'bg-white/10' : 'bg-orange-50']"><Clock class="w-6 h-6 text-orange-500" v-if="activeStatus !== 0" /><Clock class="w-6 h-6 text-white" v-else /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeStatus === 0 ? 'text-orange-100' : 'text-slate-400']">待审核</div>
          <div class="text-2xl font-extrabold">{{ statsMap['0'] || 0 }}</div>
        </div>
      </div>
      <div 
        @click="handleStatusFilter(1)"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeStatus === 1 ? 'bg-primary-500 border-primary-500 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeStatus === 1 ? 'bg-white/10' : 'bg-primary-50']"><Shield class="w-6 h-6 text-primary-600" v-if="activeStatus !== 1" /><Shield class="w-6 h-6 text-white" v-else /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeStatus === 1 ? 'text-primary-100' : 'text-slate-400']">已通过</div>
          <div class="text-2xl font-extrabold">{{ statsMap['1'] || 0 }}</div>
        </div>
      </div>
      <div 
        @click="handleStatusFilter(2)"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeStatus === 2 ? 'bg-rose-500 border-rose-500 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeStatus === 2 ? 'bg-white/10' : 'bg-rose-50']"><X class="w-6 h-6 text-rose-500" v-if="activeStatus !== 2" /><X class="w-6 h-6 text-white" v-else /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeStatus === 2 ? 'text-rose-100' : 'text-slate-400']">已驳回</div>
          <div class="text-2xl font-extrabold">{{ statsMap['2'] || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- Registration List -->
    <div class="bg-white rounded-[2.5rem] border border-slate-100 shadow-sm overflow-hidden p-8 space-y-6">
      <div class="flex justify-end">
        <button
          @click="handleExport"
          :disabled="exporting"
          class="flex items-center space-x-2 px-5 py-2.5 rounded-2xl font-bold border border-slate-200 text-slate-600 hover:bg-slate-50 transition-all disabled:opacity-70"
        >
          <Download class="w-4 h-4" />
          <span>{{ exporting ? '导出中...' : '导出报名数据' }}</span>
        </button>
      </div>
      <div v-if="registrations.length === 0" class="text-center py-20 text-slate-400 font-medium">
        暂无待处理的报名申请
      </div>
      
      <div 
        v-for="reg in registrations" 
        :key="reg.id"
        class="group flex flex-col md:flex-row md:items-center justify-between p-6 bg-slate-50 hover:bg-white rounded-[2rem] border border-transparent hover:border-slate-100 hover:shadow-xl hover:shadow-slate-100 transition-all duration-500"
      >
        <div class="flex items-center space-x-6">
          <img :src="reg.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + reg.userId" class="w-16 h-16 rounded-2xl bg-white shadow-sm" />
          <div class="space-y-1">
            <div class="flex items-center space-x-3">
              <span class="text-lg font-extrabold text-slate-800">{{ reg.nickname || '用户 ID: ' + reg.userId }}</span>
              <div :class="['px-3 py-1 rounded-full font-bold text-[10px] border flex items-center space-x-1', getStatusLabel(reg.status).bg, getStatusLabel(reg.status).color, getStatusLabel(reg.status).border]">
                <component :is="getStatusLabel(reg.status).icon" class="w-3 h-3" />
                <span>{{ getStatusLabel(reg.status).text }}</span>
              </div>
            </div>
            <p class="text-sm text-slate-500 font-medium">活动：{{ reg.activityTitle || '活动 ID: ' + reg.activityId }} • 提交于 {{ reg.createTime?.split('T')[0] }}</p>
            <p class="text-xs text-slate-400 line-clamp-1 italic">“ {{ reg.remark || '无备注' }} ”</p>
          </div>
        </div>

        <div class="flex items-center space-x-3 mt-4 md:mt-0">
          <button 
            v-if="reg.status === 0"
            @click="handleAudit(reg.id, 1)"
            class="flex items-center space-x-2 bg-primary-500 text-white px-5 py-2.5 rounded-xl font-bold shadow-lg shadow-primary-200 hover:bg-primary-600 transition-all active:scale-95"
          >
            <Check class="w-4 h-4" />
            <span>通过</span>
          </button>
          <button 
            v-if="reg.status === 0"
            @click="handleAudit(reg.id, 2)"
            class="flex items-center space-x-2 bg-white text-rose-500 border border-rose-100 px-5 py-2.5 rounded-xl font-bold hover:bg-rose-50 transition-all active:scale-95"
          >
            <X class="w-4 h-4" />
            <span>驳回</span>
          </button>
          <button class="p-2 text-slate-300 hover:text-slate-500 rounded-xl transition-all">
            <MoreVertical class="w-5 h-5" />
          </button>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="registrations.length > 0" class="p-6 flex justify-end border-t border-slate-50">
        <el-pagination
          v-model:current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchRegistrations"
        />
      </div>
    </div>
  </div>
</template>
