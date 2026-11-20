# Mathematics Zero to Hero — Three Experiences Redesign Proposal

**Status:** Proposal for approval — no structural frontend changes made yet.
**Date:** 2026-11-20
**Scope:** Angular frontend redesign across Primary, Basic and Secondary cycles.
**Design reference:** UI UX Pro Max skill at
`CheckMeMS/chess-web/.agent/skills/ui-ux-pro-max` (inspected and queried, not assumed).

---

## 1. Executive summary

The platform will ship **one brand, one shared design system, three distinct products**:

| Cycle | Product name | Product metaphor | Core question it answers |
|---|---|---|---|
| Primary | **Math Adventure** | A guided adventure map | “What do I explore next?” |
| Basic | **Math Challenge** | A training dashboard | “How am I improving, and what do I train next?” |
| Secondary | **Math Lab & Bac Mastery** | An academic workspace | “What do I master, what is weak, and am I ready for the Bac?” |

They differ in **layout model, navigation model, information density, typography,
palette, lesson format, exercise interaction, progress language and reward model** —
not merely in colour.

Two constraints shape the delivery:

1. **Only `GET /api/v1/health` exists today.** Authentication, curriculum, content,
   exercises, learning and gamification services are planned Phases 2–7.
   Therefore learner-data screens can be built now as real shells with typed inputs
   and honest loading/empty/error states, but **cannot be populated with real data
   yet**.
2. **No fake backend and no invented curriculum.** This proposal does not add mock
   endpoints or fabricated lesson content. Design previews in this document are
   explicitly illustrative wireframes, not shipped data.

---

## 2. What was inspected (evidence base)

### 2.1 Repository audit

| Area | Current state | Impact |
|---|---|---|
| Angular | 22.3, standalone, zoneless, strict TS 6, Vitest | Keep |
| Styling | Tailwind 4 via PostCSS, `src/styles/tokens.css`, `src/styles/base.css` | Refactor into layered tokens, keep Tailwind |
| Fonts | Self-hosted `@fontsource-variable/inter`, `@fontsource/noto-sans-arabic` | Extend per experience, self-hosted |
| i18n | Signal-based `LanguageService`, `fr`/`ar` dictionary, `lang`/`dir` on `<html>` | Keep and extend |
| UI primitives | `AppButton` directive, `AppCard`, `LanguageSwitch` | Keep semantics, restyle through semantic tokens |
| Routing | Root lazy-loads `features/home`; `**` not-found | Replace with experience routes, preserve not-found |
| API | `API_BASE_URL` → Gateway; `HealthService` only | Keep health check, move detail to `/status` |
| Backend | Gateway on port **8090**, Eureka, Compose Postgres/RabbitMQ | Do not break |
| Tests | 10 frontend tests incl. health success/error and i18n/RTL | Must remain green |
| Leftover | `src/app/app.html` still contains the Angular starter page but is unused | Safe to clean up |
| Math UI | None installed | Introduce per blueprint, lazy-loaded |

### 2.2 UI UX Pro Max workflow actually executed

Commands run from the skill directory:

```bash
python3 scripts/search.py "<query>" --design-system -p "<name>" -f markdown
python3 scripts/search.py "<query>" -d style|color|typography|chart|product|ux -n N
python3 scripts/search.py "<query>" --stack html-tailwind -n N
```

The skill has no Angular stack; `html-tailwind` was used as the closest
implementation reference and translated to Angular/Tailwind 4.

### 2.3 What the skill recommended, what we adopt, what we reject

| Query / output | Recommendation | Decision | Reason |
|---|---|---|---|
| Educational App (product) | Claymorphism + Micro-interactions; playful palette + clear hierarchy | **Adopt selectively** | Soft depth and micro-interactions fit Primary; full claymorphism reads toy-like |
| Primary design system | Claymorphism, indigo/orange, Baloo 2 + Comic Neue | **Partly adopt** | Keep warmth, rounded forms and playfulness; reject Comic Neue for legibility and Arabic; reject indigo as Primary lead because Basic owns indigo |
| Primary style search | Claymorphism, Neumorphism, Bento, Inclusive Design | **Claymorphism (restrained) + Inclusive Design** | Inclusive Design directly supports the accessibility requirement |
| Primary typography | Kids/Education (Baloo 2 + Comic Neue), Soft Rounded (Varela Round + Nunito Sans), Playful Creative (Fredoka + Nunito) | **Fredoka + Nunito Sans; Arabic Baloo Bhaijaan 2 + Noto Sans Arabic** | Fredoka is friendly but not babyish; better reading performance than Comic Neue; Arabic display face matches the rounded tone |
| Basic design system | Data-Dense Dashboard, Fira Code + Fira Sans, dark navy/green | **Reject the dark default; adopt density and drill-down** | The brief asks for navy/indigo/turquoise, modern and moderately gamified — not a BI terminal |
| Basic style search | Data-Dense Dashboard, Drill-Down Analytics | **Adopt drill-down and KPI hierarchy; reject 8px padding/12px type** | Compact but readable; teen-appropriate |
| Basic typography | Startup Bold (Outfit/Rubik), Geometric Modern (Outfit + Work Sans) | **Outfit + Inter** | Confident geometric display; Inter is already shipped and is a stronger UI/body face than adding Work Sans |
| Secondary design system | AI Personalization Landing + Dark Mode (OLED), Fira Code/Fira Sans | **Reject** | Dark OLED is not an academic workspace and conflicts with the required off-white/graphite direction |
| Secondary colour search | Consulting/Banking navy + gold | **Adopt navy authority; reject gold luxury** | The brief specifies emerald/cyan, not gold; gold reads “premium brand”, not “Bac lab” |
| Secondary typography | Academic/Research (Crimson Pro + Atkinson Hyperlegible) | **Crimson Pro + Inter; Arabic Noto Naskh Arabic** | Scholarly serif headings; Inter for dense, legible UI and tabular numbers; KaTeX for mathematics |
| Charts | Line for trends, radar for multi-variable, bullet/gauge for target, heatmap for intensity | **Adopt with accessibility limits** | Radar limited to 6 axes + data table; gauge always shows a number; heatmap always has a numerical legend; no pie above 5 slices |
| UX guidelines | Keyboard nav, skip links, back behaviour, heading order, skip tutorials, sticky-nav offset, focus rings, loading/empty/error states, reduced motion | **Adopt as acceptance criteria** | Already partly present in the Phase 1 shell |
| html-tailwind guidance | Hover feedback, responsive padding, consistent cards, reduced motion | **Adopt** | Matches existing Tailwind 4 setup |

