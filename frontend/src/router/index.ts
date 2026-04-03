import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'activities',
        name: 'Activities',
        component: () => import('../views/activity/ActivityList.vue'),
        meta: { title: '活动广场' }
      },
      {
        path: 'activities/:id',
        name: 'ActivityDetail',
        component: () => import('../views/activity/ActivityDetail.vue'),
        meta: { title: '活动详情' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/user/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '管理面板' }
      },
      {
        path: 'activities',
        name: 'AdminActivities',
        component: () => import('../views/admin/ActivityManage.vue'),
        meta: { title: '活动管理' }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/CategoryManage.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'registrations',
        name: 'AdminRegistrations',
        component: () => import('../views/admin/RegistrationAudit.vue'),
        meta: { title: '报名审核' }
      },
      {
        path: 'checkins',
        name: 'AdminCheckins',
        component: () => import('../views/admin/CheckinManage.vue'),
        meta: { title: '签到管理' }
      },
      {
        path: 'reviews',
        name: 'AdminReviews',
        component: () => import('../views/admin/ReviewManage.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('../views/admin/SystemSettings.vue'),
        meta: { title: '系统设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    void to
    void from
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

router.beforeEach(async (to, from, next) => {
  void from
  document.title = `${to.meta.title} - 公益活动系统`
  
  const token = localStorage.getItem('token')
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)

  if (requiresAuth && !token) {
    next('/login')
    return
  }

  // 如果需要管理员权限，可以进一步校验 (这里简化处理，后续可从 store 或接口获取用户信息校验)
  if (requiresAdmin && token) {
    // 简单的模拟校验，实际生产环境应根据后端返回的权限字段判断
    next()
  } else {
    next()
  }
})

export default router
