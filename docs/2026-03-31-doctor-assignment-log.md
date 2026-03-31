# 2026-03-31 Doctor Assignment Log

## Goal

Complete the doctor-side consultation claiming flow:

- let doctors claim and release consultation orders inside their department
- show assignment status clearly in doctor and admin pages
- persist assignment data in MySQL 5.7
- keep this slice documented for later follow-up work

## What Was Added

### 1. New assignment table

Added table:

- `db_consultation_doctor_assignment`

Stored fields include:

- consultation id
- doctor snapshot
- department snapshot
- assignment status
- claim time
- release time
- create time
- update time

This table is used for queue ownership only, and stays separate from:

- `db_consultation_doctor_handle`
- `db_consultation_doctor_conclusion`

### 2. SQL updates

Updated:

- `sql/mysql57-init.sql`

Added upgrade SQL:

- `sql/mysql57-upgrade-2026-03-31-doctor-assignment.sql`

This upgrade file is a clean executable SQL script for MySQL 5.7, not a patch diff.

### 3. Admin backend aggregation

Updated admin consultation record aggregation so both:

- list response
- detail response

now include:

- `doctorAssignment`

This lets the admin side directly see whether a consultation is:

- unclaimed
- claimed
- released

and by which doctor.

### 4. Doctor frontend page

Rebuilt:

- `template-front/src/views/doctor/DoctorConsultationPage.vue`

Doctor page now supports:

- ownership filtering: all / unclaimed / mine / others
- claim button
- release button
- assignment status display in list
- assignment info display in detail drawer
- lock editing when a record is claimed by another doctor
- submit handling and structured conclusion only when current doctor can operate

### 5. Admin frontend page

Rebuilt:

- `template-front/src/views/admin/ConsultationRecordPage.vue`

Admin page now shows:

- assignment status in the list
- assigned doctor in the list
- assignment archive section in the detail dialog
- doctor handling archive
- doctor structured conclusion
- AI result archive
- user feedback
- rule hit logs
- structured intake answers

## Business Rules In Effect

- only doctors with valid bound doctor profiles can claim or release
- only consultations in the same department can be claimed
- completed consultations cannot be claimed again
- if another doctor has already entered handling flow, claim is blocked
- releasing is only allowed for self-claimed consultations
- if a consultation already entered processing or completed handling state, release is blocked
- submitting doctor handling auto-claims the consultation when no active claim exists
- if another doctor already owns the active claim, handling submission is blocked

## Files Changed

### Backend

- `template-backend/src/main/java/cn/gugufish/service/impl/ConsultationRecordAdminServiceImpl.java`

### Frontend

- `template-front/src/views/doctor/DoctorConsultationPage.vue`
- `template-front/src/views/admin/ConsultationRecordPage.vue`

### SQL

- `sql/mysql57-init.sql`
- `sql/mysql57-upgrade-2026-03-31-doctor-assignment.sql`

### Docs

- `docs/2026-03-31-doctor-assignment-log.md`

## Verification

Verified successfully:

- backend: `mvn -q -DskipTests compile`
- frontend: `npm run build`

## Suggested Next Step

After this slice, a natural next step is one of:

1. doctor-side follow-up records for completed consultations
2. doctor-side quick reply / common advice templates
3. doctor performance and AI consistency statistics
4. richer doctor workspace filters such as by triage level and pending aging
