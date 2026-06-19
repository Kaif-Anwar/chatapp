-- Message file/image attachments

ALTER TABLE messages ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(512) NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS attachment_name VARCHAR(255) NULL;
