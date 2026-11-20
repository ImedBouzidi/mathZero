# Mathematics Zero to Hero

> **Project blueprint and technical specification**\
> A bilingual, gamified mathematics learning platform for the Tunisian
> school system, from primary school to Baccalaureate.

## 1. Product Overview

### 1.1 Vision

**Mathematics Zero to Hero** helps learners build mathematical
understanding progressively, from foundational arithmetic to advanced
secondary-school topics. The experience combines:

-   A guided, game-like learning path inspired by language-learning
    apps.
-   Original lessons aligned with the Tunisian curriculum.
-   Interactive mathematical visualizations inspired by dynamic geometry
    and graphing tools.
-   Exercises with immediate, meaningful feedback.
-   Progress tracking, mastery indicators, XP, achievements, and
    streaks.
-   French and Arabic interfaces, including proper right-to-left (RTL)
    support.

The product should feel like a premium educational platform while
remaining approachable and playful. Mathematical correctness, clarity,
accessibility, and curriculum alignment take priority over decorative
gamification.

### 1.2 Product principles

1.  **Tunisian curriculum first:** the Tunisian curriculum is the source
    of truth.
2.  **Original educational content:** lessons and exercises are authored
    for this platform.
3.  **Visual understanding:** explain abstract ideas through manipulable
    graphs, diagrams, and animations.
4.  **Progressive learning:** prerequisites and foundational skills
    guide the learning sequence.
5.  **Bilingual by design:** French and Arabic are first-class
    languages, not afterthoughts.
6.  **Server-authoritative learning records:** the backend validates
    answers, XP, mastery, and achievements.
7.  **Privacy by default:** collect only what is needed, especially
    because learners may be minors.
8.  **Incremental microservices:** keep clear bounded contexts and deploy only the services needed for the current milestone.

### 1.3 Target audience

-   Primary-school learners.
-   Preparatory/basic-education learners.
-   Secondary-school learners.
-   Baccalaureate learners across relevant specializations.
-   Later extensions may support teachers, parents, and independent
    adult learners.

### 1.4 Languages and localization

-   Initial languages: French (`fr`) and Arabic (`ar`).
-   Support RTL layout for Arabic and LTR layout for French.
-   Mathematical expressions, equations, graphs, coordinates, and
    symbolic input should remain consistently LTR where appropriate,
    even on Arabic pages.
-   Use a font stack that supports Latin and Arabic glyphs, such as
    Inter for Latin and Noto Sans Arabic for Arabic.
-   Store translated content separately from structural lesson data.

### 1.6 External pedagogical resource strategy

Use official, documented provider interfaces only. Treat provider access and content reuse as separate questions: an API may permit metadata discovery without granting rights to copy, translate, redistribute or commercially reuse the underlying material.

| Source | Intended use | Integration rule |
|---|---|---|
| CNTE / Edusoutien (Tunisia) | Highest-priority local alignment and official supplementary resources | No public developer API is assumed. Contact CNTE for documented API, export, partnership and reuse terms; do not scrape restricted content. |
| Wikimedia APIs | Supplementary definitions, reference material and openly licensed media | Public APIs can be used without a general API token; send a descriptive User-Agent and retain item-level licence/attribution. |
| YouTube Data API v3 | Search and retrieve video metadata; link/embed through official YouTube mechanisms | Requires a Google Cloud API key. Keep it server-side, monitor quota and follow YouTube API/embedding terms. |
| OpenStax | Supplementary textbook references and exercises where appropriate | Check the exact item licence. Standard CC BY-NC-SA material has non-commercial and share-alike conditions; obtain permission or exclude it if incompatible with commercial use. |
| Khan Academy | Optional manually curated links | Do not build on the historical deprecated/removed API; use only a currently documented integration if one becomes available. |

Implementation priority: manual catalogue and editorial curation, then Wikimedia, then YouTube, then authorized CNTE/Edusoutien access, with OpenStax subject to licence review. Start with a small reviewed collection for one or two curriculum topics rather than indexing the whole internet.

### 1.5 Curriculum strategy

Use a **hybrid curriculum approach**:

-   The Tunisian curriculum is authoritative for levels, grades,
    specializations, subjects, chapters, and learning objectives.
-   Carefully mapped French-language resources may enrich explanations
    or provide supplementary practice.
-   French resources must not silently replace Tunisian curriculum
    requirements.
-   Every external resource must record its source, licence/usage
    conditions, language, target level, and review status.

------------------------------------------------------------------------

## 2. MVP Scope and Product Roadmap

### 2.1 MVP objectives

The first release should prove one complete learning journey:

1.  A learner creates an account and signs in.
2.  The learner selects their school level, grade, and specialization
    where applicable.
3.  The learner browses the Tunisian curriculum.
4.  The learner opens an original bilingual lesson.
5.  The learner manipulates an interactive mathematical visualization.
6.  The learner answers exercises.
7.  The server validates answers and records attempts.
8.  The learner receives feedback and earns XP.
9.  The learner sees progress and mastery information.

### 2.2 MVP features

-   Registration, login, logout, and profile.
-   Integrated authentication (no Keycloak requirement for the initial
    release).
-   Curriculum explorer.
-   Grade, specialization, subject, chapter, and skill navigation.
-   Bilingual lesson presentation.
-   Lesson activities and interactive math visualizations.
-   Multiple-choice and numeric-answer exercises initially.
-   Server-side answer validation.
-   Exercise attempt history.
-   Lesson completion and basic learning progress.
-   XP transactions and a small initial set of achievements.
-   Learner dashboard.
-   Admin content creation and publication workflow, at least in a basic
    form.
-   A curated external-resource catalogue with manual registration and review; automated provider integrations can follow incrementally.
-   Responsive French/Arabic UI.

### 2.3 Defer until after MVP

-   Payments and subscriptions.
-   AI tutor and AI-generated exercises.
-   Teacher classrooms and parent dashboards.
-   Public leaderboards and social features.
-   Real-time competitions.
-   Dedicated symbolic mathematics microservice.
-   Flutter mobile client (the API should be designed to support it
    later).
-   Advanced adaptive learning models.

Do not make the MVP dependent on paid AI API tokens. AI integration
should be optional and provider-agnostic when introduced.

------------------------------------------------------------------------

## 3. Recommended Technical Architecture

### 3.1 Architecture decision

Use an **Angular frontend with Spring Boot microservices**.

The target architecture is microservices, but implementation must be incremental. Do not create every service, broker, registry, and deployment platform in the first sprint. Start with the platform foundation and identity/curriculum capabilities, then add services in dependency order. Keep each service small and independently testable.

Core principles:

-   Each microservice owns its business logic and data.
-   Services expose versioned REST APIs through a single API Gateway.
-   A service must not read or write another service's database.
-   Use synchronous REST for queries and immediate commands where a
    response is required.
-   Use asynchronous domain events for cross-service side effects and
    long-running workflows.
-   Keep the Angular application independent of internal service URLs;
    it calls only the API Gateway.
-   Keep mathematical evaluation deterministic and testable. Do not make
    the math engine dependent on an AI provider.
-   Avoid Kubernetes until Docker Compose deployment and service
    operations are stable.

### 3.2 Technology stack

  ----------------------------------------------------------------------------
  Layer                   Technology                   Purpose
  ----------------------- ---------------------------- -----------------------
  Frontend                Angular, TypeScript          Main web application

  Angular architecture    Standalone components,       Modular frontend
                          lazy-loaded feature routes   

  UI                      Angular Material or          Accessible components
                          PrimeNG + Tailwind CSS       and custom styling

  State                   Angular Signals for local    Predictable state
                          state; NgRx                  
                          SignalStore/ComponentStore   
                          only where complexity        
                          warrants it                  

  API                     Angular HttpClient, RxJS     HTTP and asynchronous
                                                       flows

  Localization            Transloco or ngx-translate   French/Arabic
                                                       translations

  Formula display         KaTeX                        Mathematical notation

  Formula input           MathLive                     Learner mathematical
                                                       input

  Graphing                JSXGraph                     Interactive graphs and
                                                       constructions

  Numeric utilities       Math.js with a restricted    Safe numeric
                          parser                       calculations

  Backend                 Java 21 + Spring Boot        Business microservices

  Service APIs            Spring MVC, Spring           REST and persistence
                          Validation, Spring Data JPA  

  Gateway                 Spring Cloud Gateway         Routing, cross-cutting
                                                       API policies

  Service discovery       Netflix Eureka / Spring      Service registration
                          Cloud Netflix                and discovery

  Configuration           Spring Cloud Config Server,  Externalized service
                          introduced when needed       configuration

  Security                Spring Security, OAuth2      Authentication and
                          Resource Server support      authorization

  Service communication   OpenFeign or WebClient for   Inter-service
                          REST; RabbitMQ for domain    communication
                          events                       

  Database                PostgreSQL                   Service-owned
                                                       relational data

  Migrations              Flyway                       Versioned schema per
                                                       service

  Resilience              Resilience4j                 Timeouts, retries,
                                                       circuit breakers

  Observability           Actuator, Micrometer,        Health and metrics
                          Prometheus, Grafana          

  API documentation       Springdoc OpenAPI            API contracts

  Testing                 JUnit 5, Mockito,            Automated quality
                          Testcontainers, Angular      
                          testing tools, Playwright    

  Containers              Docker + Docker Compose      Local and early
                                                       production deployment

  CI/CD                   GitHub Actions               Build, test, image
                                                       publishing, deployment

  Orchestration           Docker Compose first;        Runtime operations
                          Kubernetes only at a later   
                          scale milestone              
  ----------------------------------------------------------------------------

Use compatible, supported releases when creating the repository. Pin
versions in build files and update them deliberately. Do not copy old
tutorial versions without checking compatibility across Spring Boot and
Spring Cloud release trains.

### 3.3 Target microservice architecture

