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
  selector: 'app-secondary-exams',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10">
      <app-page-header
        [eyebrow]="i18n.translate('secondary.nav.exams')"
        [title]="i18n.translate('secondary.exams.title')"
        [description]="i18n.translate('secondary.exams.subtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div
        class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1.3fr)_minmax(17rem,0.7fr)] lg:items-start"
      >
        <app-card [title]="i18n.translate('secondary.exams.bacBlanc')" heading="h2">
          <app-empty-state
            icon="file-text"
            [title]="i18n.translate('secondary.exams.empty.title')"
            [body]="i18n.translate('secondary.exams.empty.body')"
          >
            <a routerLink="/secondary" appButton variant="ghost" size="sm">
              <app-icon name="arrow-left" [size]="16" />
              {{ i18n.translate('common.back') }}
            </a>
          </app-empty-state>
        </app-card>

        <app-card [title]="i18n.translate('secondary.nav.revision')" heading="h2">
          <p class="text-sm leading-6 text-muted">
            {{ i18n.translate('secondary.revision.pageSubtitle') }}
          </p>
          <a routerLink="/secondary/revision" appButton variant="secondary" size="sm" class="mt-4">
            {{ i18n.translate('secondary.revision.cta') }}
            <app-icon name="arrow-right" [size]="16" />
          </a>
        </app-card>
      </div>
    </section>
  `,
})
export class SecondaryExams {
  protected readonly i18n = inject(LanguageService);
}
