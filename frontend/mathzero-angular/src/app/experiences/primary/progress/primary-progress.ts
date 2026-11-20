import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppPageHeader } from '../../../design-system/page-header/app-page-header';

@Component({
  selector: 'app-primary-progress',
  imports: [RouterLink, AppButton, AppCard, AppEmptyState, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10">
      <app-page-header
        [eyebrow]="i18n.translate('primary.progress.pageTitle')"
        [title]="i18n.translate('primary.progress.pageTitle')"
        [description]="i18n.translate('primary.progress.body')"
      />

      <div class="mt-7 grid gap-5 lg:grid-cols-2">
        <app-card [title]="i18n.translate('primary.progress.stars')" heading="h2">
          <app-empty-state
            icon="star"
            [title]="i18n.translate('common.noData')"
            [body]="i18n.translate('primary.stars.hint')"
          />
        </app-card>

        <app-card [title]="i18n.translate('primary.progress.streak')" heading="h2">
          <app-empty-state
            icon="flame"
            [title]="i18n.translate('common.noData')"
            [body]="i18n.translate('primary.progress.empty')"
          />
        </app-card>

        <app-card [title]="i18n.translate('primary.progress.weekly')" heading="h2">
          <app-empty-state
            icon="map"
            [title]="i18n.translate('primary.progress.empty')"
            [body]="i18n.translate('primary.progress.body')"
          >
            <a routerLink="/primary" appButton variant="primary" size="sm">
              {{ i18n.translate('primary.map.cta') }}
            </a>
          </app-empty-state>
        </app-card>

        <app-card [title]="i18n.translate('primary.badges.title')" heading="h2">
          <app-empty-state
            icon="award"
            [title]="i18n.translate('primary.badges.empty')"
            [body]="i18n.translate('primary.progress.empty')"
          >
            <a routerLink="/primary/rewards" appButton variant="secondary" size="sm">
              {{ i18n.translate('primary.nav.rewards') }}
            </a>
          </app-empty-state>
        </app-card>
      </div>
    </section>
  `,
})
export class PrimaryProgress {
  protected readonly i18n = inject(LanguageService);
}
