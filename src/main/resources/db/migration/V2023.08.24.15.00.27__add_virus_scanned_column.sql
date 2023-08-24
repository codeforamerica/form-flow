-- Add the 'virus_scanned' column to the 'user_files' table
ALTER TABLE user_files ADD COLUMN virus_scanned BOOLEAN DEFAULT FALSE;

-- Set the 'virus_scanned' value to false for existing rows
UPDATE user_files SET virus_scanned = FALSE;