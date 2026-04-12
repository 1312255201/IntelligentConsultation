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

    private static final String SERVICE_FEEDBACK_TABLE = "db_consultation_service_feedback";
    private static final String SERVICE_FEEDBACK_HANDLE_INDEX = "idx_consultation_service_feedback_doctor_handle";

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
        if (!tableExists(schema, SERVICE_FEEDBACK_TABLE)) {
            log.info("数据库结构升级检查跳过：表 {} 不存在", SERVICE_FEEDBACK_TABLE);
            return;
        }
        ensureConsultationServiceFeedbackHandleColumns(schema);
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
            throw new IllegalStateException(action + "失败，请检查数据库账号是否有 ALTER 权限，或手动执行 sql/mysql57-upgrade-2026-04-11-doctor-service-feedback-handle.sql", exception);
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
