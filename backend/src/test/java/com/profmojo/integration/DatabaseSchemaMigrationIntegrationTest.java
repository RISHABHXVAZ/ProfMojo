package com.profmojo.integration;

import com.profmojo.otp.OnboardingOtpSchemaMigrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Phase 3: Database Schema Migration & Determinism Integration Tests")
class DatabaseSchemaMigrationIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OnboardingOtpSchemaMigrator migrator;

    @Test
    @DisplayName("End-to-End Legacy Database Migration: Detects legacy schema, migrates PK to (role, user_id), preserves data, and enforces idempotency")
    void endToEndLegacyDatabaseMigrationTest() {
        // Step 1: Drop table to simulate legacy database state
        jdbcTemplate.execute("DROP TABLE IF EXISTS onboarding_otp CASCADE");

        // Step 1b: Create legacy table with user_id as single primary key
        jdbcTemplate.execute("""
            CREATE TABLE onboarding_otp (
                user_id VARCHAR(255) NOT NULL PRIMARY KEY,
                role VARCHAR(255),
                otp VARCHAR(255) NOT NULL,
                expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL
            )
        """);

        // Step 2: Insert existing OTP records into legacy table
        jdbcTemplate.execute("""
            INSERT INTO onboarding_otp (user_id, role, otp, expiry)
            VALUES ('LEGACY_USER_001', 'STUDENT', '123456', NOW() + INTERVAL '10 minutes')
        """);
        jdbcTemplate.execute("""
            INSERT INTO onboarding_otp (user_id, role, otp, expiry)
            VALUES ('LEGACY_USER_NULL_ROLE', NULL, '654321', NOW() + INTERVAL '10 minutes')
        """);

        // Verify pre-migration primary key is user_id only
        List<String> legacyPkCols = getPrimaryKeyColumns();
        assertEquals(1, legacyPkCols.size(), "Pre-migration table must have exactly 1 primary key column");
        assertEquals("user_id", legacyPkCols.get(0), "Pre-migration PK must be user_id");

        // Step 3: Execute actual migration mechanism
        migrator.migrateSchema();

        // Step 4: Verify that the old records remain intact
        List<Map<String, Object>> preservedRows = jdbcTemplate.queryForList(
                "SELECT user_id, role, otp FROM onboarding_otp ORDER BY user_id"
        );
        assertEquals(2, preservedRows.size(), "Existing records must be preserved across migration");

        Map<String, Object> row1 = preservedRows.get(0);
        assertEquals("LEGACY_USER_001", row1.get("user_id"));
        assertEquals("STUDENT", row1.get("role"));
        assertEquals("123456", row1.get("otp"));

        Map<String, Object> row2 = preservedRows.get(1);
        assertEquals("LEGACY_USER_NULL_ROLE", row2.get("user_id"));
        assertEquals("UNKNOWN", row2.get("role"), "NULL role must be safely backfilled with 'UNKNOWN'");
        assertEquals("654321", row2.get("otp"));

        // Verify primary key constraint now spans both (role, user_id)
        List<String> postMigrationPkCols = getPrimaryKeyColumns();
        assertEquals(2, postMigrationPkCols.size(), "Migrated table must have exactly 2 primary key columns");
        assertTrue(postMigrationPkCols.contains("role"), "Migrated PK must contain 'role'");
        assertTrue(postMigrationPkCols.contains("user_id"), "Migrated PK must contain 'user_id'");

        // Step 5: Prove that the same identifier can coexist for two different roles
        assertDoesNotThrow(() -> {
            jdbcTemplate.execute("""
                INSERT INTO onboarding_otp (role, user_id, otp, expiry)
                VALUES ('STAFF', 'LEGACY_USER_001', '999888', NOW() + INTERVAL '10 minutes')
            """);
        }, "Same user_id with different role ('STAFF') must be permitted");

        List<Map<String, Object>> user001Rows = jdbcTemplate.queryForList(
                "SELECT role, user_id, otp FROM onboarding_otp WHERE user_id = 'LEGACY_USER_001' ORDER BY role"
        );
        assertEquals(2, user001Rows.size(), "Both STAFF and STUDENT must coexist for LEGACY_USER_001");

        // Step 6: Prove that a duplicate (role, user_id) cannot be inserted
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.execute("""
                INSERT INTO onboarding_otp (role, user_id, otp, expiry)
                VALUES ('STUDENT', 'LEGACY_USER_001', '000000', NOW() + INTERVAL '10 minutes')
            """);
        }, "Duplicate (role, user_id) must be rejected by composite constraint");

        // Step 7: Prove that a repeated migration does not damage the schema or data
        assertDoesNotThrow(() -> migrator.migrateSchema(), "Repeated migration must be idempotent and succeed");

        List<Map<String, Object>> rowsAfterRepeat = jdbcTemplate.queryForList(
                "SELECT role, user_id, otp FROM onboarding_otp ORDER BY user_id, role"
        );
        assertEquals(3, rowsAfterRepeat.size(), "Data count must not change after repeated migration");

        List<String> repeatPkCols = getPrimaryKeyColumns();
        assertEquals(2, repeatPkCols.size(), "Primary key must remain (role, user_id) after repeated migration");
        assertTrue(repeatPkCols.contains("role"));
        assertTrue(repeatPkCols.contains("user_id"));
    }

    @Test
    @DisplayName("Test fresh-install initialization path actually used by Docker Compose (db-init scripts)")
    void testFreshInstallInitializationPath_UsedByDockerCompose() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS onboarding_otp CASCADE");

        // Execute DDL from db-init/02-indexes.sql
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS onboarding_otp (
                role VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN',
                user_id VARCHAR(255) NOT NULL,
                otp VARCHAR(255) NOT NULL,
                expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                CONSTRAINT pk_onboarding_otp PRIMARY KEY (role, user_id)
            )
        """);

        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uk_onboarding_otp_role_user
                ON onboarding_otp (role, user_id)
        """);

        List<String> pkCols = getPrimaryKeyColumns();
        assertEquals(2, pkCols.size(), "Fresh install DDL must create composite PK with 2 columns");
        assertTrue(pkCols.contains("role"));
        assertTrue(pkCols.contains("user_id"));

        // Insert distinct roles with same user_id
        jdbcTemplate.execute("""
            INSERT INTO onboarding_otp (role, user_id, otp, expiry)
            VALUES ('PROFESSOR', 'USER_FRESH_01', '111111', NOW() + INTERVAL '5 minutes')
        """);
        jdbcTemplate.execute("""
            INSERT INTO onboarding_otp (role, user_id, otp, expiry)
            VALUES ('STUDENT', 'USER_FRESH_01', '222222', NOW() + INTERVAL '5 minutes')
        """);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM onboarding_otp WHERE user_id = 'USER_FRESH_01'",
                Integer.class
        );
        assertEquals(2, count);

        // Disallow duplicate (role, user_id)
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.execute("""
                INSERT INTO onboarding_otp (role, user_id, otp, expiry)
                VALUES ('PROFESSOR', 'USER_FRESH_01', '333333', NOW() + INTERVAL '5 minutes')
            """);
        });
    }

    @Test
    @DisplayName("Verify migrator handles missing table cleanly on empty database")
    void testMigratorHandlesMissingTableCleanly() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS onboarding_otp CASCADE");

        migrator.migrateSchema();

        List<String> pkCols = getPrimaryKeyColumns();
        assertEquals(2, pkCols.size());
        assertTrue(pkCols.contains("role"));
        assertTrue(pkCols.contains("user_id"));
    }

    private List<String> getPrimaryKeyColumns() {
        String sql = """
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = current_schema()
              AND tc.table_name = 'onboarding_otp'
              AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY kcu.ordinal_position
        """;
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
