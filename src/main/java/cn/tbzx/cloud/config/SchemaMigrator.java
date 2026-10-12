package cn.tbzx.cloud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 轻量 schema 迁移: 启动时检查并补列/补索引 (Flyway 的简化替代, 演示项目用)
 * 幂等 — 列/索引已存在则跳过, 重复启动安全
 * @Order(0) 先于 DemoDataInitializer, 保证结构就绪后再种数据
 */
@Slf4j
@Order(0)
@Component
public class SchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;

    public SchemaMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("device_location", "session_id",
                "BIGINT DEFAULT NULL COMMENT '所属出行 session.id (轨迹归属键)'");
        addIndexIfMissing("device_location", "idx_device_session_ts",
                "(device_id, session_id, point_ts)");
    }

    private void addColumnIfMissing(String table, String column, String defn) {
        String sql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = '" + table + "' "
                + "AND column_name = '" + column + "'";
        if (count(sql) == 0) {
            exec("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + defn);
            log.info("[migration] {}.{} column added", table, column);
        }
    }

    private void addIndexIfMissing(String table, String index, String cols) {
        String sql = "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = '" + table + "' "
                + "AND index_name = '" + index + "'";
        if (count(sql) == 0) {
            exec("ALTER TABLE `" + table + "` ADD INDEX `" + index + "` " + cols);
            log.info("[migration] {}.{} index added", table, index);
        }
    }

    private int count(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            log.warn("[migration] check failed: {}", e.getMessage());
            return 0;
        }
    }

    private void exec(String sql) {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (Exception e) {
            log.error("[migration] exec failed: {} | {}", sql, e.getMessage());
        }
    }
}
