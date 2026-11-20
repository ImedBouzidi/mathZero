import { Component, input } from '@angular/core';

import { AppIcon } from '../icon/app-icon';
import { IconName } from '../icon/icon-paths';

/** Honest empty state used until a service supplies real content. */
@Component({
  selector: 'app-empty-state',
  imports: [AppIcon],
  template: `
    <div class="flex flex-col items-center justify-center px-4 py-8 text-center sm:py-10">
      <span class="grid size-14 place-items-center rounded-full bg-subtle text-accent">
        <app-icon [name]="icon()" [size]="26" />
      </span>
      <h3 class="mt-4 text-base font-semibold text-ink">{{ title() }}</h3>
      <p class="mt-2 max-w-md text-sm leading-6 text-muted">{{ body() }}</p>
      <div class="mt-5">
        <ng-content />
      </div>
    </div>
  `,
})
export class AppEmptyState {
  readonly icon = input<IconName>('info');
  readonly title = input.required<string>();
  readonly body = input.required<string>();
}