**Deliberate design position:** the skill is a decision aid, not a template. Blindly
following the “Dark Mode (OLED)” result for Secondary or “Comic Neue” for Primary
would violate the product brief.

---

## 3. Experience architecture and selection

### 3.1 Configuration, not scattered checks

One registry drives routing, theming, labels and capability flags:

```ts
export type EducationCycle = 'primary' | 'basic' | 'secondary';

export interface ExperienceDefinition {
  readonly id: EducationCycle;
  readonly basePath: string;          // 'primary' | 'basic' | 'secondary'
  readonly theme: EducationCycle;     // data-experience value
  readonly labelKey: TranslationKey;
  readonly taglineKey: TranslationKey;
  readonly capabilities: {
    readonly mathLab: boolean;
    readonly leaderboard: boolean;
    readonly writtenAnswers: boolean;
    readonly dailyMissions: boolean;
    readonly examMode: boolean;
  };
}
```

Proposed routes:

```text
/                         Brand entry + experience preview (and status summary)
/status                   Existing health-check detail (preserved)
/onboarding               Missing grade / new learner
/onboarding/level         Grade selection (fed by curriculum API when available)
/primary/*                Math Adventure
/basic/*                  Math Challenge
/secondary/*              Math Lab & Bac Mastery
/profile                  Shared profile
/settings                 Language, level, privacy
/**                       Not found (preserved)
```

Once authentication lands, `/` becomes a resolver-based redirect to the learner’s
experience. Until then, the three experience links are clearly labelled **Aperçu**
so no one mistakes them for a signed-in learner account.

### 3.2 Selection rules

| Case | Behaviour |
|---|---|
| Grade present | `cycle = grade.cycle` from the curriculum profile; redirect to that experience |
| Missing grade | `/onboarding/level`; no default cycle and no inference of mathematical ability |
| New registration | After identity is available: if no grade → onboarding; otherwise → experience home |
| Grade change | Update profile via API, recompute cycle, announce change in a live region, redirect to new home if the cycle changed |
| Deep link to another cycle | Cycle guard redirects to the learner’s own experience and explains why |
| Language change | Independent of experience; updates `lang`/`dir`, persists; never changes cycle |
| Unknown grade id | Safe state: “niveau indisponible”, retry + contact; never guess |
| Offline / API failure | Explicit error state with retry; no cached fake progress |

The `cycle` value belongs to the **curriculum/identity model**, not to a hardcoded
list of Tunisian grade names in the frontend. This keeps curriculum versions
server-authoritative and avoids inventing data.

### 3.3 Theme application

- `data-experience="primary|basic|secondary"` is set on the experience shell and on
  `<html>` for body-level surfaces and body-attached overlays.
- Each experience exposes the same **semantic token contract** with different values.
- Components consume semantic tokens only. No component hardcodes a cycle colour.
- Leaving an experience removes the attribute; shared/auth screens use the brand theme.

---

## 4. Shared design system

### 4.1 Token layers

```text
styles/
  tokens/
    base.css          # type scale, spacing, radii, shadows, motion, z-index
    themes/
      primary.css     # semantic values for Math Adventure
      basic.css       # semantic values for Math Challenge
      secondary.css   # semantic values for Math Lab
  base.css            # resets, focus, skip link, reduced motion
  styles.css          # entry point
```

**Base tokens (shared):** spacing `4/8/12/16/24/32/48/64`, type scale, line heights,
radii, elevation, duration `120/200/320ms`, easing, z-index.

**Semantic tokens (per experience):**

```css
--bg-canvas; --bg-surface; --bg-subtle; --bg-inverse;
--text-primary; --text-secondary; --text-muted; --text-inverse;
--border-subtle; --border-strong;
--accent; --accent-hover; --accent-contrast;
--accent-2; --success; --warning; --danger; --info;
--focus-ring;
--nav-bg; --nav-text; --nav-active-bg; --nav-active-text;
--card-radius; --control-radius; --card-border; --card-shadow;
--font-display; --font-body; --font-math;
--tap-target; --content-max; --sidebar-width; --rail-width;
```

Tailwind 4 utilities are generated from these semantic variables (`bg-surface`,
`text-muted`, `border-subtle`, `rounded-card`, etc.). Legacy Phase 1 utilities that
are still referenced are migrated rather than left as a second colour system.

### 4.2 Shared primitives (behaviour is common, appearance is themed)

- **Actions:** Button, IconButton, LinkButton, SplitButton (secondary only).
- **Forms:** Input, Textarea, Select, Checkbox, Radio, Switch, Field + error/hint,
  SearchField, NumberField, MathInput wrapper (MathLive, later).
