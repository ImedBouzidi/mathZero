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
  selector: 'app-secondary-revision',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10">
      <app-page-header
        [eyebrow]="i18n.translate('secondary.nav.revision')"
        [title]="i18n.translate('secondary.revision.pageTitle')"
        [description]="i18n.translate('secondary.revision.pageSubtitle')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div class="mt-6 grid gap-4">
        <app-card [title]="i18n.translate('secondary.weak.title')" heading="h2">
          <app-empty-state
            icon="alert-triangle"
            [title]="i18n.translate('secondary.weak.empty.title')"
            [body]="i18n.translate('secondary.weak.empty.body')"
          />
        </app-card>

        <app-card [title]="i18n.translate('secondary.revision.empty.title2')" heading="h2">
          <app-empty-state
            icon="rotate-3d"
            [title]="i18n.translate('secondary.revision.empty.title2')"
            [body]="i18n.translate('secondary.revision.empty.body2')"
          >
            <a routerLink="/secondary/chapters" appButton variant="primary" size="sm">
              {{ i18n.translate('secondary.nav.chapters') }}
              <app-icon name="arrow-right" [size]="16" />
            </a>
          </app-empty-state>
        </app-card>
      </div>
    </section>
  `,
})
export class SecondaryRevision {
  protected readonly i18n = inject(LanguageService);
}
