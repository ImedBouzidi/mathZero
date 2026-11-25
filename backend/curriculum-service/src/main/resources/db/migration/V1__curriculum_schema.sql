CREATE TABLE curricula (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name_fr VARCHAR(180) NOT NULL,
    name_ar VARCHAR(180) NOT NULL
);

CREATE TABLE curriculum_versions (
    id UUID PRIMARY KEY,
    curriculum_id UUID NOT NULL REFERENCES curricula(id),
    version_code VARCHAR(80) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT uq_curriculum_version_code UNIQUE (curriculum_id, version_code),
    CONSTRAINT ck_curriculum_version_dates
        CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX idx_curriculum_versions_effective
    ON curriculum_versions(curriculum_id, status, effective_from, effective_to);

CREATE TABLE school_levels (
    id UUID PRIMARY KEY,
    curriculum_version_id UUID NOT NULL REFERENCES curriculum_versions(id),
    code VARCHAR(80) NOT NULL,
    name_fr VARCHAR(180) NOT NULL,
    name_ar VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    CONSTRAINT uq_school_level_code UNIQUE (curriculum_version_id, code)
);

CREATE TABLE grades (
    id UUID PRIMARY KEY,
    school_level_id UUID NOT NULL REFERENCES school_levels(id),
    code VARCHAR(80) NOT NULL,
    name_fr VARCHAR(180) NOT NULL,
    name_ar VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    CONSTRAINT uq_grade_code UNIQUE (school_level_id, code)
);

CREATE TABLE specializations (
    id UUID PRIMARY KEY,
    curriculum_version_id UUID NOT NULL REFERENCES curriculum_versions(id),
    code VARCHAR(80) NOT NULL,
    name_fr VARCHAR(180) NOT NULL,
    name_ar VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    CONSTRAINT uq_specialization_code UNIQUE (curriculum_version_id, code)
);

CREATE TABLE grade_specializations (
    grade_id UUID NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    specialization_id UUID NOT NULL REFERENCES specializations(id) ON DELETE CASCADE,
    PRIMARY KEY (grade_id, specialization_id)
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY,
    curriculum_version_id UUID NOT NULL REFERENCES curriculum_versions(id),
    code VARCHAR(80) NOT NULL,
    name_fr VARCHAR(180) NOT NULL,
    name_ar VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    CONSTRAINT uq_subject_code UNIQUE (curriculum_version_id, code)
);

CREATE TABLE grade_subjects (
    grade_id UUID NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    PRIMARY KEY (grade_id, subject_id)
);

CREATE TABLE chapters (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subjects(id),
    code VARCHAR(80) NOT NULL,
    title_fr VARCHAR(180) NOT NULL,
    title_ar VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    CONSTRAINT uq_chapter_code UNIQUE (subject_id, code)
);

CREATE TABLE skills (
    id UUID PRIMARY KEY,
    curriculum_version_id UUID NOT NULL REFERENCES curriculum_versions(id),
    code VARCHAR(80) NOT NULL,
    title_fr VARCHAR(240) NOT NULL,
    title_ar VARCHAR(240) NOT NULL,
    objective_fr TEXT NOT NULL,
    objective_ar TEXT NOT NULL,
    CONSTRAINT uq_skill_code UNIQUE (curriculum_version_id, code)
);

CREATE TABLE chapter_skills (
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL CHECK (display_order >= 0),
    PRIMARY KEY (chapter_id, skill_id),
    CONSTRAINT uq_chapter_skill_order UNIQUE (chapter_id, display_order)
);

CREATE TABLE skill_prerequisites (
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    prerequisite_skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (skill_id, prerequisite_skill_id),
    CONSTRAINT ck_skill_prerequisite_not_self CHECK (skill_id <> prerequisite_skill_id)
);

CREATE TABLE curriculum_audit_events (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_curriculum_audit_entity
    ON curriculum_audit_events(entity_type, entity_id, occurred_at);
