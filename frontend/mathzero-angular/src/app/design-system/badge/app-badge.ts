import { Component, computed, input } from '@angular/core';

export type BadgeTone = 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'reward' | 'info';

const TONE_CLASSES: Record<BadgeTone, string> = {
  neutral: 'bg-subtle text-muted',
  accent: 'bg-accent-soft text-accent',
  success: 'bg-success-soft text-success',
  warning: 'bg-warning-soft text-warning',
  danger: 'bg-danger-soft text-danger',
  reward: 'bg-reward/20 text-ink',
  info: 'bg-second-soft text-second',
};

@Component({
  selector: 'app-badge',
  template: `<span [class]="classes()"><ng-content /></span>`,
})
export class AppBadge {
  readonly tone = input<BadgeTone>('neutral');

  protected readonly classes = computed(
    () =>
      `inline-flex items-center gap-1.5 rounded-pill px-2.5 py-0.5 text-xs font-semibold leading-5 ${TONE_CLASSES[this.tone()]}`,
  );
}
