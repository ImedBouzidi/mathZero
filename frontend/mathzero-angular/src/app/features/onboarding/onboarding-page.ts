import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppEmptyState } from '../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../design-system/icon/app-icon';

@Component({
  selector: 'app-onboarding-page',
  imports: [RouterLink, AppButton, AppEmptyState, AppIcon],
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

      <div class="mt-8 rounded-surface border border-line bg-surface shadow-panel">
        <app-empty-state
          icon="graduation-cap"
          [title]="i18n.translate('onboarding.empty.title')"
          [body]="i18n.translate('onboarding.empty.body')"
        >
          <a routerLink="/" appButton variant="primary">
            {{ i18n.translate('onboarding.explore') }}
            <app-icon name="arrow-right" [size]="16" />
          </a>
        </app-empty-state>
      </div>

      <p class="mt-5 flex items-start gap-2 text-xs leading-5 text-muted">
        <app-icon name="info" [size]="16" class="mt-0.5 flex-none" />
        <span>{{ i18n.translate('onboarding.note') }}</span>
      </p>
    </section>
  `,
})
export class OnboardingPage {
  protected readonly i18n = inject(LanguageService);
}
