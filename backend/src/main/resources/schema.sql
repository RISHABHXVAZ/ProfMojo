-- Phase 2 Database Optimization: 5 Confirmed Production Indexes

-- 1. Amenity request FIFO staff assignment queue
CREATE INDEX IF NOT EXISTS idx_amenity_fifo_queue
    ON amenity_request (department, status, created_at ASC);

-- 2. Active SLA monitor partial index
CREATE INDEX IF NOT EXISTS idx_amenity_sla_active
    ON amenity_request (sla_deadline)
    WHERE status = 'ASSIGNED' AND sla_breached = false;

-- 3. Notification feed ordered by created_at DESC
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications (recipient_id, created_at DESC);

-- 4. Student enrollment lookup
CREATE INDEX IF NOT EXISTS idx_enrollment_student
    ON class_enrollment (student_reg_no);

-- 5. Amenity request items collection table
CREATE INDEX IF NOT EXISTS idx_amenity_items_req_id
    ON amenity_request_items (amenity_request_id);

-- Phase 3 Database Hardening: Onboarding OTP composite table & uniqueness constraint
CREATE TABLE IF NOT EXISTS onboarding_otp (
    role VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN',
    user_id VARCHAR(255) NOT NULL,
    otp VARCHAR(255) NOT NULL,
    expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_onboarding_otp PRIMARY KEY (role, user_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_onboarding_otp_role_user
    ON onboarding_otp (role, user_id);
