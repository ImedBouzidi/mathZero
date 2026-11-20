-- Development-only bootstrap for the database-per-service layout.
--
-- This script runs once, when the PostgreSQL volume is created. Each future
-- service owns exactly one of these databases; sharing the PostgreSQL server
-- does not grant access to another service's tables.
--
-- Per-service roles and least-privilege credentials are introduced together
-- with each service migration (see docs/architecture/service-catalog.md).

CREATE DATABASE identity_db;
CREATE DATABASE curriculum_db;
CREATE DATABASE content_db;
CREATE DATABASE resource_db;
CREATE DATABASE exercise_db;
CREATE DATABASE learning_db;
CREATE DATABASE gamification_db;
CREATE DATABASE analytics_db;
