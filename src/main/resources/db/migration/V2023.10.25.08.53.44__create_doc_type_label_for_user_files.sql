ALTER TABLE user_files
    ADD COLUMN doc_type_label VARCHAR(255) DEFAULT '${user_file_doc_type_default_label}';