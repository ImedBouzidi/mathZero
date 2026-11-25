import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppBadge } from '../../../design-system/badge/app-badge';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { AppPageHeader } from '../../../design-system/page-header/app-page-header';
import { LevelResources } from '../../../shared/ui/level-resources/level-resources';

@Component({
  selector: 'app-secondary-dashboard',
  imports: [
    RouterLink,
    AppBadge,
    AppButton,
    AppCard,
    AppEmptyState,
    AppIcon,
    AppPageHeader,
    LevelResources,
  ],
  template: `
    <section
      class="secondary-overview mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10"
    >
      <app-page-header
        [eyebrow]="i18n.translate('secondary.nav.overview')"
        [title]="i18n.translate('secondary.workspace.title')"
        [description]="i18n.translate('secondary.welcome')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div class="secondary-overview__grid mt-7">
        <div class="secondary-overview__primary">
          <div class="secondary-overview__summary">
            <app-card [title]="i18n.translate('secondary.mastery.title')" heading="h2">
              <app-empty-state
                icon="bar-chart"
                [title]="i18n.translate('common.noData')"
                [body]="i18n.translate('secondary.mastery.empty')"
              />
            </app-card>

            <app-card [title]="i18n.translate('secondary.readiness.title')" heading="h2">
              <app-empty-state
                icon="target"
                [title]="i18n.translate('common.noData')"
                [body]="i18n.translate('secondary.readiness.empty')"
              >
                <a routerLink="/secondary/exams" appButton variant="secondary" size="sm">
                  {{ i18n.translate('secondary.nav.exams') }}
                  <app-icon name="arrow-right" [size]="16" />
                </a>
              </app-empty-state>
            </app-card>
          </div>

          <app-card
            class="secondary-overview__matrix"
            [title]="i18n.translate('secondary.matrix.title')"
            heading="h2"
          >
            <app-empty-state
              icon="grid"
              [title]="i18n.translate('common.noData')"
              [body]="i18n.translate('secondary.matrix.empty')"
            >
              <a routerLink="/secondary/chapters" appButton variant="ghost" size="sm">
                {{ i18n.translate('secondary.nav.chapters') }}
                <app-icon name="arrow-right" [size]="16" />
              </a>
            </app-empty-state>
          </app-card>
        </div>

        <div class="secondary-overview__aside">
          <app-card [title]="i18n.translate('secondary.weak.title')" heading="h2">
            <app-empty-state
              icon="search"
              [title]="i18n.translate('common.noData')"
              [body]="i18n.translate('secondary.weak.empty.body')"
            />
          </app-card>

          <app-card [title]="i18n.translate('secondary.revision.title')" heading="h2">
            <app-empty-state
              icon="rotate-3d"
              [title]="i18n.translate('secondary.revision.empty.title')"
              [body]="i18n.translate('secondary.revision.empty.body')"
            >
              <a routerLink="/secondary/revision" appButton variant="secondary" size="sm">
                {{ i18n.translate('secondary.revision.cta') }}
              </a>
            </app-empty-state>
          </app-card>

          <app-card [title]="i18n.translate('secondary.recommended.title')" heading="h2">
            <app-empty-state
              icon="sparkles"
              [title]="i18n.translate('common.noData')"
              [body]="i18n.translate('secondary.recommended.empty')"
            >
              <a routerLink="/secondary/exams" appButton variant="ghost" size="sm">
                {{ i18n.translate('secondary.nav.exams') }}
                <app-icon name="arrow-right" [size]="16" />
              </a>
            </app-empty-state>
          </app-card>

          <app-card [title]="i18n.translate('resources.forLevel')" heading="h2">
            <app-level-resources [cycle]="'SECONDARY'" />
          </app-card>
        </div>
      </div>
    </section>
  `,
  styleUrl: './secondary-dashboard.scss',
})
export class SecondaryDashboard {
  protected readonly i18n = inject(LanguageService);
}
