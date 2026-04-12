# 2026-04-12 问诊归档摘要导出补齐记录

## 本次目标

在已完成“问诊归档摘要”聚合与展示的基础上，继续补齐患者侧留存能力，让用户可以直接下载本次问诊的归档文本。

## 已完成内容

### 1. 用户侧新增归档摘要导出接口

- 更新 [ConsultationController.java](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-backend/src/main/java/cn/gugufish/controller/ConsultationController.java)
- 新增接口：`GET /api/user/consultation/record/archive/export?recordId=...`

接口行为：
- 复用现有 `recordDetail` 返回中的 `archiveSummary.plainText`
- 下载文件名格式为 `consultation-archive-<consultationNo>.txt`
- 以 `text/plain;charset=UTF-8` 返回
- 额外写入 UTF-8 BOM，兼容 Windows 记事本直接打开中文文本

### 2. 患者详情页新增“下载摘要”操作

- 更新 [ConsultationPage.vue](/c:/Users/13122/IdeaProjects/IntelligentConsultation/template-front/src/views/index/ConsultationPage.vue)

新增能力：
- 在“问诊归档摘要”面板工具栏增加“下载摘要”按钮
- 复用前端现有 `download` 能力发起下载
- 下载成功后给出明确提示

### 3. 保持与已有复制能力并存

- 当前摘要工具栏同时支持：
  - 复制摘要
  - 下载摘要

这样患者既可以快速复制粘贴，也可以直接把归档文本留档。

## 当前效果

- 患者打开问诊详情后，不仅可以集中回看归档摘要，还可以直接下载为文本文件
- 下载后的文本可在 Windows 环境中直接打开，中文内容不会因为编码问题出现明显乱码

## 验证

- 前端：`cmd /c npm run build` 通过
- 后端：`"C:\Users\13122\AppData\Local\Programs\IntelliJ IDEA Ultimate\plugins\maven\lib\maven3\bin\mvn.cmd" -q -DskipTests compile` 通过

## 后续建议

1. 在此基础上继续补 `PDF` 导出，便于正式归档与打印
2. 将相同导出能力复用到医生端详情页和管理端详情页
3. 继续补更标准化的“检查建议 / 用药建议 / 复诊建议”字段，提升导出内容的业务完整度
