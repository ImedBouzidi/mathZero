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
  selector: 'app-basic-progress',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-6 sm:px-6 sm:py-8">
      <app-page-header
        [eyebrow]="i18n.translate('basic.nav.progress')"
        [title]="i18n.translate('basic.progress.title')"
        [description]="i18n.translate('basic.progress.subtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div
        class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1.35fr)_minmax(17rem,0.65fr)] lg:items-start"
      >
        <div class="grid gap-4">
          <app-card [title]="i18n.translate('basic.chapters.title')" heading="h2">
            <app-empty-state
              icon="layers"
              [title]="i18n.translate('basic.progress.empty.title')"
              [body]="i18n.translate('basic.progress.empty.body')"
            >
              <a routerLink="/basic/lessons" appButton variant="primary" size="sm">
                {{ i18n.translate('basic.nav.chapters') }}
                <app-icon name="arrow-right" [size]="16" />
              </a>
            </app-empty-state>
          </app-card>

          <app-card [title]="i18n.translate('basic.objective.title')" heading="h2">
            <app-empty-state
              icon="target"
              [title]="i18n.translate('basic.progress.empty.title')"
              [body]="i18n.translate('basic.progress.empty.body')"
            />
          </app-card>
        </div>

        <aside class="grid gap-4">
          <app-card [title]="i18n.translate('basic.activity.title')" heading="h2">
            <app-empty-state
              icon="bar-chart"
              [title]="i18n.translate('basic.activity.empty')"
              [body]="i18n.translate('basic.activity.hint')"
            />
          </app-card>

          <app-card [title]="i18n.translate('basic.streak.title')" heading="h2">
            <app-empty-state
              icon="flame"
              [title]="i18n.translate('common.noData')"
              [body]="i18n.translate('basic.progress.empty.body')"
            />
          </app-card>
        </aside>
      </div>
    </section>
  `,
})
export class BasicProgress {
  protected readonly i18n = inject(LanguageService);
}
