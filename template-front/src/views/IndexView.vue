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
import { ChatDotRound, DocumentChecked, House, Service, User, UserFilled } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/index/profile',
    title: '基本信息管理',
    description: '维护登录密码、联系邮箱和个人头像。',
    icon: User
  },
  {
    index: '/index/patient',
    title: '就诊人管理',
    description: '维护本人及家庭成员就诊档案，作为后续问诊主体。',
    icon: UserFilled
  },
  {
    index: '/index/health',
    title: '健康档案',
    description: '维护过敏史、既往史、慢病史和长期用药信息。',
    icon: DocumentChecked
  },
  {
    index: '/index/consultation',
    title: '发起问诊',
    description: '选择问诊分类、填写前置信息并提交当前诉求。',
    icon: Service
  },
  {
    index: '/index/triage',
    title: 'AI 导诊工作区',
    description: '集中查看 AI 导诊会话、导诊结果和推荐医生，并继续补充病情。',
    icon: ChatDotRound
  },
  {
    index: '/index/overview',
    title: '系统概览',
    description: '查看当前系统建设进度和基础能力概览。',
    icon: House
  }
]

const footerText = '当前阶段已完成基础资料维护、就诊人管理、健康档案、发起问诊入口与 AI 导诊会话基础能力。'
</script>
