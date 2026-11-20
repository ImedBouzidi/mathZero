import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { AppPageHeader } from '../../../design-system/page-header/app-page-header';

@Component({
  selector: 'app-primary-rewards',
  imports: [RouterLink, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10">
      <app-page-header
        [eyebrow]="i18n.translate('primary.nav.rewards')"
        [title]="i18n.translate('primary.rewards.title')"
        [description]="i18n.translate('primary.rewards.subtitle')"
      />

      <div class="mt-8 grid gap-5 lg:grid-cols-[0.85fr_1.15fr] lg:items-start">
        <app-card [title]="i18n.translate('primary.rewards.jar')" heading="h2">
          <app-empty-state
            icon="star"
            [title]="i18n.translate('common.noData')"
            [body]="i18n.translate('primary.stars.hint')"
          />
        </app-card>

        <app-card [title]="i18n.translate('primary.rewards.badges.title')" heading="h2">
          <app-empty-state
            icon="award"
            [title]="i18n.translate('primary.rewards.empty')"
            [body]="i18n.translate('primary.badges.empty')"
          >
            <a routerLink="/primary" appButton variant="primary" size="sm">
              {{ i18n.translate('primary.map.cta') }}
            </a>
          </app-empty-state>
        </app-card>
      </div>

      <div class="mt-6">
        <a routerLink="/primary" appButton variant="ghost">
          <app-icon name="arrow-left" [size]="16" />
          {{ i18n.translate('primary.map.cta') }}
        </a>
      </div>
    </section>
  `,
})
export class PrimaryRewards {
  protected readonly i18n = inject(LanguageService);
}
