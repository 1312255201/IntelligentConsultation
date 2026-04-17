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
import { Bell, Calendar, ChatLineSquare, DataAnalysis, Document, EditPen, Files, FirstAidKit, Tickets, User } from '@element-plus/icons-vue'
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
    description: '集中查看待认领问诊、新消息、待回复与随访事项。',
    icon: Bell,
    badgeKey: 'doctor-reminder'
  },
  {
    index: '/doctor/consultation',
    title: '在线接诊问诊',
    description: '完成接诊、病历书写、处方开具和问诊沟通。',
    icon: Document
  },
  {
    index: '/doctor/medical-record',
    title: '电子病例书写',
    description: '聚焦结构化病历、诊断结论和患者指导意见书写。',
    icon: Files
  },
  {
    index: '/doctor/prescription',
    title: '处方开具',
    description: '聚焦检查建议、处方录入、禁忌提醒和联用冲突检测。',
    icon: Tickets
  },
  {
    index: '/doctor/advice',
    title: '患者医嘱管理',
    description: '集中查看处理建议、患者指导和随访安排。',
    icon: FirstAidKit
  },
  {
    index: '/doctor/knowledge',
    title: '医学知识维护',
    description: '沉淀个人高频经验、宣教内容和标准化解释。',
    icon: EditPen
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

const footerText = '当前医生端已经形成“工作台 -> 在线接诊 -> 病历书写 -> 处方开具 -> 医嘱与知识沉淀”的主链路，可直接覆盖接诊、病历、处方、随访与知识维护。'
</script>
