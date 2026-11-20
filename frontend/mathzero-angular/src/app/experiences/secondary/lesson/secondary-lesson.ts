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
  selector: 'app-secondary-lesson',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section
      class="secondary-reader mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10"
    >
      <app-page-header
        [eyebrow]="i18n.translate('secondary.nav.chapters')"
        [title]="i18n.translate('secondary.lesson.title')"
        [description]="i18n.translate('secondary.lesson.blockNote')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div class="secondary-reader__layout mt-7">
        <aside class="grid gap-4">
          <app-card [title]="i18n.translate('secondary.lesson.outline')" heading="h2">
            <app-empty-state
              icon="list-checks"
              [title]="i18n.translate('common.noData')"
              [body]="i18n.translate('secondary.chapters.empty.body')"
            />
          </app-card>

          <app-card [title]="i18n.translate('secondary.lesson.notes')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('secondary.lesson.blockNote') }}
            </p>
          </app-card>
        </aside>

        <app-card
          class="secondary-reader__content"
          [title]="i18n.translate('secondary.lesson.definition')"
          heading="h2"
        >
          <div class="secondary-reader__rule" aria-hidden="true"></div>
          <app-empty-state
            icon="file-text"
            [title]="i18n.translate('secondary.lesson.empty.title')"
            [body]="i18n.translate('secondary.lesson.empty.body')"
          >
            <a routerLink="/secondary/chapters" appButton variant="secondary" size="sm">
              <app-icon name="arrow-left" [size]="16" />
              {{ i18n.translate('secondary.nav.chapters') }}
            </a>
          </app-empty-state>
        </app-card>

        <app-card
          class="secondary-reader__lab"
          [title]="i18n.translate('secondary.lesson.exploreLab')"
          heading="h2"
        >
          <p class="text-sm leading-6 text-muted">{{ i18n.translate('secondary.lab.note') }}</p>
          <a routerLink="/secondary/lab" appButton variant="ghost" size="sm" class="mt-4">
            {{ i18n.translate('secondary.lab.open') }}
            <app-icon name="arrow-right" [size]="16" />
          </a>
        </app-card>
      </div>
    </section>
  `,
  styles: `
    .secondary-reader__layout,
    .secondary-reader__layout aside {
      display: grid;
      gap: 1rem;
    }

    .secondary-reader__rule {
      block-size: 1px;
      background: var(--border-subtle);
    }

    @media (min-width: 980px) {
      .secondary-reader__layout {
        grid-template-columns: minmax(15rem, 0.6fr) minmax(0, 1.4fr);
        align-items: start;
        gap: 1.25rem;
      }

      .secondary-reader__layout aside {
        grid-row: span 2;
      }
    }
  `,
})
export class SecondaryLesson {
  protected readonly i18n = inject(LanguageService);
}
