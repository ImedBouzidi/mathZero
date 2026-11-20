import { Component, input } from '@angular/core';

/** Page-level heading with an optional eyebrow, description and action slot. */
@Component({
  selector: 'app-page-header',
  template: `
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div class="min-w-0">
        @if (eyebrow()) {
          <p class="text-xs font-semibold uppercase tracking-[0.1em] text-accent">
            {{ eyebrow() }}
          </p>
        }
        <h1 class="mt-1.5 text-2xl font-semibold tracking-tight text-ink sm:text-3xl">
          {{ title() }}
        </h1>
        @if (description()) {
          <p class="mt-2 max-w-2xl text-sm leading-6 text-muted">{{ description() }}</p>
        }
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <ng-content />
      </div>
    </header>
  `,
})
export class AppPageHeader {
  readonly eyebrow = input<string>();
  readonly title = input.required<string>();
  readonly description = input<string>();
}
