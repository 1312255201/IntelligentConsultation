# 2026-03-31 Doctor Follow-Up Log

## Goal

Build the next step after doctor handling and structured conclusion:

- allow doctors to append multiple follow-up records after a consultation is completed
- make follow-up history visible to doctor, admin, and user
- prepare reusable follow-up data for later revisit management, quality review, and AI replay

## What Was Added

### 1. New follow-up table

Added:

- `db_consultation_doctor_follow_up`

This table stores multiple doctor follow-up records for the same consultation.

Stored fields include:

- consultation id
- doctor snapshot
- department snapshot
- follow-up type
- patient status change
- follow-up summary
- follow-up advice
- next-step arrangement
- whether another revisit is needed
- next follow-up date
- create time
- update time

### 2. SQL updates

Updated:

- `sql/mysql57-init.sql`

Added upgrade SQL:

- `sql/mysql57-upgrade-2026-03-31-doctor-followup.sql`

This is a clean executable MySQL 5.7 SQL script, not a patch diff.

### 3. Backend model and query layer

Added:

- `ConsultationDoctorFollowUp`
- `ConsultationDoctorFollowUpMapper`
- `ConsultationDoctorFollowUpVO`
- `ConsultationDoctorFollowUpQueryService`
- `ConsultationDoctorFollowUpQueryServiceImpl`
- `DoctorConsultationFollowUpSubmitVO`

### 4. Doctor submit API

Added API:

- `POST /api/doctor/consultation/follow-up`

Doctors can submit:

- follow-up type
- patient status
- summary
- advice
- next step
- need revisit flag
- next follow-up date

### 5. Business rules

Follow-up records are allowed only when:

- the current account is bound to a valid doctor profile
- the consultation belongs to the same department
- the consultation status is already `completed`
- doctor handling record exists and is also `completed`
- the current doctor is the doctor who handled that consultation

If another revisit is needed, the next follow-up date is required.

### 6. Detail aggregation

Added `doctorFollowUps` into:

- doctor consultation detail response
- admin consultation detail response
- user consultation detail response

### 7. Frontend pages updated

#### Doctor page

Updated:

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

Doctor can now:

- view follow-up history
- add a new follow-up record
- choose follow-up type
- choose patient change status
- mark whether another revisit is needed
- set the next follow-up date

#### Admin page

Updated:

- `template-front/src/views/admin/ConsultationRecordPage.vue`

Admin can now view:

- follow-up history list
- follow-up type
- patient status change
- summary
- advice
- next-step arrangement
- revisit requirement
- next follow-up date

#### User page

Updated:

- `template-front/src/views/index/ConsultationPage.vue`

User can now view:

- doctor follow-up history
- summary of each follow-up
- advice
- next-step arrangement
- whether another follow-up is planned

## Files Changed

### Backend

- `template-backend/src/main/java/cn/gugufish/entity/dto/ConsultationDoctorFollowUp.java`
- `template-backend/src/main/java/cn/gugufish/mapper/ConsultationDoctorFollowUpMapper.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationDoctorFollowUpVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/request/DoctorConsultationFollowUpSubmitVO.java`
- `template-backend/src/main/java/cn/gugufish/service/ConsultationDoctorFollowUpQueryService.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationDoctorFollowUpQueryServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/DoctorWorkspaceService.java`
- `template-backend/src/main/java/cn/gugufish/controller/doctor/DoctorWorkspaceController.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/AdminConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/entity/vo/response/ConsultationRecordVO.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/DoctorWorkspaceServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`
- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationServiceImpl.java`

### Frontend

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`
- `template-front/src/views/index/ConsultationPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-03-31-doctor-followup.sql`

### Docs

- `docs/2026-03-31-doctor-followup-log.md`

## Verification

Verified successfully:

- backend: `mvn -q -DskipTests compile`
- frontend: `npm run build`

## Suggested Next Step

After this slice, a good next step is one of:

1. doctor common reply / advice templates
2. doctor follow-up reminder board
3. doctor and AI consistency statistics
4. completed consultation archive filters and export support
