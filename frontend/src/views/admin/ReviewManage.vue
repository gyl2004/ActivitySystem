<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, X, MoreVertical, MessageSquare, Star, PieChart, Smile, Frown, Meh } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(true)
const reviews = ref<any[]>([])
const statsMap = ref<any>({ status: {}, sentiment: {}, total: 0 })
const pagination = ref({ current: 1, size: 10, total: 0 })
const activeStatus = ref<number | null>(null)
const activeSentiment = ref<string | null>(null)

const fetchStats = async () => {
  try {
    const data: any = await request.get('/api/reviews/statistics')
    statsMap.value = data
  } catch (error) {
    console.error(error)
  }
}

const fetchReviews = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/reviews', {
      params: {
        current: pagination.value.current,
        size: pagination.value.size,
        status: activeStatus.value,
        sentiment: activeSentiment.value
      }
    })
    reviews.value = data.records
    pagination.value.total = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleStatusFilter = (status: number | null) => {
  activeStatus.value = status
  activeSentiment.value = null
  pagination.value.current = 1
  fetchReviews()
}

const handleSentimentFilter = (sentiment: string | null) => {
  activeSentiment.value = sentiment
  activeStatus.value = null
  pagination.value.current = 1
  fetchReviews()
}

const handleAudit = (id: number, status: number) => {
  const action = status === 1 ? '通过' : '驳回'
  ElMessageBox.confirm(`确定要${action}该评价吗？`, '评价审核', { type: status === 1 ? 'success' : 'warning' })
    .then(async () => {
      await request.put(`/api/reviews/${id}/audit`, {
        status: status,
        auditRemark: action + '评价'
      })
      ElMessage.success(`已${action}`)
      fetchStats()
      fetchReviews()
    })
}

const handleRevoke = (id: number) => {
  ElMessageBox.confirm('确定要撤回该评价吗？撤回后用户端将不再展示。', '撤回评价', { type: 'warning' })
    .then(async () => {
      await request.put(`/api/reviews/${id}/audit`, {
        status: 2,
        auditRemark: '撤回评价'
      })
      ElMessage.success('已撤回')
      fetchStats()
      fetchReviews()
    })
}

const handleRestore = (id: number) => {
  ElMessageBox.confirm('确定要恢复该评价吗？恢复后用户端将重新展示。', '恢复评价', { type: 'success' })
    .then(async () => {
      await request.put(`/api/reviews/${id}/audit`, {
        status: 1,
        auditRemark: '恢复评价'
      })
      ElMessage.success('已恢复')
      fetchStats()
      fetchReviews()
    })
}

const getSentimentColor = (sentiment: string) => {
  const sentimentMap: any = {
    'positive': 'text-emerald-500 bg-emerald-50 border-emerald-100',
    'negative': 'text-rose-500 bg-rose-50 border-rose-100',
    'neutral': 'text-slate-400 bg-slate-50 border-slate-100'
  }
  return sentimentMap[sentiment] || sentimentMap['neutral']
}

onMounted(() => {
  fetchStats()
  fetchReviews()
})
</script>

