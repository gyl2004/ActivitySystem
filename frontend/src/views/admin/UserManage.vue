<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const users = ref<any[]>([])
const roles = ref<any[]>([])
const pagination = ref({ current: 1, size: 10, total: 0 })
const keyword = ref('')

const fetchRoles = async () => {
  const allRoles: any = await request.get('/api/admin/roles')
  // 业务限制：超级管理员只能赋予“管理员” (admin) 或“志愿者” (volunteer) 身份
  roles.value = allRoles.filter((r: any) => r.roleKey === 'admin' || r.roleKey === 'volunteer')
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/api/admin/users', {
      params: {
        current: pagination.value.current,
        size: pagination.value.size,
        keyword: keyword.value || undefined
      }
    })
    users.value = data.records.map((u: any) => ({
      ...u,
      _roleId: u.roleId || null
    }))
    pagination.value.total = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.value.current = 1
  fetchUsers()
}

const handleAssign = async (row: any) => {
  if (!row._roleId) {
    ElMessage.warning('请选择角色')
    return
  }
  await request.put(`/api/admin/users/${row.id}/role`, { roleId: row._roleId })
  ElMessage.success('角色已更新')
  fetchUsers()
}

onMounted(async () => {
  await fetchRoles()
  await fetchUsers()
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">用户管理</h1>
        <p class="text-slate-500 mt-1">查看用户并分配角色（简化版单角色）</p>
      </div>
    </div>

    <div class="bg-white rounded-2xl p-6 border border-slate-200">
      <div class="flex flex-col md:flex-row gap-4 mb-6">
        <el-input v-model="keyword" placeholder="按用户名/昵称搜索" clearable />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table :data="users" v-loading="loading" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="nickname" label="昵称" width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="90" />
        <el-table-column prop="volunteerDuration" label="志愿时长" width="110" />
        <el-table-column prop="roleKey" label="当前角色" width="140" />
        <el-table-column label="分配角色" min-width="260">
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <el-select v-model="row._roleId" placeholder="选择角色" style="width: 160px">
                <el-option v-for="r in roles" :key="r.id" :label="`${r.roleName} (${r.roleKey})`" :value="r.id" />
              </el-select>
              <el-button type="primary" size="small" @click="handleAssign(row)">保存</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
      </el-table>

      <div class="flex justify-end mt-6">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="pagination.total"
          :page-size="pagination.size"
          :current-page="pagination.current"
          @current-change="(p: number) => { pagination.current = p; fetchUsers() }"
        />
      </div>
    </div>
  </div>
</template>
