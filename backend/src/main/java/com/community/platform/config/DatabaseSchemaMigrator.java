package com.community.platform.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaMigrator {

    private final DataSource dataSource;

    @PostConstruct
    public void migrate() {
        try {
            addBookingServiceTypeColumn();
        } catch (SQLException e) {
            throw new IllegalStateException("Database schema migration failed", e);
        }
    }

    private void addBookingServiceTypeColumn() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!columnExists(connection, "user", "booking_service_type")) {
                execute(connection, """
                        ALTER TABLE `user`
                        ADD COLUMN `booking_service_type` VARCHAR(50) DEFAULT NULL
                        COMMENT 'booking staff service type: dining/accompany/home_visit'
                        AFTER `staff_type`
                        """);

                if (!indexExists(connection, "user", "idx_booking_service_type")) {
                    execute(connection, "CREATE INDEX `idx_booking_service_type` ON `user` (`booking_service_type`)");
                }
                log.info("Added user.booking_service_type column for booking staff isolation");
            }

            execute(connection, """
                    UPDATE `user`
                    SET `booking_service_type` = 'dining'
                    WHERE `role` = 'staff'
                      AND `staff_type` = 'booking'
                      AND `booking_service_type` IS NULL
                    """);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        return count(connection, """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, tableName, columnName) > 0;
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        return count(connection, """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """, tableName, indexName) > 0;
    }

    private int count(Connection connection, String sql, String first, String second) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, first);
            statement.setString(2, second);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
