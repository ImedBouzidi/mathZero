-- Each curated resource belongs to one experience cycle so the public pages
-- and the three dashboards can show only the material for their level.
-- Nullable for legacy rows: a resource without a cycle is not shown publicly.
ALTER TABLE external_resources
    ADD COLUMN cycle VARCHAR(20);

ALTER TABLE external_resources
    ADD CONSTRAINT ck_external_resources_cycle
        CHECK (cycle IS NULL OR cycle IN ('PRIMARY', 'BASIC', 'SECONDARY'));

CREATE INDEX idx_external_resources_cycle
    ON external_resources(review_status, cycle);
