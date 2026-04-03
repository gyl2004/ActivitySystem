<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Plus, Search, MoreVertical, Edit2, Trash2, CheckCircle2, XCircle, MapPin, Image as ImageIcon, RotateCcw, Loader2, Download, Copy, QrCode } from 'lucide-vue-next'
import request from '../../utils/request'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { downloadBlob } from '../../utils/download'

const loading = ref(true)
const activities = ref<any[]>([])
const categories = ref<any[]>([])
const pagination = ref({ current: 1, size: 10, total: 0 })
const searchQuery = ref('')
const exporting = ref(false)

const qrDialogVisible = ref(false)
const qrLoading = ref(false)
const qrCodeValue = ref('')
const qrActivityTitle = ref('')

// Dialog state
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  id: null as number | null,
  title: '',
  categoryId: null as number | null,
  summary: '',
  content: '',
  coverImage: '',
  startTime: '',
  endTime: '',
  registrationStart: '',
  registrationEnd: '',
  locationName: '',
  address: '',
  maxParticipants: 0,
  points: 0,
  volunteerDuration: 0
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

// 监听分类变化，自动设置默认图片
const handleCategoryChange = (val: number) => {
  // 只有在创建模式，或者编辑模式下图片为空时，才自动填充默认图
  if (dialogType.value === 'create' || !form.coverImage) {
    form.coverImage = DEFAULT_IMAGES[val] || FALLBACK_IMAGE
  }
}

// 模拟图片上传
const uploadLoading = ref(false)
const handleUpload = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e: any) => {
    const file = e.target.files[0]
    if (file) {
      uploadLoading.value = true
      // 这里应该是调用后端上传接口，目前先模拟一个延时并使用随机图
      setTimeout(() => {
        form.coverImage = `https://picsum.photos/seed/${Math.random()}/800/600`
        uploadLoading.value = false
        ElMessage.success('图片上传成功 (模拟)')
      }, 1000)
    }
  }
  input.click()
}

// 获取活动展示图片
const getActivityImage = (row: any) => {
  return row.coverImage || DEFAULT_IMAGES[row.categoryId] || FALLBACK_IMAGE
}

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  registrationStart: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEnd: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }],
  locationName: [{ required: true, message: '请输入地点名称', trigger: 'blur' }]
}

const fetchCategories = async () => {
  try {
    const data: any = await request.get('/api/activity-categories')
    categories.value = data
  } catch (error) {
    console.error(error)
  }
}

