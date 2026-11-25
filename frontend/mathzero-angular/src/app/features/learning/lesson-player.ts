import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AnswerResult, LearningService, Lesson } from '../../core/api/learning.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppFormula } from '../../design-system/formula/app-formula';
import { AppIcon } from '../../design-system/icon/app-icon';
import { AppProgressBar } from '../../design-system/progress/app-progress-bar';
import { LessonDemo } from './lesson-demo';

type PlayerMode = 'lesson' | 'quiz' | 'done';

/**
 * Animated lesson + quiz player for one approved resource. Every step and
 * question comes from the server-generated content; the correct answer is only
 * revealed by the answer endpoint after the learner submits a choice.
 */
@Component({
  selector: 'app-lesson-player',
  imports: [RouterLink, AppFormula, AppIcon, AppProgressBar, LessonDemo],
  template: `
    <section class="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6 sm:py-10" [attr.dir]="i18n.direction()">
      @if (loading()) {
        <p class="text-center text-sm text-muted" role="status" aria-live="polite">
          {{ i18n.translate('common.loading') }}
        </p>
      } @else if (error()) {
        <div class="rounded-2xl border border-red-200 bg-red-50 p-5 text-center" role="alert">
          <p class="text-sm text-red-800">{{ i18n.translate('lesson.player.loadError') }}</p>
          <button class="focus-ring mt-3 rounded-control border border-red-200 px-3 py-2 text-sm font-semibold text-red-800" type="button" (click)="load()">
            {{ i18n.translate('common.retry') }}
          </button>
        </div>
      } @else if (lesson(); as lesson) {
        <header class="mb-6 rounded-3xl bg-nav p-6 text-white shadow-card">
          <p class="text-xs font-bold uppercase tracking-[0.18em] text-cyan-200">
            {{ i18n.translate('lesson.player.eyebrow') }}
          </p>
          <h1 class="mt-2 text-2xl font-semibold sm:text-3xl">{{ lesson.title }}</h1>
          @if (lesson.summary) {
            <p class="mt-2 text-sm text-white/75">{{ lesson.summary }}</p>
          }
          <div class="mt-5 flex flex-wrap items-center gap-3">
            <a class="focus-ring inline-flex items-center gap-1 text-xs font-semibold text-cyan-200 underline" [href]="lesson.sourceUrl" target="_blank" rel="noopener">
              {{ i18n.translate('lesson.player.openSource') }}
              <app-icon name="external-link" [size]="12" />
            </a>
            <span class="text-[0.65rem] uppercase tracking-wider text-white/60">{{ lesson.licenceName }}</span>
          </div>
        </header>

        <div class="mb-6">
          <app-progress-bar [value]="progress()" [label]="i18n.translate('lesson.player.step')" [showValue]="true" />
        </div>

        @if (mode() === 'lesson') {
          @for (item of [step()]; track stepIndex()) {
            @if (item; as step) {
              <article class="step-card rounded-3xl border border-line bg-surface p-6 shadow-card sm:p-8" [attr.data-kind]="step.kind">
                <p class="text-xs font-bold uppercase tracking-[0.16em] text-accent">
                  {{ i18n.translate('lesson.player.step') }} {{ stepIndex() + 1 }} / {{ lesson.steps.length }}
                </p>
                <h2 class="mt-3 text-xl font-semibold text-ink sm:text-2xl">{{ step.title }}</h2>
                <p class="mt-4 text-sm leading-7 text-ink/80">{{ step.body }}</p>
                @if (step.formula) {
                  <div class="formula mt-5 overflow-x-auto rounded-2xl bg-subtle p-4 text-center" dir="ltr">
                    <app-formula [expression]="step.formula" />
                  </div>
                }
                @if (step.kind === 'demo') {
                  <div class="mt-5">
                    <app-lesson-demo [demo]="demo()" />
                  </div>
                }
              </article>
            }
          }
          <div class="mt-6 flex flex-wrap items-center justify-between gap-3">
            <button class="focus-ring rounded-control border border-line px-4 py-2 text-sm font-semibold text-ink hover:bg-subtle disabled:opacity-40" type="button" [disabled]="stepIndex() === 0" (click)="previousStep()">
              {{ i18n.translate('lesson.player.previous') }}
            </button>
            <button class="focus-ring rounded-control bg-accent px-5 py-2 text-sm font-semibold text-white hover:opacity-90" type="button" (click)="nextStep()">
              {{ stepIndex() === lesson.steps.length - 1 ? i18n.translate('lesson.player.startQuiz') : i18n.translate('lesson.player.next') }}
            </button>
          </div>
        } @else if (mode() === 'quiz') {
          @for (item of [question()]; track questionIndex()) {
            @if (item; as question) {
              <article class="step-card rounded-3xl border border-line bg-surface p-6 shadow-card sm:p-8">
                <p class="text-xs font-bold uppercase tracking-[0.16em] text-accent">
                  {{ i18n.translate('lesson.player.quiz') }} {{ questionIndex() + 1 }} / {{ lesson.exercises.length }}
                </p>
                <h2 class="mt-3 text-lg font-semibold text-ink sm:text-xl">{{ question.prompt }}</h2>
                <p class="mt-1 text-xs text-muted">{{ question.hint }}</p>
                <div class="mt-5 grid gap-2 sm:grid-cols-2">
                  @for (option of question.options; track $index) {
                    <button
                      class="option focus-ring rounded-2xl border px-4 py-3 text-left text-sm font-medium transition-colors"
                      [class.border-accent]="selectedOption() === $index"
                      [class.bg-accent-soft]="selectedOption() === $index"
                      [class.border-line]="selectedOption() !== $index"
                      [disabled]="answerResult() !== null || answerBusy()"
                      type="button"
                      (click)="selectOption($index)"
                    >
                      {{ option }}
                    </button>
                  }
                </div>
                @if (answerResult(); as result) {
                  <div class="feedback mt-5 rounded-2xl p-4" [class.correct]="result.correct" [class.wrong]="!result.correct" role="status" aria-live="polite">
                    <p class="text-sm font-semibold">{{ result.correct ? i18n.translate('lesson.player.correct') : i18n.translate('lesson.player.wrong') }}</p>
                    @if (!result.correct) {
                      <p class="mt-1 text-sm">{{ i18n.translate('lesson.player.correctAnswer') }} {{ result.correctAnswer }}</p>
                    }
                    <p class="mt-1 text-sm text-muted">{{ result.explanation }}</p>
                  </div>
                  @if (result.solution; as solution) {
                    <div class="mt-5">
                      <p class="mb-2 text-xs font-bold uppercase tracking-wider text-accent">
                        {{ i18n.translate('lesson.demo.solution') }}
                      </p>
                      <app-lesson-demo [demo]="solution" [compact]="true" />
                    </div>
                  }
                }
                @if (answerError()) {
                  <p class="mt-3 text-sm text-red-700" role="alert">{{ i18n.translate('lesson.player.loadError') }}</p>
                }
              </article>
            }
          }
          <div class="mt-6 flex flex-wrap items-center justify-end gap-3">
            @if (answerResult() === null) {
              <button class="focus-ring rounded-control bg-accent px-5 py-2 text-sm font-semibold text-white disabled:opacity-50" type="button" [disabled]="selectedOption() === null || answerBusy()" (click)="submitAnswer()">
                {{ i18n.translate('lesson.player.check') }}
              </button>
            } @else {
              <button class="focus-ring rounded-control bg-accent px-5 py-2 text-sm font-semibold text-white" type="button" (click)="nextQuestion()">
                {{ questionIndex() === lesson.exercises.length - 1 ? i18n.translate('lesson.player.finish') : i18n.translate('lesson.player.continue') }}
              </button>
            }
          </div>
        } @else {
          <article class="step-card rounded-3xl border border-line bg-surface p-8 text-center shadow-card">
            <p class="text-xs font-bold uppercase tracking-[0.16em] text-accent">
              {{ i18n.translate('lesson.player.score') }} : {{ score() }} / {{ lesson.exercises.length }}
            </p>
            <h2 class="mt-3 text-2xl font-semibold text-ink">{{ i18n.translate('lesson.player.done') }}</h2>
            <p class="mt-2 text-sm text-muted">{{ i18n.translate('lesson.player.doneBody') }}</p>
            <div class="mt-6 flex flex-wrap justify-center gap-3">
              <button class="focus-ring rounded-control border border-line px-4 py-2 text-sm font-semibold text-ink hover:bg-subtle" type="button" (click)="restart()">
                {{ i18n.translate('lesson.player.restart') }}
              </button>
              <a class="focus-ring rounded-control bg-accent px-4 py-2 text-sm font-semibold text-white" routerLink="/resources">
                {{ i18n.translate('lesson.player.backToResources') }}
              </a>
            </div>
          </article>
        }
      }
    </section>
  `,
  styles: [
    `
      :host { display: block; }
      header { animation: header-float 620ms cubic-bezier(.22,.8,.3,1) both; }
      @keyframes header-float {
        from { opacity: 0; transform: translateY(-14px); }
        to { opacity: 1; transform: none; }
      }
      .step-card { animation: lesson-enter 420ms cubic-bezier(.22,.8,.3,1) both; }
      @keyframes lesson-enter {
        from { opacity: 0; transform: translateY(18px) scale(.985); }
        to { opacity: 1; transform: none; }
      }
      .option { background: var(--color-canvas, #fff); color: var(--color-ink, #111); }
      .option:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 18px rgb(0 0 0 / 8%); }
      .feedback.correct { background: rgb(22 163 74 / 10%); color: rgb(21 128 61); animation: lesson-pop 320ms ease-out both; }
      .feedback.wrong { background: rgb(220 38 38 / 8%); color: rgb(153 27 27); animation: lesson-shake 320ms ease-out both; }
      @keyframes lesson-pop { from { opacity: 0; transform: scale(.94); } to { opacity: 1; transform: none; } }
      @keyframes lesson-shake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-6px); }
        75% { transform: translateX(6px); }
      }
      .option { animation: option-rise 320ms ease-out both; }
      .option:nth-child(2) { animation-delay: 50ms; }
      .option:nth-child(3) { animation-delay: 100ms; }
      .option:nth-child(4) { animation-delay: 150ms; }
      @keyframes option-rise {
        from { opacity: 0; transform: translateY(8px); }
        to { opacity: 1; transform: none; }
      }
      @media (prefers-reduced-motion: reduce) {
        header, .step-card, .option, .feedback.correct, .feedback.wrong { animation: none !important; }
        .option:hover:not(:disabled) { transform: none; }
      }
    `,
  ],
})
export class LessonPlayer implements OnInit {
  readonly resourceId = input.required<string>();
  protected readonly i18n = inject(LanguageService);
  private readonly learning = inject(LearningService);

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly lesson = signal<Lesson | null>(null);
  protected readonly mode = signal<PlayerMode>('lesson');
  protected readonly stepIndex = signal(0);
  protected readonly questionIndex = signal(0);
  protected readonly selectedOption = signal<number | null>(null);
  protected readonly answerResult = signal<AnswerResult | null>(null);
  protected readonly answerBusy = signal(false);
  protected readonly answerError = signal(false);
  protected readonly score = signal(0);

