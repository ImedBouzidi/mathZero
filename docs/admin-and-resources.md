# Administrator and resource workflows

## Provision the first administrator

The identity service creates its initial `ADMIN` account from environment
configuration; public registration continues to assign only `STUDENT`.

1. Copy `backend/identity-service/.env.example` to
   `backend/identity-service/.env` if the private file does not already exist.
2. Set `APP_ADMIN_PASSWORD` in that file to a private password of at least
   twelve characters. The file is ignored by Git.
3. Start the identity service with `./manage-services.sh start identity` or
   Docker Compose. The local service manager and Compose load the service-local
   environment file. For a direct Maven run, export the file's values first.

On startup, the bootstrap creates the configured account if its email is new.
It is safe to run again: an existing ADMIN is left in place, except its password
hash is updated if the configured password changed. It refuses to elevate an
existing non-admin account with that email. If no password is configured, the
bootstrap skips account creation and logs a warning.

For production, provide the password from a secret manager and set
`APP_ADMIN_BOOTSTRAP_ENABLED=false` after provisioning. Never put the password
in application configuration, test fixtures, shell history, or documentation.

## Import and publish curriculum JSON

An ADMIN can use `/admin` in the existing Angular application to upload a JSON
document, validate it without writing, and then import it as a draft or publish
it. The server independently validates all references, duplicate codes,
effective dates and prerequisite cycles. Import writes are transactional and
audited. Re-importing updates draft nodes by their natural codes; published
versions are immutable and require a new version code.

The CLI accepts an explicit JSON path and defaults to dry-run:

```bash
./infra/scripts/import-curriculum.sh /path/to/verified-curriculum.json
```

Only perform the write after reviewing the validation result:

```bash
DRY_RUN=false ./infra/scripts/import-curriculum.sh /path/to/verified-curriculum.json
```

The CLI reads the administrator email and password from
`backend/identity-service/.env` (or `ADMIN_EMAIL` / `ADMIN_PASSWORD` overrides),
uses the Gateway's CSRF-protected login flow, and does not print its access
token. The JSON `publish` flag is honored only by a non-dry-run import. In the
Angular UI, publishing must be selected explicitly after validation.

The import request includes curriculum code and bilingual names, version code
and effective dates, source reference, reviewer, levels and grades,
specializations, subjects and chapters, skills, and prerequisite codes. See
the [`CurriculumImport` API schema](../contracts/openapi/curriculum-service.yaml)
for the complete nested shape and field limits. Source references and reviewer
names are retained with the version. No sample official curriculum is bundled:
only import material verified against an authoritative source.
An archived, explicitly unreviewed mathematics draft is available at
`infra/samples/curriculum/tunisian-mathematics-archive-2011.json`. It is based on
legacy Ministry/Edunet programme documents and must remain in DRAFT status until
the source dates, primary-degree PDFs and grade mappings are reviewed by an
authoritative Tunisian education reviewer.

## Curate learning resources

The Phase 4A resource service stores links and rights metadata in its own
`resource_db`, then turns each approved resource into an animated lesson and a
short quiz (see below). Administrators add manual entries in the same `/admin`
Angular screen. Resource links and license links must use HTTPS; title,
language, resource type, experience cycle, license identifier/name/URL and
attribution are required.

Every resource must belong to exactly one experience cycle: `PRIMARY`, `BASIC`
or `SECONDARY`. The public `/resources` page groups approved resources by
cycle, and each dashboard (`/primary`, `/basic`, `/secondary`) shows only the
resources for its own cycle. Resources without a cycle are not shown publicly.

The review lifecycle is:

```text
DRAFT → SUBMITTED → APPROVED
                   ↘ REJECTED → SUBMITTED
```

Only `APPROVED` resources are returned from the public
`GET /api/v1/resources` endpoint. The administrator API supports listing the
review queue, inspecting each resource's source and rights metadata, recording
decisions and reading its audit history from the `/admin` screen. Grade and
subject values are curriculum codes; there are no cross-service database
foreign keys.

## Animated lessons and exercises

