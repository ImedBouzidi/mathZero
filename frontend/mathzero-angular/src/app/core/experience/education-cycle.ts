import { TranslationKey } from '../i18n/translations';

export type EducationCycle = 'primary' | 'basic' | 'secondary';

export interface ExperienceCapabilities {
  readonly mathLab: boolean;
  readonly leaderboard: boolean;
  readonly writtenAnswers: boolean;
  readonly dailyMissions: boolean;
  readonly examMode: boolean;
}

export interface ExperienceDefinition {
  readonly id: EducationCycle;
  readonly basePath: string;
  readonly nameKey: TranslationKey;
  readonly taglineKey: TranslationKey;
  readonly capabilities: ExperienceCapabilities;
}

/**
 * Single source of truth for experience routing, theming and capabilities.
 * The learner's cycle comes from the curriculum profile, never from a
 * hardcoded list of grade names in the frontend.
 */
export const EXPERIENCES: Record<EducationCycle, ExperienceDefinition> = {
  primary: {
    id: 'primary',
    basePath: '/primary',
    nameKey: 'landing.primary.name',
    taglineKey: 'landing.primary.tagline',
    capabilities: {
      mathLab: false,
      leaderboard: false,
      writtenAnswers: false,
      dailyMissions: true,
      examMode: false,
    },
  },
  basic: {
    id: 'basic',
    basePath: '/basic',
    nameKey: 'landing.basic.name',
    taglineKey: 'landing.basic.tagline',
    capabilities: {
      mathLab: false,
      leaderboard: true,
      writtenAnswers: false,
      dailyMissions: true,
      examMode: false,
    },
  },
  secondary: {
    id: 'secondary',
    basePath: '/secondary',
    nameKey: 'landing.secondary.name',
    taglineKey: 'landing.secondary.tagline',
    capabilities: {
      mathLab: true,
      leaderboard: false,
      writtenAnswers: true,
      dailyMissions: false,
      examMode: true,
    },
  },
};

export const EXPERIENCE_LIST: readonly ExperienceDefinition[] = [
  EXPERIENCES.primary,
  EXPERIENCES.basic,
  EXPERIENCES.secondary,
];

export function isEducationCycle(value: unknown): value is EducationCycle {
  return value === 'primary' || value === 'basic' || value === 'secondary';
}

export function experienceHome(cycle: EducationCycle): string {
  return EXPERIENCES[cycle].basePath;
}

/** Route to the animated lesson generated for an approved resource. */
export function lessonPath(cycle: string | null | undefined, resourceId: string): string | null {
  const normalized = (cycle ?? '').toLowerCase();
  if (!isEducationCycle(normalized)) {
    return null;
  }
  const base = EXPERIENCES[normalized].basePath;
  const segment = normalized === 'primary' ? 'lessons' : 'lesson';
  return `${base}/${segment}/${encodeURIComponent(resourceId)}`;
}