<template>
  <div class="space-y-8">
    <div>
      <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">评价管理</h2>
      <p class="text-slate-500 font-medium mt-1">审核用户评价，分析情感倾向，维护社区氛围。</p>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div 
        @click="handleStatusFilter(null)"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeStatus === null && activeSentiment === null ? 'bg-slate-900 border-slate-900 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeStatus === null && activeSentiment === null ? 'bg-white/10' : 'bg-slate-50']"><PieChart class="w-6 h-6" /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeStatus === null && activeSentiment === null ? 'text-slate-300' : 'text-slate-400']">全部评价</div>
          <div class="text-2xl font-extrabold">{{ statsMap.total || 0 }}</div>
        </div>
      </div>
      <div 
        @click="handleSentimentFilter('positive')"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeSentiment === 'positive' ? 'bg-emerald-500 border-emerald-500 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeSentiment === 'positive' ? 'bg-white/10' : 'bg-emerald-50']"><Smile class="w-6 h-6 text-emerald-500" v-if="activeSentiment !== 'positive'" /><Smile class="w-6 h-6 text-white" v-else /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeSentiment === 'positive' ? 'text-emerald-100' : 'text-slate-400']">正面评价</div>
          <div class="text-2xl font-extrabold">{{ statsMap.sentiment['positive'] || 0 }}</div>
        </div>
      </div>
      <div 
        @click="handleSentimentFilter('neutral')"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeSentiment === 'neutral' ? 'bg-slate-400 border-slate-400 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeSentiment === 'neutral' ? 'bg-white/10' : 'bg-slate-50']"><Meh class="w-6 h-6 text-slate-400" v-if="activeSentiment !== 'neutral'" /><Meh class="w-6 h-6 text-white" v-else /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeSentiment === 'neutral' ? 'text-slate-100' : 'text-slate-400']">中性评价</div>
          <div class="text-2xl font-extrabold">{{ statsMap.sentiment['neutral'] || 0 }}</div>
        </div>
      </div>
      <div 
        @click="handleSentimentFilter('negative')"
        :class="['p-6 rounded-3xl border shadow-sm flex items-center space-x-4 cursor-pointer transition-all', activeSentiment === 'negative' ? 'bg-rose-500 border-rose-500 text-white' : 'bg-white border-slate-100 text-slate-600 hover:bg-slate-50']"
      >
        <div :class="['p-3 rounded-2xl', activeSentiment === 'negative' ? 'bg-white/10' : 'bg-rose-50']"><Frown class="w-6 h-6 text-rose-500" v-if="activeSentiment !== 'negative'" /><Frown class="w-6 h-6 text-white" v-else /></div>
        <div>
          <div :class="['text-xs font-bold uppercase', activeSentiment === 'negative' ? 'text-rose-100' : 'text-slate-400']">负面评价</div>
          <div class="text-2xl font-extrabold">{{ statsMap.sentiment['negative'] || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- Review Grid -->
    <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-2 gap-8">
      <div v-if="reviews.length === 0" class="md:col-span-2 text-center py-20 text-slate-400 font-medium">
        暂无符合条件的活动评价
      </div>
      
      <div 
        v-for="review in reviews" 
        :key="review.id"
        class="bg-white p-8 rounded-[3rem] border border-slate-100 shadow-sm hover:shadow-xl hover:shadow-slate-100 transition-all duration-500 flex flex-col h-full group"
      >
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center space-x-4">
            <img :src="review.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + review.userId" class="w-14 h-14 rounded-2xl bg-slate-50 shadow-inner group-hover:scale-110 transition-transform" />
            <div>
              <div class="text-lg font-bold text-slate-800">{{ review.nickname || '用户 ID: ' + review.userId }}</div>
              <div class="text-xs text-slate-400 font-bold uppercase tracking-widest">{{ review.activityTitle || '活动 ID: ' + review.activityId }}</div>
            </div>
          </div>
          <div :class="['px-3 py-1 rounded-full font-bold text-[10px] border', getSentimentColor(review.sentiment)]">
            {{ review.sentiment?.toUpperCase() }}
          </div>
        </div>

        <div class="flex items-center space-x-1 mb-6">
          <Star 
            v-for="i in 5" :key="i" 
            :class="['w-4 h-4', i <= review.rating ? 'text-yellow-400 fill-yellow-400' : 'text-slate-200']" 
          />
        </div>

        <p class="text-slate-600 leading-relaxed italic flex-grow">
          “ {{ review.content }} ”
        </p>

        <div class="pt-8 border-t border-slate-50 flex items-center justify-between mt-auto">
          <div class="text-xs text-slate-400 font-medium flex items-center space-x-2">
            <MessageSquare class="w-3.5 h-3.5" />
            <span>{{ review.replyCount || 0 }} 条回复</span>
            <span class="ml-2">• {{ review.createTime?.split('T')[0] }}</span>
          </div>
          <div class="flex items-center space-x-2">
            <button 
              v-if="review.status === 0"
              @click="handleAudit(review.id, 1)"
              class="p-2 text-primary-500 hover:bg-primary-50 rounded-xl transition-all shadow-sm hover:shadow-md"
            >
              <Check class="w-5 h-5" />
            </button>
            <button 
              v-if="review.status === 0"
              @click="handleAudit(review.id, 2)"
              class="p-2 text-rose-500 hover:bg-rose-50 rounded-xl transition-all shadow-sm hover:shadow-md"
            >
              <X class="w-5 h-5" />
            </button>
            <button 
              v-if="review.status === 1"
              @click="handleRevoke(review.id)"
              class="p-2 text-rose-500 hover:bg-rose-50 rounded-xl transition-all shadow-sm hover:shadow-md"
            >
              <X class="w-5 h-5" />
            </button>
            <button 
              v-if="review.status === 2"
              @click="handleRestore(review.id)"
              class="p-2 text-primary-500 hover:bg-primary-50 rounded-xl transition-all shadow-sm hover:shadow-md"
            >
              <Check class="w-5 h-5" />
            </button>
            <button class="p-2 text-slate-300 hover:text-slate-500 rounded-xl transition-all">
              <MoreVertical class="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="reviews.length > 0" class="p-6 flex justify-end">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchReviews"
      />
    </div>
  </div>
</template>