``` text
                         Learner / Admin Browser
                                  |
                                  v
                       Angular Web Application
                                  |
                           HTTPS /api/v1
                                  |
                                  v
                         Spring Cloud Gateway
                                  |
              +-------------------+-------------------+
              |                   |                   |
              v                   v                   v
       Identity Service    Curriculum Service    Content Service
              |                   |                   |
         identity_db         curriculum_db          content_db
                                  |
                                  v
                           Resource Service
                             resource_db
              |
              +-------------------+-------------------+
                                  |
                   +--------------+--------------+
                   |              |              |
                   v              v              v
             Exercise Service  Learning Service  Math Service
                   |              |              |
               exercise_db     learning_db       math_db
                   |              |
                   +------+-------+
                          |
                          v
                   Gamification Service
                          |
                     gamification_db

             Analytics Service (read models / projections)
                          |
                      analytics_db

  Resource Service -> Wikimedia / YouTube / authorized CNTE access
          |
      resource_db

     Eureka discovery (service registration)
     Config Server (centralized configuration, optional in first phase)
     RabbitMQ (domain events and asynchronous workflows)
     PostgreSQL (separate database ownership per service)
     Prometheus + Grafana + centralized logs (observability)
```

The diagram is logical: the services can share a PostgreSQL server
during early development, but each service must have its own database
and credentials. Sharing a database server is not the same as sharing
tables or schemas.

### 3.4 Service responsibilities and ownership

  ------------------------------------------------------------------------
  Service                  Responsibilities        Owns data
  ------------------------ ----------------------- -----------------------
  `gateway-service`        Routing, CORS, request  No business database
                           correlation, rate       
                           limiting policies,      
                           token relay/validation  
                           where appropriate       

  `identity-service`       Registration, login,    `identity_db`
                           password lifecycle,     
                           refresh sessions,       
                           roles, profile basics   

  `curriculum-service`     Curriculum versions,    `curriculum_db`
                           school levels, grades,  
                           specializations,        
                           subjects, chapters,     
                           skills, prerequisites   

  `content-service`        Original lessons,        `content_db`
                           translations, activities,
                           editorial workflow,     
                           publication versions    

  `resource-service`       External resource       `resource_db`
                           discovery, provider      
                           adapters, normalized    
                           metadata, caching,      
                           licensing, curriculum   
                           mapping, editorial      
                           review and approval    

  `math-service`           Safe expression         `math_db` only if
                           parsing/evaluation,     persistence is needed;
                           supported math          stateless initially
                           operations, validation  
                           of math activity        
                           configuration           

  `exercise-service`       Exercise definitions,   `exercise_db`
                           answer specifications,  
                           attempt submission and  
                           validation              

  `learning-service`       Learning paths, lesson  `learning_db`
                           progress, mastery,      
                           revision schedule,      
                           recommendations         

  `gamification-service`   XP ledger, levels,      `gamification_db`
                           achievements, streaks,  
                           challenges              

  `analytics-service`      Read-optimized learner  `analytics_db`
                           and admin reports,      
                           event projections       
  ------------------------------------------------------------------------

Do not split services merely to match the diagram. A service should have
a clear bounded context, independent data ownership, and a reason to be
independently deployed. The `math-service` can remain stateless and be
deployed with the exercise service initially if operating it separately
adds no value.

### 3.5 Inter-service communication

**Synchronous REST** - Use for operations where the caller needs an
immediate result, such as retrieving a curriculum chapter or validating
a submitted answer. - Define explicit DTOs and API contracts. - Add
connection/read timeouts. - Use bounded retries only for safe/idempotent
operations. - Never retry non-idempotent commands without an idempotency
strategy.

**Asynchronous events** - Use RabbitMQ for events such as
`UserRegistered`, `LessonCompleted`, `ExerciseAttemptRecorded`,
`SkillMasteryUpdated`, and `XpAwarded`. - Events should contain
identifiers and necessary facts, not secrets or unnecessary personal
data. - Use an outbox pattern for reliable event publication. - Make
consumers idempotent because messages may be delivered more than once. -
Version event schemas.

**External provider boundary:** only `resource-service` calls Wikimedia, YouTube, CNTE/Edusoutien or other external educational providers. Other backend services consume normalized resource IDs/DTOs through its API or versioned events. The Angular application calls only the Gateway.

**Avoid distributed transactions.** Each service commits its own database transaction and publishes events reliably. Use eventual consistency for cross-service projections and workflows.

### 3.6 Authentication and API security model

For the first implementation, create an isolated `identity-service`
using Spring Security. It issues short-lived JWT access tokens and
rotates opaque refresh tokens. Other services validate JWTs as resource
servers using a shared public key/JWK endpoint. The Angular app
communicates with the Gateway, not directly with internal services.

A future Keycloak/OIDC migration should remain possible by keeping token
validation standards-based and avoiding custom identity assumptions in
business services.

### 3.7 Repository layout

Use a monorepo initially so shared contracts, local infrastructure, and
coordinated changes are straightforward.

``` text
math-zero-to-hero/
├── frontend/
│   └── mathzero-angular/
├── backend/
│   ├── gateway-service/
│   ├── identity-service/
│   ├── curriculum-service/
│   ├── content-service/
│   ├── resource-service/
│   ├── math-service/
│   ├── exercise-service/
│   ├── learning-service/
│   ├── gamification-service/
│   └── analytics-service/
├── contracts/
│   ├── openapi/
│   └── events/
├── infra/
│   ├── docker/
│   ├── compose/
│   ├── monitoring/
│   └── scripts/
├── docs/
│   ├── product/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   ├── curriculum/
│   ├── implementation-phases/
│   └── decisions/
├── .github/
│   └── workflows/
├── .env.example
├── .gitignore
└── README.md
```

Do not create empty services just to fill this structure. Add each
service when its implementation phase begins.

### 3.8 Local development ports

Reserve and document ports centrally. Example:

  Component                 Port
  ---------------------- -------
  Angular dev server        4200
  API Gateway               8080
  Eureka                    8761
  Config Server             8888
  Identity Service          8081
  Curriculum Service        8082
  Content Service           8083
  Exercise Service          8084
  Resource Service          8089
  Learning Service          8085
  Gamification Service      8086
  Math Service              8087
  Analytics Service         8088
  RabbitMQ management      15672
  PostgreSQL                5432
  Prometheus                9090
  Grafana                   3000

Change ports if they conflict with existing applications, and keep the
mapping in `.env.example` and the architecture documentation.

## 4. Authentication, Authorization, and Security

### 4.1 Authentication approach

Implement authentication inside the Spring Boot application for the MVP.

Recommended flow:

-   Registration with validated email and password.
-   Password hashing with BCrypt or another Spring Security-supported
    adaptive password encoder.
-   Short-lived access token.
-   Rotating refresh token stored as a hash server-side.
-   Refresh token delivered using a secure, HttpOnly cookie.
-   Logout revokes the refresh token/session.
-   Password reset and email verification tokens are single-use,
    expiring, and stored securely.
-   Detect refresh-token reuse and revoke the affected token
    family/session.

Choose cookie and CSRF settings deliberately. If refresh tokens are
cookie-based, implement CSRF protection for state-changing requests and
configure `Secure`, `HttpOnly`, and an appropriate `SameSite` policy in
production.

### 4.2 Authorization

Initial roles:

-   `STUDENT`
-   `CONTENT_EDITOR`
-   `REVIEWER`
-   `ADMIN`

Enforce permissions on the server. Never rely on hidden frontend buttons
as an authorization mechanism.

### 4.3 Security requirements

-   Validate all input on the server.
-   Apply rate limits to login, registration, password reset, and answer
    submission.
-   Check ownership before returning learner-specific records.
-   Prevent one learner from reading or modifying another learner's
    progress.
-   Keep secrets out of source control.
-   Use HTTPS in production.
-   Add audit events for sensitive administrative actions.
-   Avoid logging passwords, tokens, or unnecessary personal data.
-   Use secure database credentials and least-privilege database access.
-   Add dependency and container image scanning to CI.

### 4.4 Learner privacy

Because some users may be children:

-   Collect only data required for learning and account operation.
-   Avoid collecting precise location or unnecessary date-of-birth
    details.
-   Keep learner progress private by default.
-   Do not introduce unrestricted public chat in the MVP.
-   Design account deletion and data export processes.
-   Review applicable Tunisian privacy and data-protection obligations
    before launch.
-   Obtain appropriate parental consent where legally required.

------------------------------------------------------------------------

## 5. Database Design

### 5.1 Database conventions

-   Database: `mathzero_db`.
-   Primary keys: UUID.
-   Timestamps: `TIMESTAMPTZ`.
-   Use foreign keys and appropriate unique constraints.
-   Add indexes for common filters and joins.
-   Use Flyway migrations for every schema change.
-   Use `JSONB` only for genuinely flexible activity or exercise
    configuration, not as a substitute for relational modeling.
-   Store attempts and XP transactions as append-only records where
    practical.
-   Use idempotency keys or unique constraints to prevent duplicate XP
    awards.

### 5.2 Identity tables

  ---------------------------------------------------------------------
  Table                              Main fields / purpose
  ---------------------------------- ----------------------------------
  `users`                            `id`, `email`, `password_hash`,
                                     `status`, `created_at`,
                                     `updated_at`

  `user_profiles`                    `user_id`, `display_name`,
                                     `preferred_language`, `avatar_key`

  `user_roles`                       `user_id`, `role`

  `refresh_tokens`                   `id`, `user_id`, `token_hash`,
                                     `expires_at`, `revoked_at`,
                                     `replaced_by_id`

  `email_verification_tokens`        `id`, `user_id`, `token_hash`,
                                     `expires_at`, `used_at`

  `password_reset_tokens`            `id`, `user_id`, `token_hash`,
                                     `expires_at`, `used_at`

  `user_sessions`                    `id`, `user_id`, `created_at`,
                                     `last_seen_at`, `revoked_at`
  ---------------------------------------------------------------------

### 5.3 Curriculum tables

  ---------------------------------------------------------------------
  Table                              Main fields / purpose
  ---------------------------------- ----------------------------------
  `curricula`                        Curriculum name, country, version,
                                     effective dates

  `school_levels`                    Primary, preparatory/basic,
                                     secondary

  `grades`                           Grade name, level, sequence

  `specializations`                  Secondary-school streams

  `grade_specializations`            Grades linked to applicable
                                     specializations

  `subjects`                         Mathematics and other future
                                     subjects

  `grade_subjects`                   Subjects available for each
                                     grade/specialization

  `chapters`                         Ordered curriculum chapters

  `skills`                           Atomic learning objectives or
                                     competencies

  `chapter_skills`                   Skills associated with chapters

  `skill_prerequisites`              Directed prerequisite
                                     relationships
  ---------------------------------------------------------------------

Curriculum records should be version-aware so future curriculum changes
do not silently rewrite historical learning context.