- **Overlays:** Dialog, Sheet/Drawer, Popover, Tooltip, Toast, ConfirmDialog.
- **Navigation:** Tabs, SegmentedControl, Breadcrumb, Pagination, SkipLink.
- **Data:** ProgressBar, ProgressRing, Stat, Badge/Chip, Avatar, DataTable, Timeline,
  Skeleton, EmptyState, ErrorState, InlineAlert.
- **Learning:** Formula (KaTeX), GraphCanvas (JSXGraph wrapper), ExerciseShell,
  AnswerFeedback, HintDisclosure, MasteryBadge, StreakStrip.
- **Layout primitives (unopinionated):** Container, Stack, Cluster, Grid, Sidebar.
  Each experience composes its own shell; no shared “one dashboard” layout.

All primitives meet: visible focus, 44/48px targets where touch, disabled and
loading states, ARIA where needed, RTL via logical properties, reduced motion.

### 4.3 Icons and brand

- One `app-icon` component with a curated inline SVG set (Lucide-style, ISC),
  avoiding a runtime icon dependency.
- Shared lockup: `∑` monogram + “Mathematics Zero to Hero”.
- Primary uses the full playful lockup; Basic uses a compact horizontal lockup;
  Secondary uses the monogram in the rail and the wordmark in the workspace bar.
- No emoji as functional icons.

### 4.4 Mathematics

Blueprint-mandated stack: **KaTeX + MathLive + JSXGraph + Math.js** (restricted
parser). Introduction order:

1. **Now:** KaTeX for formula rendering in lesson/exercise shells.
2. **With content APIs:** MathLive for written answers (Secondary), JSXGraph +
   Math.js for graphs and the Math Lab.
3. All math libraries are **lazy-loaded only in lesson/lab routes** to protect the
   initial bundle.

Rules: formulas stay LTR in Arabic pages; math gets its own `dir="ltr"` wrapper and a
localized accessible label; KaTeX MathML output is preserved for screen readers.

---

## 5. Experience 1 — Primary: Math Adventure

### 5.1 Design direction

A warm, illustrated **journey**, not a dashboard. The home screen is a map; progress
is a path; rewards are collected objects. Large type, one concept per screen, short
sentences, generous touch targets. Playful but controlled: no candy-coloured chaos,
no baby-toy styling, no constant animation.

**Adopted from UI UX Pro Max:** Claymorphism (soft depth, rounded forms), Inclusive
Design, Educational App product guidance, playful palette + clear hierarchy.
**Rejected:** full thick-border/double-shadow claymorphism everywhere, Comic Neue,
indigo as the lead colour, dark mode.

### 5.2 Palette — “Sunrise Paper”

| Token | Hex | Usage | Contrast |
|---|---|---|---|
| `--bg-canvas` | `#FFF8EE` | Warm paper background | — |
| `--bg-surface` | `#FFFFFF` | Cards, sheets | — |
| `--bg-subtle` | `#FFF1DF` | Section tints | — |
| `--text-primary` | `#2B2140` | Body and headings | 14.3:1 on canvas |
| `--text-secondary` | `#6B5B7A` | Supporting text | 5.9:1 on canvas |
| `--accent` | `#C2410C` | Primary CTA, links | 5.2:1 with white |
| `--accent-soft` | `#F26B3A` | Large shapes, illustration | 5.0:1 with ink |
| `--accent-2` | `#1D6FD6` | Sky/info, secondary action | 4.9:1 with white |
| `--success` | `#047857` | Correct answer, completed | 5.5:1 with white |
| `--success-soft` | `#D1FAE5` | Success tint | 6.8:1 with `#065F46` |
| `--reward` | `#F2B705` | Stars, badges | 8.3:1 with ink |
| `--play` | `#6D4AFF` | Optional playful accent | 5.2:1 with white |
| `--border-subtle` | `#EAD9C6` | Card borders | — |
| `--focus-ring` | `#1D4ED8` | Focus | 3:1+ against surfaces |

Rules: coral is a **surface/large-shape** colour; deep coral is the text/button
colour. Never white text on `#F26B3A` or `#0E9F6E`.

### 5.3 Typography

| Role | Latin | Arabic | Sizes |
|---|---|---|---|
| Display | Fredoka 600/700 | Baloo Bhaijaan 2 600/700 | 40/48 hero, 32 page title |
| Body | Nunito Sans 400/600/700 | Noto Sans Arabic 400/600/700 | 17px/1.6, minimum 16px |
| Numerals | Fredoka/Nunito tabular | Noto Sans Arabic | Large, high contrast |

Justification: the skill’s “Playful Creative” and “Soft Rounded” pairings; Fredoka
keeps personality while remaining legible; Nunito Sans has open counters and strong
x-height; Baloo Bhaijaan 2 provides a matching rounded Arabic display voice.

### 5.4 Navigation

- **≤1023px:** sticky top “backpack bar” (avatar, stars, streak, language) +
  **bottom tab bar** with four destinations: **Carte**, **Missions**, **Récompenses**,
  **Moi**.
- **≥1024px:** top bar + horizontal journey ribbon. No sidebar, no nested menus.
- Maximum depth: experience → chapter → lesson. Back always returns to the map.
- No hidden gestures; drag interactions always have a tap/select alternative.

### 5.5 Dashboard wireframe — desktop ≥1024

