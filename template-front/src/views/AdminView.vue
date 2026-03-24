<template>
  <WorkspaceShell
    brand-mark="AD"
    brand-title="系统管理后台"
    brand-subtitle="Administration Console"
    page-kicker="Admin Workspace"
    :footer-text="footerText"
    :menu-items="menuItems"
  />
</template>

<script setup>
import { OfficeBuilding, User } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/admin/department',
    title: '科室信息维护',
    description: '管理员可以统一维护科室名称、编码、位置和状态。',
    icon: OfficeBuilding
  },
  {
    index: '/admin/profile',
    title: '个人信息',
    description: '维护管理员自己的登录密码、邮箱和头像。',
    icon: User
  }
]

const footerText = '管理员登录后默认进入科室管理工作台。'
</script>