### 5.4 Content tables

  ---------------------------------------------------------------------
  Table                              Main fields / purpose
  ---------------------------------- ----------------------------------
  `lessons`                          Chapter, slug, difficulty, status,
                                     version

  `lesson_translations`              Lesson, language, title, summary,
                                     body/content

  `activities`                       Lesson, activity type, order,
                                     configuration

  `exercises`                        Skill, type, difficulty,
                                     configuration, status

  `exercise_translations`            Exercise, language, prompt, hints

  `exercise_solutions`               Exercise, answer specification,
                                     explanation

  ---------------------------------------------------------------------

Keep translated content separate from the language-independent
structure. External provider metadata, licences, cache entries and review
records belong to `resource-service`; lessons may store resource IDs as
references but must not duplicate the provider catalogue.

### 5.5 Resource-service tables

| Table | Main fields / purpose |
|---|---|
| `external_resources` | Provider, external ID, canonical URL, title, description, type, language, status, last checked |
| `resource_licenses` | Licence identifier/name, terms URL, reuse restrictions, attribution text |
| `resource_attributions` | Required creator/source attribution and display text |
| `resource_curriculum_mappings` | Resource, curriculum identifiers, grade, subject, chapter, skill and mapping rationale |
| `resource_tags` | Search and topic tags |
| `resource_reviews` | Reviewer, decision, notes, timestamps and audit trail |
| `provider_cache` | Provider/query key, normalized response metadata, expiry |
| `provider_sync_logs` | Provider, operation, outcome, quota/error metadata and timestamp |

Use a unique constraint on `(provider, external_id)` where the provider
exposes a stable identifier. Store provider payload snapshots only when
necessary and permitted; never store API secrets in these tables.

### 5.6 Learning tables

  ---------------------------------------------------------------------
  Table                              Main fields / purpose
  ---------------------------------- ----------------------------------
  `learning_paths`                   Grade/specialization learning path

  `learning_path_items`              Ordered lessons, chapters, or
                                     assessments

  `lesson_progress`                  User, lesson, status, completion
                                     timestamps

  `exercise_attempts`                User, exercise, answer, result,
                                     duration, timestamp

  `student_mastery`                  User, skill, score, evidence
                                     count, updated timestamp

  `revision_schedule`                User, skill/exercise, due date,
                                     interval

  `chapter_assessments`              Assessment definition and
                                     configuration
  ---------------------------------------------------------------------

### 5.7 Gamification tables

  ---------------------------------------------------------------------
  Table                              Main fields / purpose
  ---------------------------------- ----------------------------------
  `xp_transactions`                  User, amount, reason, source,
                                     idempotency key, timestamp

  `achievements`                     Achievement definition and
                                     criteria

  `user_achievements`                User, achievement, unlocked
                                     timestamp

  `user_streaks`                     User, current streak, best streak,
                                     last activity date

  `user_level_progress`              User, level, XP threshold/progress
  ---------------------------------------------------------------------

### 5.8 Future premium tables

These are extension points, not MVP requirements:

-   `plans`
-   `subscriptions`
-   `feature_entitlements`
-   `payment_transactions`

Do not collect payment details directly. Use a suitable payment provider
if billing is introduced.

### 5.9 Initial SQL sketch

This is an illustrative starting point; create production schema through
Flyway migrations and add constraints/indexes as implementation
proceeds.

``` sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'fr',
    avatar_key VARCHAR(255)
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE student_mastery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id),
    mastery_score NUMERIC(5,2) NOT NULL DEFAULT 0,
    evidence_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, skill_id),
    CHECK (mastery_score >= 0 AND mastery_score <= 100),
    CHECK (evidence_count >= 0)
);
```

------------------------------------------------------------------------

## 6. REST API Specification

### 6.1 API conventions

-   Base path: `/api/v1`.
-   JSON request and response bodies.
-   Use consistent validation and error formats.
-   Use pagination for potentially large collections.
-   Use stable resource identifiers.
-   Document endpoints with OpenAPI.
-   Version breaking API changes deliberately.

Example success response:

``` json
{
  "data": {
    "id": "resource-uuid"
  },
  "meta": {
    "requestId": "request-id"
  }
}
```

Example error response:

``` json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The submitted data is invalid.",
    "details": [
      {
        "field": "email",
        "message": "Enter a valid email address."
      }
    ]
  },
  "meta": {
    "requestId": "request-id"
  }
}
```

### 6.2 Authentication endpoints

  Method   Endpoint                  Purpose
  -------- ------------------------- ---------------------------------------------
  POST     `/auth/register`          Create learner account
  POST     `/auth/login`             Authenticate
  POST     `/auth/refresh`           Rotate refresh token and issue access token
  POST     `/auth/logout`            Revoke current session
  POST     `/auth/logout-all`        Revoke all sessions
  GET      `/auth/me`                Current account and profile
  POST     `/auth/verify-email`      Verify email
  POST     `/auth/forgot-password`   Start password reset
  POST     `/auth/reset-password`    Complete password reset

### 6.3 Curriculum endpoints

  Method   Endpoint                  Purpose
  -------- ------------------------- -----------------------------------
  GET      `/curricula`              List curriculum versions
  GET      `/school-levels`          List school levels
  GET      `/grades`                 List grades
  GET      `/specializations`        List specializations
  GET      `/subjects`               List subjects for learner context
  GET      `/chapters`               List chapters by grade/subject
  GET      `/chapters/{chapterId}`   Chapter details
  GET      `/skills/{skillId}`       Skill details and prerequisites

### 6.4 Content endpoints

  Method   Endpoint                                    Purpose
  -------- ------------------------------------------- ---------------------------------------
  GET      `/lessons/{lessonId}`                       Retrieve lesson in requested language
  GET      `/lessons/{lessonId}/activities`            Retrieve lesson activities
  GET      `/lessons/{lessonId}/resources`             Retrieve approved resource IDs/references
  POST     `/admin/lessons`                            Create lesson
  PUT      `/admin/lessons/{lessonId}`                 Update draft lesson
  POST     `/admin/lessons/{lessonId}/submit-review`   Submit for review
  POST     `/admin/lessons/{lessonId}/publish`         Publish approved lesson

### 6.5 External pedagogical resource endpoints

These routes are handled by `resource-service` through the Gateway. The Angular app never calls provider APIs directly.

  Method   Endpoint                                      Purpose
  -------- --------------------------------------------- ----------------------------------------------
  GET      `/resources/search`                           Search indexed/available resources by query, grade, language and type
  GET      `/resources/{resourceId}`                      Retrieve normalized resource metadata and attribution
  GET      `/resources/recommended`                       Retrieve approved resources mapped to a lesson, chapter or skill
  GET      `/resources/providers`                         List enabled providers and supported capabilities
  POST     `/admin/resources`                             Register a resource manually or from a provider result
  POST     `/admin/resources/{resourceId}/submit-review`  Submit metadata for editorial review
  PATCH    `/admin/resources/{resourceId}/review`          Approve, reject or request changes
  POST     `/admin/resources/{resourceId}/refresh`         Refresh provider metadata where supported

Example search: `/resources/search?query=derivatives&grade=BAC&language=fr&type=VIDEO`. Search results are discovery candidates, not automatically approved learning material. Learner-facing recommendations must return only approved resources.

### 6.6 Exercise endpoints

  --------------------------------------------------------------------------------------
  Method             Endpoint                                 Purpose
  ------------------ ---------------------------------------- --------------------------
  GET                `/exercises`                             Find exercises by
                                                              skill/chapter/difficulty

  GET                `/exercises/{exerciseId}`                Retrieve exercise

  POST               `/exercises/{exerciseId}/attempts`       Submit an answer

  GET                `/exercises/{exerciseId}/attempts`       Retrieve learner attempt
                                                              history

  POST               `/assessments/{assessmentId}/attempts`   Submit assessment
  --------------------------------------------------------------------------------------

The server must determine correctness and award XP. Never trust a
`correct`, `score`, or `xp` value supplied by the browser.

### 6.7 Learning endpoints

  ---------------------------------------------------------------------------------
  Method             Endpoint                                  Purpose
  ------------------ ----------------------------------------- --------------------
  GET                `/learning/dashboard`                     Learner dashboard

  GET                `/learning/paths`                         Available learning
                                                               paths

  POST               `/learning/lessons/{lessonId}/start`      Start lesson

  POST               `/learning/lessons/{lessonId}/complete`   Complete lesson

  GET                `/learning/mastery`                       Mastery overview

  GET                `/learning/revision/due`                  Skills/exercises due
                                                               for revision

  GET                `/learning/recommendations`               Prerequisite-based
                                                               recommendations
  ---------------------------------------------------------------------------------

### 6.8 Gamification and analytics endpoints

  --------------------------------------------------------------------------
  Method                Endpoint                       Purpose
  --------------------- ------------------------------ ---------------------
  GET                   `/gamification/profile`        XP and level

  GET                   `/gamification/achievements`   Achievement catalogue
                                                       and learner status

  GET                   `/gamification/streak`         Current and best
                                                       streak

  GET                   `/analytics/overview`          Learning overview

  GET                   `/analytics/skills`            Skill-level progress

  GET                   `/analytics/weak-points`       Skills needing
                                                       revision

  GET                   `/analytics/history`           Learning activity
                                                       history
  --------------------------------------------------------------------------

### 6.9 Administration endpoints

Provide protected endpoints for:

-   Curriculum and grade management.
-   Specialization and subject management.
-   Chapter and skill management.
-   Lesson and translation editing.
-   Exercise and solution editing.
-   Review queues and publication.
-   User and role administration.

All admin endpoints require server-side authorization and audit logging.

------------------------------------------------------------------------

## 7. Frontend Application

### 7.1 Angular application structure

Use Angular with standalone components and lazy-loaded feature routes.
Keep domain features isolated and use shared UI components only for
genuinely reusable presentation.

``` text
frontend/mathzero-angular/src/
├── app/
│   ├── core/
│   │   ├── auth/
│   │   ├── guards/
│   │   ├── interceptors/
│   │   ├── api/
│   │   └── config/
│   ├── shared/
│   │   ├── components/
│   │   ├── directives/
│   │   ├── pipes/
│   │   └── models/
│   ├── features/
│   │   ├── onboarding/
│   │   ├── dashboard/
│   │   ├── curriculum/
│   │   ├── lessons/
│   │   ├── math-lab/
│   │   ├── exercises/
│   │   ├── learning/
│   │   ├── gamification/
│   │   ├── analytics/
│   │   └── admin/
│   ├── layout/
│   └── app.routes.ts
├── assets/
│   ├── i18n/
│   └── images/
├── environments/
└── styles/
```

