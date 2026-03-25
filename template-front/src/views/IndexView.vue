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
import { DocumentChecked, House, Service, User, UserFilled } from '@element-plus/icons-vue'
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
    index: '/index/health',
    title: '健康档案',
    description: '按就诊人维护过敏史、既往史、慢病史和长期用药等核心健康信息。',
    icon: DocumentChecked
  },
  {
    index: '/index/consultation',
    title: '发起问诊',
    description: '选择问诊分类、装配前置表单并提交当前就诊需求，作为后续智能导诊入口。',
    icon: Service
  },
  {
    index: '/index/overview',
    title: '系统概览',
    description: '查看当前系统建设进度与基础状态。',
    icon: House
  }
]

const footerText = '当前阶段已完成基础资料维护、就诊人管理、健康档案、发起问诊入口与头像上传能力。'
</script>
