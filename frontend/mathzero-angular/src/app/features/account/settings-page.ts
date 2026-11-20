import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { EXPERIENCE_LIST } from '../../core/experience/education-cycle';
import { LanguageService } from '../../core/i18n/language.service';
import { Language } from '../../core/i18n/translations';
import { AppButton } from '../../design-system/button/app-button';
import { AppCard } from '../../design-system/card/app-card';
import { AppIcon } from '../../design-system/icon/app-icon';
import { AppPageHeader } from '../../design-system/page-header/app-page-header';

@Component({
  selector: 'app-settings-page',
  imports: [RouterLink, AppButton, AppCard, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-5xl px-4 py-12 sm:px-6 sm:py-16">
      <app-page-header
        [eyebrow]="i18n.translate('account.settings.title')"
        [title]="i18n.translate('account.settings.title')"
        [description]="i18n.translate('account.settings.subtitle')"
      />

      <div class="mt-8 grid gap-5 md:grid-cols-2">
        <app-card [title]="i18n.translate('account.settings.language.title')" heading="h2">
          <p class="text-sm leading-6 text-muted">
            {{ i18n.translate('account.settings.language.body') }}
          </p>
          <fieldset class="mt-4">
            <legend class="sr-only">{{ i18n.translate('account.settings.language.title') }}</legend>
            <div class="grid gap-2">
              @for (language of languages; track language.code) {
                <label
                  class="flex cursor-pointer items-center justify-between gap-3 rounded-control border border-line bg-canvas px-4 py-3 text-sm font-medium text-ink transition-colors has-checked:border-accent has-checked:bg-accent-soft"
                >
                  <span>{{ i18n.translate(language.labelKey) }}</span>
                  <input
                    type="radio"
                    name="language"
                    class="size-4 accent-[var(--accent)]"
                    [value]="language.code"
                    [checked]="i18n.language() === language.code"
                    (change)="selectLanguage(language.code)"
                  />
                </label>
              }
            </div>
          </fieldset>
        </app-card>

        <app-card [title]="i18n.translate('account.settings.motion.title')" heading="h2">
          <p class="text-sm leading-6 text-muted">
            {{ i18n.translate('account.settings.motion.body') }}
          </p>
          <div class="mt-4 flex items-center gap-3 rounded-control border border-line bg-canvas px-4 py-3">
            <span class="grid size-9 flex-none place-items-center rounded-full bg-subtle text-accent">
              <app-icon name="sparkles" [size]="18" />
            </span>
            <p class="text-sm font-medium text-ink">
              {{ i18n.translate('account.settings.motion.title') }}
            </p>
          </div>
        </app-card>
      </div>

      <div class="mt-5">
        <app-card [title]="i18n.translate('account.settings.experience.title')" heading="h2">
          <p class="text-sm leading-6 text-muted">
            {{ i18n.translate('account.settings.experience.body') }}
          </p>
          <div class="mt-4 grid gap-3 sm:grid-cols-3">
            @for (experience of experiences; track experience.id) {
              <a
                [routerLink]="experience.basePath"
                class="focus-ring interactive-card rounded-surface border border-line bg-canvas p-4"
              >
                <p class="text-xs font-semibold uppercase tracking-[0.08em] text-accent">
                  {{ i18n.translate(experience.taglineKey) }}
                </p>
                <p class="mt-1 text-sm font-semibold text-ink">
                  {{ i18n.translate(experience.nameKey) }}
                </p>
              </a>
            }
          </div>
        </app-card>
      </div>

      <div class="mt-6 flex flex-wrap gap-3">
        <a routerLink="/profile" appButton variant="secondary">
          <app-icon name="user" [size]="16" />
          {{ i18n.translate('common.profile') }}
        </a>
        <a routerLink="/" appButton variant="ghost">{{ i18n.translate('common.home') }}</a>
      </div>
    </section>
  `,
})
export class SettingsPage {
  protected readonly i18n = inject(LanguageService);
  protected readonly experiences = EXPERIENCE_LIST;
  protected readonly languages: readonly { code: Language; labelKey: 'language.fr' | 'language.ar' }[] = [
    { code: 'fr', labelKey: 'language.fr' },
    { code: 'ar', labelKey: 'language.ar' },
  ];

  protected selectLanguage(language: Language): void {
    this.i18n.setLanguage(language);
  }
}