### 7.2 Main routes

``` text
/
├── /login
├── /register
├── /onboarding
├── /dashboard
├── /curriculum
├── /curriculum/:gradeId
├── /chapters/:chapterId
├── /lessons/:lessonId
├── /math-lab
├── /practice
├── /achievements
├── /analytics
└── /admin
```

### 7.3 Angular implementation conventions

-   Prefer standalone components and feature-level lazy loading.
-   Use typed reactive forms for registration, onboarding, and admin
    editors.
-   Use Angular Signals for local component state and simple derived
    state.
-   Use RxJS for asynchronous streams and HTTP workflows.
-   Add NgRx SignalStore or ComponentStore only when cross-feature state
    complexity justifies it.
-   Keep API clients in feature-specific data-access services.
-   Centralize authentication, error handling, request correlation, and
    API base URL configuration.
-   Use route guards for navigation UX, but always enforce authorization
    in backend services.
-   Avoid putting business rules in components.
-   Use OnPush change detection where appropriate.
-   Add component tests for critical interaction states.

### 7.4 Design direction

Use a polished educational visual identity:

-   Deep navy/charcoal foundations.
-   Electric violet as a key interactive accent.
-   Emerald for progress and positive learning feedback.
-   Clear cards and strong information hierarchy.
-   Restrained, purposeful motion.
-   Avoid excessive gradients, confetti, and decorative animation.
-   Use Inter and Noto Sans Arabic (or equivalent font families with
    full glyph coverage).
-   Ensure accessible contrast and visible keyboard focus.

Responsive targets:

-   Mobile: below 640 px.
-   Tablet: 640--1024 px.
-   Desktop: above 1024 px.

### 7.5 RTL requirements

-   Set document `dir` according to the selected language.
-   Use CSS logical properties (`margin-inline`, `padding-inline`,
    etc.).
-   Mirror directional navigation elements where appropriate.
-   Keep mathematical notation and graph axes readable and consistently
    oriented.
-   Test Arabic text mixed with Latin variables, numbers, and equations.
-   Avoid using text alignment alone as a substitute for correct RTL
    layout.

## 8. Interactive Mathematics Engine

This is the product's primary differentiator and should be treated as a
core platform capability.

### 8.1 Initial libraries

  Library         Role
  --------------- ------------------------------------------------------
  JSXGraph        Graphs, coordinate systems, geometric constructions
  Math.js         Restricted expression parsing and numeric evaluation
  KaTeX           Display mathematical notation
  MathLive        Mathematical input fields
  Framer Motion   UI transitions and guided visual explanations

### 8.2 First visualization milestone: interactive parabola

Build an interactive graph for:

\[ f(x) = ax\^2 + bx + c \]

Learners should be able to:

-   Adjust sliders for `a`, `b`, and `c`.
-   See the curve update immediately.
-   View the vertex and axis of symmetry.
-   See the equation update.
-   Explore how each coefficient changes the graph.
-   Read a short explanation in French or Arabic.
-   Reset the visualization to a guided example.

The backend should store declarative visualization configuration; the
frontend should render it through trusted, predefined visualization
components.

### 8.3 Planned visualization types

1.  Number line.
2.  Fractions and fraction operations.
3.  Function curves.
4.  Tangents and derivatives.
5.  Limits and continuity intuition.
6.  Exponential and logarithmic functions.
7.  Arithmetic and geometric sequences.
8.  Geometric constructions.
9.  Complex plane.

Prioritize based on the selected grade and learning objectives rather
than implementing every visualization at once.

### 8.4 Declarative visualization configuration

Example:

``` json
{
  "type": "function-curve",
  "version": 1,
  "expression": "a*x^2 + b*x + c",
  "variables": {
    "a": { "value": 1, "min": -3, "max": 3, "step": 0.1 },
    "b": { "value": 0, "min": -5, "max": 5, "step": 0.1 },
    "c": { "value": 0, "min": -5, "max": 5, "step": 0.1 }
  },
  "viewport": {
    "xMin": -5,
    "xMax": 5,
    "yMin": -5,
    "yMax": 10
  },
  "features": {
    "showVertex": true,
    "showAxisOfSymmetry": true
  }
}
```

Validate configuration against a strict schema before rendering.

### 8.5 Mathematical safety and correctness

-   Never use JavaScript `eval()` or `new Function()` to evaluate
    learner expressions.
-   Use an allowlisted parser and supported operation/function set.
-   Validate variable names and expression complexity.
-   Handle division by zero, invalid logarithms, square roots of
    negative real values, overflow, and undefined points.
-   Respect domains and discontinuities.
-   Distinguish numerical approximations from formal mathematical proof.
-   Use tested mathematical examples and edge cases.
-   Keep answer evaluation and grading rules on the server.

------------------------------------------------------------------------

## 9. Lesson and Exercise Content Model

### 9.1 Lesson structure

A lesson should contain language-independent metadata and localized
content.

Example conceptual structure:

``` json
{
  "code": "SECONDARY_ANALYSIS_FUNCTIONS_001",
  "gradeCode": "SECONDARY_GRADE_X",
  "chapterCode": "FUNCTIONS",
  "difficulty": "BEGINNER",
  "estimatedMinutes": 15,
  "translations": {
    "fr": {
      "title": "Découvrir les fonctions",
      "summary": "Comprendre la notion de fonction.",
      "sections": []
    },
    "ar": {
      "title": "اكتشاف الدوال",
      "summary": "فهم مفهوم الدالة.",
      "sections": []
    }
  }
}
```

The grade and chapter codes above are illustrative placeholders. Use
verified Tunisian curriculum identifiers in real content.

### 9.2 Supported lesson section types

-   `EXPLANATION`
-   `FORMULA`
-   `EXAMPLE`
-   `VISUALIZATION`
-   `INTERACTIVE_ACTIVITY`
-   `SUMMARY`
-   `CHECKPOINT`

### 9.3 Initial exercise types

-   Multiple choice.
-   Numeric answer.
-   Expression answer (after safe parser and equivalence rules are
    implemented).
-   Step ordering (later).
-   Graph interaction (later).
-   Geometry construction (later).

### 9.4 Editorial workflow

``` text
DRAFT
  |
  v
IN_REVIEW
  |
  v
APPROVED
  |
  v
PUBLISHED
  |
  v
ARCHIVED
```

Requirements:

-   Keep content versions.
-   Require mathematical and editorial review.
-   Verify curriculum mapping, grade, specialization, and language
    quality.
-   Validate every exercise answer key.
-   Do not automatically publish AI-generated content.
-   Record who created, reviewed, approved, and published each item.

### 9.5 External resource registry (owned by resource-service)

Keep external resources separate from original lessons and lesson activities. `content-service` may reference approved resource IDs, but it must not own provider credentials or call external providers. For each external resource, record:

-   Title and provider.
-   URL or internal reference.
-   Language.
-   Relevant grade and topic.
-   Source and licence/usage conditions.
-   Retrieval or verification date.
-   Editorial review status.

Do not scrape or republish third-party materials without checking their terms. Store metadata and canonical links by default; copy or transform content only when the licence and permission explicitly allow it. An API response or public URL does not grant reuse rights.

------------------------------------------------------------------------

## 10. Learning Progression and Gamification

### 10.1 Exercise attempt lifecycle

``` text
Load exercise
    |
Learner submits answer
    |
Server validates answer
    |
Persist attempt
    |
Update skill mastery
    |
Schedule revision if appropriate
    |
Issue XP transaction (idempotently)
    |
Return feedback and next recommendation
```

### 10.2 Mastery model

A first implementation may use a transparent heuristic:

\[ M = 100(0.30A + 0.25D + 0.25R + 0.20C) \]

Where:

-   `A` = normalized accuracy.
-   `D` = normalized difficulty of successfully completed tasks.
-   `R` = retention across later revision attempts.
-   `C` = consistency across attempts.

All inputs are normalized to the interval `[0,1]`.

This is a starting heuristic, not a validated educational measurement
model. Calibrate it using learner data, educator feedback, and testing.
Do not present it as an official school grade.

Suggested display bands:

      Score Learner-facing description
  --------- ----------------------------
      0--39 Foundations to strengthen
     40--59 Developing
     60--79 Competent
     80--94 Mastered
    95--100 Excellent retention

### 10.3 Prerequisite recommendations

Represent skills as a directed prerequisite graph.

If a learner struggles with a skill:

1.  Identify prerequisite skills.
2.  Find the earliest relevant weak prerequisite.
3.  Recommend a short lesson or exercise set.
4.  Reassess the target skill after revision.

Avoid recommending advanced content when a necessary foundation is
missing.

### 10.4 Gamification

Initial features:

-   XP for validated learning activity.
-   Learner levels.
-   Achievements.
-   Daily learning streak.
-   Optional daily challenge.

Rules:

-   Award XP only on the server.
-   Make award operations idempotent.
-   Define clear achievement criteria.
-   Avoid punitive streak mechanics that shame learners.
-   Do not expose a learner's progress publicly by default.

------------------------------------------------------------------------

## 11. Testing and Quality Assurance

### 11.1 Backend tests

-   Unit tests for services and validators.
-   Repository tests.
-   PostgreSQL integration tests using Testcontainers.
-   Authentication and authorization tests.
-   Cross-user data isolation tests.
-   Curriculum versioning tests.
-   Exercise answer validation tests.
-   XP idempotency tests.
-   Content publication workflow tests.

### 11.2 Frontend tests

-   Vitest for unit tests.
-   React Testing Library for component behavior.
-   Playwright for end-to-end flows.
-   French and Arabic localization tests.
-   RTL/LTR layout tests.
-   Keyboard navigation and accessibility checks.
-   Visualization interaction tests.

### 11.3 Mathematical test examples

For `f(x) = x²`:

-   `f(0) = 0`.
-   `f(2) = 4`.
-   `f(-2) = 4`.
-   `f'(2) = 4` when derivative support is enabled.

For `u_n = 3 × 2^n`:

-   `u_0 = 3`.
-   `u_1 = 6`.
-   `u_2 = 12`.
-   `u_5 = 96`.

Also test:

