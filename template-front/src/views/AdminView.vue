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
import { Calendar, DataAnalysis, Document, HomeFilled, Location, OfficeBuilding, Tickets, User, UserFilled } from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: '/admin/homepage',
    title: '主页设置',
    description: '',
    icon: HomeFilled
  },
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
    index: '/admin/doctor-service',
    title: '医生服务标签',
    description: '',
    icon: Tickets
  },
  {
    index: '/admin/doctor-schedule',
    title: '医生排班管理',
    description: '',
    icon: Calendar
  },
  {
    index: '/admin/consultation-category',
    title: '问诊分类管理',
    description: '',
    icon: Tickets
  },
  {
    index: '/admin/consultation-template',
    title: '前置模板管理',
    description: '',
    icon: Document
  },
  {
    index: '/admin/body-part',
    title: '身体部位字典',
    description: '维护智能导诊识别使用的身体部位层级数据',
    icon: Location
  },
  {
    index: '/admin/symptom',
    title: '症状字典管理',
    description: '维护症状标准词、关键词和别名映射关系',
    icon: Tickets
  },
  {
    index: '/admin/triage-level',
    title: '分诊等级字典',
    description: '维护风险等级、优先级和建议动作',
    icon: DataAnalysis
  },
  {
    index: '/admin/red-flag',
    title: '红旗规则管理',
    description: '配置高风险触发条件与紧急分诊规则',
    icon: Document
  },
  {
    index: '/admin/consultation-record',
    title: '导诊记录中心',
    description: '查看用户问诊记录、初步分诊结果与规则命中留痕',
    icon: DataAnalysis
  },
  {
    index: '/admin/profile',
    title: '个人信息',
    description: '',
    icon: User
  }
]
</script>
