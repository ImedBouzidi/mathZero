import { Component, OnChanges, OnDestroy, SimpleChanges, computed, inject, input, signal } from '@angular/core';

import { Demo, DemoFrame } from '../../core/api/learning.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppFormula } from '../../design-system/formula/app-formula';
import { AppIcon } from '../../design-system/icon/app-icon';

/**
 * Replayable worked demonstration. The frames come from the server generator;
 * this component only animates them. Playback is opt-in and can be paused, so
 * the experience remains usable with reduced-motion settings.
 */
@Component({
  selector: 'app-lesson-demo',
  imports: [AppFormula, AppIcon],
  template: `
    @if (demo(); as demo) {
      <section class="demo-shell rounded-3xl border border-line bg-canvas p-5 shadow-card sm:p-6" [class.compact]="compact()" [attr.data-kind]="demo.kind">
        <header class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p class="text-[0.65rem] font-bold uppercase tracking-[0.18em] text-accent">
              {{ i18n.translate('lesson.demo.eyebrow') }}
            </p>
            <h3 class="mt-1 text-lg font-semibold text-ink">{{ demo.title }}</h3>
          </div>
          <div class="flex items-center gap-2">
            <button class="focus-ring rounded-control border border-line px-3 py-1.5 text-xs font-semibold text-ink hover:bg-subtle" type="button" (click)="togglePlay()">
              <app-icon [name]="playing() ? 'pause' : 'play'" [size]="12" />
              <span class="ms-1">{{ i18n.translate(playing() ? 'lesson.demo.pause' : 'lesson.demo.play') }}</span>
            </button>
            <button class="focus-ring rounded-control border border-line px-3 py-1.5 text-xs font-semibold text-ink hover:bg-subtle" type="button" (click)="replay()">
              {{ i18n.translate('lesson.demo.replay') }}
            </button>
          </div>
        </header>

        @if (frame(); as frame) {
          @for (item of [frame]; track frameIndex()) {
            <div class="stage mt-5 grid gap-5 sm:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
              <div class="visual flex min-h-44 items-center justify-center rounded-2xl bg-subtle p-4" [attr.data-kind]="demo.kind">
                @switch (demo.kind) {
                  @case ('FRACTIONS') {
                    <div class="fraction-disc" [style.background]="fractionGradient(item.progress)">
                      <span>{{ item.left }}</span>
                      <small>{{ item.right }}</small>
                    </div>
                  }
                  @case ('EQUATION') {
                    <div class="balance" [class.balance-tilt]="item.progress < 50">
                      <div class="pan">{{ item.left }}</div>
                      <div class="beam"></div>
                      <div class="pan">{{ item.right }}</div>
                    </div>
                  }
                  @case ('GEOMETRY') {
                    <div class="rectangle">
                      <span class="width">{{ item.left }}</span>
                      <span class="height">{{ item.right }}</span>
                      <div class="area-fill" [style.width.%]="item.progress"></div>
                    </div>
                  }
                  @case ('FUNCTION') {
                    <div class="machine">
                      <span class="chip">{{ item.left }}</span>
                      <span class="pipe"></span>
                      <span class="chip accent-chip">f</span>
                      <span class="pipe"></span>
                      <span class="chip">{{ item.right }}</span>
                    </div>
                  }
                  @case ('TRIGONOMETRY') {
                    <svg class="triangle" viewBox="0 0 160 110" role="img" [attr.aria-label]="item.left + ' ' + item.right">
                      <polygon points="10,100 150,100 10,10" />
                      <text x="80" y="106">{{ item.left }}</text>
                      <text x="92" y="50">{{ item.right }}</text>
                      <text x="18" y="56">c</text>
                    </svg>
                  }
                  @case ('ARITHMETIC') {
                    <div class="arithmetic">
                      <span class="number-card">{{ item.left }}</span>
                      <span class="merge-arrow">→</span>
                      <span class="number-card result-card">{{ item.right }}</span>
                    </div>
                  }
                  @case ('MULTIPLICATION') {
                    <div class="times-table-wrap">
                      <p class="mb-2 text-center text-xs font-semibold text-muted">
                        {{ i18n.translate('lesson.demo.multiplicationTable') }}
                      </p>
                      <div class="table-scroll">
                        <table class="times-table" [attr.aria-label]="i18n.translate('lesson.demo.multiplicationTable')">
                          <thead>
                            <tr>
                              <th scope="col">×</th>
                              @for (factor of tableFactors; track factor) {
                                <th scope="col">{{ factor }}</th>
                              }
                            </tr>
                          </thead>
                          <tbody>
                            @for (row of tableFactors; track row) {
                              <tr>
                                <th scope="row">{{ row }}</th>
                                @for (column of tableFactors; track column) {
                                  <td [class.active]="isMultiplicationCell(item, row, column)">{{ row * column }}</td>
                                }
                              </tr>
                            }
                          </tbody>
                        </table>
                      </div>
                    </div>
                  }
                  @default {
                    <ol class="timeline">
                      @for (step of demo.frames; track $index) {
                        <li [class.active]="$index === frameIndex()">{{ step.label }}</li>
                      }
                    </ol>
                  }
                }
              </div>

              <div class="narration">
                <p class="text-xs font-bold uppercase tracking-wider text-accent">
                  {{ item.label }} · {{ frameIndex() + 1 }} / {{ demo.frames.length }}
                </p>
                <p class="mt-2 text-sm leading-6 text-ink/85">{{ item.narration }}</p>
                @if (item.formula) {
                  <div class="mt-3 overflow-x-auto rounded-xl bg-surface p-3 text-center" dir="ltr">
                    <app-formula [expression]="item.formula" />
                  </div>
                }
                <div class="mt-3 flex items-center justify-center gap-3 text-sm font-semibold text-ink" dir="ltr">
                  <span>{{ item.left }}</span>
                  <span class="text-accent">→</span>
                  <span>{{ item.right }}</span>
                </div>
                <div class="mt-3 h-2 overflow-hidden rounded-pill bg-surface" role="progressbar" [attr.aria-valuenow]="item.progress" aria-valuemin="0" aria-valuemax="100">
                  <div class="rail-fill h-full rounded-pill" [style.width.%]="item.progress"></div>
                </div>
              </div>
            </div>
          }
        }

        <footer class="mt-5 flex flex-wrap items-center justify-between gap-3">
          <div class="flex items-center gap-1.5">
            @for (item of demo.frames; track $index) {
              <button
                class="focus-ring h-2.5 rounded-pill transition-all"
                [class.w-6]="$index === frameIndex()"
                [class.w-2.5]="$index !== frameIndex()"
                [class.bg-accent]="$index === frameIndex()"
                [class.bg-subtle]="$index !== frameIndex()"
                type="button"
                [attr.aria-label]="i18n.translate('lesson.demo.step') + ' ' + ($index + 1)"
                [attr.aria-current]="$index === frameIndex() ? 'step' : null"
                (click)="goTo($index)"
              ></button>
            }
          </div>
          <div class="flex items-center gap-2">
            <button class="focus-ring rounded-control border border-line px-3 py-1.5 text-xs font-semibold text-ink disabled:opacity-40 hover:bg-subtle" type="button" [disabled]="frameIndex() === 0" (click)="previous()">
              {{ i18n.translate('lesson.player.previous') }}
            </button>
            <button class="focus-ring rounded-control bg-accent px-3 py-1.5 text-xs font-semibold text-white hover:opacity-90" type="button" (click)="next()">
              {{ frameIndex() === demo.frames.length - 1 ? i18n.translate('lesson.demo.replay') : i18n.translate('lesson.player.next') }}
            </button>
          </div>
        </footer>
      </section>
    }
  `,
  styles: [
    `
      :host { display: block; }
      .compact { padding: 1rem; }
      .stage { animation: demo-step 460ms cubic-bezier(.22,.8,.3,1) both; }
      @keyframes demo-step {
        from { opacity: 0; transform: translateY(12px); }
        to { opacity: 1; transform: none; }
      }
      .rail-fill { background: var(--color-accent, #4f46e5); transition: width 700ms cubic-bezier(.22,.8,.3,1); }
      .fraction-disc {
        display: grid; place-content: center; gap: .1rem;
        width: 7rem; aspect-ratio: 1; border-radius: 999px;
        text-align: center; color: #fff; font-weight: 700;
        box-shadow: 0 12px 30px -16px rgb(0 0 0 / 55%);
        animation: disc-pop 520ms ease-out both;
      }
      .fraction-disc small { font-size: .65rem; opacity: .85; }
      @keyframes disc-pop { from { transform: scale(.7) rotate(-12deg); opacity: 0; } to { transform: none; opacity: 1; } }
      .balance { display: grid; gap: .4rem; justify-items: center; animation: balance-in 500ms ease-out both; }
      .balance .pan { min-width: 5rem; padding: .5rem .8rem; border: 2px solid var(--color-accent, #4f46e5); border-radius: .9rem; text-align: center; font-weight: 700; color: var(--color-ink, #111); background: var(--color-surface, #fff); }
      .balance .beam { width: 9rem; height: 4px; border-radius: 99px; background: var(--color-accent, #4f46e5); transition: transform 500ms ease; }
      .balance-tilt .beam { transform: rotate(6deg); }
      @keyframes balance-in { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
      .rectangle { position: relative; width: 10rem; height: 6rem; border: 2px solid var(--color-accent, #4f46e5); border-radius: .6rem; overflow: hidden; background: var(--color-surface, #fff); }
      .rectangle .area-fill { position: absolute; inset-block: 0; inset-inline-start: 0; background: rgb(79 70 229 / 18%); transition: width 700ms ease; }
      .rectangle .width, .rectangle .height { position: absolute; z-index: 1; font-size: .75rem; font-weight: 700; color: var(--color-ink, #111); }
      .rectangle .width { bottom: .2rem; left: .4rem; }
      .rectangle .height { top: .3rem; right: .4rem; }
      .machine { display: flex; align-items: center; gap: .4rem; }
      .machine .chip { min-width: 3.2rem; padding: .5rem .3rem; border-radius: .8rem; border: 1px solid var(--color-line, #ddd); background: var(--color-surface, #fff); text-align: center; font-weight: 700; color: var(--color-ink, #111); }
      .machine .accent-chip { background: var(--color-accent, #4f46e5); color: #fff; animation: machine-pulse 1.4s ease-in-out infinite; }
      .machine .pipe { width: 1.4rem; height: 3px; background: var(--color-accent, #4f46e5); position: relative; overflow: hidden; }
      .machine .pipe::after { content: ''; position: absolute; inset-block: -2px; width: .5rem; background: #fff; animation: pipe-flow 1.1s linear infinite; }
      @keyframes pipe-flow { from { left: -0.6rem; } to { left: 100%; } }
      @keyframes machine-pulse { 50% { transform: scale(1.08); } }
      .triangle { width: 11rem; max-height: 8rem; }
      .triangle polygon { fill: rgb(79 70 229 / 14%); stroke: var(--color-accent, #4f46e5); stroke-width: 3; animation: shape-draw 600ms ease-out both; }
      .triangle text { fill: var(--color-ink, #111); font-size: 13px; font-weight: 700; }
      @keyframes shape-draw { from { opacity: 0; transform: scale(.8); transform-origin: center; } to { opacity: 1; transform: none; } }
      .arithmetic { display: flex; align-items: center; gap: .6rem; }
      .number-card { padding: .7rem 1rem; border-radius: 1rem; background: var(--color-surface, #fff); border: 1px solid var(--color-line, #ddd); font-weight: 700; color: var(--color-ink, #111); box-shadow: 0 8px 20px -14px rgb(0 0 0 / 60%); animation: card-slide 480ms ease-out both; }
      .result-card { background: var(--color-accent, #4f46e5); color: #fff; border-color: transparent; animation-delay: 140ms; }
      .merge-arrow { color: var(--color-accent, #4f46e5); font-size: 1.2rem; animation: arrow-nudge 1s ease-in-out infinite; }
      @keyframes card-slide { from { opacity: 0; transform: translateX(-10px); } to { opacity: 1; transform: none; } }
      @keyframes arrow-nudge { 50% { transform: translateX(4px); } }
      .times-table-wrap { width: 100%; max-width: 24rem; }
      .table-scroll { overflow-x: auto; }
      .times-table { width: 100%; border-collapse: separate; border-spacing: 2px; font-size: .62rem; text-align: center; }
      .times-table th, .times-table td { min-width: 1.45rem; height: 1.45rem; border-radius: .35rem; }
      .times-table th { background: var(--color-surface, #fff); color: var(--color-muted, #666); font-weight: 700; }
      .times-table td { background: var(--color-surface, #fff); color: var(--color-ink, #111); transition: transform 300ms ease, background 300ms ease; }
      .times-table td.active { transform: scale(1.15); background: var(--color-accent, #4f46e5); color: #fff; font-weight: 800; }
      .timeline { display: grid; gap: .35rem; margin: 0; padding: 0; list-style: none; }
      .timeline li { padding: .35rem .7rem; border-radius: .6rem; font-size: .8rem; color: var(--color-ink, #111); background: var(--color-surface, #fff); opacity: .5; transition: all 350ms ease; }
      .timeline li.active { opacity: 1; font-weight: 700; color: #fff; background: var(--color-accent, #4f46e5); transform: translateX(4px); }
      @media (prefers-reduced-motion: reduce) {
        .stage, .fraction-disc, .balance, .machine .accent-chip, .machine .pipe::after,
        .triangle polygon, .number-card, .merge-arrow { animation: none !important; }
        .rail-fill, .rectangle .area-fill, .balance .beam, .times-table td { transition: none !important; }
      }
    `,
  ],
})
export class LessonDemo implements OnChanges, OnDestroy {
  readonly demo = input.required<Demo | null>();
  readonly compact = input(false);
  protected readonly i18n = inject(LanguageService);
  protected readonly tableFactors = Array.from({ length: 10 }, (_, index) => index + 1);