-   Division by zero.
-   Invalid logarithm domains.
-   Invalid square-root domains for real-valued exercises.
-   Very large values and overflow.
-   Unsupported functions and malicious expressions.
-   Discontinuities and undefined graph points.
-   Decimal precision and answer tolerance.

Every published exercise must have a reviewed answer key and automated
validation tests.

------------------------------------------------------------------------

## 12. Local Development and Deployment

### 12.1 Local environment

Use Docker Compose for infrastructure and services. During early phases,
run Angular with `ng serve` and run the Spring Boot services from the
IDE or Maven/Gradle. Add services to Compose as they become ready.

Recommended local infrastructure:

-   PostgreSQL server with one database per service.
-   RabbitMQ for asynchronous events.
-   Eureka for service discovery.
-   Config Server only when centralized configuration becomes useful.
-   Prometheus and Grafana after service health/metrics endpoints exist.

### 12.2 Database-per-service setup

A single PostgreSQL container may host several logical databases during
development:

``` text
PostgreSQL server
├── identity_db
├── curriculum_db
├── content_db
├── resource_db
├── exercise_db
├── learning_db
├── gamification_db
└── analytics_db
```

Each service must have: - Its own database credentials. - Its own Flyway
migration history. - Its own JPA entities and repositories. - No direct
access to another service's tables.

For a stateless `math-service`, do not create a database unless it needs
to persist configuration or audit data.

### 12.3 Environment variables

Keep secrets out of Git. Example `.env.example`:

``` dotenv
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_ADMIN_USER=mathzero_admin
POSTGRES_ADMIN_PASSWORD=change-me-locally

GATEWAY_PORT=8080
EUREKA_PORT=8761
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

IDENTITY_DB=identity_db
CURRICULUM_DB=curriculum_db
CONTENT_DB=content_db
RESOURCE_DB=resource_db
EXERCISE_DB=exercise_db

# Provider credentials are server-side secrets; never expose them to Angular.
YOUTUBE_API_KEY=
# Wikimedia can be used without an API key; configure a descriptive User-Agent in the service.
LEARNING_DB=learning_db
GAMIFICATION_DB=gamification_db
ANALYTICS_DB=analytics_db

JWT_ISSUER=http://localhost:8081
ANGULAR_API_BASE_URL=http://localhost:8080/api/v1
```

Use separate credentials for each database in real environments. The
example values are local placeholders only.

### 12.4 Production topology

``` text
Learner browser
      |
     HTTPS
      |
Angular static hosting / CDN
      |
     HTTPS
      |
API Gateway
      |
Private service network
      |
Spring Boot microservices
      |
Service-owned PostgreSQL databases

RabbitMQ: asynchronous events
Eureka: service discovery where applicable
Prometheus/Grafana: metrics and dashboards
Centralized logs and error monitoring
```

Production requirements:

-   HTTPS everywhere.
-   Private networking for internal services and databases.
-   Managed PostgreSQL or a properly maintained PostgreSQL deployment.
-   Automated backups and tested restore procedures.
-   Secret management outside Git.
-   Health and readiness endpoints.
-   Centralized logs with correlation IDs.
-   Metrics and alerting.
-   Database connection limits and pool configuration.
-   Non-root, multi-stage container images.
-   Deployment rollback strategy.
-   Resource limits and autoscaling only where supported and justified.

Use Docker Compose for local development and small controlled
deployments. Consider Kubernetes only after the team can operate the
services, monitor them, secure them, and justify the additional
complexity.

### 12.5 CI/CD pipeline

Suggested GitHub Actions workflow:

1.  Detect changed frontend and backend projects.
2.  Run Angular lint, typecheck/build, and unit tests.
3.  Run Maven/Gradle formatting, compilation, and unit tests for changed
    services.
4.  Run Testcontainers integration tests.
5.  Validate OpenAPI and event contract compatibility.
6.  Run dependency and container security checks.
7.  Build versioned Docker images.
8.  Publish images to a registry.
9.  Deploy to staging.
10. Run smoke and end-to-end tests.
11. Promote to production after approval.

Use immutable image tags (commit SHA or release version), not only
`latest`.

### 12.6 Git workflow

-   `main`: production-ready code.
-   `dev`: integration branch.
-   `feature/<service-or-feature>-<name>`: feature branches.
-   `fix/<service-or-feature>-<name>`: bug fixes.
-   Use pull requests and required CI checks before merging.
-   Keep API and event contract changes reviewed alongside service code.

## 13. Development Backlog

The backlog is organized by implementation dependency. Complete the
acceptance criteria for a phase before expanding the scope. Split
stories further and estimate them with the actual team.

### Epic E01 --- Platform foundation

-   [ ] US-001 Create monorepo and architecture documentation.
-   [ ] US-002 Initialize Angular application with standalone routing.
-   [ ] US-003 Create shared Spring Boot project conventions.
-   [ ] US-004 Configure PostgreSQL and database-per-service setup.
-   [ ] US-005 Add Docker Compose for PostgreSQL, RabbitMQ, and Eureka.
-   [ ] US-006 Establish design system and shared Angular components.

### Epic E02 --- Gateway and identity

-   [ ] US-007 Implement Spring Cloud Gateway.
-   [ ] US-008 Implement identity-service registration and login.
-   [ ] US-009 Implement secure JWT access and rotating refresh tokens.
-   [ ] US-010 Implement profile and preferred language.
-   [ ] US-011 Implement role-based authorization.
-   [ ] US-012 Implement email verification and password reset.

### Epic E03 --- Curriculum service

-   [ ] US-013 Model versioned Tunisian curricula.
-   [ ] US-014 Manage school levels and grades.
-   [ ] US-015 Manage specializations.
-   [ ] US-016 Manage subjects and chapters.
-   [ ] US-017 Manage skills and prerequisites.
-   [ ] US-018 Build Angular curriculum explorer.

### Epic E04 --- Content service

-   [ ] US-019 Create bilingual lesson structure.
-   [ ] US-020 Display lessons in French and Arabic.
-   [ ] US-021 Build lesson authoring interface.
-   [ ] US-022 Store and render activity configurations.
-   [ ] US-023 Implement review and publication workflow.
-   [ ] US-024 Keep content-service lesson references separate from external resource metadata.

### Epic E05 --- External pedagogical resources

-   [ ] US-024A Create resource-service and resource_db with Flyway migrations.
-   [ ] US-024B Define normalized provider/resource DTOs and provider adapter interface.
-   [ ] US-024C Implement manual resource registration and admin curation first.
-   [ ] US-024D Implement Wikimedia search/metadata adapter with User-Agent and attribution.
-   [ ] US-024E Implement YouTube Data API adapter with server-side API key and quota handling.
-   [ ] US-024F Add licence, attribution, language, grade, topic and curriculum mapping metadata.
-   [ ] US-024G Implement editorial review states and learner-only approved-resource queries.
-   [ ] US-024H Add cache, timeouts, rate limits, circuit breaker and provider outage behavior.
-   [ ] US-024I Request authorized CNTE/Edusoutien API, export or partnership access; implement only after access is confirmed.
-   [ ] US-024J Evaluate OpenStax reuse licence compatibility before importing or adapting material.

### Epic E06 --- Math engine

-   [ ] US-025 Build shared graphing component.
-   [ ] US-026 Implement interactive parabola.
-   [ ] US-027 Display vertex and axis of symmetry.
-   [ ] US-028 Implement safe expression parsing and numeric evaluation.
-   [ ] US-029 Add tangent and derivative visualizations.
-   [ ] US-030 Add limits and sequence visualizations.

### Epic E07 --- Exercise service

-   [ ] US-031 Implement multiple-choice exercises.
-   [ ] US-032 Implement numeric-answer exercises.
-   [ ] US-033 Implement safe expression-answer evaluation.
-   [ ] US-034 Persist exercise attempts.
-   [ ] US-035 Return explanations and corrective feedback.
-   [ ] US-036 Implement chapter assessments.

### Epic E08 --- Learning service

-   [ ] US-037 Build learning paths.
-   [ ] US-038 Track lesson completion.
-   [ ] US-039 Calculate and persist skill mastery.
-   [ ] US-040 Recommend prerequisite revision.
-   [ ] US-041 Schedule revision activities.
-   [ ] US-042 Build learner dashboard.

### Epic E09 --- Gamification service

-   [ ] US-043 Implement XP ledger and idempotency.
-   [ ] US-044 Implement learner levels.
-   [ ] US-045 Implement achievements.
-   [ ] US-046 Implement daily streaks.
-   [ ] US-047 Add daily challenges.
-   [ ] US-048 Build achievement and progress screens.

### Epic E10 --- Analytics service

-   [ ] US-049 Build learning overview.
-   [ ] US-050 Show skill mastery reports.
-   [ ] US-051 Identify weak points.
-   [ ] US-052 Show learning history.
-   [ ] US-053 Add revision and retention indicators.

### Epic E11 --- Administration and production

-   [ ] US-054 Build admin dashboard and content permissions.
-   [ ] US-055 Manage curriculum and content through protected APIs.
-   [ ] US-056 Implement service-to-service events and outbox pattern.
-   [ ] US-057 Add observability, health checks, metrics, and logs.
-   [ ] US-058 Complete service integration and end-to-end testing.
-   [ ] US-059 Build production containers and CI/CD.
-   [ ] US-060 Document deployment, backup, restore, and incident
    procedures.

### Epic E12 --- Future capabilities

-   AI math tutor.
-   AI-assisted exercise drafting with human review.
-   Adaptive recommendations.
-   Teacher classrooms and assignments.
-   Parent progress dashboard.
-   Subscription plans and feature entitlements.
-   Flutter mobile application using the same Gateway API.
-   Realtime competitions.
-   Additional mathematical engines only when a concrete requirement
    emerges.

## 14. Suggested Implementation Phases