Approving a resource (from the suggestion board or the review queue) also
generates its learning content in the same transaction. An
administrator does not write lesson text or questions: the resource service uses
a deterministic, offline, bilingual generator seeded from the resource title,
description, topic, language and cycle. Topic templates cover fractions,
equations, geometry, functions, trigonometry, multiplication and arithmetic,
with a general fallback. Multiplication lessons are adapted to the resource's
cycle: Primary practices the 1–10 table and carrying in a one-digit
multiplication; Basic works through two-digit partial products; Secondary
practices larger products with multiple carries. The animated demonstration
shows the 1–10 table and highlights the fact used in each step. These lessons
are available in French and Arabic. Existing generated lessons are refreshed
the first time they are opened after a generator update. Every lesson ends with
a source/rights step that carries the approved resource's recorded licence and
attribution. The generated material is introductory and is not official
curriculum content; it contains no external AI calls.

The learner experience is available on `/resources` and in the three cycle
dashboards: each approved resource card has a **Leçon animée** / **درس متحرك**
link. The player reveals one animated step at a time, renders formulas with
KaTeX, then presents a **replayable visual demonstration**: a worked example is
split into frames (observe, transform, calculate, check) with topic-specific
visuals for fractions, equations, geometry, functions, trigonometry,
multiplication and arithmetic. Multiplication frames show the 1–10 table and
highlight the fact used in each step. The learner can play, pause, replay or
jump between frames;
`prefers-reduced-motion` disables the decorative motion. Answers are checked by
`POST /api/v1/learning/lessons/{resourceId}/answers`; correct answers are stored
only in `resource_db` and are never included in
`GET /api/v1/learning/lessons/{resourceId}`. The quiz result is revealed only
after submission, together with an animated **solution walkthrough** that shows
how the answer was obtained. Content for resources approved before this feature
was added is generated lazily on the first lesson read, including the
demonstration frames (`V4__learning_demo.sql`).

The public contract for these endpoints is in the
[Resource API schema](../contracts/openapi/resource-service.yaml) under the
**Learning** tag. If the wording of the offline templates needs to change,
update `LearningContentGenerator` and increment its content version so existing
generated rows refresh on their next read; approval itself does not call any
external provider.

## External provider search

`GET /api/v1/admin/resources/providers` reports the status of the manual,
Wikimedia, OpenStax, GeoGebra, PhET, YouTube and CNTE adapters. Provider status
is not a content search or import.

Manual creation is no longer exposed in the Angular UI. Every external adapter
is disabled by default except Wikimedia. The JSON search, import and
manual-create endpoints remain available for integrations:
`GET /api/v1/admin/resources/search?q=&limit=&language=fr|ar` lists candidates
and `POST /api/v1/admin/resources/providers/import` stores a chosen result as a
`DRAFT` in `resource_db`. The server re-resolves the selected URL through the
same provider and uses the adapter's licence, attribution, language and resource
type, so clients cannot inject rights metadata or results from another
provider. The Angular admin dashboard no longer exposes a manual external-search
panel or a manual creation form; administrators work from the suggestion board
instead.

The `/admin` landing page shows one **suggestion board** with a provider dropdown
(`Tous les fournisseurs`, Wikimedia, OpenStax, GeoGebra, PhET). Selecting a
provider filters the three cycle columns — `PRIMARY`, `BASIC` and `SECONDARY` —
to that source; the default shows every source. The board is built from the
configured topic queries of every
enabled provider (`app.suggestions.providers.<provider>.queries` in
`application.yml`). The whole board is cached for `SUGGESTIONS_CACHE_TTL`
(legacy `WIKIMEDIA_SUGGESTIONS_CACHE_TTL` remains a fallback), limited per query
by `SUGGESTIONS_LIMIT_PER_QUERY` and capped for each provider, cycle and
language by `SUGGESTIONS_MAX_PER_CYCLE`. The cap is intentionally not global:
otherwise the first provider in configuration order would fill every slot and
the other dropdown entries would look empty, and Arabic candidates would be
dropped once the French queries filled a cycle. Disabled providers contribute
no candidates.

OpenStax's catalogue `search` matches book titles rather than full text, so its
configured queries use title words that exist in the public catalogue
(`prealgebra`, `elementary algebra`, `intermediate algebra`,
`algebra and trigonometry`, `calculus`). Queries that only appear in a book's
description return nothing.

