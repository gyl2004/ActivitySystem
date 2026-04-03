<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, MapPin, Users, Heart, Share2, Star, CheckCircle, Info, MessageCircle, ArrowLeft, Send, Loader2, QrCode, Clock, Trophy, Smile, Meh, Frown } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeTab = ref('content')
const activity = ref<any>(null)
const reviews = ref<any[]>([])
const reviewTotal = ref(0)
const commentTotal = ref(0)
const similarActivities = ref<any[]>([])
const registrationStatus = ref<any>(null)
const checkinStatus = ref<any>(null)
const loading = ref(true)
const submitting = ref(false)
const checkingIn = ref(false)
const reviewContent = ref('')
const reviewRating = ref(5)
const repliesMap = ref<Record<string, any[]>>({})
const replyDraftMap = ref<Record<string, string>>({})
const replyLoadingMap = ref<Record<string, boolean>>({})

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
  if (!activity) return FALLBACK_IMAGE
  return activity.coverImage || DEFAULT_IMAGES[activity.categoryId] || FALLBACK_IMAGE
}

const fetchCheckinStatus = async () => {
  if (!localStorage.getItem('token')) return
  try {
    const data = await request.get(`/api/checkins/status/${route.params.id}`)
    checkinStatus.value = data
  } catch (error) {
    console.error('获取签到状态失败', error)
  }
}

const fetchRegistrationStatus = async () => {
  if (!localStorage.getItem('token')) return
  try {
    const data = await request.get(`/api/registrations/status/${route.params.id}`)
    registrationStatus.value = data
    if (data && data.status === 1) {
      fetchCheckinStatus()
    }
  } catch (error) {
    console.error('获取报名状态失败', error)
  }
}

const fetchActivity = async () => {
  try {
    const data = await request.get(`/api/activities/${route.params.id}`)
    activity.value = data
    fetchReviews()
    fetchCommentCount()
    fetchRegistrationStatus()
    fetchSimilarActivities()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchSimilarActivities = async () => {
  try {
    const data: any = await request.get(`/api/recommendations/similar/${route.params.id}`, {
      params: { limit: 3 }
    })
    similarActivities.value = data
  } catch (error) {
    console.error('获取相似活动失败', error)
  }
}

const fetchReviews = async () => {
  try {
    const data: any = await request.get(`/api/reviews/activity/${route.params.id}`, {
      params: { current: 1, size: 10 }
    })
    reviews.value = data.records
    const total = typeof data.total === 'number' ? data.total : 0
    const recordsLen = data.records?.length || 0
    reviewTotal.value = total > 0 ? total : recordsLen
    const replySum = (data.records || []).reduce((sum: number, r: any) => sum + (r.replyCount || 0), 0)
    if (commentTotal.value <= 0) {
      commentTotal.value = reviewTotal.value + replySum
    }
  } catch (error) {
    console.error(error)
  }
}

const fetchCommentCount = async () => {
  try {
    const data: any = await request.get(`/api/reviews/activity/${route.params.id}/count`)
    commentTotal.value = data.total || 0
  } catch (error) {
    console.error(error)
  }
}

const fetchReplies = async (reviewId: number) => {
  replyLoadingMap.value[reviewId] = true
  try {
    const data: any = await request.get(`/api/reviews/${reviewId}/replies`, {
      params: { current: 1, size: 50 }
    })
    repliesMap.value[reviewId] = data.records || []
  } catch (error) {
    console.error(error)
  } finally {
    replyLoadingMap.value[reviewId] = false
  }
}

const toggleReplies = async (reviewId: number) => {
  if (repliesMap.value[reviewId]) {
    delete repliesMap.value[reviewId]
    return
  }
  await fetchReplies(reviewId)
}

const handleReply = async (reviewId: number, parentId: number | null = null) => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再回复')
    router.push('/login')
    return
  }
  const content = (replyDraftMap.value[reviewId] || '').trim()
  if (!content) return
  try {
    await request.post('/api/reviews/reply', {
      reviewId,
      content,
      parentId: parentId || 0
    })
    replyDraftMap.value[reviewId] = ''
    await fetchReplies(reviewId)
    fetchReviews()
    fetchCommentCount()
    ElMessage.success('回复成功')
  } catch (error) {
    console.error(error)
  }
}