The project should be delivered in phases rather than starting all
microservices simultaneously. The durations are indicative and should be
adjusted to actual team capacity.

  -----------------------------------------------------------------------
  Phase                   Outcome                 Main deliverable
  ----------------------- ----------------------- -----------------------
  0                       Requirements and        Decisions, repo,
                          engineering baseline    conventions, API
                                                  standards

  1                       Platform foundation     Angular shell, Gateway,
                                                  Eureka, PostgreSQL,
                                                  Compose

  2                       Identity and onboarding Registration, login,
                                                  profile, protected
                                                  routes

  3                       Curriculum              Tunisian curriculum
                                                  service and explorer

  4                       Content and lessons     Bilingual lesson
                                                  service and admin
                                                  workflow

  4A                      External resources      Resource catalogue,
                                                  manual review, Wikimedia
                                                  and YouTube adapters

  5                       Math Lab                Interactive parabola
                                                  and safe math
                                                  evaluation

  6                       Exercises               Answer validation,
                                                  attempts, feedback

  7                       Learning and            Progress, mastery, XP,
                          gamification            achievements

  8                       Analytics and           Learner reports and
                          administration          editorial tools

  9                       Production readiness    Events, observability,
                                                  CI/CD, security,
                                                  deployment
  -----------------------------------------------------------------------

### Phase exit rule

Do not move to the next phase just because code compiles. Each phase
must have: - Working acceptance criteria. - Automated tests for critical
behavior. - API documentation updated. - Database migrations
committed. - Local run instructions verified. - No unresolved
high-severity security issue.

## 15. Detailed Implementation Steps and AI Coding Prompts

Use these prompts sequentially with Claude Code, OpenCode, Copilot, or
another coding agent. Run one phase at a time. The agent must inspect
the existing repository before editing it and must not replace working
code blindly.

### 15.1 How to use the prompts

For each phase:

1.  Create a Git branch, for example
    `feature/phase-1-platform-foundation`.
2.  Open the repository root in the coding agent.
3.  Paste the phase prompt.
4.  Ask the agent to inspect the repository and propose a file-level
    plan before making broad changes.
5.  Review the plan and approve implementation.
6.  Run the commands and tests locally.
7.  Review the diff, security implications, and generated migrations.
8.  Update the phase checklist and documentation.
9.  Commit the completed phase.
10. Only then begin the next phase.

**Important:** Do not ask the coding agent to build the entire platform
in one prompt. Require small, testable increments and explicit
acceptance checks.

### 15.2 Master project prompt

``` text
You are the lead architect and senior full-stack engineer for Mathematics Zero to Hero.

PRODUCT
Build a bilingual French/Arabic gamified mathematics learning platform aligned with the Tunisian school curriculum, from primary school through Baccalaureate. It uses original lessons, interactive mathematical visualizations, exercises, learning progression, and carefully designed gamification.

MANDATORY STACK
- Frontend: Angular + TypeScript, standalone components, lazy-loaded routes.
- UI: Angular Material or PrimeNG with Tailwind CSS where appropriate.
- Backend: Java 21 + Spring Boot microservices.
- Gateway: Spring Cloud Gateway.
- Service discovery: Eureka.
- Configuration: Spring Cloud Config Server when introduced by the implementation plan.
- Persistence: PostgreSQL, database-per-service, Flyway migrations.
- Security: Spring Security, JWT access tokens, rotating refresh tokens.
- Messaging: RabbitMQ for asynchronous domain events when required.
- Resilience: timeouts, bounded retries, circuit breakers where justified.
- Observability: Spring Actuator, Micrometer, Prometheus, Grafana.
- Math UI: KaTeX, MathLive, JSXGraph, and a restricted Math.js parser.
- Infrastructure: Docker and Docker Compose first; Kubernetes is not an MVP requirement.
- CI: GitHub Actions.

ARCHITECTURAL RULES
1. Use a microservice architecture with clear bounded contexts.
2. Each service owns its data and database credentials. No cross-service database access.
3. Angular calls only the API Gateway; never expose internal service URLs to the browser.
4. Use REST for request/response operations and RabbitMQ events for asynchronous workflows.
5. Avoid distributed transactions. Use an outbox pattern and idempotent consumers where events are used.
6. Keep services independently testable and deployable.
7. Do not create unnecessary abstractions or empty services.
8. Do not introduce Kubernetes, Kafka, Redis, or extra infrastructure without a documented requirement.
9. Use OpenAPI for REST contracts and version event schemas.
10. Do not use JavaScript eval() or new Function() for mathematical evaluation.

PRODUCT RULES
- Tunisian curriculum is the source of truth.
- French resources may enrich but not replace Tunisian curriculum mapping.
- Lessons and exercises are original and bilingual.
- Arabic must have proper RTL layout; mathematical expressions remain correctly oriented.
- The server validates answers, mastery, XP, and achievements.
- Learner data is private by default and minimized, especially for minors.
- AI-generated content must be reviewed before publication.
- The MVP must not depend on paid AI APIs.

ENGINEERING RULES
- Inspect the current repository before modifying it.
- Preserve working code and existing conventions unless a change is justified.
- Work phase by phase; do not implement future phases prematurely.
- Before coding, provide a concise plan listing files, modules, APIs, migrations, and tests.
- After coding, run relevant tests and report exact commands and results.
- Add unit, integration, and contract tests for critical behavior.
- Add validation, authorization, error handling, logging, and documentation.
- Never claim a test passed unless it was actually run.
- Do not leave placeholder TODO implementations for acceptance-critical functionality.

For the current task, implement only the phase explicitly requested by the user. Ask before making destructive changes or changing architectural decisions.
```

### 15.3 Phase 0 --- Requirements, repository, and architecture baseline

**Steps** 1. Create the Git repository and `main`/`dev` branches. 2. Add
`README.md`, `.gitignore`, `.editorconfig`, and `.env.example`. 3. Add
`docs/architecture/` and record the microservice boundaries. 4. Record
decisions: Angular, Spring Boot microservices, database ownership,
REST/event policy, identity strategy. 5. Define naming conventions, API
error format, logging format, and versioning. 6. Create a service
inventory and port map. 7. Do not generate every service yet.

**Prompt**

``` text
Implement Phase 0 only: establish the project engineering baseline.

Inspect the repository first. Create or update:
- README.md with product vision, stack, prerequisites, and local development overview.
- docs/architecture/system-overview.md with the target Angular + Spring Boot microservices architecture.
- docs/architecture/service-catalog.md with service responsibilities, owned databases, ports, and dependencies.
- docs/architecture/communication.md defining REST versus RabbitMQ event usage.
- docs/decisions/ADR-001-microservices.md and additional concise ADRs for database-per-service, Angular, and identity strategy.
- .gitignore, .editorconfig, and .env.example.

Do not create application services yet. Do not add credentials. Ensure all architectural decisions are consistent with the project specification. Show the resulting tree and explain how to verify the repository baseline.
```

**Acceptance checks** - The service catalog and architecture diagram
agree. - No secrets are committed. - The README explains how later
phases will be run. - Architecture decisions are recorded rather than
hidden in code.

### 15.4 Phase 1 --- Angular shell and backend infrastructure

**Steps** 1. Generate the Angular application under
`frontend/mathzero-angular`. 2. Configure standalone routing, strict
TypeScript, linting, formatting, and environment configuration. 3.
Create the shared design tokens and basic responsive application shell.
4. Create a Spring Boot Gateway service. 5. Add Eureka server and
register the Gateway. 6. Add PostgreSQL and RabbitMQ to Docker Compose.
7. Add health checks and documented ports. 8. Confirm Angular can call a
Gateway health endpoint.

**Prompt**

``` text
Implement Phase 1 only: platform foundation.

Frontend:
- Create an Angular application at frontend/mathzero-angular.
- Use standalone components, strict TypeScript, lazy-loaded routes, and a clean feature-first structure.
- Add a responsive app shell with header, navigation, content area, and language switch placeholder.
- Add initial design tokens and accessible shared UI primitives.
- Configure environment-based API base URL. The browser must call the Gateway only.

Backend/infrastructure:
- Create gateway-service using Spring Cloud Gateway.
- Create Eureka server and register the Gateway.
- Add Docker Compose for PostgreSQL and RabbitMQ, with health checks and persistent development volumes.
- Add Actuator health endpoints.
- Centralize ports and non-secret environment variables.

Do not implement business services or authentication yet. Pin compatible Spring Boot and Spring Cloud versions. Verify dependency compatibility. Add README instructions and smoke tests. Run Angular build and backend tests, and report exact results.
```

**Acceptance checks** - Angular runs locally. - Eureka starts and shows
the Gateway registered. - Gateway health endpoint responds. - PostgreSQL
and RabbitMQ become healthy in Compose. - No business service bypasses
the Gateway from the frontend.

### 15.5 Phase 2 --- Identity service and onboarding

**Steps** 1. Create `identity-service` with its own `identity_db`. 2.
Add Flyway migrations for users, profiles, roles, refresh sessions, and
token records. 3. Implement registration, login, refresh, logout, and
current-user endpoints. 4. Hash passwords with BCrypt or a supported
adaptive encoder. 5. Issue short-lived JWT access tokens and rotate
refresh tokens. 6. Configure JWT validation at Gateway and resource
services. 7. Add Angular login, registration, onboarding, auth state,
and route guards. 8. Test token expiry, refresh reuse, logout, and
cross-user access.

**Prompt**

``` text
Implement Phase 2 only: identity-service and Angular authentication.

Create identity-service as an independently deployable Spring Boot service with its own identity_db and Flyway migrations.

Implement:
- Registration with validation and unique normalized email.
- Login.
- Short-lived signed JWT access tokens.
- Opaque refresh tokens stored only as hashes, rotated on refresh.
- Refresh-token reuse detection and session revocation.
- Logout and logout-all.
- Current-user/profile endpoint.
- Roles STUDENT, CONTENT_EDITOR, REVIEWER, ADMIN.
- Secure password hashing and rate-limit-ready service boundaries.

Security:
- Other services must validate JWTs as resource servers.
- Configure CORS narrowly at the Gateway.
- Use secure HttpOnly refresh-token cookies and CSRF protection appropriate to the cookie strategy.
- Do not log credentials or tokens.
- Do not trust frontend role claims for authorization decisions.

Angular:
- Add typed login and registration forms.
- Add auth API client, interceptor, auth state, route guards, and onboarding route.
- Handle loading, validation, expired sessions, and API errors.
- Keep the API base URL pointed at the Gateway.

Add unit and integration tests, including refresh rotation/reuse and authorization tests. Document local setup. Do not add Keycloak or social login in this phase.
```

**Acceptance checks** - Registration and login work through Gateway. -
Refresh rotation works and reuse is detected. - Protected routes
redirect appropriately. - Backend authorization is tested independently
of frontend guards.

### 15.6 Phase 3 --- Curriculum service