```text
┌──────────────────────────────────────────────────────────────────────┐
│ [∑] Math Adventure          ★ 24   ● 3 days   [FR|ع]        [avatar] │
├──────────────────────────────────────────────────────────────────────┤
│  Bonjour Yasmine !                              ┌──────────────────┐ │
│  Prête pour une nouvelle aventure ?             │ Mission du jour  │ │
│                                                 │ 3 petits défis   │ │
│  ┌────────────── LA CARTE ──────────────────┐   │ ▓▓▓░░  2/3       │ │
│  │  ●────●────◉────○────○────○             │   │ [Continuer]      │ │
│  │ Ch.1  Ch.2  Ch.3  Ch.4  Ch.5            │   └──────────────────┘ │
│  │             ▲ tu es ici                  │   ┌──────────────────┐ │
│  │  [Continuer : Les fractions]             │   │ Ma leçon         │ │
│  └──────────────────────────────────────────┘   │ Les fractions    │ │
│                                                 │ [Ouvrir]         │ │
│  ┌── Étoiles ─────┐  ┌── Badges ───────────┐   └──────────────────┘ │
│  │ ★ 24 / 50      │  │ [badge] [badge] [ ? ]│                        │
│  └────────────────┘  └──────────────────────┘                        │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.6 Dashboard wireframe — mobile ≤767

```text
┌──────────────────────────┐
│ [∑]  ★24   ●3       [ع]  │
├──────────────────────────┤
│ Bonjour Yasmine !        │
│ [ Mission du jour  → ]   │
│                          │
│  ● Chapitre 1  Terminé   │
│  │                       │
│  ◉ Chapitre 2  En cours  │
│  │   [ Continuer ]       │
│  │                       │
│  ○ Chapitre 3  À venir   │
│  ○ Chapitre 4  À venir   │
│                          │
├──────────────────────────┤
│ [Carte] [Missions]       │
│ [Récompenses] [Moi]      │
└──────────────────────────┘
```

### 5.7 Lesson presentation

- Full-width “storybook” card; one concept per page; progress dots.
- 2–4 short sentences per page, large type, generous line height.
- Visual area first: countable objects, number line, fraction strips, shapes.
- Optional read-aloud button using the browser SpeechSynthesis API (no backend).
- “Montre-moi” micro-demo: one animated object at a time, 200–320ms, disabled by
  `prefers-reduced-motion`.
- Big “Suivant” and “Retour”; no forced linear tour without a skip.

### 5.8 Exercise interaction

| Type | Interaction | Feedback |
|---|---|---|
| Counting | Tap objects/answer pad | Immediate, visual + text |
| Number line | Drag marker or tap position | Snap + explanation |
| Matching | Tap pair (not drag-only) | Pairs lock, gentle success |
| Drag & drop | Drag with tap-to-place fallback | Immediate |
| Multiple choice | Large cards | Explain why, allow retry |

One task per screen. No punitive timer. Stars per attempt, but never lose earned
stars. Wrong answer → “Regarde : …” explanation, then retry.

### 5.9 Progress and gamification

- Journey map with locked/unlocked/completed nodes.
- Star jar, badge shelf, collectible objects.
- 7-day trail (not a corporate bar chart).
- Daily mission, gentle streak, no leaderboard, no loss-aversion mechanics.
- Rewards are announced with `aria-live="polite"`; confetti is optional, brief and
  disabled under reduced motion.

### 5.10 Responsive and accessibility

- 375 / 768 / 1024 / 1440 validated.
- 48px minimum touch targets; 17px body minimum.
- Single column ≤767; map + side cards 2-column ≥1024.
- Icons always paired with text for navigation.
- No colour-only correctness; shapes/icons/labels reinforce.
- No flashing; no autoplay audio.

---

## 6. Experience 2 — Basic: Math Challenge

### 6.1 Design direction

A **training dashboard** for collège learners: structured, confident, compact,
moderately gamified. It answers “what do I continue, what do I train, how am I
progressing?” Desktop sidebar and mobile bottom navigation. Mature enough for teens;
not a corporate BI screen and not a children’s game.

**Adopted:** Data-Dense Dashboard’s drill-down and KPI hierarchy, “Startup Bold”
geometric typography, navy/indigo/turquoise palette.
**Rejected:** dark BI default, Fira Code as a display face, 12px body text, 8px card
padding, childish mascots/confetti.

### 6.2 Palette — “Navy Circuit”

| Token | Hex | Usage | Contrast |
|---|---|---|---|
| `--bg-canvas` | `#F5F7FB` | App background | — |
| `--bg-surface` | `#FFFFFF` | Cards | — |
| `--text-primary` | `#0B1220` | Text | 17.5:1 on canvas |
| `--text-secondary` | `#52617A` | Muted | 6.3:1 on white |
| `--nav-bg` | `#0C1B33` | Sidebar | — |
| `--nav-text` | `#C7D2E5` | Sidebar labels | 11.3:1 on navy |
| `--accent` | `#4338CA` | Primary action | 7.9:1 with white |
| `--accent-2` | `#2563EB` | Links, focus | 5.2:1 with white |
| `--turquoise` | `#0F766E` | Active indicator, mastery | 5.5:1 with white |
| `--turquoise-bright` | `#2DD4BF` | Rail active marker only | 9:1+ on navy |
| `--warning` | `#D97706` | Streak, attention | 3.3:1 with white (large/icon only) |
| `--success` | `#15803D` | Correct, complete | 4.8:1 with white |
| `--border-subtle` | `#DCE3EF` | Card borders | — |
| `--focus-ring` | `#2563EB` | Focus | 3:1+ |

### 6.3 Typography

| Role | Latin | Arabic | Sizes |
|---|---|---|---|
| Display | Outfit 600/700 | Noto Sans Arabic 600/700 | 30/38 page title |
| Body | Inter 400/500/600 | Noto Sans Arabic 400/500/600 | 15px/1.55 |
| Data | Inter tabular-nums | Noto Sans Arabic tabular | 13–14px |
| Math | KaTeX | KaTeX | per formula |

