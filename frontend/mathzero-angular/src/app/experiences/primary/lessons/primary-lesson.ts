import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { AppPageHeader } from '../../../design-system/page-header/app-page-header';
import { CycleChapters } from '../../../shared/ui/cycle-chapters/cycle-chapters';

@Component({
  selector: 'app-primary-lesson',
  imports: [RouterLink, AppButton, AppCard, AppIcon, AppPageHeader, CycleChapters],
  template: `
    <section
      class="primary-lesson-page mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10"
    >
      <app-page-header
        [eyebrow]="i18n.translate('primary.nav.map')"
        [title]="i18n.translate('primary.chapters.title')"
        [description]="i18n.translate('primary.chapters.subtitle')"
      />

      <div class="mt-7 grid gap-5 lg:grid-cols-[minmax(0,1fr)_18rem] lg:items-start">
        <app-card
          class="primary-lesson-page__story"
          [title]="i18n.translate('curriculum.chapters')"
          heading="h2"
        >
          <div class="primary-lesson-page__illustration" aria-hidden="true">
            <span><app-icon name="book-open" [size]="28" /></span>
            <span><app-icon name="sparkles" [size]="19" /></span>
            <span><app-icon name="puzzle" [size]="21" /></span>
          </div>
          <app-cycle-chapters [cycle]="'PRIMARY'" />
        </app-card>

        <aside class="grid gap-5">
          <app-card [title]="i18n.translate('primary.map.title')" heading="h2">
            <p class="text-sm leading-6 text-muted">{{ i18n.translate('primary.map.subtitle') }}</p>
            <a routerLink="/primary" appButton variant="ghost" size="sm" class="mt-4">
              <app-icon name="map" [size]="16" />
              {{ i18n.translate('primary.map.cta') }}
            </a>
          </app-card>

          <app-card [title]="i18n.translate('primary.exercises.title')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('primary.exercises.subtitle') }}
            </p>
            <a routerLink="/primary/exercises" appButton variant="secondary" size="sm" class="mt-4">
              {{ i18n.translate('primary.mission.cta') }}
              <app-icon name="arrow-right" [size]="16" />
            </a>
          </app-card>
        </aside>
      </div>
    </section>
  `,
  styles: `
    .primary-lesson-page__illustration {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1.5rem;
      min-block-size: 8rem;
      overflow: hidden;
      border: 1px solid var(--border-subtle);
      border-radius: calc(var(--card-radius) * 0.8);
      background: var(--bg-subtle);
      color: var(--accent);
    }

    .primary-lesson-page__illustration span {
      display: grid;
      place-items: center;
      inline-size: 3.5rem;
      block-size: 3.5rem;
      border-radius: 1.1rem;
      background: var(--bg-surface);
      box-shadow: var(--card-shadow);
    }

    .primary-lesson-page__illustration span:nth-child(2) {
      align-self: flex-start;
      margin-block-start: 1.25rem;
      color: var(--reward);
    }

    .primary-lesson-page__illustration span:nth-child(3) {
      align-self: flex-end;
      margin-block-end: 1rem;
      color: var(--accent-2);
    }
  `,
})
export class PrimaryLesson {
  protected readonly i18n = inject(LanguageService);
}
