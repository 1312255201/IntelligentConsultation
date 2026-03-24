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
import { OfficeBuilding, User, UserFilled } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
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
    index: '/admin/profile',
    title: '个人信息',
    description: '',
    icon: User
  }
]
</script>
