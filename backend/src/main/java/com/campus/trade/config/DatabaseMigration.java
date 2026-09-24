package com.campus.trade.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库迁移：手动添加 F7 新增的 orders 表列。
 * Hibernate ddl-auto=update 无法为已有数据的表添加 NOT NULL 列（amount），
 * 因此在此手动执行 ALTER TABLE。
 */
@Component
public class DatabaseMigration {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void migrate() {
        addColumnIfNotExists("orders", "amount", "numeric(10,2) DEFAULT 0 NOT NULL");
        addColumnIfNotExists("orders", "shipping_company", "varchar(50)");
        addColumnIfNotExists("orders", "tracking_no", "varchar(50)");
        addColumnIfNotExists("orders", "shipped_at", "timestamp");
        addColumnIfNotExists("orders", "completed_at", "timestamp");
        addColumnIfNotExists("orders", "refund_reason", "varchar(255)");
        addColumnIfNotExists("orders", "refunded_at", "timestamp");
    }

    private void addColumnIfNotExists(String table, String column, String definition) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + definition
            );
            System.out.println("[DB Migration] 列已确保存在: " + table + "." + column);
        } catch (Exception e) {
            System.err.println("[DB Migration] 跳过 " + table + "." + column + ": " + e.getMessage());
        }
    }
}
