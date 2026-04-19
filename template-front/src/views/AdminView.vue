<template>
  <WorkspaceShell
    brand-mark="AD"
    brand-title="系统后台管理中心"
    brand-subtitle="后台管理中心"
    page-kicker="后台工作区"
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
  UserFilled,
  Grid,
  SetUp,
  Guide,
  Connection,
  Collection
} from '@element-plus/icons-vue'
import WorkspaceShell from '@/components/WorkspaceShell.vue'

const menuItems = [
  {
    index: 'user-auth-group',
    title: '用户与权限管理',
    icon: User,
    children: [
      {
        index: '/admin/account',
        title: '系统登录信息管理',
        description: '查看系统账号、调整角色并重置登录密码。',
        icon: Setting
      },
      {
        index: '/admin/user',
        title: '平台用户详情',
        description: '查看用户建档、问诊、支付和处方覆盖情况。',
        icon: UserFilled
      },
      {
        index: '/admin/profile',
        title: '管理员个人设置',
        description: '维护当前管理员账号的邮箱、密码和头像。',
        icon: User
      }
    ]
  },
  {
    index: 'doctor-group',
    title: '医疗专家与排班',
    icon: Calendar,
    children: [
      {
        index: '/admin/doctor',
        title: '医生信息维护',
        description: '维护医生档案、照片、专长信息与账号绑定。',
        icon: UserFilled
      },
      {
        index: '/admin/doctor-service',
        title: '医生服务标签',
        description: '维护医生服务方向和能力分类，支持导诊精准推荐。',
        icon: Tickets
      },
      {
        index: '/admin/doctor-schedule',
        title: '医生排班日历',
        description: '动态配置医生当周排班、接诊方式与号源容量。',
        icon: Calendar
      }
    ]
  },
  {
    index: 'medical-data-group',
    title: '基础医疗字典库',
    icon: Collection,
    children: [
      {
        index: '/admin/resource',
        title: '全院医疗资源大盘',
        description: '统一监控科室分布、医生比例、排班饱和度等核心资产。',
        icon: Grid
      },
      {
        index: '/admin/department',
        title: '科室信息维护',
        description: '维护标准科室基础数据，供患者问诊、挂号落点复用。',
        icon: OfficeBuilding
      },
      {
        index: '/admin/body-part',
        title: '身体部位结构字典',
        description: '维护高精度智能导诊识别引擎使用的身体部位映射数据。',
        icon: Location
      },
      {
        index: '/admin/symptom',
        title: '临床症状字典',
        description: '维护标准临床症状词、同义别名与高频匹配关键字。',
        icon: Tickets
      },
      {
        index: '/admin/medicine',
        title: '临床药品控制目录',
        description: '维护处方药品基础信息、过敏禁忌提醒与合并用药冲突规则。',
        icon: Document
      }
    ]
  },
  {
    index: 'consultation-group',
    title: '问诊服务记录流',
    icon: ShoppingCart,
    children: [
      {
        index: '/admin/consultation-category',
        title: '服务入口与分类',
        description: '维护患者前端看到的症状分类与对应背后的真实医疗科室归属。',
        icon: List
      },
      {
        index: '/admin/consultation-template',
        title: '前置症状采集模版',
        description: '配置患者针对系统各类主诉必须填写的前置健康评估表单。',
        icon: Document
      },
      {
        index: '/admin/consultation-record',
        title: '问诊记录审计中心',
        description: '全程追踪并查阅患者问诊历史记录、全链路分诊结果和履约评价闭环。',
        icon: DataAnalysis
      },
      {
        index: '/admin/order',
        title: '问诊支付流水信息',
        description: '监控真实产生的问诊订单金额流水、实际支付状态和对账记录。',
        icon: ShoppingCart
      }
    ]
  },
  {
    index: 'ai-triage-group',
    title: '智能导诊策略库',
    icon: Connection,
    children: [
      {
        index: '/admin/triage-level',
        title: '分诊与急重症等级',
        description: '结构化维护系统医疗风险等级定义、优先级排队参数和急救建议动作。',
        icon: DataAnalysis
      },
      {
        index: '/admin/red-flag',
        title: '分诊红旗预警规则',
        description: '高度警觉的高风险词触发匹配模型和极端预警分诊强制定向转岗规则。',
        icon: Document
      },
      {
        index: '/admin/triage-knowledge',
        title: '导诊医学知识图谱',
        description: '管理支撑大模型 智能导诊引擎使用的外部指南库和事实医学说明。',
        icon: Guide
      },
      {
        index: '/admin/triage-case',
        title: '导诊仿真案例样本',
        description: '沉淀典型症状诊断推断样本，为导诊算法检索匹配、结果校验提供真实标注数据。',
        icon: Document
      },
      {
        index: '/admin/consultation-dispatch',
        title: '候诊池智能分配',
        description: '调节分配引擎的空闲推荐排序权重系数、并行推荐池容量和候诊超时回收阈值。',
        icon: SetUp
      },
      {
        index: '/admin/consultation-ai',
        title: '智能底层配置',
        description: '实时监控导诊大模型接口延迟情况、生成参数配置控制和患者交互安全审计总览。',
        icon: DataAnalysis
      }
    ]
  },
  {
    index: 'system-platform-group',
    title: '系统级功能配置',
    icon: Setting,
    children: [
      {
        index: '/admin/global-config',
        title: '全局核心参数',
        description: '统一配置应用级全局功能开关、智能系统阈值阀门和各默认兜底路由配置。',
        icon: Setting
      },
      {
        index: '/admin/platform',
        title: '患者端首页展示',
        description: '可动态替换运营级的门户首页文案、手动推选权威医生和经典真实好转案例。',
        icon: HomeFilled
      },
      {
        index: '/admin/operation-log',
        title: '用户行为请求审计',
        description: '查询过去一段时间内细颗粒度的所有系统内部请求日志、核心响应结果溯源和性能开销。',
        icon: Operation
      }
    ]
  }
]
</script>
