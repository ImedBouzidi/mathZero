import { Component, input } from '@angular/core';

export type CardHeading = 'h2' | 'h3' | 'h4';

/** Reusable surface with an optional heading and supporting text. */
@Component({
  selector: 'app-card',
  template: `
    <article
      class="flex h-full flex-col overflow-hidden rounded-surface border border-line bg-surface shadow-panel"
    >
      @if (title()) {
        <header class="px-5 pt-5 sm:px-6 sm:pt-6">
          @switch (heading()) {
            @case ('h3') {
              <h3 class="text-base font-semibold tracking-tight text-ink">{{ title() }}</h3>
            }
            @case ('h4') {
              <h4 class="text-sm font-semibold tracking-tight text-ink">{{ title() }}</h4>
            }
            @default {
              <h2 class="text-base font-semibold tracking-tight text-ink">{{ title() }}</h2>
            }
          }
          @if (subtitle()) {
            <p class="mt-1.5 text-sm leading-6 text-muted">{{ subtitle() }}</p>
          }
        </header>
      }
      <div class="flex-1 p-5 sm:p-6">
        <ng-content />
      </div>
    </article>
  `,
})
export class AppCard {
  readonly title = input<string>();
  readonly subtitle = input<string>();
  readonly heading = input<CardHeading>('h2');
}
