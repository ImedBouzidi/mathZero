import { Component, input } from '@angular/core';

/** Reusable surface with an optional heading and supporting text. */
@Component({
  selector: 'app-card',
  template: `
    <article class="card">
      @if (title()) {
        <header class="card__header">
          <h2 class="card__title">{{ title() }}</h2>
          @if (subtitle()) {
            <p class="card__subtitle">{{ subtitle() }}</p>
          }
        </header>
      }
      <div class="card__body">
        <ng-content />
      </div>
    </article>
  `,
})
export class AppCard {
  readonly title = input<string>();
  readonly subtitle = input<string>();
}
