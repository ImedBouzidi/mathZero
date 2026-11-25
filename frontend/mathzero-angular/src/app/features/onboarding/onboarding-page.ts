import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { EducationCycle, EXPERIENCE_LIST, experienceHome } from '../../core/experience/education-cycle';
import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppIcon } from '../../design-system/icon/app-icon';

@Component({
  selector: 'app-onboarding-page',
  imports: [AppButton, AppIcon],
  template: `
    <section class="mx-auto w-full max-w-3xl px-4 py-14 sm:px-6 sm:py-20">
      <p class="text-xs font-semibold uppercase tracking-[0.1em] text-accent">
        {{ i18n.translate('onboarding.eyebrow') }}
      </p>
      <h1 class="mt-2 text-3xl font-semibold tracking-tight text-ink sm:text-4xl">
        {{ i18n.translate('onboarding.title') }}
      </h1>
      <p class="mt-4 text-base leading-7 text-muted">
        {{ i18n.translate('onboarding.body') }}
      </p>

      <div class="mt-8 grid gap-3 sm:grid-cols-3">
        @for (experience of experiences; track experience.id) {
          <button
            type="button"
            appButton
            variant="secondary"
            class="h-full flex-col items-start text-start"
            (click)="select(experience.id)"
          >
            {{ i18n.translate(experience.nameKey) }}
          </button>
        }
      </div>
      @if (saving()) {
        <p class="mt-4 text-sm text-muted">Enregistrement…</p>
      }
      @if (error()) {
        <p class="mt-4 text-sm text-red-600" role="alert">{{ error() }}</p>
      }

      <p class="mt-5 flex items-start gap-2 text-xs leading-5 text-muted">
        <app-icon name="info" [size]="16" class="mt-0.5 flex-none" />
        <span>{{ i18n.translate('onboarding.note') }}</span>
      </p>
    </section>
  `,
})
export class OnboardingPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly i18n = inject(LanguageService);
  protected readonly experiences = EXPERIENCE_LIST;
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected select(cycle: EducationCycle): void {
    this.saving.set(true);
    this.error.set('');
    this.auth.updateProfile({ cycle }).subscribe({
      next: () => void this.router.navigateByUrl(experienceHome(cycle)),
      error: (error: { error?: { message?: string } }) =>
        this.error.set(error.error?.message || 'Impossible d’enregistrer le parcours'),
      complete: () => this.saving.set(false),
    });
  }
}
