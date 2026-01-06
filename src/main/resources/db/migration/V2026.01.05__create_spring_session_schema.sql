-- This, along with the disabling Spring's autocreation in application.yaml, will
-- mean that Flyway is used to create the appropriate tables, indices, and triggers
-- for Spring to properly store session data

-- 1. SPRING_SESSION table
CREATE TABLE IF NOT EXISTS SPRING_SESSION
(
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT   NOT NULL,
    LAST_ACCESS_TIME      BIGINT   NOT NULL,
    MAX_INACTIVE_INTERVAL INT      NOT NULL,
    EXPIRY_TIME           BIGINT   NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (PRIMARY_ID)
);

-- 2. Indexes on SPRING_SESSION
CREATE UNIQUE INDEX IF NOT EXISTS spring_session_ix1
    ON SPRING_SESSION (SESSION_ID);

CREATE INDEX IF NOT EXISTS spring_session_ix2
    ON SPRING_SESSION (EXPIRY_TIME);

CREATE INDEX IF NOT EXISTS spring_session_ix3
    ON SPRING_SESSION (PRINCIPAL_NAME);

-- 3. SPRING_SESSION_ATTRIBUTES table
CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk
        PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
);

-- 4. Foreign key (add only if missing)
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM information_schema.table_constraints
                       WHERE constraint_name = 'spring_session_attributes_fk'
                         AND table_name = 'spring_session_attributes') THEN
            ALTER TABLE SPRING_SESSION_ATTRIBUTES
                ADD CONSTRAINT spring_session_attributes_fk
                    FOREIGN KEY (SESSION_PRIMARY_ID)
                        REFERENCES SPRING_SESSION (PRIMARY_ID)
                        ON DELETE CASCADE;
        END IF;
    END
$$;
