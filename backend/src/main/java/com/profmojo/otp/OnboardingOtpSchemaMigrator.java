package com.profmojo.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Executes a deterministic, idempotent schema migration for the {@code onboarding_otp} table.
 *
 * Migration contract:
 * 1. Missing table: Creates table with composite primary key (role, user_id).
 * 2. Already migrated: Detects (role, user_id) composite PK and exits cleanly without modifying data or schema.
 * 3. Legacy table (single-column user_id PK):
 *    - Preserves all existing rows.
 *    - Ensures the 'role' column exists, defaulting any existing NULL values to 'UNKNOWN'.
 *    - Drops the old primary key constraint.
 *    - Adds composite primary key constraint (role, user_id).
 *    - Ensures the composite unique index exists.
 * 4. Idempotency: Safe against repeated execution.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class OnboardingOtpSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateSchema();
    }

    public synchronized void migrateSchema() {
        log.info("Inspecting onboarding_otp schema version and primary key constraint...");

        Integer tableCount = jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name = 'onboarding_otp'
            """, Integer.class);

        if (tableCount == null || tableCount == 0) {
            log.info("Table 'onboarding_otp' does not exist. Creating with composite primary key (role, user_id)...");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS onboarding_otp (
                    role VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN',
                    user_id VARCHAR(255) NOT NULL,
                    otp VARCHAR(255) NOT NULL,
                    expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                    CONSTRAINT pk_onboarding_otp PRIMARY KEY (role, user_id)
                )
            """);
            createUniqueIndexIfNotExists();
            log.info("Table 'onboarding_otp' created successfully.");
            return;
        }

        List<String> pkColumns = jdbcTemplate.queryForList("""
            SELECT kcu.column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
              AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = current_schema()
              AND tc.table_name = 'onboarding_otp'
              AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY kcu.ordinal_position
        """, String.class);

        if (pkColumns.size() == 2 && pkColumns.contains("role") && pkColumns.contains("user_id")) {
            log.info("onboarding_otp table already has composite primary key (role, user_id). Schema is up to date.");
            createUniqueIndexIfNotExists();
            return;
        }

        log.warn("Legacy onboarding_otp primary key detected: {}. Migrating to composite primary key (role, user_id)...", pkColumns);

        Integer roleColCount = jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'onboarding_otp'
              AND column_name = 'role'
        """, Integer.class);

        if (roleColCount == null || roleColCount == 0) {
            log.info("Adding 'role' column to onboarding_otp table...");
            jdbcTemplate.execute("ALTER TABLE onboarding_otp ADD COLUMN role VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN'");
        } else {
            jdbcTemplate.execute("UPDATE onboarding_otp SET role = 'UNKNOWN' WHERE role IS NULL");
            jdbcTemplate.execute("ALTER TABLE onboarding_otp ALTER COLUMN role SET NOT NULL");
        }

        jdbcTemplate.execute("ALTER TABLE onboarding_otp ALTER COLUMN user_id SET NOT NULL");

        List<String> pkConstraintNames = jdbcTemplate.queryForList("""
            SELECT tc.constraint_name
            FROM information_schema.table_constraints tc
            WHERE tc.table_schema = current_schema()
              AND tc.table_name = 'onboarding_otp'
              AND tc.constraint_type = 'PRIMARY KEY'
        """, String.class);

        for (String constraintName : pkConstraintNames) {
            log.info("Dropping legacy primary key constraint: {}", constraintName);
            jdbcTemplate.execute("ALTER TABLE onboarding_otp DROP CONSTRAINT " + constraintName);
        }

        log.info("Adding composite primary key constraint 'pk_onboarding_otp' on (role, user_id)...");
        jdbcTemplate.execute("ALTER TABLE onboarding_otp ADD CONSTRAINT pk_onboarding_otp PRIMARY KEY (role, user_id)");

        createUniqueIndexIfNotExists();
        log.info("onboarding_otp schema migration to composite primary key completed successfully.");
    }

    private void createUniqueIndexIfNotExists() {
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_onboarding_otp_role_user ON onboarding_otp (role, user_id)");
    }
}
