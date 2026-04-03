<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Category {
  id: number
  parentId: number
  name: string
  sort: number
  status: number
  createTime: string
}

const loading = ref(false)
const categories = ref<Category[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = ref<any>({
  id: null,
  parentId: 0,
  name: '',
  sort: 0,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const fetchCategories = async () => {
  loading.value = true
  try {
    categories.value = await request.get('/api/admin/activity-categories')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  form.value = { id: null, parentId: 0, name: '', sort: 0, status: 1 }
  dialogVisible.value = true
}

const openEdit = (row: Category) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  await formRef.value.validate()
  const payload = {
    parentId: form.value.parentId || 0,
    name: form.value.name,
    sort: form.value.sort || 0,
    status: form.value.status
  }
  if (isEdit.value) {
    await request.put(`/api/admin/activity-categories/${form.value.id}`, payload)
    ElMessage.success('更新成功')
  } else {
    await request.post('/api/admin/activity-categories', payload)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  await fetchCategories()
}

const handleDelete = async (row: Category) => {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '提示', { type: 'warning' })
  await request.delete(`/api/admin/activity-categories/${row.id}`)
  ElMessage.success('删除成功')
  await fetchCategories()
}

onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">分类管理</h1>
        <p class="text-slate-500 mt-1">维护活动分类，用于活动创建与筛选</p>
      </div>
      <el-button type="primary" @click="openCreate">新建分类</el-button>
    </div>

    <div class="bg-white rounded-2xl p-6 border border-slate-200">
      <el-table :data="categories" v-loading="loading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column prop="parentId" label="父ID" width="90" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新建分类'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：社区关爱" />
        </el-form-item>
        <el-form-item label="父分类ID">
          <el-input-number v-model="form.parentId" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
