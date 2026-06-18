import { createRouter, createWebHistory } from 'vue-router';
import { AUTH_TOKEN_KEY } from '../constants/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/tasks',
    name: 'tasks',
    component: () => import('../views/TaskPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teams',
    name: 'my-teams',
    component: () => import('../views/MyTeamsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teams/:teamId(\\d+)',
    name: 'team-space',
    component: () => import('../views/TeamSpaceView.vue'),
    meta: { requiresAuth: true },
    props: true
  },
  {
    path: '/teams/:teamId(\\d+)/task-graph',
    name: 'team-task-graph',
    component: () => import('../views/TaskDependencyGraph.vue'),
    meta: { requiresAuth: true },
    props: true
  },
  {
    path: '/teams/:badTeamId',
    redirect: { name: 'my-teams' }
  },
  {
    path: '/task',
    redirect: '/tasks'
  },
  // 新增：任务依赖图谱路由
  {
    path: '/task-graph',
    name: 'task-graph',
    component: () => import('../views/TaskDependencyGraph.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/task-logs',
    name: 'personal-task-logs',
    component: () => import('../views/PersonalTaskLogsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teams/:teamId(\\d+)/task-logs',
    name: 'team-task-logs',
    component: () => import('../views/TeamTaskLogsView.vue'),
    meta: { requiresAuth: true },
    props: true
  },
  {
    path: '/',
    redirect: '/tasks'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 登录权限校验 - 使用与 authStore 一致的 key
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (to.meta.requiresAuth && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

export default router;