Justification: the skill’s “Geometric Modern”/“Startup Bold” direction; Outfit gives
confident geometry; Inter is already shipped, has excellent numerals and Arabic
pairing, and avoids adding a third body sans.

### 6.4 Navigation

- **≥1024:** 240px persistent left sidebar: **Tableau de bord, Chapitres, Exercices,
  Défis, Progrès, Profil**. Top bar: greeting, search, streak, XP, language, avatar.
- **768–1023:** 72px icon rail with labels on hover/focus; top bar remains.
- **<768:** top app bar + drawer; bottom nav: **Accueil, Cours, Exercices, Progrès**;
  profile via avatar.
- Sidebar collapses to a drawer; current section is always visible.
- RTL mirrors the sidebar to the right.

### 6.5 Dashboard wireframe — desktop ≥1024

```text
┌────────────┬─────────────────────────────────────────────────────────┐
│ ∑ MATH     │ Bonjour, Adam        ⌕   ●5   ⚡1 240 XP   [FR|ع] [av] │
│ CHALLENGE  ├─────────────────────────────────────────────────────────┤
│            │ ┌──────────────────────────┐ ┌─────────┐ ┌──────────┐ │
│ Tableau    │ │ CONTINUER                │ │ OBJECTIF│ │ NIVEAU 4 │ │
│ Chapitres  │ │ Équations du 1er degré   │ │ 3 / 5   │ │ ▓▓▓░ 68% │ │
│ Exercices  │ │ Chapitre 4 · 12 min      │ │ ◔ 60%   │ │ 1 240 XP │ │
│ Défis      │ │ [ Reprendre ]            │ └─────────┘ └──────────┘ │
│ Progrès    │ └──────────────────────────┘                          │
│ Profil     │ ┌────────────────────────────┐ ┌─────────────────────┐ │
│            │ │ PROGRESSION DES CHAPITRES  │ │ ACTIVITÉ · 7 JOURS  │ │
│            │ │ Nombres      ▓▓▓▓▓▓░ 82%   │ │  ▁ ▂ ▅ ▃ ▆ ▂ ▇      │ │
│            │ │ Géométrie    ▓▓▓░░░░ 55%   │ └─────────────────────┘ │
│            │ │ Fonctions    ▓▓░░░░░ 34%   │ ┌─────────────────────┐ │
│            │ └────────────────────────────┘ │ DÉFI DE LA SEMAINE  │ │
│            │ ┌────────────────────────────┐ │ Algèbre · 5 jours   │ │
│            │ │ REPRENDRE / RECOMMANDÉ     │ │ [ Rejoindre ]       │ │
│            │ └────────────────────────────┘ └─────────────────────┘ │
└────────────┴─────────────────────────────────────────────────────────┘
```

### 6.6 Dashboard wireframe — mobile ≤767

```text
┌──────────────────────────┐
│ ☰  Math Challenge   ●5   │
├──────────────────────────┤
│ Bonjour, Adam            │
│ ┌──────────────────────┐ │
│ │ CONTINUER            │ │
│ │ Équations du 1er deg │ │
│ │ [ Reprendre ]        │ │
│ └──────────────────────┘ │
│ ┌──────────┐ ┌─────────┐ │
│ │ OBJECTIF │ │ NIVEAU  │ │
│ │ ◔ 60%    │ │ 68%     │ │
│ └──────────┘ └─────────┘ │
│ Chapitres      ▓▓▓░░     │
│ Activité       ▁▂▅▃▆     │
│ [ Défi de la semaine ]   │
├──────────────────────────┤
│ [Accueil][Cours][Exos]   │
│ [Progrès]                │
└──────────────────────────┘
```

### 6.7 Lesson presentation

- Chapter header: objective, prerequisites, estimated time, mastery target.
- Concept tabs/sections; worked example as a **step-by-step stepper**.
- Formula blocks rendered with KaTeX; “copy formula” optional.
- Inline “Vérifie ta compréhension” with one quick question.
- Interactive graph/diagram in a bordered canvas; keyboard-operable parameters.
- Key-points summary and “Passer aux exercices”.
- Reading measure 60–72 characters; 15px minimum; 1.55 line height.

### 6.8 Exercise interaction

- Exercise set with progress bar and question navigator.
- Progressive hints (revealed one at a time); no penalty for using hints.
- Optional timed challenge: timer visible, pausable, never on the default path.
- Mixed types: MCQ, numeric, short answer, matching, ordering, graph reading.
- Immediate or end-of-set feedback (configurable per exercise type); detailed
  correction with the reasoning, not just the answer.
- Retry allowed; XP awarded once per validated attempt, server-authoritative.
- Keyboard: number keys for MCQ, Enter to validate, arrow keys in navigators.

### 6.9 Progress and gamification

- XP + level bar; weekly objective ring; 7-day activity bars.
- Chapter completion bars; optional skill radar limited to 6 axes with a data table.
- Streak strip, achievements, opt-in class challenges/leaderboard.
- Privacy: leaderboard off by default, display name or alias, no progress shared
  without consent. No public ranking of minors by default.

### 6.10 Responsive and accessibility

- 12-column desktop grid, 8-column tablet, 4-column mobile.
- 44px touch targets; 15px body; tabular numerals for data.
- Charts always accompanied by a table or textual summary.
- Timer announcements are polite, not every second.
- Focus order follows visual order; no keyboard traps in drawer/dialog.

---

