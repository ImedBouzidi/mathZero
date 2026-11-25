CREATE TABLE external_resources (
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    url VARCHAR(2048) NOT NULL,
    canonical_url VARCHAR(2048),
    embed_url VARCHAR(2048),
    language VARCHAR(10) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    grade_code VARCHAR(80),
    subject_code VARCHAR(80),
    topic VARCHAR(255),
    licence_code VARCHAR(80) NOT NULL,
    licence_name VARCHAR(180) NOT NULL,
    licence_url VARCHAR(2048) NOT NULL,
    attribution_text TEXT NOT NULL,
    attribution_url VARCHAR(2048),
    retrieved_at TIMESTAMP WITH TIME ZONE,
    verified_at TIMESTAMP WITH TIME ZONE,
    review_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (review_status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    review_notes TEXT,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_resource_provider_external UNIQUE (provider, external_id)
);
CREATE INDEX idx_external_resources_status ON external_resources(review_status, created_at);
CREATE INDEX idx_external_resources_provider ON external_resources(provider, external_id);
CREATE INDEX idx_external_resources_taxonomy
    ON external_resources(language, grade_code, subject_code);

CREATE TABLE resource_curriculum_mappings (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES external_resources(id) ON DELETE CASCADE,
    curriculum_version_id UUID,
    grade_code VARCHAR(80),
    subject_code VARCHAR(80),
    chapter_code VARCHAR(80),
    skill_code VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_resource_mappings_resource ON resource_curriculum_mappings(resource_id);

CREATE TABLE resource_reviews (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES external_resources(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL,
    decision VARCHAR(20) NOT NULL
        CHECK (decision IN ('SUBMIT', 'APPROVE', 'REJECT')),
    notes TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_resource_reviews_resource ON resource_reviews(resource_id, reviewed_at);

CREATE TABLE provider_cache (
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    cache_key VARCHAR(512) NOT NULL,
    payload TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_provider_cache_key UNIQUE (provider, cache_key)
);
CREATE INDEX idx_provider_cache_expiry ON provider_cache(expires_at);

CREATE TABLE provider_sync_logs (
    id UUID PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    operation VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('SUCCESS', 'FAILURE', 'SKIPPED')),
    message TEXT,
    item_count INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_provider_sync_logs_provider
    ON provider_sync_logs(provider, occurred_at);
