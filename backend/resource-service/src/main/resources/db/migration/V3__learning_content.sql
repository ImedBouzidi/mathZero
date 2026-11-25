-- Each approved resource gets a generated, animated mini-lesson and quiz.
-- Steps and questions are stored as JSON text so the content can evolve without
-- changing the schema again. Correct answers stay server-side.
CREATE TABLE learning_contents (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL UNIQUE REFERENCES external_resources(id) ON DELETE CASCADE,
    cycle VARCHAR(20) NOT NULL,
    language VARCHAR(10) NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    steps_json TEXT NOT NULL,
    exercises_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_learning_contents_cycle ON learning_contents(cycle);
