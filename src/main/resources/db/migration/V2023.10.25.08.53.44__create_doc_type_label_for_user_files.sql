ALTER TABLE user_files
    ADD COLUMN doc_type_label VARCHAR(255) NOT NULL DEFAULT '${user_file_doc_type_default}';