An administrator checks the title, link, language and provider rights shown on
a card, then clicks **Approve**
(`POST /api/v1/admin/resources/suggestions/approve`). The request carries the
provider from the board; the server only accepts a suggestion still present in
the current cache. The owning adapter supplies the licence, attribution,
language and external ID. Approval stores the resource as `APPROVED`, writes an
audit record and generates its lesson/quiz, so it appears immediately in the
matching dashboard. Nothing is published by a search or import alone.

- Manual curation remains available through the JSON API for integrations.
- Wikimedia search is available in French and Arabic through the Wikimedia API.
  Suggestions carry CC BY-SA 4.0 rights metadata and Wikipedia attribution.
- OpenStax reads the documented CMS catalogue of books (`books.Book`). It is
  disabled unless `OPENSTAX_ENABLED=true`. The public catalogue can be used
  without credentials; deployments that need authenticated access supply either
  `OPENSTAX_BEARER_TOKEN`, or `OPENSTAX_TOKEN_URL` plus `OPENSTAX_CLIENT_ID` and
  `OPENSTAX_CLIENT_SECRET` for a client-credentials OAuth token. OpenStax books
  are English-language, so their suggestion queries use `en` and approved
  resources are stored with language `en`. Imports and approvals record
  CC BY 4.0 attribution by default, but the exact book licence must be checked
  before approval.
- GeoGebra uses its documented material search endpoint
  (`/search/materials?query=&lang=`) and links to `https://www.geogebra.org/m/…`.
  It is disabled unless `GEOGEBRA_ENABLED=true`; no key is required. GeoGebra
  materials are subject to the GeoGebra non-commercial licence by default, so
  commercial use requires separate permission. The adapter records
  `GEOGEBRA_NONCOMMERCIAL` and its licence URL; verify the individual material.
- PhET reads the public simulation metadata endpoint and filters the French or
  Arabic localized titles and descriptions. It is disabled unless
  `PHET_ENABLED=true`. The large catalogue is cached in memory for
  `PHET_METADATA_CACHE_SECONDS` (default one hour). The licence is taken from
  each simulation's own metadata and may be CC BY-NC 4.0 or another licence;
  check it before approval.
- YouTube remains scaffolded; its credentials stay server-side.
- CNTE remains blocked until written authorization and a documented API
  contract are available. Setting a URL or token alone does not enable calls.

Suggestion-board tuning is also environment-only: `SUGGESTIONS_CACHE_TTL`,
`SUGGESTIONS_LIMIT_PER_QUERY`, `SUGGESTIONS_MAX_PER_CYCLE`. The older
`WIKIMEDIA_SUGGESTIONS_CACHE_TTL`, `WIKIMEDIA_SUGGESTIONS_LIMIT` and
`WIKIMEDIA_SUGGESTIONS_MAX_PER_CYCLE` names remain supported as fallbacks.

Provider settings are environment-only (`WIKIMEDIA_ENABLED`, `OPENSTAX_ENABLED`,
`OPENSTAX_API_URL`, `OPENSTAX_SITE_URL`, `OPENSTAX_BEARER_TOKEN`,
`OPENSTAX_TOKEN_URL`, `OPENSTAX_CLIENT_ID`, `OPENSTAX_CLIENT_SECRET`,
`GEOGEBRA_ENABLED`, `GEOGEBRA_API_URL`, `GEOGEBRA_SITE_URL`, `PHET_ENABLED`,
`PHET_METADATA_URL`, `PHET_SITE_URL`, `PHET_METADATA_CACHE_SECONDS`,
`YOUTUBE_ENABLED`, `YOUTUBE_API_KEY`, `CNTE_AUTHORIZED`, `CNTE_API_URL`, and
`CNTE_API_TOKEN`). Searches send the administrator's query to the enabled
providers. Keep credentials out of Angular, logs, tracked files, and
provider-status responses. Do not mark an unreviewed resource approved.

## API references

- [Curriculum API and JSON import contract](../contracts/openapi/curriculum-service.yaml)
- [Resource curation and review contract](../contracts/openapi/resource-service.yaml)
