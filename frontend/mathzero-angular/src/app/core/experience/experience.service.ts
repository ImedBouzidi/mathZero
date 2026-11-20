import { DOCUMENT, inject, Injectable, signal } from '@angular/core';

import { EducationCycle, EXPERIENCES, ExperienceDefinition } from './education-cycle';

/**
 * Applies the active experience theme to the document.
 *
 * The same attribute drives body backgrounds and body-attached overlays, while
 * the shell also exposes it locally. It is removed when leaving an experience so
 * account and brand pages fall back to the neutral brand tokens.
 */
@Injectable({ providedIn: 'root' })
export class ExperienceService {
  private readonly document = inject(DOCUMENT);
  private readonly activeState = signal<EducationCycle | null>(null);

  readonly active = this.activeState.asReadonly();

  activate(cycle: EducationCycle): void {
    this.activeState.set(cycle);
    this.document.documentElement.dataset['experience'] = cycle;
  }

  deactivate(): void {
    this.activeState.set(null);
    delete this.document.documentElement.dataset['experience'];
  }

  definition(cycle: EducationCycle): ExperienceDefinition {
    return EXPERIENCES[cycle];
  }

  isActive(cycle: EducationCycle): boolean {
    return this.activeState() === cycle;
  }
}
