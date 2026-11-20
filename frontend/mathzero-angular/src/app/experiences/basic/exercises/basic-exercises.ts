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
  selector: 'app-basic-exercises',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="px-4 py-6 sm:px-6 sm:py-8">
      <app-page-header
        [eyebrow]="i18n.translate('basic.nav.exercises')"
        [title]="i18n.translate('basic.exercises.title')"
        [description]="i18n.translate('basic.exercises.subtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div
        class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1.35fr)_minmax(17rem,0.65fr)] lg:items-start"
      >
        <app-card [title]="i18n.translate('basic.exercises.navigator')" heading="h2">
          <app-empty-state
            icon="list-checks"
            [title]="i18n.translate('basic.exercises.empty.title')"
            [body]="i18n.translate('basic.exercises.empty.body')"
          >
            <a routerLink="/basic/lessons" appButton variant="secondary" size="sm">
              {{ i18n.translate('basic.continue.cta') }}
              <app-icon name="arrow-right" [size]="16" />
            </a>
          </app-empty-state>
        </app-card>

        <div class="grid gap-4">
          <app-card [title]="i18n.translate('basic.exercises.hint')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('basic.exercises.empty.body') }}
            </p>
          </app-card>
          <app-card [title]="i18n.translate('basic.lesson.example')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('basic.lesson.empty.body') }}
            </p>
            <a routerLink="/basic/lessons" appButton variant="ghost" size="sm" class="mt-4">
              {{ i18n.translate('basic.nav.chapters') }}
              <app-icon name="arrow-right" [size]="16" />
            </a>
          </app-card>
        </div>
      </div>
    </section>
  `,
})
export class BasicExercises {
  protected readonly i18n = inject(LanguageService);
}
