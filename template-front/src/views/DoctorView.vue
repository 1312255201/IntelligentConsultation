<template>
  <WorkspaceShell
    brand-mark="DR"
    brand-title="医生工作台"
    brand-subtitle="Doctor Workspace"
    page-kicker="Doctor Workspace"
    :footer-text="footerText"
    :menu-items="menuItems"
  />
</template>

<script setup>
import { Calendar, ChatLineSquare, DataAnalysis, Document, User } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/doctor/workbench',
    title: '医生工作台',
    description: '查看接诊概览、服务标签和近期工作安排。',
    icon: DataAnalysis
  },
  {
    index: '/doctor/consultation',
    title: '科室问诊列表',
    description: '查看当前科室下的问诊记录、分诊结果和反馈信息。',
    icon: Document,
    badgeKey: 'doctor-consultation'
  },
  {
    index: '/doctor/schedule',
    title: '我的排班',
    description: '查看近期排班、接诊方式和号源使用情况。',
    icon: Calendar
  },
  {
    index: '/doctor/reply-template',
    title: '常用回复模板',
    description: '维护个人常用回复内容，处理问诊和随访时可快捷插入。',
    icon: ChatLineSquare
  },
  {
    index: '/doctor/profile',
    title: '账号资料',
    description: '维护邮箱、密码和头像。',
    icon: User
  }
]

const footerText = '当前版本先完成医生侧的工作台、科室问诊查看和排班概览，后续再继续补接诊处理动作。'
</script>
