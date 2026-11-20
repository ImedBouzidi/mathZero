import { Component, inject } from '@angular/core';

import { LanguageService } from '../../core/i18n/language.service';
import { Language } from '../../core/i18n/translations';

/** Compact bilingual switch used by the brand shell and every experience. */
@Component({
  selector: 'app-language-switch',
  template: `
    <div
      class="inline-flex items-center gap-0.5 rounded-control border border-line bg-surface p-0.5"
      role="group"
      [attr.aria-label]="i18n.translate('language.label')"
    >
      <button
        type="button"
        class="min-h-8 cursor-pointer rounded-[calc(var(--control-radius)-0.15rem)] px-2.5 text-xs font-bold transition-colors"
        [class.bg-accent]="i18n.language() === 'fr'"
        [class.text-accent-contrast]="i18n.language() === 'fr'"
        [class.text-muted]="i18n.language() !== 'fr'"
        [attr.aria-pressed]="i18n.language() === 'fr'"
        [attr.aria-label]="i18n.translate('language.fr')"
        (click)="select('fr')"
      >
        FR
      </button>
      <button
        type="button"
        class="min-h-8 cursor-pointer rounded-[calc(var(--control-radius)-0.15rem)] px-2.5 text-xs font-bold transition-colors"
        [class.bg-accent]="i18n.language() === 'ar'"
        [class.text-accent-contrast]="i18n.language() === 'ar'"
        [class.text-muted]="i18n.language() !== 'ar'"
        [attr.aria-pressed]="i18n.language() === 'ar'"
        [attr.aria-label]="i18n.translate('language.ar')"
        (click)="select('ar')"
      >
        ع
      </button>
    </div>
  `,
})
export class LanguageSwitch {
  protected readonly i18n = inject(LanguageService);

  protected select(language: Language): void {
    this.i18n.setLanguage(language);
  }
}