  protected readonly step = computed(() => this.lesson()?.steps[this.stepIndex()] ?? null);
  protected readonly demo = computed(() => this.lesson()?.demo ?? null);
  protected readonly question = computed(() => this.lesson()?.exercises[this.questionIndex()] ?? null);
  protected readonly progress = computed(() => {
    const lesson = this.lesson();
    if (!lesson) return 0;
    const total = lesson.steps.length + lesson.exercises.length;
    const done = this.mode() === 'lesson'
      ? this.stepIndex()
      : this.mode() === 'quiz' ? lesson.steps.length + this.questionIndex() : total;
    return total === 0 ? 100 : Math.round((done / total) * 100);
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.learning.getLesson(this.resourceId()).subscribe({
      next: (lesson) => {
        this.lesson.set(lesson);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  protected nextStep(): void {
    const lesson = this.lesson();
    if (!lesson) return;
    if (this.stepIndex() < lesson.steps.length - 1) {
      this.stepIndex.update((value) => value + 1);
    } else {
      this.mode.set('quiz');
    }
  }

  protected previousStep(): void {
    this.stepIndex.update((value) => Math.max(0, value - 1));
  }

  protected selectOption(index: number): void {
    if (this.answerResult() === null) {
      this.selectedOption.set(index);
    }
  }

  protected submitAnswer(): void {
    const question = this.question();
    const optionIndex = this.selectedOption();
    if (!question || optionIndex === null || this.answerResult() !== null) return;
    this.answerBusy.set(true);
    this.answerError.set(false);
    this.learning.answer(this.resourceId(), { questionId: question.id, optionIndex }).subscribe({
      next: (result) => {
        this.answerResult.set(result);
        if (result.correct) this.score.update((value) => value + 1);
        this.answerBusy.set(false);
      },
      error: () => {
        this.answerError.set(true);
        this.answerBusy.set(false);
      },
    });
  }

  protected nextQuestion(): void {
    const lesson = this.lesson();
    if (!lesson) return;
    if (this.questionIndex() < lesson.exercises.length - 1) {
      this.questionIndex.update((value) => value + 1);
      this.selectedOption.set(null);
      this.answerResult.set(null);
      this.answerError.set(false);
    } else {
      this.mode.set('done');
    }
  }

  protected restart(): void {
    this.mode.set('lesson');
    this.stepIndex.set(0);
    this.questionIndex.set(0);
    this.selectedOption.set(null);
    this.answerResult.set(null);
    this.answerError.set(false);
    this.score.set(0);
  }
}
