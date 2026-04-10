# 2026-04-10 医生表单 AI 字段治理面板增强记录

## 1. 本轮目标

上一轮已经完成了医生表单 AI 的字段维度排行，但管理端页面仍然偏静态：

- 只能看到固定顺序的字段卡片
- 还不能按重点指标切换排序
- 还缺少字段级带入率、保存率这类转化视角

因此本轮继续沿现有 `ConsultationAiConfigPage.vue` 增强，不新开页面，直接把字段治理能力补成可筛选、可排序、可看转化率的面板。

## 2. 本轮完成

### 2.1 后端补充字段级治理指标

更新：

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageFieldVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`

字段维度响应中新增：

- `relatedCount`
- `applyRate`
- `saveRate`

统计口径：

- `relatedCount`
  - 统计某字段相关的日志总数
  - 相关日志包括字段级 `AI 重写`
  - 也包括后续被 `AI + 模板拼装` 命中的字段日志
- `applyRate`
  - `appliedCount / relatedCount`
- `saveRate`
  - `savedCount / relatedCount`

这样管理端就不只能看“次数”，还能看“这个字段被 AI 触达后，最终有多少真的被医生带入和保存”。

### 2.2 管理端字段排行增强

更新：

- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

在“医生端 AI 处理 / 随访草稿使用”区域中，字段排行增强为：

- 支持按场景筛选
  - 全部场景
  - 医生处理
  - 随访记录
- 支持按指标排序
  - 相关日志数
  - 字段重写数
  - 模板拼装数
  - 带入次数
  - 保存次数
  - 带入率
  - 保存率
- 字段卡片显示当前排序主指标
- 字段卡片继续保留字段基础计数信息

页面仍然放在现有治理页里，没有新增路由或拆分子页面，保持了当前项目的管理端结构。

## 3. 设计取舍

本轮没有把排序和筛选下沉到后端接口参数，而是先放在前端完成，原因是：

- 当前字段排行数据量很小，前端排序足够
- 这样不需要改控制器和接口协议
- 后续如果字段治理继续扩展到模板级下钻，再考虑服务端分页和排序也不迟

同时，本轮字段级比率的分母使用 `relatedCount`，而不是简单使用“字段重写数”：

- 因为字段的真实落地不仅来自字段重写
- 还包括通过模板拼装进入表单的字段触达
- 这个口径更贴近治理视角，而不只是生成视角

## 4. 涉及文件

- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminDoctorFormAiUsageFieldVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationAiAdminServiceImpl.java`
- `template-front/src/views/admin/ConsultationAiConfigPage.vue`

## 5. 验证

本轮已完成验证：

- 前端执行 `cmd /c npm run build` 通过
- 后端执行 Maven 编译通过

## 6. 下一步建议

建议下一轮继续优先推进：

1. 字段治理下钻到“字段 + 模板标题”，找出哪个模板最常参与某字段拼装。
2. 增加字段治理导出能力，方便离线分析高频字段。
3. 医生端补“基于当前草稿继续改写”能力，降低字段级重写时其他字段漂移。
