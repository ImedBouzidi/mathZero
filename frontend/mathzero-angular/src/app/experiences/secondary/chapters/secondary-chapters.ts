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
  selector: 'app-secondary-chapters',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10">
      <app-page-header
        [eyebrow]="i18n.translate('secondary.nav.chapters')"
        [title]="i18n.translate('secondary.chapters.title')"
        [description]="i18n.translate('secondary.chapters.subtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div class="mt-6 grid gap-4">
        <app-card>
          <app-empty-state
            icon="book-open"
            [title]="i18n.translate('secondary.chapters.empty.title')"
            [body]="i18n.translate('secondary.chapters.empty.body')"
          >
            <a routerLink="/secondary" appButton variant="ghost" size="sm">
              <app-icon name="arrow-left" [size]="16" />
              {{ i18n.translate('common.back') }}
            </a>
          </app-empty-state>
        </app-card>

        <app-card [title]="i18n.translate('secondary.lesson.title')" heading="h2">
          <div class="p-4 rounded-surface border border-dashed border-line-strong bg-canvas/50">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('secondary.lesson.empty.body') }}
            </p>
            <a routerLink="/secondary/lab" appButton variant="ghost" size="sm" class="mt-3">
              {{ i18n.translate('secondary.lesson.exploreLab') }}
              <app-icon name="sparkles" [size]="16" />
            </a>
          </div>
        </app-card>
      </div>
    </section>
  `,
})
export class SecondaryChapters {
  protected readonly i18n = inject(LanguageService);
}