## 7. Experience 3 — Secondary: Math Lab & Bac Mastery

### 7.1 Design direction

An **academic workspace**, not a gamified dashboard. Dense, quiet, precise. The
learner moves between overview, chapters, lab, exams and revision without losing
context. Editorial serif headings, graphite UI, off-white paper, restrained
emerald/cyan. The Lab is a first-class destination, not a widget.

**Adopted:** Academic/Research typography, consulting/banking navy authority,
drill-down, bullet/gauge/heatmap guidance.
**Rejected:** OLED dark default, gold luxury, Fira Code display, childish badges,
confetti, public leaderboards.

### 7.2 Palette — “Midnight Paper”

| Token | Hex | Usage | Contrast |
|---|---|---|---|
| `--bg-canvas` | `#FAFAF7` | Reading/workspace paper | — |
| `--bg-surface` | `#FFFFFF` | Panels, sheets | — |
| `--bg-subtle` | `#F3F2EC` | Section bands | — |
| `--text-primary` | `#161D26` | Body/headings | 16.2:1 on paper |
| `--text-secondary` | `#5B6675` | Metadata | 5.8:1 on white |
| `--nav-bg` | `#0B1526` | Icon rail | — |
| `--nav-text` | `#AEB9C9` | Rail icons/labels | 9.2:1 on navy |
| `--nav-active` | `#1B2C47` | Active rail item | — |
| `--accent` | `#0F766E` | Mastery, primary action | 5.5:1 with white |
| `--accent-2` | `#0E7490` | Lab/links/cyan | 5.4:1 with white |
| `--emerald-bright` | `#34D399` | Rail active marker, graph | on navy only |
| `--warning` | `#B45309` | Exam readiness caution | 5.0:1 with white |
| `--danger` | `#B91C1C` | Errors | 5.9:1 with white |
| `--border-subtle` | `#E3E1D8` | Rules/borders | — |
| `--focus-ring` | `#0E7490` | Focus | 3:1+ |

No gradients, no glass, no glow. Depth comes from 1px rules, paper bands and a
single low shadow on floating panels.

### 7.3 Typography

| Role | Latin | Arabic | Sizes |
|---|---|---|---|
| Editorial display | Crimson Pro 600/700 | Noto Naskh Arabic 600/700 | 36/44 hero, 28 page title |
| UI/body | Inter 400/500/600 | Noto Sans Arabic 400/500/600 | 15px/1.6 prose, 13px data |
| Mathematics | KaTeX (Computer Modern-compatible) | KaTeX | per formula |
| Data | Inter tabular-nums | Noto Sans Arabic tabular | 12.5–14px |

Justification: the skill’s “Academic/Research” pairing. Crimson Pro gives scholarly
authority without looking like a marketing site; Inter keeps dense UI legible and
already ships; Naskh gives Arabic the same editorial register. Atkinson Hyperlegible
was considered but would add a fourth body face with little gain over Inter.

### 7.4 Navigation

- **≥1280:** 72px icon rail + 280px contextual panel + main workspace + optional
  320px notes/tools rail.
- Rail: **Aperçu, Chapitres, Math Lab, Examens, Révision**. Contextual panel lists
  chapters/sections for the current area.
- Workspace bar: breadcrumb, command palette (`Ctrl/⌘ K`), focus mode, language,
  profile.
- **768–1279:** rail + main; contextual panel becomes a slide-over.
- **<768:** single pane; segmented control (Aperçu / Chapitres / Lab / Examens /
  Révision); outline in a bottom sheet.
- Lab mode can hide the contextual panel and expand to a near-full-width canvas,
  with parameters in a side panel (desktop) or bottom sheet (mobile).

### 7.5 Dashboard wireframe — desktop ≥1280

```text
┌────┬───────────────┬─────────────────────────────────────────────────────┐
│ ∑  │ 2nde → Bac    │ Math Lab / Maîtrise          ⌘K   Focus   [FR|ع] ⚙ │
│ ▣  ├───────────────┼─────────────────────────────────────────────────────┤
│ ◫  │ CHAPITRES     │ ┌────────────────────┐ ┌──────────────────────────┐ │
│ ƒ  │ ▸ Fonctions   │ │ MAÎTRISE GLOBALE   │ │ PRÉPARATION BAC          │ │
│ △  │ ▸ Dérivées    │ │ 72 %  Compétent    │ │ 64 %  ▓▓▓▓▓▓░░░  cible 80│ │
│ ⏱  │ ▸ Suites      │ │ ▓▓▓▓▓▓▓░░░         │ │ 4 sujets · 2 complets    │ │
│    │ ▸ Exponentiel │ └────────────────────┘ └──────────────────────────┘ │
│    │ ▸ Intégrales  │ ┌─ MATRICE DE MAÎTRISE ────────────────────────────┐ │
│    │               │ │ Chapitre       A    D    R    C    Score         │ │
│    │               │ │ Fonctions      ▓    ▓    ░    ▓     78          │ │
│    │               │ │ Dérivées       ▓    ░    ░    ▓     54          │ │
│    │               │ │ Suites         ▓    ▓    ▓    ░     81          │ │
│    │               │ └──────────────────────────────────────────────────┘ │
│    │               │ ┌─ CONCEPTS FRAGILES ──────┐ ┌─ RÉVISIONS ─────────┐ │
│    │               │ │ Limites en ±∞      38 %  │ │ Aujourd'hui         │ │
│    │               │ │ Dérivées composées 46 %  │ │ 18:00 · 20 min      │ │
│    │               │ │ [Réviser]                │ │ [Ouvrir le plan]    │ │
│    │               │ └──────────────────────────┘ └─────────────────────┘ │
└────┴───────────────┴─────────────────────────────────────────────────────┘
```