**Steps** 1. Create `curriculum-service` and `curriculum_db`. 2. Model
versioned curriculum, school levels, grades, specializations, subjects,
chapters, skills, and prerequisites. 3. Add seed data only for verified
curriculum records. 4. Implement read APIs and protected admin write
APIs. 5. Add Angular curriculum explorer and grade selection. 6. Add
tests for curriculum versioning and prerequisite graph integrity.

**Prompt**

``` text
Implement Phase 3 only: Tunisian curriculum service and Angular curriculum explorer.

Create curriculum-service with its own curriculum_db and Flyway migrations.

Model:
- curricula and curriculum versions
- school_levels and grades
- specializations and grade-specialization mapping
- subjects and grade-subject mapping
- chapters with ordering
- skills/learning objectives
- chapter-skill mapping
- skill prerequisites

Requirements:
- Tunisian curriculum is the source of truth.
- Store effective dates/version metadata.
- Do not invent official curriculum content. If verified source data is not supplied, create clearly labeled development fixtures only.
- Expose paginated/filterable read endpoints through Gateway.
- Protect admin mutations with roles.
- Validate prerequisite relationships and prevent cycles.
- Publish OpenAPI documentation.

Angular:
- Build grade and specialization selection.
- Build curriculum explorer by level, grade, subject, and chapter.
- Include loading, empty, and error states.
- Support French/Arabic labels and RTL.

Add tests for version selection, relationships, authorization, and prerequisite cycle rejection. Do not implement lesson content in this phase.
```

**Acceptance checks** - Curriculum data is versioned. - The Angular
explorer loads only through Gateway. - No unverified data is presented
as official curriculum. - Admin writes are protected and audited.

### 15.7 Phase 4 --- Content service and bilingual lessons

**Steps** 1. Create `content-service` and `content_db`. 2. Model
lessons, translations, sections, activities, resources, versions, and
editorial states. 3. Implement authoring, review, approval, publish, and
archive APIs. 4. Link content to curriculum identifiers through API
contracts, not database foreign keys across services. 5. Build Angular
lesson reader and basic admin editor. 6. Add one complete original
lesson in French and Arabic.

**Prompt**

``` text
Implement Phase 4 only: content-service and bilingual lesson experience.

Create content-service with its own database and Flyway migrations.

Implement:
- Lessons with stable codes, curriculum references, difficulty, estimated time, and version.
- Separate French and Arabic translations.
- Ordered lesson sections: explanation, formula, example, visualization, interactive activity, summary, checkpoint.
- Activity configuration stored as validated JSON.
- Resource registry with provider, URL/reference, language, licence, source, and review status.
- Editorial workflow: DRAFT -> IN_REVIEW -> APPROVED -> PUBLISHED -> ARCHIVED.
- Role-based author, reviewer, and publisher permissions.
- Immutable published versions or equivalent version history.

Cross-service rule:
- Store curriculum IDs/codes as external references; never query curriculum_db directly.

Angular:
- Build bilingual lesson reader with KaTeX formula rendering.
- Build a basic protected lesson editor and preview.
- Correctly support RTL while keeping formulas readable.

Create one original sample lesson in French and Arabic, clearly marked as sample content until reviewed. Add workflow, authorization, localization, and API tests.
```

**Acceptance checks** - A learner can open a published lesson in either
language. - Draft lessons are not visible to learners. - Review and
publish permissions are enforced. - Formula rendering works in both
language directions.

### 15.7A Phase 4A/4B --- External resource service and provider integrations

**Recommended sequence**

1. Build the resource catalogue and manual admin curation before connecting providers.
2. Add Wikimedia API integration for supplementary definitions, media and reference material.
3. Add YouTube Data API integration for video metadata and official embeds.
4. Contact CNTE and Edusoutien for documented API, export or partnership access; do not assume a public API exists.
5. Evaluate OpenStax content only after checking the exact licence and whether its non-commercial/share-alike conditions fit the product's business model.

**Prompt**

``` text
Implement the external pedagogical resource capability as a dedicated resource-service. Do not move original lessons out of content-service. Inspect the existing repository and API conventions before coding.

Backend: Java 21, Spring Boot, Spring Security resource-server JWT validation, Spring Data JPA, PostgreSQL resource_db, Flyway, WebClient, Actuator, Springdoc OpenAPI and Gateway routing. Register with Eureka if service discovery is enabled in the current environment.

Responsibilities:
- Own external resource metadata, provider adapters, cache, licence/attribution records, curriculum mappings and editorial review.
- Define a ResourceProvider interface and normalized DTOs so provider-specific fields do not leak into Angular or other services.
- Start with manual resource registration and curation; then implement separate adapters for Wikimedia and YouTube.
- Wikimedia: use documented public APIs, send a descriptive User-Agent, respect rate limits, retain source URLs and per-item licence/attribution metadata.
- YouTube: use the official YouTube Data API v3 for metadata/search and the official player/embed for playback. Read the API quota rules. Keep YOUTUBE_API_KEY only in server-side environment/secret storage; never put it in Angular, Git, logs or API responses.
- CNTE/Edusoutien: create an integration placeholder and document the access request. Do not invent endpoints, scrape restricted pages or implement an undocumented API. Activate only after written authorization and technical documentation are obtained.
- OpenStax: do not bulk-copy content. Record exact licence and attribution; flag non-commercial/share-alike restrictions for legal/product review before use, especially if subscriptions may be introduced.
- Do not depend on the deprecated/removed historical Khan Academy API.

Data model should include provider, external ID, canonical URL, title, description, resource type, language, licence identifier/text, attribution, grade, subject, chapter/skill mapping, tags, last-fetched timestamp, cache expiry, review status, reviewer and audit timestamps. Use explicit states such as DISCOVERED, PENDING_REVIEW, APPROVED, REJECTED and ARCHIVED.

Expose Gateway routes for resource search, details, recommendations and protected admin registration/review. Search results from providers are candidates only. Learner-facing recommendations must include approved resources only. Store metadata and links by default; do not mirror full third-party content unless rights explicitly permit it.

Engineering requirements:
- Provider-specific timeouts, bounded retries for safe requests, rate limiting, cache and circuit breakers. Handle quota exhaustion and provider outages gracefully; manual catalogue browsing must continue to work.
- Validate all query parameters and sanitize displayed metadata. Do not execute provider-supplied HTML or scripts.
- Use role-based permissions (CONTENT_EDITOR, REVIEWER, ADMIN), audit all review decisions and prevent self-approval where practical.
- Add tests with mocked HTTP responses; tests must not require real provider keys or network access. Test malformed provider data, duplicate external IDs, cache expiry, quota errors, unavailable providers, licence metadata, review transitions and authorization.
- Publish OpenAPI documentation and add resource-service to Docker Compose, service inventory, port map, health checks and CI.

Angular: after the API contract is ready, add a resource discovery panel to lesson/chapter pages. Display provider, language, resource type, attribution/licence and review-approved status. Open external resources safely in a new tab or use official embeds where permitted. Include empty, loading, error and provider-unavailable states. Support French/Arabic labels and RTL.

Do not add AI-generated resource summaries, automatic publishing, scraping, content downloading or unapproved provider integrations in this phase. Deliver the service in small increments and report tests and any provider access limitations.
```

**Acceptance checks**
- Resource-service owns resource_db and is reachable only through Gateway from Angular.
- Manual resources can be registered, reviewed, approved and shown to learners.
- Wikimedia and YouTube adapters return normalized metadata and handle quotas/outages.
- Provider credentials remain server-side.
- Licence and attribution metadata are visible and retained.
- CNTE/Edusoutien remain inactive until authorized access is confirmed.

### 15.8 Phase 5 --- Interactive Math Lab

**Steps** 1. Create a reusable Angular math visualization host. 2.
Integrate JSXGraph, KaTeX, and MathLive. 3. Implement strict
visualization configuration schemas. 4. Implement a restricted
expression parser; never use `eval` or `new Function`. 5. Build the
interactive parabola as the first complete visualization. 6. Add
numerical and edge-case tests. 7. Decide whether `math-service` is
independently deployed now or remains a small stateless service
colocated with exercise evaluation until scaling justifies separation.

**Prompt**

``` text
Implement Phase 5 only: the interactive Math Lab, prioritizing the parabola.

Frontend:
- Build a reusable Angular visualization host.
- Integrate JSXGraph for graphing, KaTeX for formula display, and MathLive for mathematical input.
- Build a function-curve visualization for f(x) = a*x^2 + b*x + c.
- Provide accessible sliders for a, b, and c.
- Update the curve, formula, vertex, and axis of symmetry in real time.
- Provide reset, guided example, and bilingual explanation.
- Ensure graph controls work on touch and keyboard.

Math evaluation:
- Use a restricted parser/allowlist. Never use eval() or new Function().
- Validate expression length, allowed symbols, operations, and functions.
- Handle domain errors, division by zero, undefined points, overflow, and non-finite results.
- Add unit tests for valid and invalid expressions.

Backend:
- Define a versioned visualization configuration contract.
- If creating math-service, keep it stateless initially unless persistence is required.
- Document whether evaluation happens in the browser for visualization responsiveness, on the server for grading, or both. The server remains authoritative for graded answers.

Do not implement derivatives, limits, or all planned graph types yet. Complete the parabola to production-quality acceptance criteria first.
```

**Acceptance checks** - Sliders update graph and derived values. -
Arabic and French explanations display correctly. - Invalid expressions
are rejected safely. - Mathematical outputs pass unit tests.

### 15.9 Phase 6 --- Exercise service

**Steps** 1. Create `exercise-service` and `exercise_db`. 2. Model
exercise definitions, translations, answer specifications, attempts, and
feedback. 3. Implement multiple-choice and numeric exercises first. 4.
Validate answers on the server. 5. Add expression equivalence only after
safe parsing and precision policy are tested. 6. Implement idempotent
attempt submission. 7. Build Angular exercise runner and feedback
states.

**Prompt**

``` text
Implement Phase 6 only: exercise-service and learner exercise flow.

Create exercise-service with its own database and migrations.

Implement:
- Exercise definitions linked to curriculum/skill IDs as external references.
- French/Arabic prompts and hints.
- Multiple-choice and numeric-answer types.
- Server-side answer validation.
- Attempt history, timestamps, duration, and feedback.
- Idempotency key for submission so network retries do not duplicate attempts or rewards.
- Explanations and hints that do not reveal the answer before submission.
- Protected authoring endpoints for content roles.

Angular:
- Build an exercise runner with progress, answer input, submit, retry, and feedback.
- Support keyboard, touch, and screen-reader-friendly states.
- Display mathematical notation through KaTeX.

Never accept correctness, score, or XP from the client. Add tests for wrong answers, correct answers, boundary values, repeated requests, unauthorized attempts, and cross-user isolation. Start with multiple choice and numeric answers; do not expand exercise types prematurely.
```

