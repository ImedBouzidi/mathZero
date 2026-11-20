import { Component, computed, input } from '@angular/core';

export type RingTone = 'accent' | 'success' | 'warning' | 'reward' | 'second';

@Component({
  selector: 'app-progress-ring',
  template: `
    <div class="inline-flex flex-col items-center gap-2">
      <div
        class="relative"
        [style.width.px]="size()"
        [style.height.px]="size()"
        role="progressbar"
        [attr.aria-label]="label() || ariaLabel()"
        aria-valuemin="0"
        aria-valuemax="100"
        [attr.aria-valuenow]="normalized()"
      >
        <svg
          [attr.width]="size()"
          [attr.height]="size()"
          [attr.viewBox]="'0 0 ' + size() + ' ' + size()"
          class="-rotate-90"
          aria-hidden="true"
        >
          <circle
            class="ring-track"
            fill="none"
            [attr.cx]="center()"
            [attr.cy]="center()"
            [attr.r]="radius()"
            [attr.stroke-width]="stroke()"
          />
          <circle
            [class]="'ring-value ' + toneClass()"
            fill="none"
            [attr.cx]="center()"
            [attr.cy]="center()"
            [attr.r]="radius()"
            [attr.stroke-width]="stroke()"
            stroke-linecap="round"
            [attr.stroke-dasharray]="circumference()"
            [attr.stroke-dashoffset]="offset()"
          />
        </svg>
        <span
          class="absolute inset-0 grid place-items-center font-display font-semibold tabular-nums text-ink"
          [style.font-size.px]="size() / 5"
        >
          {{ normalized() }}<span class="text-[0.55em] text-muted">%</span>
        </span>
      </div>
      @if (label()) {
        <span class="max-w-28 text-center text-xs font-medium leading-4 text-muted">{{ label() }}</span>
      }
    </div>
  `,
  styles: `
    .ring-track {
      stroke: var(--border-subtle);
    }

    .ring-value {
      stroke: var(--accent);
      transition: stroke-dashoffset 400ms ease;
    }

    .ring--success {
      stroke: var(--success);
    }

    .ring--warning {
      stroke: var(--warning);
    }

    .ring--reward {
      stroke: var(--reward);
    }

    .ring--second {
      stroke: var(--accent-2);
    }
  `,
})
export class AppProgressRing {
  readonly value = input(0);
  readonly size = input(96);
  readonly stroke = input(8);
  readonly label = input<string>();
  readonly ariaLabel = input('Progression');
  readonly tone = input<RingTone>('accent');

  protected readonly normalized = computed(() =>
    Math.max(0, Math.min(100, Math.round(this.value()))),
  );
  protected readonly center = computed(() => this.size() / 2);
  protected readonly radius = computed(() => (this.size() - this.stroke()) / 2);
  protected readonly circumference = computed(() => 2 * Math.PI * this.radius());
  protected readonly offset = computed(
    () => this.circumference() * (1 - this.normalized() / 100),
  );
  protected readonly toneClass = computed(() => `ring--${this.tone()}`);
}