### 7.6 Dashboard wireframe — mobile ≤767

```text
┌──────────────────────────┐
│ ☰  Math Lab     ⌘K   ⚙  │
├──────────────────────────┤
│ [Aperçu][Chapitres][Lab] │
│ [Examens][Révision]      │
├──────────────────────────┤
│ MAÎTRISE GLOBALE         │
│ 72 %   Compétent         │
│ ▓▓▓▓▓▓▓░░░               │
│ ┌──────────────────────┐ │
│ │ PRÉPARATION BAC  64% │ │
│ │ cible 80 %           │ │
│ └──────────────────────┘ │
│ CONCEPTS FRAGILES        │
│ • Limites en ±∞    38 % →│
│ • Dérivées composées 46%→│
│ RÉVISIONS                │
│ Aujourd'hui · 18:00      │
└──────────────────────────┘
```

### 7.7 Lesson presentation

- Three-pane reading workspace: outline (left), lesson (center), notes/tools (right).
- Structured blocks: **Définition**, **Théorème**, **Démonstration**, **Exemple
  résolu**, **Remarque**, **Erreur fréquente**.
- Worked examples with step controls; each step can be revealed or shown fully.
- Proofs are collapsible but complete; no “proof by authority”.
- “Explorer dans le Lab” links a formula/graph to a preconfigured lab state.
- Reading measure 66–72 characters; 1.6 line height; generous space around formulas.
- Keyboard: `J/K` next/previous section (when not in an input), `Ctrl/⌘ K` palette,
  `Esc` closes tools.
- Print/export-friendly correction styles (later).

### 7.8 Exercise interaction

- **Exam-paper** layout: question, workspace, answer area, navigation.
- Multi-step problems with sub-questions; written answers with MathLive preview.
- Optional “Bac blanc” mode: timer, no hints, results mapped to mastery.
- Detailed correction: expected reasoning, common errors, and a link to the weak
  prerequisite.
- Self-assessment after written work (“compris / à revoir”), which feeds revision,
  never XP directly.
- Review queue for errors; spaced revision based on server schedule.
- No confetti; a restrained mastery delta after validation.

### 7.9 Progress and gamification

- Mastery matrix (accuracy, difficulty, retention, consistency) with server score
  and learner-facing band:
  - 0–39 Fondations à consolider
  - 40–59 En développement
  - 60–79 Compétent
  - 80–94 Maîtrisé
  - 95–100 Excellente rétention
- Exam readiness as a **bullet chart** with target marker and a numerical label;
  never presented as an official school grade.
- Weak concepts ranked list; revision schedule timeline; Bac coverage by topic.
- Academic milestones only (chapter mastered, mock exam completed). No public
  leaderboard, no collectible characters, no oversized badges.

### 7.10 Math Lab

Dedicated workspace, lazy-loaded, with:

- Function plotting, adjustable parameters, domain/range controls.
- Limits and continuity exploration; derivative and tangent construction.
- Exponential/logarithmic functions; sequences (arithmetic/geometric).
- Geometric constructions.
- Coordinate readout, table of values, reset, and “copy state” link.
- Keyboard-operable parameter inputs; graph canvas `dir="ltr"`.
- On mobile, graph fills the viewport and parameters move to a bottom sheet.
- Reduced-motion mode disables animated transitions but keeps interaction.

### 7.11 Responsive and accessibility

- Desktop 3-pane ≥1280; 2-pane 768–1279; single pane <768.
- 40px desktop controls, 44px touch controls.
- Dense tables become stacked definition lists on mobile.
- Every chart has a table/summary; every graph has a textual description.
- High contrast for long reading; no pure black; no low-contrast grey-on-grey.
- Focus mode hides non-essential chrome without hiding navigation.

---

## 8. Cross-experience comparison

| Dimension | Primary | Basic | Secondary |
|---|---|---|---|
| Home metaphor | Adventure map | Training dashboard | Mastery workspace |
| Navigation | Top bar + bottom tabs | Sidebar / rail / bottom nav | Icon rail + contextual panel + workspace bar |
| Density | Very low | Medium | High |
| Radius | 20–24px | 12–16px | 6–10px |
| Depth | Soft, restrained | Subtle cards | Rules and paper bands |
| Display type | Fredoka / Baloo Bhaijaan 2 | Outfit / Noto Sans Arabic | Crimson Pro / Noto Naskh Arabic |
| Body type | Nunito Sans 17px | Inter 15px | Inter 15px |
| Primary accent | Deep coral | Indigo | Emerald |
| Progress | Map, stars, badges | XP, level, weekly ring, bars | Mastery matrix, readiness bullet, revision |
| Gamification | High, gentle | Medium, opt-in social | Low, academic |
| Lesson | Storybook, one concept | Structured chapter + stepper | Academic reader + proofs + lab |
| Exercise | Playful manipulatives | Training set + hints | Exam paper + written answers |
| Mobile nav | Bottom tabs | Bottom nav + drawer | Segmented workspace + sheets |
| Motion | Playful, reduced | Functional | Minimal |

---

## 9. Shared vs experience-specific

### Shared (`core/`, `shared/`, `design-system/`)

- Brand lockup and monogram.
- API client, envelope handling, request ID, error normalisation.
- Authentication/session (when Phase 2 lands).
- Bilingual dictionary and language/direction service.
- Experience registry, cycle resolver, guards, onboarding routing.
- User profile, grade and locale.
- Typed curriculum/content/learning/gamification contracts.
- Design tokens: base + semantic contract.
- Primitives: buttons, fields, dialogs, sheets, toasts, tabs, progress, badges,
  tables, skeletons, empty/error states, formula renderer, graph wrapper.
