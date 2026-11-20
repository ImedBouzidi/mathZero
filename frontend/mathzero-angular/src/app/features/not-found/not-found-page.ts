import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../shared/ui/button/app-button';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink, AppButton],
  template: `
    <section class="mx-auto w-full max-w-3xl px-4 py-24 text-center sm:px-6">
      <p class="font-mono text-sm text-violet-400" dir="ltr">404</p>
      <h1 class="mt-4 text-3xl font-bold tracking-tight text-mist-50 sm:text-4xl">
        {{ i18n.translate('notFound.title') }}
      </h1>
      <p class="mt-3 text-mist-300">{{ i18n.translate('notFound.body') }}</p>
      <a routerLink="/" appButton variant="primary" class="mt-8">
        {{ i18n.translate('notFound.home') }}
      </a>
    </section>
  `,
})
export class NotFoundPage {
  protected readonly i18n = inject(LanguageService);
}
