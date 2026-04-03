<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Key, UserPlus, Search } from 'lucide-vue-next'

const loading = ref(false)
const checkins = ref<any[]>([])
const pagination = ref({ current: 1, size: 10, total: 0 })

const filters = ref<any>({
  activityId: null,
  userId: null
})

// 手动签到相关
const manualDialogVisible = ref(false)
const manualFormRef = ref()
const pendingUsers = ref<any[]>([])
const loadingPending = ref(false)
const manualForm = ref<any>({
  activityId: null,
  userId: null
})

const manualRules = {
  activityId: [{ required: true, message: '请输入活动ID', trigger: 'blur' }],
  userId: [{ required: true, message: '请选择要代签的用户', trigger: 'change' }]
}

const handleActivityChange = async () => {
  if (!manualForm.value.activityId) {
    pendingUsers.value = []
    return
  }
  
  loadingPending.value = true
  try {
    const data: any = await request.get('/api/admin/checkins/pending-users', {
      params: { activityId: manualForm.value.activityId }
    })
    pendingUsers.value = data
    if (pendingUsers.value.length === 0) {
      ElMessage.info('该活动目前没有待签到的已报名用户')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loadingPending.value = false
  }
}

// 签到码相关
const codeDialogVisible = ref(false)
const codeActivityId = ref('')
const generatedCode = ref('')
const codeExpire = ref(5)

const fetchCheckins = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/admin/checkins', {
      params: {
        current: pagination.value.current,
        size: pagination.value.size,
        activityId: filters.value.activityId || undefined,
        userId: filters.value.userId || undefined
      }
    })
    checkins.value = data.records
    pagination.value.total = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.current = 1
  fetchCheckins()
}

const openManual = () => {
  manualForm.value = { activityId: null, userId: null }
  pendingUsers.value = []
  manualDialogVisible.value = true
}

const submitManual = async () => {
  await manualFormRef.value.validate()
  await ElMessageBox.confirm('确认执行管理员手动签到？', '提示', { type: 'warning' })
  await request.post('/api/admin/checkins/manual', {
    activityId: manualForm.value.activityId,
    userId: manualForm.value.userId
  })
  ElMessage.success('签到成功')
  manualDialogVisible.value = false
  fetchCheckins()
}

const handleExport = () => {
  const token = localStorage.getItem('token')
  const prefix = localStorage.getItem('tokenPrefix') || 'Bearer'
  const activityId = filters.value.activityId || ''
  const userId = filters.value.userId || ''
  
  // 统一导出 URL，带上 token 参数
  const url = `http://localhost:8080/api/admin/checkins/export?token=${token}&activityId=${activityId}&userId=${userId}`
  window.open(url, '_blank')
}

const openGenerateCode = () => {
  codeActivityId.value = ''
  generatedCode.value = ''
  codeDialogVisible.value = true
}

const handleGenerateCode = async () => {
  if (!codeActivityId.value) {
    ElMessage.warning('请输入活动ID')
    return
  }
  const data: any = await request.post('/api/admin/checkins/code', null, {
    params: {
      activityId: codeActivityId.value,
      expireMinutes: codeExpire.value
    }
  })
  generatedCode.value = data
  ElMessage.success('签到码生成成功')
}

