# 2026-03-31 Doctor Handling Log

## Goal

Continue the doctor workspace so doctors can do more than just view consultation records:

- claim and process a consultation record
- save doctor-facing handling notes
- move the consultation through `processing` and `completed`
- expose the doctor handling result to doctor, user, and admin views

## What Was Added

### 1. New doctor handling table

Added a dedicated table instead of overloading `db_consultation_record`:

- `db_consultation_doctor_handle`

Stored fields:

- consultation id
- doctor snapshot
- department snapshot
- handling status
- doctor summary
- medical advice
- follow-up plan
- internal remark
- receive time
- complete time

Why this structure:

- easier to audit later
- easier to extend into multi-step doctor operations
- keeps consultation base data and doctor processing data separated

### 2. Backend data/query layer

Added:

- `ConsultationDoctorHandle`
- `ConsultationDoctorHandleMapper`
- `ConsultationDoctorHandleVO`
- `ConsultationDoctorHandleQueryService`
- `ConsultationDoctorHandleQueryServiceImpl`

These are now used to aggregate doctor handling data into existing detail responses.

### 3. Doctor submit API

Added:

- `POST /api/doctor/consultation/handle`

Request object:

- `DoctorConsultationHandleSubmitVO`

Supported statuses:

- `processing`
- `completed`

Current rules:

- doctor account must already be bound to a doctor profile
- doctor profile must be enabled
- consultation must belong to the doctor’s department
- doctor summary is required
- medical advice is required when completing the consultation
- if another doctor has already taken the consultation, overwrite is blocked
- completed consultations cannot be rolled back to processing

### 4. Consultation detail aggregation

Added `doctorHandle` to:

- user consultation detail response
- admin consultation detail response
- doctor consultation detail response

This means all three sides can now see the latest doctor processing result from the same data source.

### 5. Frontend pages updated

#### Doctor side

Updated doctor consultation page to support:

- viewing existing doctor handling status
- editing summary, advice, follow-up plan, internal remark
- marking consultation as `processing`
- marking consultation as `completed`
- viewing richer answer content, including uploaded images

#### Admin side

Updated admin consultation detail page to show:

- doctor handling archive
- doctor name
- receive/complete time
- doctor summary
- medical advice
- follow-up plan
- internal remark

#### User side

Updated user consultation detail page to show:

- whether a doctor has taken over
- handling progress
- doctor summary
- medical advice
- follow-up plan

User side does not expose internal remark.

## SQL Files

Updated init SQL:

- `sql/mysql57-init.sql`

Added upgrade SQL for existing databases:

- `sql/mysql57-upgrade-2026-03-31-doctor-handling.sql`

Import guidance:

1. New database: use the latest `mysql57-init.sql`
2. Existing database: run `mysql57-upgrade-2026-03-31-doctor-handling.sql`

## Files Changed

### Backend

- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationDoctorHandle.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationHandleSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationDoctorHandleVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/mapper/ConsultationDoctorHandleMapper.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationDoctorHandleQueryService.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationDoctorHandleQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`

### Frontend

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`
- `template-front/src/views/index/ConsultationPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-03-31-doctor-handling.sql`

## Verification

Verified successfully:

- backend: `mvn -q -DskipTests compile`
- frontend: `npm run build`

## Suggested Next Step

After this slice, the next natural stage is:

1. doctor-side structured reply templates
2. doctor-side consultation assignment / queue ownership
3. doctor-side follow-up records
4. stronger “final archived conclusion” structure for later AI training