  protected readonly frameIndex = signal(0);
  protected readonly playing = signal(false);
  protected readonly frame = computed(() => this.demo()?.frames[this.frameIndex()] ?? null);

  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['demo']) {
      this.stop();
      this.frameIndex.set(0);
    }
  }

  ngOnDestroy(): void {
    this.stop();
  }

  protected fractionGradient(progress: number): string {
    return `conic-gradient(var(--color-accent, #4f46e5) ${progress}%, rgb(79 70 229 / 12%) ${progress}%)`;
  }

  protected isMultiplicationCell(frame: DemoFrame, row: number, column: number): boolean {
    const factors = /^(\d+)\s*[×x]\s*(\d+)$/.exec(frame.left);
    return factors !== null && row === Number(factors[1]) && column === Number(factors[2]);
  }

  protected togglePlay(): void {
    if (this.playing()) {
      this.pause();
    } else {
      this.play();
    }
  }

  protected play(): void {
    const frames = this.demo()?.frames.length ?? 0;
    if (frames === 0) return;
    if (this.frameIndex() >= frames - 1) this.frameIndex.set(0);
    this.playing.set(true);
    this.timer = setInterval(() => this.advance(true), 1700);
  }

  protected pause(): void {
    this.stop();
  }

  protected replay(): void {
    this.stop();
    this.frameIndex.set(0);
    this.play();
  }

  protected goTo(index: number): void {
    this.stop();
    this.frameIndex.set(index);
  }

  protected previous(): void {
    this.stop();
    this.frameIndex.update((value) => Math.max(0, value - 1));
  }

  protected next(): void {
    this.stop();
    this.advance(false);
  }

  private advance(auto: boolean): void {
    const total = this.demo()?.frames.length ?? 0;
    if (this.frameIndex() >= total - 1) {
      this.stop();
      if (auto) this.frameIndex.set(0);
      return;
    }
    this.frameIndex.update((value) => value + 1);
  }

  private stop(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.playing.set(false);
  }
}
