import { Component, computed, input } from '@angular/core';

export type StatTone = 'default' | 'accent' | 'success' | 'warning' | 'muted';

const VALUE_TONE: Record<StatTone, string> = {
  default: 'text-ink',
  accent: 'text-accent',
  success: 'text-success',
  warning: 'text-warning',
  muted: 'text-muted',
};

@Component({
  selector: 'app-stat',
  template: `
    <div class="flex h-full flex-col justify-between gap-3">
      <p class="text-xs font-semibold uppercase tracking-[0.08em] text-muted">{{ label() }}</p>
      <p [class]="valueClasses()">{{ value() }}</p>
      @if (hint()) {
        <p class="text-xs leading-5 text-muted">{{ hint() }}</p>
      }
    </div>
  `,
})
export class AppStat {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string>();
  readonly tone = input<StatTone>('default');

  protected readonly valueClasses = computed(
    () => `font-display text-3xl font-semibold leading-none tracking-tight ${VALUE_TONE[this.tone()]}`,
  );
}
