import { computed, Injectable, signal } from '@angular/core';

import { EducationCycle } from '../experience/education-cycle';
import { Language } from '../i18n/translations';

/**
 * Server-backed learner profile contract.
 *
 * It is intentionally nullable: no profile is invented before the identity
 * service exists. Phase 2 will populate it after authentication.
 */
export interface StudentProfile {
  readonly id: string;
  readonly displayName: string;
  readonly gradeId: string | null;
  readonly gradeLabel: string | null;
  readonly cycle: EducationCycle | null;
  readonly locale: Language;
  readonly avatarUrl: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly state = signal<StudentProfile | null>(null);

  readonly profile = this.state.asReadonly();
  readonly cycle = computed<EducationCycle | null>(() => this.state()?.cycle ?? null);
  readonly isAuthenticated = computed(() => this.state() !== null);
  readonly displayName = computed(() => this.state()?.displayName ?? null);
  readonly gradeLabel = computed(() => this.state()?.gradeLabel ?? null);

  setProfile(profile: StudentProfile | null): void {
    this.state.set(profile);
  }
}