onMounted(() => {
  fetchCheckins()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">签到管理</h1>
        <p class="text-slate-500 mt-1">控制活动现场签到流程，并维护签到数据</p>
      </div>
      <div class="flex items-center space-x-3">
        <el-button type="success" :icon="Download" @click="handleExport">导出记录</el-button>
        <el-button type="warning" :icon="Key" @click="openGenerateCode">生成签到码</el-button>
        <el-button type="primary" :icon="UserPlus" @click="openManual">手动代签</el-button>
      </div>
    </div>

    <div class="bg-white rounded-2xl p-6 border border-slate-200 space-y-4">
      <div class="flex flex-col md:flex-row md:items-end gap-4">
        <div class="flex-1">
          <div class="text-sm font-bold text-slate-600 mb-2">活动ID</div>
          <el-input v-model="filters.activityId" placeholder="可选，筛选活动" />
        </div>
        <div class="flex-1">
          <div class="text-sm font-bold text-slate-600 mb-2">用户ID</div>
          <el-input v-model="filters.userId" placeholder="可选，筛选用户" />
        </div>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <div class="bg-white rounded-2xl p-6 border border-slate-200">
      <el-table :data="checkins" v-loading="loading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="activityId" label="活动ID" width="100" />
        <el-table-column prop="activityTitle" label="活动标题" min-width="180" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="checkinTime" label="签到时间" width="180" />
        <el-table-column label="方式" width="120">
          <template #default="{ row }">
            <el-tag :type="row.checkinType === 2 ? 'warning' : 'success'">{{ row.checkinType === 2 ? '手动' : '签到码' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="140" />
      </el-table>

      <div class="flex justify-end mt-6">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="pagination.total"
          :page-size="pagination.size"
          :current-page="pagination.current"
          @current-change="(p: number) => { pagination.current = p; fetchCheckins() }"
        />
      </div>
    </div>

    <el-dialog v-model="manualDialogVisible" title="管理员手动代签" width="520px">
      <div class="mb-6 p-4 bg-blue-50 rounded-xl text-blue-700 text-sm flex items-start gap-3">
        <div class="p-1 bg-blue-100 rounded-lg shrink-0">
          <Search class="w-4 h-4" />
        </div>
        <div>
          请先输入活动 ID，系统将自动为您列出该活动下**已报名且尚未签到**的志愿者名单。
        </div>
      </div>

      <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-width="100px" label-position="top">
        <el-form-item label="活动 ID" prop="activityId">
          <el-input v-model="manualForm.activityId" placeholder="请输入活动 ID" @change="handleActivityChange" />
        </el-form-item>
        
        <el-form-item label="选择待签到用户" prop="userId">
          <el-select 
            v-model="manualForm.userId" 
            placeholder="请选择用户" 
            class="w-full" 
            :loading="loadingPending"
            :disabled="!manualForm.activityId || pendingUsers.length === 0"
          >
            <el-option 
              v-for="u in pendingUsers" 
              :key="u.userId" 
              :label="`${u.nickname} (@${u.username})`" 
              :value="u.userId" 
            />
          </el-select>
          <div v-if="manualForm.activityId && pendingUsers.length === 0 && !loadingPending" class="text-xs text-rose-500 mt-2">
            该活动下暂无可代签的志愿者
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!manualForm.userId" @click="submitManual">确认签到</el-button>
      </template>
    </el-dialog>

    <!-- 生成签到码对话框 -->
    <el-dialog v-model="codeDialogVisible" title="生成活动签到码" width="450px" center>
      <div class="space-y-6">
        <div class="space-y-2">
          <div class="text-sm font-bold text-slate-600">活动 ID</div>
          <el-input v-model="codeActivityId" placeholder="请输入要开启签到的活动 ID" />
        </div>
        <div class="space-y-2">
          <div class="text-sm font-bold text-slate-600">有效期 (分钟)</div>
          <el-input-number v-model="codeExpire" :min="1" :max="1440" class="w-full" />
        </div>
        
        <div v-if="generatedCode" class="mt-8 p-8 bg-slate-50 rounded-2xl border-2 border-dashed border-slate-200 text-center">
          <div class="text-sm font-bold text-slate-400 uppercase tracking-widest mb-2">当前签到码</div>
          <div class="text-6xl font-black text-primary-600 tracking-tighter">{{ generatedCode }}</div>
          <div class="text-xs text-slate-400 mt-4 font-bold">请展示给现场志愿者，输入此码完成签到</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="codeDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleGenerateCode">生成/刷新签到码</el-button>
      </template>
    </el-dialog>
  </div>
</template>
