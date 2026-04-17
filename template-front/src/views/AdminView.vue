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
import {
  Calendar,
  DataAnalysis,
  Document,
  HomeFilled,
  List,
  Location,
  OfficeBuilding,
  Operation,
  Setting,
  ShoppingCart,
  Tickets,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/admin/account',
    title: '系统登录信息管理',
    description: '查看系统账号、调整角色并重置登录密码。',
    icon: User
  },
  {
    index: '/admin/user',
    title: '用户信息管理',
    description: '查看用户建档、问诊、支付和处方覆盖情况。',
    icon: UserFilled
  },
  {
    index: '/admin/order',
    title: '订单信息管理',
    description: '查看问诊订单、支付状态和关联问诊记录。',
    icon: ShoppingCart
  },
  {
    index: '/admin/resource',
    title: '医疗资源管理',
    description: '统一查看科室、医生、排班、药品和知识资产。',
    icon: OfficeBuilding
  },
  {
    index: '/admin/operation-log',
    title: '操作日志查看',
    description: '查看系统请求日志、响应结果和处理耗时。',
    icon: Operation
  },
  {
    index: '/admin/global-config',
    title: '全局配置管理',
    description: '统一查看平台展示、智能分配和 AI 导诊核心配置。',
    icon: Setting
  },
  {
    index: '/admin/platform',
    title: '平台信息管理',
    description: '维护首页展示内容、推荐医生和案例素材。',
    icon: HomeFilled
  },
  {
    index: '/admin/department',
    title: '科室信息维护',
    description: '维护科室基础数据，供问诊、挂号和导诊复用。',
    icon: OfficeBuilding
  },
  {
    index: '/admin/doctor',
    title: '医生信息维护',
    description: '维护医生档案、照片、专长信息与医生账号绑定。',
    icon: UserFilled
  },
  {
    index: '/admin/doctor-service',
    title: '医生服务标签',
    description: '维护医生服务方向和能力标签，支持导诊推荐。',
    icon: Tickets
  },
  {
    index: '/admin/doctor-schedule',
    title: '医生排班管理',
    description: '配置医生排班、接诊方式与号源容量。',
    icon: Calendar
  },
  {
    index: '/admin/consultation-category',
    title: '问诊分类管理',
    description: '维护问诊入口分类与科室归属关系。',
    icon: List
  },
  {
    index: '/admin/consultation-template',
    title: '前置模板管理',
    description: '维护问诊前置信息采集模板和字段。',
    icon: Document
  },
  {
    index: '/admin/body-part',
    title: '身体部位字典',
    description: '维护智能导诊识别使用的身体部位数据。',
    icon: Location
  },
  {
    index: '/admin/symptom',
    title: '症状字典管理',
    description: '维护症状标准词、别名与匹配关键字。',
    icon: Tickets
  },
  {
    index: '/admin/medicine',
    title: '药品目录管理',
    description: '维护药品基础信息、禁忌提醒与联用冲突规则。',
    icon: Tickets
  },
  {
    index: '/admin/triage-level',
    title: '分诊等级字典',
    description: '维护风险等级、优先级和建议动作。',
    icon: DataAnalysis
  },
  {
    index: '/admin/red-flag',
    title: '红旗规则管理',
    description: '配置高风险触发条件和紧急分诊规则。',
    icon: Document
  },
  {
    index: '/admin/triage-knowledge',
    title: '导诊知识库管理',
    description: '维护 AI 导诊可复用的知识、指南和说明。',
    icon: Document
  },
  {
    index: '/admin/triage-case',
    title: '导诊案例库管理',
    description: '沉淀结构化案例，为检索、推荐和复盘提供样本。',
    icon: Document
  },
  {
    index: '/admin/consultation-record',
    title: '问诊记录中心',
    description: '查看用户问诊记录、分诊结果和反馈闭环。',
    icon: DataAnalysis
  },
  {
    index: '/admin/consultation-dispatch',
    title: '智能分配策略',
    description: '配置推荐医生排序权重、候选数量和超时阈值。',
    icon: DataAnalysis
  },
  {
    index: '/admin/consultation-ai',
    title: 'AI 导诊配置',
    description: '查看模型参数、审计样本和导诊使用概况。',
    icon: DataAnalysis
  },
  {
    index: '/admin/profile',
    title: '个人信息',
    description: '维护管理员账号的邮箱、密码和头像。',
    icon: User
  }
]
</script>
