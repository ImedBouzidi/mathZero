import { Component, computed, input } from '@angular/core';

export type ProgressTone = 'accent' | 'success' | 'warning' | 'reward' | 'second';

const FILL_CLASSES: Record<ProgressTone, string> = {
  accent: 'bg-accent',
  success: 'bg-success',
  warning: 'bg-warning',
  reward: 'bg-reward',
  second: 'bg-second',
};

@Component({
  selector: 'app-progress-bar',
  template: `
    @if (label()) {
      <div class="mb-2 flex items-baseline justify-between gap-3">
        <span class="text-sm font-medium text-ink">{{ label() }}</span>
        @if (showValue()) {
          <span class="text-xs font-semibold tabular-nums text-muted">{{ normalized() }} %</span>
        }
      </div>
    }
    <div
      class="h-2 overflow-hidden rounded-pill bg-subtle"
      role="progressbar"
      [attr.aria-label]="label() || ariaLabel()"
      aria-valuemin="0"
      aria-valuemax="100"
      [attr.aria-valuenow]="normalized()"
    >
      <div
        [class]="'h-full rounded-pill transition-[width] duration-300 ' + fillClass()"
        [style.width.%]="normalized()"
      ></div>
    </div>
  `,
})
export class AppProgressBar {
  readonly value = input(0);
  readonly label = input<string>();
  readonly ariaLabel = input('Progression');
  readonly tone = input<ProgressTone>('accent');
  readonly showValue = input(true);

  protected readonly normalized = computed(() =>
    Math.max(0, Math.min(100, Math.round(this.value()))),
  );
  protected readonly fillClass = computed(() => FILL_CLASSES[this.tone()]);
}
