<template>
  <WorkspaceShell
    brand-mark="AD"
    brand-title="系统管理后台"
    brand-subtitle="Administration Console"
    page-kicker="Admin Workspace"
    :menu-items="menuItems"
  />
</template>

<script setup>
import { Calendar, Document, HomeFilled, OfficeBuilding, Tickets, User, UserFilled } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/admin/homepage',
    title: '主页设置',
    description: '',
    icon: HomeFilled
  },
  {
    index: '/admin/department',
    title: '科室信息维护',
    description: '',
    icon: OfficeBuilding
  },
  {
    index: '/admin/doctor',
    title: '医生信息维护',
    description: '',
    icon: UserFilled
  },
  {
    index: '/admin/doctor-service',
    title: '医生服务标签',
    description: '',
    icon: Tickets
  },
  {
    index: '/admin/doctor-schedule',
    title: '医生排班管理',
    description: '',
    icon: Calendar
  },
  {
    index: '/admin/consultation-category',
    title: '问诊分类管理',
    description: '',
    icon: Tickets
  },
  {
    index: '/admin/consultation-template',
    title: '前置模板管理',
    description: '',
    icon: Document
  },
  {
    index: '/admin/profile',
    title: '个人信息',
    description: '',
    icon: User
  }
]
</script>
