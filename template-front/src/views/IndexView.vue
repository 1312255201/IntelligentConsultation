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
import {
  Bell,
  ChatDotRound,
  DocumentChecked,
  Files,
  House,
  Service,
  Star,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/index/overview',
    title: '患者工作台',
    description: '查看当前待办、提醒汇总、最近问诊和健康档案覆盖情况。',
    icon: House
  },
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
    index: '/index/triage',
    title: 'AI 智能导诊',
    description: '集中查看 AI 导诊会话、结果归档和推荐医生。',
    icon: ChatDotRound
  },
  {
    index: '/index/doctor-directory',
    title: '医生信息查询',
    description: '查询医生科室、擅长方向和近期排班信息。',
    icon: UserFilled
  },
  {
    index: '/index/consultation',
    title: '在线问诊',
    description: '选择问诊分类、填写前置信息并提交当前诉求。',
    icon: Service
  },
  {
    index: '/index/case',
    title: '个人病例查询',
    description: '查看个人问诊记录、病历归档、随访和服务评价。',
    icon: Files
  },
  {
    index: '/index/prescription',
    title: '个人处方查询',
    description: '查看处方明细、用法用量和禁忌提醒。',
    icon: DocumentChecked
  },
  {
    index: '/index/feedback',
    title: '服务评价与反馈',
    description: '提交服务评价、导诊反馈、检查回传和用药反馈。',
    icon: Star
  },
  {
    index: '/index/reminder',
    title: '消息与提醒',
    description: '集中查看医生新回复、待处理问诊和随访提醒。',
    icon: Bell,
    badgeKey: 'patient-reminder'
  }
]

const footerText = '当前患者端已经形成“AI 导诊 -> 医生查询 -> 在线问诊 -> 病例与处方查询 -> 服务反馈”的完整链路。'
</script>
