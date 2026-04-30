-- V12: make email required and unique for email-based auth
-- Backfill null/empty emails for existing users
UPDATE app_user SET email = CONCAT(username, '@legacy.user')
WHERE email IS NULL OR TRIM(email) = '';

-- Add constraints
ALTER TABLE app_user ALTER COLUMN email SET NOT NULL;
ALTER TABLE app_user ADD CONSTRAINT uk_app_user_email UNIQUE (email);
