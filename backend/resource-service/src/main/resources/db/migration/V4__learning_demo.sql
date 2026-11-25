-- Replayable worked demonstrations are stored alongside the generated lesson.
-- Nullable so lessons generated before this migration can backfill on first read.
ALTER TABLE learning_contents
    ADD COLUMN demo_json TEXT;
