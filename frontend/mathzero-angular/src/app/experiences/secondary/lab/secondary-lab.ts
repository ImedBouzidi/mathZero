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
  selector: 'app-secondary-lab',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon, AppPageHeader],
  template: `
    <section
      class="secondary-lab mx-auto w-full max-w-[var(--content-max)] px-4 py-8 sm:px-6 sm:py-10"
    >
      <app-page-header
        [eyebrow]="i18n.translate('secondary.nav.lab')"
        [title]="i18n.translate('secondary.lab.title')"
        [description]="i18n.translate('secondary.lab.quality')"
      >
        <app-badge tone="accent">{{ i18n.translate('common.preview') }}</app-badge>
      </app-page-header>

      <div class="secondary-lab__workspace mt-7">
        <app-card
          class="secondary-lab__canvas"
          [title]="i18n.translate('secondary.lab.function')"
          heading="h2"
        >
          <div class="secondary-lab__canvas-inner">
            <app-empty-state
              icon="flask"
              [title]="i18n.translate('secondary.lab.empty.title')"
              [body]="i18n.translate('secondary.lab.empty.body')"
            >
              <a routerLink="/secondary/chapters" appButton variant="secondary" size="sm">
                {{ i18n.translate('secondary.nav.chapters') }}
                <app-icon name="arrow-right" [size]="16" />
              </a>
            </app-empty-state>
          </div>
        </app-card>

        <aside class="grid gap-4">
          <app-card [title]="i18n.translate('secondary.lab.parameters')" heading="h2">
            <app-empty-state
              icon="settings"
              [title]="i18n.translate('common.notAvailable')"
              [body]="i18n.translate('secondary.lab.note')"
            />
          </app-card>

          <app-card [title]="i18n.translate('secondary.lab.readout')" heading="h2">
            <p class="text-sm leading-6 text-muted">
              {{ i18n.translate('secondary.lab.empty.body') }}
            </p>
          </app-card>
        </aside>
      </div>
    </section>
  `,
  styles: `
    .secondary-lab__workspace,
    .secondary-lab__workspace aside {
      display: grid;
      gap: 1rem;
    }

    .secondary-lab__canvas-inner {
      display: grid;
      min-block-size: 18rem;
      place-items: center;
      border: 1px solid var(--border-subtle);
      background-color: var(--bg-canvas);
      background-image:
        linear-gradient(var(--border-subtle) 1px, transparent 1px),
        linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
      background-size: 2rem 2rem;
    }

    .secondary-lab__canvas-inner app-empty-state {
      inline-size: min(100% - 2rem, 34rem);
      border: 1px solid var(--border-subtle);
      background: var(--bg-surface);
    }

    @media (min-width: 900px) {
      .secondary-lab__workspace {
        grid-template-columns: minmax(0, 1.65fr) minmax(17rem, 0.65fr);
        align-items: start;
        gap: 1.25rem;
      }

      .secondary-lab__canvas-inner {
        min-block-size: 30rem;
      }
    }
  `,
})
export class SecondaryLab {
  protected readonly i18n = inject(LanguageService);
}