- Accessibility utilities: skip link, focus management, reduced motion, live regions.
- Formatting: numbers, dates, durations, pluralisation.

### Experience-specific (`experiences/{primary,basic,secondary}/`)

- Shell and navigation.
- Dashboard composition.
- Lesson renderer and reading layout.
- Exercise player and interaction patterns.
- Progress and rewards views.
- Experience theme tokens and display fonts.
- Experience-specific empty/loading/error copy.

No shared business logic is duplicated to obtain a different look. Visual difference
comes from tokens, layout composition and experience-specific components.

---

## 10. API reality and integration scope

### 10.1 What exists now

- `GET /api/v1/health` through the Gateway (port 8090).
- No identity, curriculum, content, exercise, learning or gamification endpoints.
- Phase 1 frontend tests rely on the health success/error states and i18n/RTL.

### 10.2 What can be built now without faking data

- Token system, themes, primitives, icon set, fonts.
- Experience registry, routing, guards, onboarding shell, language/RTL.
- Three real shells and navigation systems.
- Dashboard **layouts** fed by typed inputs; when inputs are `null`, render honest
  empty states (“Votre progression apparaîtra après votre première leçon”).
- Lesson/exercise **interaction components** with real local interaction behaviour
  (drag, match, number line, stepper, timer), ready for content props.
- Loading, empty, error and offline states.
- KaTeX rendering with sample *formula strings in tests*, not fabricated lessons.
- Math Lab shell and graph interaction, once JSXGraph/Math.js are introduced.

### 10.3 What is blocked

- Real dashboard values, XP, streaks, mastery, recommendations, revision schedule.
- Real chapters, lessons, exercises and corrections.
- Real grade list and cycle selection.
- Cross-user privacy and leaderboard controls.

These require Phases 2–7. The UI will not invent them.

### 10.4 Decision requested

Because the brief forbids mock data, dashboards will look structurally complete but
data-empty until the APIs land. If visual review before Phase 2 is required, the only
compliant option is a **development-only preview route** with clearly labelled sample
content, excluded from production and never presented as a learner’s data. This
proposal recommends **not** shipping such fixtures unless explicitly approved.

---

## 11. Proposed implementation sequence

### Step A — Foundation (no experience redesign yet)

1. Add layered tokens: base + three semantic theme files.
2. Add fonts (self-hosted, experience-scoped) and KaTeX.
3. Build shared primitives and the icon component.
4. Add experience registry, cycle resolver, guards, onboarding shell.
5. Move the health detail to `/status`; keep `HealthService` and its tests.
6. Clean the unused Angular starter markup; keep not-found.
7. Verify build, lint, format, tests.

### Step B — Three shells and dashboards

1. Primary shell + dashboard map + empty states.
2. Basic shell + dashboard + empty states.
3. Secondary shell + workspace + dashboard + empty states.
4. Route-level lazy loading and theme application.

### Step C — Learning surfaces

1. Primary storybook lesson + exercise playground.
2. Basic chapter reader + training set.
3. Secondary academic reader + exam paper.
4. KaTeX integration; MathLive/JSXGraph/Math.js with content/lab phase.

### Step D — Integration (when APIs exist)

1. Identity/profile/grade → experience selection.
2. Curriculum → chapters and grade list.
3. Content → lessons.
4. Exercises/learning → attempts, mastery, revision.
5. Gamification → XP, levels, streaks, achievements.

### Step E — Validation

- Three experiences, French and Arabic, RTL and LTR.
- 375 / 768 / 1024 / 1440.
- Keyboard-only paths, focus visible, skip links, heading order.
- Contrast checks for every text/background pair.
- Loading, empty, error, offline and disabled states.
- Math rendering LTR inside RTL pages.
- Existing health check and i18n tests remain green.
- `ng build`, `ng lint`, `npm run format:check`, `ng test`.

---

## 12. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Three experiences drift into one template | Explicit layout models, navigation models and wireframes; review against the comparison table |
| Primary becomes toy-like | Restrained claymorphism, controlled palette, no Comic Neue, no constant animation |
| Basic becomes corporate BI | Teen-oriented copy, moderate gamification, 15px minimum, breathable cards |
| Secondary becomes childish or dark-OLED | Editorial serif, paper workspace, academic milestones, no characters/confetti |
| Font/bundle bloat | Self-host subset weights, experience-scoped loading, budget checks |
| RTL breaks navigation/graphs | Logical properties, mirrored shells, LTR math wrapper, RTL visual tests |
| Fake data pressure | Typed inputs + honest empty states; optional dev-only preview only with approval |
| Missing backend blocks demo | Foundation and shells deliverable now; data integration follows Phases 2–7 |
| Accessibility regressions | Tokens with verified contrast, primitives with focus/ARIA, checklist per experience |

---

## 13. Approval checklist

Please confirm or adjust:

1. **Palettes and typography** for the three experiences.
2. **Secondary workspace:** off-white “paper” + midnight rail (recommended) vs a dark
   midnight workspace.
3. **No-mock-data policy:** dashboards ship with honest empty states until APIs exist
   (recommended), or approve a dev-only preview route with labelled sample content.
4. **Dependencies:** five self-hosted font families + KaTeX now; MathLive, JSXGraph
   and Math.js when lesson/lab content exists.
5. **Build order:** foundation + three shells first, then dashboards, then learning
   surfaces (recommended), or one experience end-to-end at a time.

No broad structural change will be made until this proposal is approved.