**Acceptance checks** - The server determines correctness. - Attempts
are persisted once per idempotency key. - Learners can view only their
own attempts. - Feedback is bilingual and accessible.

### 15.10 Phase 7 --- Learning progression and gamification

**Steps** 1. Create `learning-service` and `gamification-service`, each
with its own database. 2. Implement lesson progress, learning paths,
skill mastery, and revision schedule. 3. Publish domain events such as
`LessonCompleted` and `ExerciseAttemptRecorded`. 4. Use an outbox and
idempotent RabbitMQ consumers. 5. Implement XP ledger, levels,
achievements, and streaks. 6. Build the learner dashboard and progress
screens.

**Prompt**

``` text
Implement Phase 7 only: learning progression and gamification.

Create learning-service and gamification-service with separate databases and Flyway migrations.

Learning service:
- Learning paths and ordered path items.
- Lesson progress and completion.
- Skill mastery records.
- Prerequisite-based recommendations.
- Revision schedule.
- A transparent initial mastery heuristic, clearly labeled as an estimate rather than an official grade.

Gamification service:
- Append-only XP ledger.
- Levels and XP thresholds.
- Achievements and unlock records.
- Daily streaks.
- Idempotency protection for every reward.

Integration:
- Use RabbitMQ events for completed lessons and recorded exercise attempts.
- Implement an outbox pattern for reliable event publication.
- Make consumers idempotent.
- Do not introduce distributed transactions.
- Never allow the browser to award XP or mastery.

Angular:
- Dashboard with next lesson, progress, XP, achievements, streak, and revision recommendations.
- Show why a recommendation is made.
- Keep learner progress private by default.

Add tests for duplicate events, duplicate XP, prerequisite recommendations, mastery boundaries, and timezone/date handling for streaks.
```

**Acceptance checks** - Completing a lesson updates progress. -
Validated attempts contribute to mastery. - XP is awarded exactly
once. - Duplicate events do not duplicate achievements or XP.

### 15.11 Phase 8 --- Analytics and administration

**Steps** 1. Create `analytics-service` with read-optimized projections.
2. Build projections from domain events rather than cross-service
database queries. 3. Add learner reports for progress, weak skills, and
revision. 4. Complete admin interfaces for curriculum/content workflows.
5. Add audit logs and access tests. 6. Verify report freshness and
eventual consistency behavior.

**Prompt**

``` text
Implement Phase 8 only: analytics and administration.

Create analytics-service with its own read-model database. Populate projections from versioned domain events. Do not query other services' databases.

Learner analytics:
- Learning overview.
- Skill mastery report.
- Weak points and prerequisite gaps.
- Learning history.
- Revision and retention indicators.

Administration:
- Protected dashboards.
- Curriculum management links through curriculum-service APIs.
- Lesson and translation authoring through content-service APIs.
- Exercise authoring through exercise-service APIs.
- Review queues and publication status.
- Audit trail for sensitive administrative actions.

Angular:
- Build accessible analytics visualizations with clear labels and empty states.
- Clearly indicate that analytics may be eventually consistent.
- Do not expose one learner's records to another learner.

Add tests for event projection replay, duplicate event handling, role permissions, and data isolation. Document projection rebuild procedures.
```

**Acceptance checks** - Analytics are derived from service events/read
models. - Admin actions are authorized and auditable. - Learner data is
isolated. - Projection rebuilds are documented and tested.

### 15.12 Phase 9 --- Production readiness

**Steps** 1. Add health/readiness checks to all services. 2. Add
structured logs and correlation IDs. 3. Add Micrometer metrics,
Prometheus, and Grafana dashboards. 4. Add timeouts, bounded retries,
and circuit breakers where justified. 5. Add contract, integration, and
end-to-end tests. 6. Add container hardening and CI/CD. 7. Configure
backups and test restoration. 8. Perform security and privacy review. 9.
Deploy a staging environment and run smoke tests before production.

**Prompt**

``` text
Implement Phase 9 only: production readiness and deployment.

Review the complete repository and produce a gap analysis before changing files.

Implement:
- Health, readiness, and liveness checks for every service.
- Structured logs with request/correlation IDs; never log secrets or tokens.
- Micrometer metrics and Prometheus scraping.
- Grafana dashboards for Gateway, service health, latency, error rates, JVM, and database pools.
- Explicit timeouts and bounded retries for inter-service calls.
- Resilience4j circuit breakers only where failure isolation is needed.
- OpenAPI and event contract validation in CI.
- Unit, integration, contract, and Playwright end-to-end test stages.
- Multi-stage non-root Dockerfiles.
- Docker Compose staging topology and deployment documentation.
- Immutable image tags and rollback instructions.
- Database backup and restore procedure, then test the restore.
- Security review for authentication, authorization, CORS, CSRF, secrets, dependency vulnerabilities, and learner data isolation.

Do not introduce Kubernetes unless the user explicitly approves a separate deployment phase. Run all feasible checks and report exact commands, pass/fail results, and any remaining risks.
```

**Acceptance checks** - Every service exposes health and metrics. - CI
validates changed services and contracts. - Staging deployment and smoke
tests pass. - Backups can be restored. - Deployment and rollback
procedures are documented.

### 15.13 Suggested first vertical slice

Do not try to complete all grades and topics first. Deliver one
end-to-end topic:

1.  Register and log in.
2.  Select one verified Tunisian grade.
3.  Browse one mathematics chapter.
4.  Open one original lesson in French or Arabic.
5.  Explore the interactive parabola.
6.  Complete three to five reviewed exercises.
7.  Receive server-validated feedback.
8.  See lesson progress and XP reflected in the dashboard.

This slice validates the architecture, content workflow, math
visualization, assessment, and learner experience before the project
expands.

## 16. Architecture and Product Decisions to Preserve

  -----------------------------------------------------------------------
  Decision                Choice                  Rationale
  ----------------------- ----------------------- -----------------------
  Product scope           Primary through         Broad learner coverage
                          Baccalaureate           

  Languages               French + Arabic         Bilingual Tunisian
                                                  audience

  Curriculum              Tunisian source of      Local alignment with
                          truth + mapped French   supplementary material
                          enrichment              

  Content                 Original lessons         Consistent pedagogy and
                                                  licensing control

External resources       Dedicated resource-      Provider isolation,
                          service                  licence tracking and
                                                  editorial curation

  Frontend                Angular + TypeScript    Structured, scalable
                                                  SPA

  Backend                 Java 21 + Spring Boot   Independently
                          microservices           deployable bounded
                                                  contexts

  Gateway                 Spring Cloud Gateway    Single API entry point

  Discovery               Eureka                  Service registration
                                                  and discovery

  Database                PostgreSQL,             Data ownership and
                          database-per-service    isolation

  Migrations              Flyway per service      Controlled schema
                                                  evolution

  Messaging               RabbitMQ for domain     Asynchronous workflows
                          events                  without requiring Kafka

  Authentication          Dedicated               Isolated identity
                          identity-service with   boundary;
                          Spring Security         OIDC-compatible
                                                  evolution

  Math UI                 JSXGraph + Math.js      Visualization, safe
                          restricted parser +     numeric work, display,
                          KaTeX + MathLive        input

  Observability           Actuator + Micrometer + Service health and
                          Prometheus + Grafana    operational visibility

  Deployment              Docker Compose first    Manageable local and
                                                  early deployments

  Orchestration           Kubernetes only if      Avoid premature
                          later justified         operational complexity

  AI                      Optional future         MVP must work without
                          extension               paid AI tokens

  Premium                 Future extension        Validate learning
                                                  product before billing

  Mobile                  Future                  Reuse Gateway API
                          Angular-compatible API  
                          / Flutter client later  
  -----------------------------------------------------------------------

## 17. Risks and Mitigations

  ---------------------------------------------------------------------
  Risk                               Mitigation
  ---------------------------------- ----------------------------------
  Curriculum mismatch                Verify every grade/chapter against
                                     authoritative Tunisian documents
                                     and version the mapping

  Incorrect mathematics              Reviewed answer keys, automated
                                     tests, educator review

  Unsafe expression evaluation       Allowlisted parser, complexity
                                     limits, no `eval`

  Scope too large                    Build one complete vertical slice
                                     before expanding

  Poor Arabic experience             Test RTL from the first UI
                                     milestone

  Duplicate XP or attempts           Idempotency keys and database
                                     constraints

  Learner data exposure              Ownership checks, RBAC, minimal
                                     data, private-by-default progress

  AI-generated errors                Human review; never auto-publish
                                     generated content

  Hosting limits                     Validate quotas, persistence,
                                     region, and cost before production

  Content licensing issues           Maintain source and licence
                                     metadata; do not republish without
                                     permission
  ---------------------------------------------------------------------

------------------------------------------------------------------------

## 18. Definition of Done

A feature is not complete merely because it works on one developer's
machine.

A feature is done when:

-   Acceptance criteria are met.
-   Backend validation and authorization are implemented.
-   Relevant unit and integration tests pass.
-   Frontend loading, empty, error, and success states exist.
-   French and Arabic behavior is verified where applicable.
-   RTL and responsive behavior are checked.
-   Accessibility basics are respected.
-   No secrets or sensitive learner data are exposed.
-   API and technical documentation are updated.
-   Database changes are delivered as Flyway migrations.
-   CI checks pass.

------------------------------------------------------------------------

## 19. Final Product Priorities

The platform's long-term value depends on three foundations:

1.  **Mathematical trust:** explanations, graphs, exercises, and answer
    validation must be correct.
2.  **Curriculum trust:** learners and educators must recognize the
    Tunisian curriculum structure.
3.  **Learning quality:** the platform should help learners understand
    and retain concepts, not merely accumulate points.

Gamification, AI, premium subscriptions, and mobile applications can be
added progressively. They should strengthen these foundations rather
than distract from them.

**Recommended first milestone:** deliver one verified bilingual lesson
on quadratic functions, an interactive parabola, a small set of tested
exercises, server-validated attempts, and a learner progress screen.
