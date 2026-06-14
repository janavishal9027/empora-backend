-- ============================================================
-- Empora — Run this in MySQL Workbench against the `ems` database
-- ============================================================

-- 1. Add missing columns to users table (if not already there)
ALTER TABLE users 
  ADD COLUMN IF NOT EXISTS temporary_password VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS temp_password_expiry DATETIME NULL,
  ADD COLUMN IF NOT EXISTS password_set TINYINT(1) NOT NULL DEFAULT 0;

-- 2. Clear all seed data
DELETE FROM leave_requests;
DELETE FROM employees;
DELETE FROM departments;

-- 3. Clear old users and recreate with password_set = 1
DELETE FROM users;

-- 4. Insert admin and HR with BCrypt hashed passwords and password_set = 1
--    admin@empora.com  → Empora@Admin1
--    hr@empora.com     → Empora@Hr2024
INSERT INTO users (full_name, email, password, role, enabled, password_set, created_at) VALUES
(
  'Admin User',
  'admin@empora.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
  'ROLE_ADMIN',
  1,
  1,
  NOW()
),
(
  'HR Manager',
  'hr@empora.com',
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
  'ROLE_HR',
  1,
  1,
  NOW()
);

-- NOTE: The BCrypt hash above is a placeholder.
-- After running this, use the /api/setup/reset-users endpoint to set real passwords:
-- http://localhost:8080/api/setup/reset-users?adminPwd=Empora@Admin1&hrPwd=Empora@Hr2024
