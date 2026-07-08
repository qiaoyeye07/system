import { createRouter, createWebHashHistory } from 'vue-router'

// 页面懒加载
const LoginView = () => import('../views/LoginView.vue')
const RegisterView = () => import('../views/RegisterView.vue')
const HomeView = () => import('../views/HomeView.vue')
const CategoryView = () => import('../views/CategoryView.vue')
const SearchResultView = () => import('../views/SearchResultView.vue')
const ProductDetailView = () => import('../views/ProductDetailView.vue')
const PublishProductView = () => import('../views/PublishProductView.vue')
const MyProductsView = () => import('../views/MyProductsView.vue')
const MyOrdersView = () => import('../views/MyOrdersView.vue')
const OrderDetailView = () => import('../views/OrderDetailView.vue')
const ChatView = () => import('../views/ChatView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const SwapProposalView = () => import('../views/SwapProposalView.vue')
const SwapDetailView = () => import('../views/SwapDetailView.vue')
const MyReportsView = () => import('../views/MyReportsView.vue')
const UserManagementView = () => import('../views/admin/UserManagementView.vue')
const CategoryManagementView = () => import('../views/admin/CategoryManagementView.vue')
const DisputeManagementView = () => import('../views/admin/DisputeManagementView.vue')
const ReportManagementView = () => import('../views/admin/ReportManagementView.vue')

const routes = [
  // 公开页面
  { path: '/login', component: LoginView, meta: { guest: true } },
  { path: '/register', component: RegisterView, meta: { guest: true } },
  { path: '/', component: HomeView },
  { path: '/category/:id', component: CategoryView, props: true },
  { path: '/search', component: SearchResultView },
  { path: '/product/:id', component: ProductDetailView, props: true },
  { path: '/user/:id', component: ProfileView, props: true },

  // 需登录
  { path: '/publish', component: PublishProductView, meta: { auth: true } },
  { path: '/my-products', component: MyProductsView, meta: { auth: true } },
  { path: '/my-orders', component: MyOrdersView, meta: { auth: true } },
  { path: '/order/:id', component: OrderDetailView, props: true, meta: { auth: true } },
  { path: '/swap/propose/:productId', component: SwapProposalView, props: true, meta: { auth: true } },
  { path: '/swap/:id', component: SwapDetailView, props: true, meta: { auth: true } },
  { path: '/chat', component: ChatView, meta: { auth: true } },
  { path: '/chat/:contactId', component: ChatView, props: true, meta: { auth: true } },
  { path: '/my-reports', component: MyReportsView, meta: { auth: true } },

  // 管理端
  { path: '/admin/users', component: UserManagementView, meta: { role: 'ADMIN' } },
  { path: '/admin/categories', component: CategoryManagementView, meta: { role: 'ADMIN' } },
  { path: '/admin/disputes', component: DisputeManagementView, meta: { role: 'ADMIN' } },
  { path: '/admin/reports', component: ReportManagementView, meta: { role: 'ADMIN' } },

  // 404
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 导航守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  const user = userStr ? JSON.parse(userStr) : null

  // 游客页面：已登录则跳首页
  if (to.meta.guest && token) {
    return next('/')
  }

  // 需认证页面：未登录跳登录页
  if (to.meta.auth && !token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 管理员页面：角色校验
  if (to.meta.role === 'ADMIN' && user?.role !== 'ADMIN') {
    return next('/')
  }

  next()
})

export default router
