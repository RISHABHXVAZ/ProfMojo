-- Deduplicate existing notification_key rows in notifications, keeping the row with the lowest id
DELETE FROM notifications
WHERE id NOT IN (
    SELECT MIN(id)
    FROM notifications
    WHERE notification_key IS NOT NULL
    GROUP BY notification_key
) AND notification_key IS NOT NULL;

-- Create unique index on notification_key (allowing multiple nulls by default in PostgreSQL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_notification_key
    ON notifications (notification_key);
