<template>
  <WorkspaceShell
    brand-mark="IC"
    brand-title="智能问诊系统"
    brand-subtitle="Patient Workspace"
    page-kicker="Clinical Workspace"
    :footer-text="footerText"
    :menu-items="menuItems"
  />
</template>

<script setup>
import { House, User, UserFilled } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/index/profile',
    title: '基本信息管理',
    description: '维护登录密码、联系邮箱与个人头像。',
    icon: User
  },
  {
    index: '/index/patient',
    title: '就诊人管理',
    description: '维护本人及家庭成员就诊档案，后续问诊和导诊都会基于这里的就诊人信息。',
    icon: UserFilled
  },
  {
    index: '/index/overview',
    title: '系统概览',
    description: '查看当前系统建设进度与基础状态。',
    icon: House
  }
]

const footerText = '当前阶段已完成基础资料维护、就诊人管理和头像上传能力。'
</script>
