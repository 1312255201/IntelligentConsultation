package cn.gugufish.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@Order(0)
public class ConsultationSchemaUpgradeRunner implements ApplicationRunner {

    private static final String OPERATION_LOG_TABLE = "db_operation_log";
    private static final String CONSULTATION_RECORD_TABLE = "db_consultation_record";
    private static final String SERVICE_FEEDBACK_TABLE = "db_consultation_service_feedback";
    private static final String SERVICE_FEEDBACK_HANDLE_INDEX = "idx_consultation_service_feedback_doctor_handle";
    private static final String CHECK_SUGGESTION_TABLE = "db_consultation_check_suggestion";
    private static final String REPORT_FEEDBACK_TABLE = "db_consultation_report_feedback";
    private static final String MEDICATION_FEEDBACK_TABLE = "db_consultation_medication_feedback";

    private final JdbcTemplate jdbcTemplate;

    public ConsultationSchemaUpgradeRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schema = currentSchema();
        if (schema == null) {
            log.warn("数据库结构升级检查跳过：当前连接未返回 schema 名称");
            return;
        }
        ensureOperationLogTable(schema);
        if (!tableExists(schema, CONSULTATION_RECORD_TABLE)) {
            log.warn("数据库结构升级检查跳过：表 {} 不存在", CONSULTATION_RECORD_TABLE);
            return;
        }
        ensureStructuredFeedbackTables(schema);
        if (!tableExists(schema, SERVICE_FEEDBACK_TABLE)) {
            log.info("数据库结构升级检查跳过：表 {} 不存在", SERVICE_FEEDBACK_TABLE);
            return;
        }
        ensureConsultationServiceFeedbackHandleColumns(schema);
    }

    private void ensureOperationLogTable(String schema) {
        if (tableExists(schema, OPERATION_LOG_TABLE)) return;
        executeDdl("""
                CREATE TABLE `db_operation_log` (
                  `id` int NOT NULL AUTO_INCREMENT,
                  `request_id` bigint NOT NULL,
                  `request_url` varchar(255) NOT NULL,
                  `request_method` varchar(20) NOT NULL,
                  `remote_ip` varchar(64) DEFAULT NULL,
                  `account_id` int DEFAULT NULL,
                  `username` varchar(100) DEFAULT NULL,
                  `role` varchar(50) DEFAULT NULL,
                  `request_params` text,
                  `response_code` int DEFAULT NULL,
                  `response_summary` text,
                  `duration_ms` int DEFAULT NULL,
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  KEY `idx_operation_log_request_time` (`request_url`, `create_time`),
                  KEY `idx_operation_log_account_time` (`account_id`, `create_time`),
                  KEY `idx_operation_log_request_id` (`request_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """, "自动创建操作日志表");
    }

    private void ensureStructuredFeedbackTables(String schema) {
        if (!tableExists(schema, CHECK_SUGGESTION_TABLE)) {
            executeDdl("""
                    CREATE TABLE `db_consultation_check_suggestion` (
                      `id` int NOT NULL AUTO_INCREMENT,
                      `consultation_id` int NOT NULL,
                      `doctor_id` int DEFAULT NULL,
                      `doctor_name` varchar(50) DEFAULT NULL,
                      `department_id` int DEFAULT NULL,
                      `department_name` varchar(50) DEFAULT NULL,
                      `item_name` varchar(100) NOT NULL,
                      `item_type` varchar(20) NOT NULL DEFAULT 'other',
                      `urgency_level` varchar(20) NOT NULL DEFAULT 'routine',
                      `purpose` varchar(300) DEFAULT NULL,
                      `attention_note` varchar(300) DEFAULT NULL,
                      `status` tinyint(1) NOT NULL DEFAULT 1,
                      `sort` int NOT NULL DEFAULT 0,
                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      KEY `idx_consultation_check_suggestion_consultation_sort` (`consultation_id`, `status`, `sort`, `id`),
                      KEY `idx_consultation_check_suggestion_doctor_time` (`doctor_id`, `update_time`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """, "自动创建结构化检查建议表");
        }
        if (!tableExists(schema, REPORT_FEEDBACK_TABLE)) {
            executeDdl("""
                    CREATE TABLE `db_consultation_report_feedback` (
                      `id` int NOT NULL AUTO_INCREMENT,
                      `consultation_id` int NOT NULL,
                      `suggestion_id` int DEFAULT NULL,
                      `account_id` int NOT NULL,
                      `patient_id` int NOT NULL,
                      `patient_name` varchar(50) NOT NULL,
                      `report_type` varchar(20) NOT NULL DEFAULT 'other',
                      `report_name` varchar(100) DEFAULT NULL,
                      `report_summary` varchar(1000) DEFAULT NULL,
                      `report_date` date DEFAULT NULL,
                      `doctor_question` varchar(300) DEFAULT NULL,
                      `attachments_json` text,
                      `status` tinyint(1) NOT NULL DEFAULT 1,
                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      KEY `idx_consultation_report_feedback_consultation_time` (`consultation_id`, `status`, `create_time`),
                      KEY `idx_consultation_report_feedback_account_time` (`account_id`, `create_time`),
                      KEY `idx_consultation_report_feedback_suggestion` (`suggestion_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """, "自动创建检查报告回传表");
        }
        if (!tableExists(schema, MEDICATION_FEEDBACK_TABLE)) {
            executeDdl("""
                    CREATE TABLE `db_consultation_medication_feedback` (
                      `id` int NOT NULL AUTO_INCREMENT,
                      `consultation_id` int NOT NULL,
                      `prescription_id` int DEFAULT NULL,
                      `medicine_id` int DEFAULT NULL,
                      `medicine_name` varchar(100) DEFAULT NULL,
                      `account_id` int NOT NULL,
                      `patient_id` int NOT NULL,
                      `patient_name` varchar(50) NOT NULL,
                      `feedback_type` varchar(30) NOT NULL DEFAULT 'other',
                      `severity_level` varchar(20) NOT NULL DEFAULT 'mild',
                      `action_taken` varchar(20) NOT NULL DEFAULT 'consulting',
                      `feedback_summary` varchar(1000) DEFAULT NULL,
                      `doctor_question` varchar(300) DEFAULT NULL,
                      `attachments_json` text,
                      `status` tinyint(1) NOT NULL DEFAULT 1,
                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (`id`),
                      KEY `idx_consultation_medication_feedback_consultation_time` (`consultation_id`, `status`, `create_time`),
                      KEY `idx_consultation_medication_feedback_account_time` (`account_id`, `create_time`),
                      KEY `idx_consultation_medication_feedback_prescription` (`prescription_id`),
                      KEY `idx_consultation_medication_feedback_medicine` (`medicine_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """, "自动创建用药反馈表");
        }
    }

    private void ensureConsultationServiceFeedbackHandleColumns(String schema) {
        Map<String, String> missingColumnSql = new LinkedHashMap<>();
        if (!columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_status")) {
            missingColumnSql.put("doctor_handle_status",
                    "ALTER TABLE `db_consultation_service_feedback` ADD COLUMN `doctor_handle_status` tinyint(1) NOT NULL DEFAULT 0 AFTER `feedback_text`");
        }
        if (!columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_remark")) {
            missingColumnSql.put("doctor_handle_remark",
                    "ALTER TABLE `db_consultation_service_feedback` ADD COLUMN `doctor_handle_remark` varchar(1000) DEFAULT NULL AFTER `doctor_handle_status`");
        }
        if (!columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_account_id")) {
            missingColumnSql.put("doctor_handle_account_id",
                    "ALTER TABLE `db_consultation_service_feedback` ADD COLUMN `doctor_handle_account_id` int DEFAULT NULL AFTER `doctor_handle_remark`");
        }
        if (!columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_doctor_id")) {
            missingColumnSql.put("doctor_handle_doctor_id",
                    "ALTER TABLE `db_consultation_service_feedback` ADD COLUMN `doctor_handle_doctor_id` int DEFAULT NULL AFTER `doctor_handle_account_id`");
        }
        if (!columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_doctor_name")) {
            missingColumnSql.put("doctor_handle_doctor_name",
                    "ALTER TABLE `db_consultation_service_feedback` ADD COLUMN `doctor_handle_doctor_name` varchar(50) DEFAULT NULL AFTER `doctor_handle_doctor_id`");
        }
        if (!columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_time")) {
            missingColumnSql.put("doctor_handle_time",
                    "ALTER TABLE `db_consultation_service_feedback` ADD COLUMN `doctor_handle_time` datetime DEFAULT NULL AFTER `doctor_handle_doctor_name`");
        }

        missingColumnSql.forEach((column, sql) -> executeDdl(sql,
                "自动补齐问诊服务评价表缺失字段 `" + column + "`"));

        if (allHandleColumnsExist(schema) && !indexExists(schema, SERVICE_FEEDBACK_TABLE, SERVICE_FEEDBACK_HANDLE_INDEX)) {
            executeDdl(
                    "ALTER TABLE `db_consultation_service_feedback` ADD KEY `idx_consultation_service_feedback_doctor_handle` (`doctor_id`, `doctor_handle_status`, `update_time`)",
                    "自动补齐问诊服务评价表缺失索引 `" + SERVICE_FEEDBACK_HANDLE_INDEX + "`"
            );
        }
    }

    private boolean allHandleColumnsExist(String schema) {
        return columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_status")
                && columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_remark")
                && columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_account_id")
                && columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_doctor_id")
                && columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_doctor_name")
                && columnExists(schema, SERVICE_FEEDBACK_TABLE, "doctor_handle_time");
    }

    private void executeDdl(String sql, String action) {
        try {
            jdbcTemplate.execute(sql);
            log.info("{} 成功", action);
        } catch (DataAccessException exception) {
            throw new IllegalStateException(action + "失败，请检查数据库账号是否具备 DDL 权限，或手动执行 sql 目录下对应的升级脚本", exception);
        }
    }

    private String currentSchema() {
        return jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
    }

    private boolean tableExists(String schema, String tableName) {
        return countByMetadata(
                "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
                schema, tableName
        ) > 0;
    }

    private boolean columnExists(String schema, String tableName, String columnName) {
        return countByMetadata(
                "SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                schema, tableName, columnName
        ) > 0;
    }

    private boolean indexExists(String schema, String tableName, String indexName) {
        return countByMetadata(
                "SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?",
                schema, tableName, indexName
        ) > 0;
    }

    private int countByMetadata(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