const fetchActivities = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/activities', {
      params: {
        current: pagination.value.current,
        size: pagination.value.size,
        title: searchQuery.value
      }
    })
    activities.value = data.records
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
    const blob: any = await request.get('/api/activities/export', {
      params: {
        title: searchQuery.value || undefined
      },
      responseType: 'blob'
    })
    downloadBlob(blob, `活动数据_${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error(error)
  } finally {
    exporting.value = false
  }
}

const handleCopy = async (id: number) => {
  try {
    await request.post(`/api/activities/${id}/copy`)
    ElMessage.success('复制成功')
    fetchActivities()
  } catch (error) {
    console.error(error)
  }
}

const handleShowQrCode = async (row: any) => {
  qrDialogVisible.value = true
  qrLoading.value = true
  qrCodeValue.value = ''
  qrActivityTitle.value = row.title
  try {
    const code: any = await request.get(`/api/checkins/code/${row.id}?expireMinutes=5`)
    qrCodeValue.value = code
  } catch (error) {
    console.error(error)
  } finally {
    qrLoading.value = false
  }
}

const handleCreate = () => {
  dialogType.value = 'create'
  Object.assign(form, {
    id: null,
    title: '',
    categoryId: null,
    summary: '',
    content: '',
    coverImage: '',
    startTime: '',
    endTime: '',
    registrationStart: '',
    registrationEnd: '',
    locationName: '',
    address: '',
    maxParticipants: 0,
    points: 0,
    volunteerDuration: 0
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogType.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (dialogType.value === 'create') {
          await request.post('/api/activities', form)
          ElMessage.success('创建成功')
        } else {
          await request.put(`/api/activities/${form.id}`, form)
          ElMessage.success('修改成功')
        }
        dialogVisible.value = false
        fetchActivities()
      } catch (error) {
        console.error(error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handlePublish = async (id: number) => {
  try {
    await request.put(`/api/activities/${id}/publish`)
    ElMessage.success('发布成功')
    fetchActivities()
  } catch (error) {
    console.error(error)
  }
}

const handleCancel = async (id: number) => {
  try {
    await request.put(`/api/activities/${id}/cancel`)
    ElMessage.success('活动已取消')
    fetchActivities()
  } catch (error) {
    console.error(error)
  }
}

const handleRevert = async (id: number) => {
  try {
    await request.put(`/api/activities/${id}/revert`)
    ElMessage.success('活动已退回到草稿状态')
    fetchActivities()
  } catch (error) {
    console.error(error)
  }
}

const getStatusLabel = (status: number) => {
  const statusMap: any = {
    0: { text: '草稿', type: 'info' },
    1: { text: '待发布', type: 'warning' },
    2: { text: '已发布', type: 'success' },
    3: { text: '进行中', type: 'primary' },
    4: { text: '已结束', type: 'info' },
    5: { text: '已取消', type: 'danger' }
  }
  return statusMap[status] || statusMap[0]
}

const handlePageChange = (page: number) => {
  pagination.value.current = page
  fetchActivities()
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除该活动吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request.delete(`/api/activities/${id}`)
      ElMessage.success('删除成功')
      fetchActivities()
    })
}

onMounted(() => {
  fetchCategories()
  fetchActivities()
})
</script>

<template>
  <div class="space-y-8">
    <div class="flex justify-between items-end">
      <div>
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">活动管理</h2>
        <p class="text-slate-500 font-medium mt-1">发布、更新和维护您的公益活动。</p>
      </div>
      <div class="flex items-center space-x-3">
        <button
          @click="handleExport"
          :disabled="exporting"
          class="flex items-center space-x-2 bg-white text-slate-700 px-6 py-3 rounded-2xl font-bold border border-slate-200 hover:bg-slate-50 transition-all disabled:opacity-70"
        >
          <Download class="w-5 h-5" />
          <span>{{ exporting ? '导出中...' : '导出活动' }}</span>
        </button>
        <button @click="handleCreate" class="flex items-center space-x-2 bg-primary-500 text-white px-6 py-3 rounded-2xl font-bold shadow-lg shadow-primary-200 hover:bg-primary-600 transition-all active:scale-95">
          <Plus class="w-5 h-5" />
          <span>发布新活动</span>
        </button>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="bg-white p-4 rounded-3xl border border-slate-100 shadow-sm flex items-center space-x-4">
      <div class="relative flex-grow">
        <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
        <input 
          v-model="searchQuery"
          @keyup.enter="fetchActivities"
          placeholder="搜索活动标题..." 
          class="w-full pl-12 pr-4 py-3 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-primary-100 outline-none"
        />
      </div>
      <button @click="fetchActivities" class="px-6 py-3 bg-slate-900 text-white rounded-2xl font-bold hover:bg-slate-800 transition-all">搜索</button>
    </div>

    <!-- Table -->
    <div class="bg-white rounded-[2.5rem] border border-slate-100 shadow-sm overflow-hidden">
      <el-table :data="activities" v-loading="loading" style="width: 100%" :header-cell-style="{ background: '#f8fafc', fontWeight: 'bold', color: '#64748b' }">
        <el-table-column label="活动信息" min-width="300">
          <template #default="{ row }">
            <div class="flex items-center space-x-4 py-2">
              <img :src="getActivityImage(row)" class="w-16 h-12 rounded-lg object-cover bg-slate-100 shadow-sm" />
              <div>
                <div class="font-bold text-slate-800 line-clamp-1">{{ row.title }}</div>
                <div class="text-xs text-slate-400 mt-1">{{ row.locationName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <span class="text-sm font-medium text-slate-600">{{ categories.find(c => c.id === row.categoryId)?.name || '未分类' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusLabel(row.status).type" round effect="light">{{ getStatusLabel(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="参与人数" width="150">
          <template #default="{ row }">
            <div class="text-sm">
              <span class="font-bold text-slate-800">{{ row.registeredCount }}</span>
              <span class="text-slate-400"> / {{ row.maxParticipants }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span class="text-xs text-slate-500 font-medium">{{ row.createTime?.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center space-x-2">
              <button @click="handleEdit(row)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all">
                <Edit2 class="w-4 h-4" />
              </button>
              <button @click="handleDelete(row.id)" class="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-all">
                <Trash2 class="w-4 h-4" />
              </button>
              <el-dropdown trigger="click">
                <button class="p-2 text-slate-400 hover:bg-slate-50 rounded-xl transition-all">
                  <MoreVertical class="w-4 h-4" />
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="row.status < 2" @click="handlePublish(row.id)" :icon="CheckCircle2">发布活动</el-dropdown-item>
                    <el-dropdown-item v-if="row.status === 2" @click="handleCancel(row.id)" :icon="XCircle" class="!text-rose-500">取消活动</el-dropdown-item>
                    <el-dropdown-item v-if="row.status >= 2" @click="handleRevert(row.id)" :icon="RotateCcw">退回草稿</el-dropdown-item>
                    <el-dropdown-item @click="handleCopy(row.id)" :icon="Copy">复制活动</el-dropdown-item>
                    <el-dropdown-item v-if="row.status >= 2" @click="handleShowQrCode(row)" :icon="QrCode">获取签到码</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="p-6 flex justify-end border-t border-slate-50">
        <el-pagination
          v-model:current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '发布新活动' : '编辑活动'"
      width="800px"
      class="sunny-dialog"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="space-y-6">
        <div class="grid grid-cols-2 gap-6">
          <el-form-item label="活动标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入活动标题" />
          </el-form-item>
          <el-form-item label="活动分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" class="w-full" @change="handleCategoryChange">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="活动封面图片" prop="coverImage">
          <div class="space-y-4 w-full">
            <div class="flex items-center space-x-4">
              <el-input v-model="form.coverImage" placeholder="请输入图片 URL 或点击右侧上传" class="flex-grow">
                <template #prefix><ImageIcon class="w-4 h-4" /></template>
              </el-input>
              <button @click="handleUpload" :disabled="uploadLoading" class="px-4 py-2 bg-primary-50 text-primary-600 rounded-xl font-bold hover:bg-primary-100 transition-all active:scale-95 disabled:opacity-50 flex items-center shrink-0">
                <Loader2 v-if="uploadLoading" class="w-4 h-4 animate-spin mr-2" />
                <Plus v-else class="w-4 h-4 mr-2" />
                <span>上传图片</span>
              </button>
            </div>
            <div v-if="form.coverImage" class="relative group w-48 h-32 rounded-2xl overflow-hidden shadow-md border-2 border-slate-100">
              <img :src="form.coverImage" class="w-full h-full object-cover" />
              <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <button @click="form.coverImage = ''" class="p-2 bg-white/20 backdrop-blur-md rounded-full text-white hover:bg-white/40">
                  <Trash2 class="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="活动摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="简单介绍一下活动..." />
        </el-form-item>

        <div class="grid grid-cols-2 gap-6">
          <el-form-item label="活动开始时间" prop="startTime">
            <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择时间" class="!w-full" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
          <el-form-item label="活动结束时间" prop="endTime">
            <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择时间" class="!w-full" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </div>

        <div class="grid grid-cols-2 gap-6">
          <el-form-item label="报名开始时间" prop="registrationStart">
            <el-date-picker v-model="form.registrationStart" type="datetime" placeholder="选择时间" class="!w-full" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
          <el-form-item label="报名截止时间" prop="registrationEnd">
            <el-date-picker v-model="form.registrationEnd" type="datetime" placeholder="选择时间" class="!w-full" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </div>

        <div class="grid grid-cols-2 gap-6">
          <el-form-item label="地点名称" prop="locationName">
            <el-input v-model="form.locationName" placeholder="如：XX 社区中心">
              <template #prefix><MapPin class="w-4 h-4" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="最大人数 (0 为不限)" prop="maxParticipants">
            <el-input-number v-model="form.maxParticipants" :min="0" class="!w-full" />
          </el-form-item>
        </div>

        <div class="grid grid-cols-2 gap-6">
          <el-form-item label="奖励积分" prop="points">
            <el-input-number v-model="form.points" :min="0" class="!w-full" />
          </el-form-item>
          <el-form-item label="志愿时长 (小时)" prop="volunteerDuration">
            <el-input-number v-model="form.volunteerDuration" :min="0" :precision="1" :step="0.5" class="!w-full" />
          </el-form-item>
        </div>

        <el-form-item label="活动详细内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入详细的活动说明 (支持 HTML)" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end space-x-4">
          <button @click="dialogVisible = false" class="px-6 py-2.5 rounded-xl border border-slate-200 font-bold text-slate-500 hover:bg-slate-50 transition-all">取消</button>
          <button @click="submitForm" :disabled="submitting" class="px-8 py-2.5 rounded-xl bg-primary-500 text-white font-bold hover:bg-primary-600 transition-all shadow-lg shadow-primary-200 disabled:opacity-70 flex items-center">
            <Loader2 v-if="submitting" class="w-4 h-4 animate-spin mr-2" />
            <span>确认{{ dialogType === 'create' ? '创建' : '保存' }}</span>
          </button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="qrDialogVisible"
      title="活动签到码"
      width="420px"
      class="sunny-dialog"
      destroy-on-close
    >
      <div class="space-y-6 text-center">
        <div class="text-slate-700 font-bold line-clamp-2 text-lg">{{ qrActivityTitle }}</div>
        <div v-if="qrLoading" class="flex justify-center py-10">
          <Loader2 class="w-8 h-8 text-primary-500 animate-spin" />
        </div>
        <div v-else-if="qrCodeValue" class="flex flex-col items-center space-y-4">
          <div class="text-6xl font-black text-primary-600 tracking-[0.2em] bg-primary-50 px-8 py-6 rounded-3xl border-2 border-primary-100 shadow-inner">
            {{ qrCodeValue }}
          </div>
          <p class="text-slate-500 text-sm">请将此 6 位数字提供给现场志愿者进行签到</p>
          <p class="text-rose-500 text-xs font-bold bg-rose-50 px-4 py-2 rounded-full">注意：该签到码有效期为 5 分钟</p>
        </div>
        <div v-else class="text-center text-slate-400 py-10">签到码生成失败</div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
:deep(.el-table) {
  --el-table-border-color: #f1f5f9;
}
:deep(.el-table__row) {
  @apply transition-all hover:bg-slate-50/50;
}
:deep(.sunny-dialog) {
  @apply rounded-[2.5rem] overflow-hidden;
}
:deep(.sunny-dialog .el-dialog__header) {
  @apply border-b border-slate-50 p-8 m-0;
}
:deep(.sunny-dialog .el-dialog__title) {
  @apply text-2xl font-extrabold text-slate-900;
}
:deep(.sunny-dialog .el-dialog__body) {
  @apply p-8;
}
:deep(.sunny-dialog .el-dialog__footer) {
  @apply border-t border-slate-50 p-8;
}
:deep(.el-form-item__label) {
  @apply font-bold text-slate-700 pb-2;
}
:deep(.el-input__wrapper), :deep(.el-textarea__inner), :deep(.el-select__wrapper) {
  @apply !rounded-xl !bg-slate-50 !border-none !shadow-none py-2;
}
:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) {
  @apply !bg-white !ring-2 !ring-primary-100;
}
</style>

<style scoped>
:deep(.el-table) {
  --el-table-border-color: #f1f5f9;
}
:deep(.el-table__row) {
  @apply transition-all hover:bg-slate-50/50;
}
</style>
