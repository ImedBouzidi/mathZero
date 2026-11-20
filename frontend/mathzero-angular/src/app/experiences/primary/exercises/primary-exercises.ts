import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { TranslationKey } from '../../../core/i18n/translations';
import { AppBadge } from '../../../design-system/badge/app-badge';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { IconName } from '../../../design-system/icon/icon-paths';
import { AppPageHeader } from '../../../design-system/page-header/app-page-header';

interface ActivityType {
  readonly icon: IconName;
  readonly labelKey: TranslationKey;
}

@Component({
  selector: 'app-primary-exercises',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10">
      <app-page-header
        [eyebrow]="i18n.translate('primary.nav.missions')"
        [title]="i18n.translate('primary.exercises.title')"
        [description]="i18n.translate('primary.exercises.subtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div class="mt-8 grid gap-5 lg:grid-cols-[1.15fr_0.85fr] lg:items-start">
        <app-card [title]="i18n.translate('primary.exercises.types.title')" heading="h2">
          <ul class="grid gap-3 sm:grid-cols-2">
            @for (activity of activityTypes; track activity.labelKey) {
              <li
                class="flex items-center gap-3 rounded-surface border border-line bg-canvas p-4"
              >
                <span class="grid size-11 flex-none place-items-center rounded-control bg-accent-soft text-accent">
                  <app-icon [name]="activity.icon" [size]="21" />
                </span>
                <span class="text-sm font-semibold text-ink">
                  {{ i18n.translate(activity.labelKey) }}
                </span>
              </li>
            }
          </ul>

          <p class="mt-4 flex items-start gap-2 text-xs leading-5 text-muted">
            <app-icon name="info" [size]="16" class="mt-0.5 flex-none" />
            <span>{{ i18n.translate('common.previewNotice') }}</span>
          </p>
        </app-card>

        <app-card>
          <app-empty-state
            icon="target"
            [title]="i18n.translate('primary.exercises.empty.title')"
            [body]="i18n.translate('primary.exercises.empty.body')"
          >
            <a routerLink="/primary" appButton variant="primary" size="sm">
              {{ i18n.translate('primary.mission.cta') }}
            </a>
          </app-empty-state>
        </app-card>
      </div>
    </section>
  `,
})
export class PrimaryExercises {
  protected readonly i18n = inject(LanguageService);

  protected readonly activityTypes: readonly ActivityType[] = [
    { icon: 'grid', labelKey: 'primary.exercises.type.count' },
    { icon: 'hand', labelKey: 'primary.exercises.type.drag' },
    { icon: 'puzzle', labelKey: 'primary.exercises.type.match' },
    { icon: 'move', labelKey: 'primary.exercises.type.line' },
    { icon: 'list-checks', labelKey: 'primary.exercises.type.choice' },
  ];
}
