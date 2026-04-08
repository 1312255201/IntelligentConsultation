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
import { Bell, Calendar, ChatLineSquare, DataAnalysis, Document, User } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/doctor/workbench',
    title: '医生工作台',
    description: '查看接诊概览、服务标签和近期工作安排。',
    icon: DataAnalysis
  },
  {
    index: '/doctor/reminder',
    title: '消息与待办',
    description: '集中查看待认领问诊、患者新消息、待回复与随访事项，并带着上下文进入详情处理。',
    icon: Bell,
    badgeKey: 'doctor-reminder'
  },
  {
    index: '/doctor/consultation',
    title: '科室问诊列表',
    description: '查看当前科室下的问诊记录、分诊结果和反馈信息。',
    icon: Document
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

const footerText = '当前医生侧已经形成“工作台 -> 消息与待办 -> 问诊列表 -> 问诊详情”的主链路，后续继续完善通知、协作和诊后运营能力。'
</script>
