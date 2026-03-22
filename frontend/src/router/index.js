import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        component: () => import('../views/TeamManagerView.vue'),
        children: [
          {
            path: '',
            name: 'Home',
            component: () => import('../views/HomeView.vue')
          },
          {
            path: 'teams/:id',
            name: 'TeamDetail',
            component: () => import('../views/TeamDetail.vue'),
            props: true
          },
          {
            path: 'teams/:id/lineups',
            name: 'LineupHistory',
            component: () => import('../views/LineupHistoryView.vue'),
            props: true
          }
        ]
      },
      {
        path: 'lineup',
        name: 'LineupGenerator',
        component: () => import('../views/LineupGenerator.vue')
      },
      {
        path: 'opponent-analysis',
        name: 'OpponentAnalysis',
        component: () => import('../views/OpponentAnalysis.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
