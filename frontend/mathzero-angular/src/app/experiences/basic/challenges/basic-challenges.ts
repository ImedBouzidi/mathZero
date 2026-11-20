import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppBadge } from '../../../design-system/badge/app-badge';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { AppPageHeader } from '../../../design-system/page-header/app-page-header';

@Component({
  selector: 'app-basic-challenges',
  imports: [
    RouterLink,
    AppBadge,
    AppButton,
    AppCard,
    AppEmptyState,
    AppIcon,
    AppPageHeader,
  ],
  template: `
    <section class="px-4 py-6 sm:px-6 sm:py-8">
      <app-page-header
        [eyebrow]="i18n.translate('basic.nav.challenges')"
        [title]="i18n.translate('basic.challenges.title')"
        [description]="i18n.translate('basic.challenges.subtitle')"
      >
        <app-badge tone="neutral">{{ i18n.translate('common.soon') }}</app-badge>
      </app-page-header>

      <div class="mt-6 grid gap-4 lg:grid-cols-[1.2fr_0.8fr] lg:items-start">
        <app-card>
          <app-empty-state
            icon="trophy"
            [title]="i18n.translate('basic.challenges.empty.title')"
            [body]="i18n.translate('basic.challenges.empty.body')"
          >
            <a routerLink="/basic" appButton variant="ghost" size="sm">
              <app-icon name="arrow-left" [size]="16" />
              {{ i18n.translate('basic.nav.dashboard') }}
            </a>
          </app-empty-state>
        </app-card>

        <app-card [title]="i18n.translate('basic.leaderboard.optIn')" heading="h2">
          <div
            class="flex items-start gap-3 rounded-control border border-line bg-canvas px-4 py-3"
          >
            <span class="grid size-9 flex-none place-items-center rounded-full bg-subtle text-muted">
              <app-icon name="shield" [size]="18" />
            </span>
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('basic.challenges.privacy') }}
            </p>
          </div>
          <button appButton type="button" variant="secondary" size="sm" class="mt-4" disabled>
            {{ i18n.translate('basic.leaderboard.optIn') }}
          </button>
        </app-card>
      </div>
    </section>
  `,
})
export class BasicChallenges {
  protected readonly i18n = inject(LanguageService);
}
