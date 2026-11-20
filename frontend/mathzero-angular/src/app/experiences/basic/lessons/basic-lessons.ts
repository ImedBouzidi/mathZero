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
  selector: 'app-basic-lessons',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="px-4 py-6 sm:px-6 sm:py-8">
      <app-page-header
        [eyebrow]="i18n.translate('basic.nav.chapters')"
        [title]="i18n.translate('basic.lessons.title')"
        [description]="i18n.translate('basic.lessons.subtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div
        class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1.4fr)_minmax(16rem,0.6fr)] lg:items-start"
      >
        <app-card [title]="i18n.translate('basic.chapters.title')" heading="h2">
          <app-empty-state
            icon="book-open"
            [title]="i18n.translate('basic.lessons.empty.title')"
            [body]="i18n.translate('basic.lessons.empty.body')"
          >
            <a routerLink="/basic" appButton variant="ghost" size="sm">
              <app-icon name="arrow-left" [size]="16" />
              {{ i18n.translate('common.back') }}
            </a>
          </app-empty-state>
        </app-card>

        <div class="grid gap-4">
          <app-card [title]="i18n.translate('basic.exercises.title')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('basic.exercises.subtitle') }}
            </p>
            <a routerLink="/basic/exercises" appButton variant="secondary" size="sm" class="mt-4">
              {{ i18n.translate('basic.nav.exercises') }}
              <app-icon name="arrow-right" [size]="16" />
            </a>
          </app-card>

          <app-card [title]="i18n.translate('basic.progress.title')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('basic.progress.empty.body') }}
            </p>
            <a routerLink="/basic/progress" appButton variant="ghost" size="sm" class="mt-4">
              {{ i18n.translate('basic.nav.progress') }}
              <app-icon name="arrow-right" [size]="16" />
            </a>
          </app-card>
        </div>
      </div>
    </section>
  `,
})
export class BasicLessons {
  protected readonly i18n = inject(LanguageService);
}
