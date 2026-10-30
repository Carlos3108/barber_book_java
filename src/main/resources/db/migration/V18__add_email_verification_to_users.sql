ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS email_verification_code_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS email_verification_sent_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS email_verification_attempts INTEGER NOT NULL DEFAULT 0;

-- Contas anteriores a esta funcionalidade continuam com acesso
UPDATE users SET email_verified_at = CURRENT_TIMESTAMP WHERE email_verified_at IS NULL;