const handleRegister = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再报名')
    router.push('/login')
    return
  }
  
  submitting.value = true
  try {
    await request.post('/api/registrations', { activityId: activity.value.id })
    ElMessage.success('报名提交成功，请等待审核')
    fetchRegistrationStatus()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleCheckin = async () => {
  checkingIn.value = true
  try {
    await request.post('/api/checkins', { 
      activityId: activity.value.id,
      checkinType: 1
    })
    ElMessage.success('签到成功！已为您发放志愿时长与积分')
    fetchCheckinStatus()
  } catch (error) {
    console.error(error)
  } finally {
    checkingIn.value = false
  }
}

const handleSubmitReview = async () => {
  if (!reviewContent.value) return
  try {
    await request.post('/api/reviews', {
      activityId: activity.value.id,
      rating: reviewRating.value,
      content: reviewContent.value
    })
    ElMessage.success('评价提交成功')
    reviewContent.value = ''
    fetchReviews()
    fetchCommentCount()
  } catch (error) {
    console.error(error)
  }
}

watch(
  () => route.params.id,
  async () => {
    window.scrollTo({ top: 0, left: 0 })
    loading.value = true
    await fetchActivity()
  },
  { immediate: true }
)
</script>

<template>
  <div class="space-y-12 pb-24">
    <!-- Breadcrumbs / Back -->
    <button 
      @click="router.back()"
      class="flex items-center space-x-2 text-slate-500 hover:text-primary-600 font-bold transition-colors group"
    >
      <ArrowLeft class="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
      <span>返回活动广场</span>
    </button>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-40">
      <Loader2 class="w-12 h-12 text-primary-500 animate-spin" />
    </div>

    <!-- Hero Section -->
    <div v-else-if="activity" class="grid grid-cols-1 lg:grid-cols-3 gap-12">
      <!-- Left: Cover & Info -->
      <div class="lg:col-span-2 space-y-10">
        <div class="relative h-[30rem] rounded-[3rem] overflow-hidden shadow-2xl">
          <img :src="getActivityImage(activity)" class="w-full h-full object-cover" alt="Cover" />
          <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent"></div>
          <div class="absolute bottom-10 left-10 text-white">
            <h1 class="text-4xl sm:text-5xl font-extrabold tracking-tight mb-4">{{ activity.title }}</h1>
            <div class="flex items-center space-x-6 text-white/90 font-medium">
              <div class="flex items-center">
                <Calendar class="w-5 h-5 mr-2 text-primary-400" />
                {{ activity.startTime?.replace('T', ' ') }}
              </div>
              <div class="flex items-center">
                <MapPin class="w-5 h-5 mr-2 text-primary-400" />
                {{ activity.locationName }}
              </div>
            </div>
          </div>
        </div>

        <!-- Tabs -->
        <div class="bg-white rounded-[2.5rem] border border-slate-100 shadow-sm overflow-hidden">
          <div class="flex border-b border-slate-50 px-8">
            <button 
              v-for="tab in [{id:'content', label:'详情说明', icon:Info}, {id:'reviews', label:'活动评价', icon:MessageCircle}]" 
              :key="tab.id"
              @click="activeTab = tab.id"
              :class="[
                'flex items-center space-x-2 px-8 py-6 font-bold text-lg transition-all relative',
                activeTab === tab.id ? 'text-primary-600' : 'text-slate-400 hover:text-slate-600'
              ]"
            >
              <component :is="tab.icon" class="w-5 h-5" />
              <span>{{ tab.id === 'reviews' ? `${tab.label}（${commentTotal || reviewTotal}）` : tab.label }}</span>
              <div v-if="activeTab === tab.id" class="absolute bottom-0 left-0 right-0 h-1 bg-primary-500 rounded-t-full"></div>
            </button>
          </div>

          <div class="p-10">
            <div v-if="activeTab === 'content'" class="prose prose-slate prose-lg max-w-none prose-headings:text-slate-900 prose-headings:font-bold prose-p:text-slate-500 prose-li:text-slate-500" v-html="activity.content || '暂无详细内容'"></div>
            
            <div v-else class="space-y-10">
              <div v-if="reviews.length === 0" class="text-center py-10 text-slate-400 font-medium">
                暂无评价，快来成为第一个分享参与感受的人吧
              </div>
              <div v-for="review in reviews" :key="review.id" class="flex space-x-6">
                <img :src="review.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + review.userId" class="w-16 h-16 rounded-2xl bg-slate-50 shrink-0 object-cover" alt="Avatar" />
                <div class="flex-grow space-y-2">
                  <div class="flex justify-between items-center">
                    <span class="text-lg font-bold text-slate-800">{{ review.nickname || '用户 ' + review.userId }}</span>
                    <div class="flex items-center space-x-3">
                      <!-- Sentiment Badge -->
                      <div v-if="review.sentiment" :class="[
                        'px-3 py-1 rounded-full text-xs font-bold flex items-center space-x-1 border',
                        review.sentiment === 'positive' ? 'bg-green-50 text-green-600 border-green-100' : 
                        review.sentiment === 'negative' ? 'bg-rose-50 text-rose-600 border-rose-100' : 
                        'bg-slate-50 text-slate-500 border-slate-100'
                      ]">
                        <Smile v-if="review.sentiment === 'positive'" class="w-3.5 h-3.5" />
                        <Frown v-else-if="review.sentiment === 'negative'" class="w-3.5 h-3.5" />
                        <Meh v-else class="w-3.5 h-3.5" />
                        <span>{{ review.sentiment === 'positive' ? '好评' : (review.sentiment === 'negative' ? '差评' : '中立') }}</span>
                      </div>
                      <span class="text-sm text-slate-400">{{ review.createTime?.split('T')[0] }}</span>
                    </div>
                  </div>
                  <div class="flex items-center space-x-1 mb-2">
                    <Star v-for="i in 5" :key="i" :class="['w-4 h-4', i <= review.rating ? 'text-yellow-400 fill-yellow-400' : 'text-slate-200']" />
                  </div>
                  <p class="text-slate-500 leading-relaxed">{{ review.content }}</p>

                  <div class="flex items-center space-x-4 pt-2">
                    <button
                      class="text-xs font-bold text-primary-600 hover:text-primary-700"
                      @click="toggleReplies(review.id)"
                    >
                      {{ repliesMap[review.id] ? '收起回复' : `查看回复（${review.replyCount || 0}）` }}
                    </button>
                  </div>

                  <div v-if="repliesMap[review.id]" class="mt-4 space-y-3">
                    <div v-if="replyLoadingMap[review.id]" class="text-xs text-slate-400">加载中...</div>
                    <div v-else>
                      <div v-if="repliesMap[review.id].length === 0" class="text-xs text-slate-400">暂无回复</div>
                      <div v-for="reply in repliesMap[review.id]" :key="reply.id" class="pl-4 border-l border-slate-200">
                        <div class="text-xs text-slate-500 font-bold">
                          用户 {{ reply.userId }}
                          <span class="text-slate-300 font-medium ml-2">{{ reply.createTime?.split('T')[0] }}</span>
                        </div>
                        <div class="text-sm text-slate-600 mt-1">{{ reply.content }}</div>
                        <button
                          class="text-xs font-bold text-primary-600 hover:text-primary-700 mt-1"
                          @click="replyDraftMap[review.id] = `@用户${reply.userId} `"
                        >
                          回复
                        </button>
                      </div>
                    </div>

                    <div class="flex items-start space-x-2 pt-2">
                      <input
                        v-model="replyDraftMap[review.id]"
                        placeholder="写下回复..."
                        class="flex-1 bg-white border border-slate-200 rounded-xl px-4 py-2 text-sm outline-none focus:border-primary-400 transition-all"
                      />
                      <button
                        @click="handleReply(review.id)"
                        class="px-4 py-2 bg-primary-500 text-white text-sm font-bold rounded-xl hover:bg-primary-600 transition-all"
                      >
                        发送
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Write Review -->
              <div class="mt-12 p-8 bg-slate-50 rounded-3xl border border-slate-100">
                <h4 class="text-xl font-bold text-slate-800 mb-6">发表您的评价</h4>
                <div class="flex items-center space-x-4 mb-6">
                  <span class="text-slate-500 font-bold">总体评分:</span>
                  <div class="flex items-center space-x-1">
                    <Star 
                      v-for="i in 5" :key="i" 
                      @click="reviewRating = i"
                      :class="['w-6 h-6 cursor-pointer transition-colors', i <= reviewRating ? 'text-yellow-400 fill-yellow-400' : 'text-slate-200']" 
                    />
                  </div>
                </div>

                <div class="relative">
                  <textarea 
                    v-model="reviewContent"
                    placeholder="分享您的参与感受..." 
                    class="w-full bg-white border border-slate-200 rounded-2xl p-6 h-32 outline-none focus:border-primary-400 focus:ring-4 focus:ring-primary-50 transition-all text-slate-700"
                  ></textarea>
                  <button 
                    @click="handleSubmitReview"
                    class="absolute bottom-4 right-4 bg-primary-500 text-white p-3 rounded-xl shadow-lg hover:bg-primary-600 transition-all active:scale-95"
                  >
                    <Send class="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Action Card -->
      <div class="space-y-8">
        <div class="bg-white rounded-[2.5rem] border border-slate-100 shadow-xl p-8 sticky top-28">
          <div class="flex justify-between items-center mb-8">
            <span class="bg-primary-50 text-primary-600 px-4 py-1.5 rounded-full font-bold text-sm">
              {{ activity.status === 2 ? '正在招募' : '进行中' }}
            </span>
            <div class="flex items-center space-x-1">
              <Star class="w-5 h-5 text-yellow-400 fill-yellow-400" />
              <span class="text-lg font-bold text-slate-800">4.8</span>
            </div>
          </div>

          <div class="space-y-6 mb-10">
            <div class="flex items-center space-x-4 text-slate-600">
              <div class="w-12 h-12 rounded-2xl bg-slate-50 flex items-center justify-center shrink-0">
                <Users class="w-6 h-6 text-primary-500" />
              </div>
              <div>
                <div class="text-xs text-slate-400 font-bold uppercase tracking-wider">招募人数</div>
                <div class="text-lg font-bold">{{ activity.registeredCount }} / {{ activity.maxParticipants }} 人</div>
              </div>
            </div>

            <div class="flex items-center space-x-4 text-slate-600">
              <div class="w-12 h-12 rounded-2xl bg-slate-50 flex items-center justify-center shrink-0">
                <CheckCircle class="w-6 h-6 text-primary-500" />
              </div>
              <div>
                <div class="text-xs text-slate-400 font-bold uppercase tracking-wider">报名截止</div>
                <div class="text-lg font-bold">{{ activity.registrationEnd?.split('T')[0] }}</div>
              </div>
            </div>

            <!-- Points & Duration -->
            <div class="grid grid-cols-2 gap-4">
              <div class="flex items-center space-x-3 p-4 bg-orange-50/50 rounded-2xl border border-orange-100">
                <Trophy class="w-6 h-6 text-orange-500" />
                <div>
                  <div class="text-[10px] text-orange-400 font-bold uppercase tracking-wider">奖励积分</div>
                  <div class="text-lg font-bold text-orange-700">+{{ activity.points || 0 }}</div>
                </div>
              </div>
              <div class="flex items-center space-x-3 p-4 bg-blue-50/50 rounded-2xl border border-blue-100">
                <Clock class="w-6 h-6 text-blue-500" />
                <div>
                  <div class="text-[10px] text-blue-400 font-bold uppercase tracking-wider">志愿时长</div>
                  <div class="text-lg font-bold text-blue-700">{{ activity.volunteerDuration || 0 }}h</div>
                </div>
              </div>
            </div>
          </div>

          <div class="space-y-4">
            <template v-if="registrationStatus && registrationStatus.status !== 3">
              <template v-if="registrationStatus.status === 1">
                <button 
                  v-if="checkinStatus"
                  disabled
                  class="w-full py-5 bg-green-50 text-green-600 rounded-[1.5rem] font-bold text-xl transition-all cursor-not-allowed flex items-center justify-center border border-green-200"
                >
                  <CheckCircle class="w-6 h-6 mr-2" />
                  <span>已签到</span>
                </button>
                <button 
                  v-else
                  @click="handleCheckin"
                  :disabled="checkingIn"
                  class="w-full py-5 bg-blue-500 text-white rounded-[1.5rem] font-bold text-xl shadow-xl shadow-blue-200 hover:bg-blue-600 hover:shadow-2xl transition-all active:scale-95 disabled:opacity-70 flex items-center justify-center"
                >
                  <Loader2 v-if="checkingIn" class="w-6 h-6 animate-spin mr-2" />
                  <QrCode v-else class="w-6 h-6 mr-2" />
                  <span>{{ checkingIn ? '签到中...' : '立即签到' }}</span>
                </button>
              </template>
              <button 
                v-else
                disabled
                class="w-full py-5 bg-slate-100 text-slate-500 rounded-[1.5rem] font-bold text-xl transition-all cursor-not-allowed flex items-center justify-center"
              >
                <span>
                  {{ registrationStatus.status === 0 ? '已报名，待审核' : (registrationStatus.status === 2 ? '报名申请已驳回' : '状态异常') }}
                </span>
              </button>
            </template>
            <template v-else>
              <button 
                @click="handleRegister"
                :disabled="submitting || activity.status !== 2"
                class="w-full py-5 bg-primary-500 text-white rounded-[1.5rem] font-bold text-xl shadow-xl shadow-primary-200 hover:bg-primary-600 hover:shadow-2xl transition-all active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center"
              >
                <Loader2 v-if="submitting" class="w-6 h-6 animate-spin mr-2" />
                <span>{{ submitting ? '提交中...' : (activity.status !== 2 ? '不在招募期间' : (registrationStatus?.status === 3 ? '重新报名参与' : '立即报名参与')) }}</span>
              </button>
            </template>
            <div class="flex gap-4">
              <button class="flex-grow py-4 bg-slate-50 text-slate-600 rounded-[1.25rem] font-bold flex items-center justify-center space-x-2 hover:bg-slate-100 transition-all active:scale-95">
                <Heart class="w-5 h-5" />
                <span>收藏</span>
              </button>
              <button class="flex-grow py-4 bg-slate-50 text-slate-600 rounded-[1.25rem] font-bold flex items-center justify-center space-x-2 hover:bg-slate-100 transition-all active:scale-95">
                <Share2 class="w-5 h-5" />
                <span>分享</span>
              </button>
            </div>
          </div>

          <p class="mt-8 text-center text-slate-400 text-sm font-medium">
            已有 {{ activity.viewCount || 0 }} 人浏览了此活动
          </p>
        </div>

        <!-- Organizer Info -->
        <div class="bg-white rounded-[2.5rem] border border-slate-100 shadow-sm p-8 flex items-center space-x-6">
          <div class="w-16 h-16 bg-primary-100 rounded-2xl flex items-center justify-center">
            <Heart class="w-8 h-8 text-primary-600" />
          </div>
          <div>
            <div class="text-slate-400 text-xs font-bold uppercase mb-1">发起方</div>
            <div class="text-lg font-bold text-slate-800">官方发布</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Similar Activities Recommendation -->
    <div v-if="similarActivities.length > 0" class="pt-12 border-t border-slate-100">
      <div class="flex items-center justify-between mb-10">
        <div>
          <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight flex items-center">
            <Sparkles class="w-8 h-8 text-primary-500 mr-3" />
            你可能感兴趣的其他活动
          </h2>
          <p class="text-slate-500 font-medium mt-2">基于当前活动为您推荐相似的项目</p>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div 
          v-for="item in similarActivities" 
          :key="item.id"
          class="group bg-white rounded-3xl border border-slate-100 shadow-sm hover:shadow-xl transition-all duration-500 overflow-hidden cursor-pointer flex flex-col"
          @click="router.push(`/activities/${item.id}`)"
        >
          <div class="relative h-48 overflow-hidden">
            <img 
              :src="getActivityImage(item)" 
              class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" 
              alt="Cover"
            />
            <div class="absolute top-4 right-4">
              <span class="bg-white/90 backdrop-blur-md px-3 py-1 rounded-full text-primary-600 font-bold text-[10px] shadow-sm">
                {{ item.locationName }}
              </span>
            </div>
          </div>
          <div class="p-6">
            <h3 class="text-lg font-bold text-slate-800 line-clamp-1 group-hover:text-primary-600 transition-colors mb-2">
              {{ item.title }}
            </h3>
            <div class="flex items-center justify-between mt-4">
              <div class="flex items-center text-slate-400 text-xs font-medium">
                <Calendar class="w-3.5 h-3.5 mr-1" />
                {{ item.startTime?.split('T')[0] }}
              </div>
              <div class="text-primary-600 font-bold text-xs">立即查看 →</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.prose h3) {
  @apply text-2xl font-bold text-slate-900 mt-10 mb-6;
}
:deep(.prose ul) {
  @apply space-y-4 my-6;
}
:deep(.prose li) {
  @apply flex items-start;
}
:deep(.prose li strong) {
  @apply text-slate-800 font-bold min-w-[6rem] inline-block;
}
</style>
