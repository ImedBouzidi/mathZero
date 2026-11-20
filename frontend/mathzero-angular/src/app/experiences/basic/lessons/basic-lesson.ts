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
  selector: 'app-basic-lesson',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="px-4 py-6 sm:px-6 sm:py-8">
      <app-page-header
        [eyebrow]="i18n.translate('basic.nav.chapters')"
        [title]="i18n.translate('basic.lesson.title')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div
        class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1.35fr)_minmax(17rem,0.65fr)] lg:items-start"
      >
        <app-card
          class="basic-reader"
          [title]="i18n.translate('basic.lesson.example')"
          heading="h2"
        >
          <div class="basic-reader__toolbar" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="mt-5">
            <app-empty-state
              icon="file-text"
              [title]="i18n.translate('basic.lesson.empty.title')"
              [body]="i18n.translate('basic.lesson.empty.body')"
            />
          </div>
        </app-card>

        <aside class="grid gap-4">
          <app-card [title]="i18n.translate('basic.lesson.objectives')" heading="h2">
            <app-empty-state
              icon="list-checks"
              [title]="i18n.translate('basic.lesson.empty.title')"
              [body]="i18n.translate('basic.lessons.empty.body')"
            />
          </app-card>

          <app-card [title]="i18n.translate('basic.lesson.check')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('basic.exercises.empty.body') }}
            </p>
            <div class="mt-4 border-t border-line pt-4">
              <a routerLink="/basic/exercises" appButton variant="ghost" size="sm">
                {{ i18n.translate('basic.lesson.next') }}
                <app-icon name="arrow-right" [size]="16" />
              </a>
            </div>
          </app-card>
        </aside>
      </div>
    </section>
  `,
  styles: `
    .basic-reader__toolbar {
      display: flex;
      gap: 0.4rem;
      border-block-end: 1px solid var(--border-subtle);
      padding-block-end: 0.75rem;
    }

    .basic-reader__toolbar span {
      inline-size: 0.5rem;
      block-size: 0.5rem;
      border-radius: 999px;
      background: var(--border-strong);
    }
  `,
})
export class BasicLesson {
  protected readonly i18n = inject(LanguageService);
}
