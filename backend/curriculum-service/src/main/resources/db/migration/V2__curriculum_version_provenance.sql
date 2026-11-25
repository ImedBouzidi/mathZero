ALTER TABLE curriculum_versions
    ADD COLUMN source_reference VARCHAR(512),
    ADD COLUMN reviewer VARCHAR(180);
