import {
  Component,
  OnChanges,
  OnInit,
  SimpleChanges,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';

import {
  CatalogAnswerResult,
  CatalogChapter,
  CatalogExercise,
  CatalogStep,
  CurriculumCycleCatalog,
  CurriculumGradeCatalog,
  CurriculumService,
} from '../../../core/api/curriculum.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AppBadge } from '../../../design-system/badge/app-badge';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';

export type CatalogCycle = 'PRIMARY' | 'BASIC' | 'SECONDARY';

/**
 * Chapter browser for one experience cycle.
 *
 * The curriculum catalogue is the server-side source of truth for grade codes
 * and chapters; this component only selects the cycle and renders its grades.
 * Draft catalogues keep their explicit review warning instead of pretending to
 * be verified content.
 */
@Component({
  selector: 'app-cycle-chapters',
  imports: [AppBadge, AppEmptyState, AppIcon],
  template: `
    @if (loading()) {
      <div
        class="space-y-3"
        role="status"
        aria-live="polite"
        [attr.aria-label]="i18n.translate('common.loading')"
      >
        @for (index of [1, 2, 3]; track index) {
          <div class="flex gap-3 rounded-2xl border border-line bg-canvas p-3">
            <div class="h-10 w-10 shrink-0 animate-pulse rounded-xl bg-subtle"></div>
            <div class="min-w-0 flex-1 space-y-2 py-1">
              <div class="h-3.5 w-2/3 animate-pulse rounded bg-subtle"></div>
              <div class="h-3 w-full animate-pulse rounded bg-subtle"></div>
            </div>
          </div>
        }
      </div>
    } @else if (error()) {
      <div
        class="flex items-center justify-between gap-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-3"
        role="alert"
      >
        <p class="text-sm text-red-800">{{ i18n.translate('chapters.loadError') }}</p>
        <button
          class="focus-ring inline-flex shrink-0 items-center gap-2 rounded-control border border-red-200 bg-canvas px-3 py-2 text-xs font-semibold text-red-800 hover:bg-red-100"
          type="button"
          (click)="load()"
        >
          <app-icon name="refresh" [size]="14" />
          {{ i18n.translate('common.retry') }}
        </button>
      </div>
    } @else if (!catalog() || catalog()!.grades.length === 0) {
      <app-empty-state
        icon="book-open"
        [title]="i18n.translate('chapters.empty.title')"
        [body]="i18n.translate('chapters.empty.body')"
      />
    } @else {
      <div
        class="flex flex-wrap gap-2"
        role="tablist"
        [attr.aria-label]="i18n.translate('chapters.chooseGrade')"
      >
        @for (grade of catalog()!.grades; track grade.gradeCode) {
          <button
            class="focus-ring rounded-pill border px-3 py-1.5 text-xs font-semibold transition-colors"
            [class.border-accent]="selectedGrade()?.gradeCode === grade.gradeCode"
            [class.bg-accent-soft]="selectedGrade()?.gradeCode === grade.gradeCode"
            [class.text-accent]="selectedGrade()?.gradeCode === grade.gradeCode"
            [class.border-line]="selectedGrade()?.gradeCode !== grade.gradeCode"
            [class.bg-surface]="selectedGrade()?.gradeCode !== grade.gradeCode"
            [class.text-muted]="selectedGrade()?.gradeCode !== grade.gradeCode"
            [attr.aria-pressed]="selectedGrade()?.gradeCode === grade.gradeCode"
            type="button"
            (click)="selectGrade(grade)"
          >
            {{ gradeName(grade) }}
          </button>
        }
      </div>

      @if (selectedGrade(); as grade) {
        @if (grade.draft) {
          <div
            class="mt-3 rounded-xl border border-dashed border-warning/40 bg-warning-soft/40 px-3 py-2 text-xs font-medium text-warning"
          >
            <p>{{ i18n.translate('chapters.reviewPending') }}</p>
            @if (grade.reviewNote) {
              <p class="mt-1 font-normal opacity-80">{{ grade.reviewNote }}</p>
            }
          </div>
        }

        <div class="mt-4 grid gap-3 sm:grid-cols-2">
          @for (chapter of grade.chapters; track chapter.id) {
            <article class="rounded-2xl border border-line bg-canvas">
              <button
                class="focus-ring flex w-full items-start justify-between gap-3 rounded-2xl p-4 text-left"
                type="button"
                [attr.aria-expanded]="isExpanded(chapter.id)"
                [attr.aria-label]="chapterToggleLabel(chapter)"
                (click)="toggleChapter(chapter.id)"
              >
                <span class="min-w-0">
                  <span class="block text-sm font-semibold text-ink">{{ chapterTitle(chapter) }}</span>
                  <span class="mt-1.5 block text-xs leading-5 text-muted">
                    {{ chapterDescription(chapter) }}
                  </span>
                </span>
                <span class="flex shrink-0 items-center gap-2">
                  <app-badge tone="neutral">
                    {{ chapter.exercises.length }} {{ i18n.translate('chapters.exercises') }}
                  </app-badge>
                  <app-icon
                    [name]="isExpanded(chapter.id) ? 'chevron-down' : 'chevron-right'"
                    [size]="16"
                  />
                </span>
              </button>

              @if (isExpanded(chapter.id)) {
                <div class="border-t border-line px-4 py-4">
                  @if (chapter.steps.length > 0) {
                    <ol class="space-y-3">
                      @for (step of chapter.steps; track $index) {
                        <li>
                          <p class="text-[0.65rem] font-bold uppercase tracking-wider text-accent">
                            {{ stepKind(step) }}
                          </p>
                          <p class="mt-0.5 text-sm font-semibold text-ink">{{ step.title }}</p>
                          <p class="mt-1 text-xs leading-5 text-muted">{{ step.body }}</p>
                          @if (step.formula) {
                            <p
                              class="mt-2 overflow-x-auto rounded-xl bg-subtle px-3 py-2 text-center font-mono text-sm text-ink"
                              dir="ltr"
                            >
                              {{ step.formula }}
                            </p>
                          }
                        </li>
                      }
                    </ol>
                  }

                  @if (chapter.exercises.length > 0) {
                    <div class="mt-4 space-y-4">
                      @for (exercise of chapter.exercises; track exercise.id) {
                        <div class="rounded-xl border border-line bg-surface p-3">
                          <p class="text-sm font-medium text-ink">{{ exercisePrompt(exercise) }}</p>
                          <p class="mt-1 text-xs text-muted">
                            {{ i18n.translate('basic.exercises.hint') }}:
                            {{ exerciseHint(exercise) }}
                          </p>
                          <div class="mt-2 grid gap-2 sm:grid-cols-2">
                            @for (option of exerciseOptions(exercise); track $index) {
                              <button
                                class="focus-ring rounded-xl border px-3 py-2 text-left text-sm font-medium transition-colors"
                                [class.border-accent]="selectedOption(exercise.id) === $index"
                                [class.bg-accent-soft]="selectedOption(exercise.id) === $index"
                                [class.border-line]="selectedOption(exercise.id) !== $index"
                                [disabled]="
                                  exerciseResult(exercise.id) !== null || exerciseBusy(exercise.id)
                                "
                                type="button"
                                (click)="selectOption(exercise.id, $index)"
                              >
                                {{ option }}
                              </button>
                            }
                          </div>

                          @if (exerciseResult(exercise.id); as result) {
                            <div
                              class="mt-3 rounded-xl px-3 py-2 text-xs"
                              [class.bg-success-soft]="result.correct"
                              [class.text-success]="result.correct"
                              [class.bg-danger-soft]="!result.correct"
                              [class.text-danger]="!result.correct"
                              role="status"
                              aria-live="polite"
                            >
                              <p class="font-semibold">
                                {{
                                  result.correct
                                    ? i18n.translate('lesson.player.correct')
                                    : i18n.translate('lesson.player.wrong')
                                }}
                              </p>
                              @if (!result.correct) {
                                <p class="mt-1">
                                  {{ i18n.translate('lesson.player.correctAnswer') }}
                                  {{ result.correctAnswer }}
                                </p>
                              }
                              <p class="mt-1 opacity-80">{{ result.explanation }}</p>
                            </div>
                          } @else {
                            <div class="mt-3 flex items-center gap-3">
                              <button
                                class="focus-ring rounded-control bg-accent px-4 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                                type="button"
                                [disabled]="
                                  selectedOption(exercise.id) === null ||
                                  exerciseBusy(exercise.id)
                                "
                                (click)="checkAnswer(grade, exercise)"
                              >
                                {{ i18n.translate('lesson.player.check') }}
                              </button>
                              @if (exerciseError(exercise.id)) {
                                <p class="text-xs text-red-700" role="alert">
                                  {{ i18n.translate('chapters.answerError') }}
                                </p>
                              }
                            </div>
                          }
                        </div>
                      }
                    </div>
                  }
                </div>
              }
            </article>
          }
        </div>
      }
    }
  `,
})
export class CycleChapters implements OnInit, OnChanges {
  readonly cycle = input.required<CatalogCycle>();
  protected readonly i18n = inject(LanguageService);
  private readonly curriculumService = inject(CurriculumService);

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly catalog = signal<CurriculumCycleCatalog | null>(null);
  protected readonly selectedGradeCode = signal<string | null>(null);
  protected readonly expandedChapters = signal<ReadonlySet<string>>(new Set<string>());
  protected readonly selectedOptions = signal<Record<string, number>>({});
  protected readonly results = signal<Record<string, CatalogAnswerResult>>({});
  protected readonly busy = signal<Record<string, boolean>>({});
  protected readonly errors = signal<Record<string, boolean>>({});

  protected readonly selectedGrade = computed<CurriculumGradeCatalog | null>(() => {
    const catalog = this.catalog();
    if (!catalog) {
      return null;
    }
    return (
      catalog.grades.find((grade) => grade.gradeCode === this.selectedGradeCode()) ??
      catalog.grades[0] ??
      null
    );
  });

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['cycle'] && !changes['cycle'].firstChange) {
      this.load();
    }
  }

  protected gradeName(grade: CurriculumGradeCatalog): string {
    return this.i18n.language() === 'ar' ? grade.gradeNameAr : grade.gradeName;
  }

  protected chapterTitle(chapter: CatalogChapter): string {
    return this.i18n.language() === 'ar' ? chapter.titleAr : chapter.title;
  }

  protected chapterDescription(chapter: CatalogChapter): string {
    return this.i18n.language() === 'ar' ? chapter.descriptionAr : chapter.description;
  }

  protected exercisePrompt(exercise: CatalogExercise): string {
    return this.i18n.language() === 'ar' ? exercise.promptAr : exercise.prompt;
  }

  protected exerciseOptions(exercise: CatalogExercise): readonly string[] {
    return this.i18n.language() === 'ar' ? exercise.optionsAr : exercise.options;
  }

  protected exerciseHint(exercise: CatalogExercise): string {
    return this.i18n.language() === 'ar' ? exercise.hintAr : exercise.hint;
  }

  protected stepKind(step: CatalogStep): string {
    switch (step.kind) {
      case 'intro':
        return this.i18n.translate('chapters.step.intro');
      case 'concept':
        return this.i18n.translate('chapters.step.concept');
      case 'example':
        return this.i18n.translate('chapters.step.example');
      case 'practice':
        return this.i18n.translate('chapters.step.practice');
      case 'source':
        return this.i18n.translate('chapters.step.source');
      case 'demo':
        return this.i18n.translate('chapters.step.demo');
      default:
        return this.i18n.translate('chapters.step.other');
    }
  }

  protected chapterToggleLabel(chapter: CatalogChapter): string {
    return this.i18n.translate(
      this.isExpanded(chapter.id) ? 'chapters.hideContent' : 'chapters.showContent',
    );
  }

  protected selectGrade(grade: CurriculumGradeCatalog): void {
    this.selectedGradeCode.set(grade.gradeCode);
    this.expandedChapters.set(new Set<string>());
  }

  protected isExpanded(chapterId: string): boolean {
    return this.expandedChapters().has(chapterId);
  }

  protected toggleChapter(chapterId: string): void {
    const next = new Set(this.expandedChapters());
    if (next.has(chapterId)) {
      next.delete(chapterId);
    } else {
      next.add(chapterId);
    }
    this.expandedChapters.set(next);
  }

  protected selectedOption(exerciseId: string): number | null {
    return this.selectedOptions()[exerciseId] ?? null;
  }

  protected exerciseResult(exerciseId: string): CatalogAnswerResult | null {
    return this.results()[exerciseId] ?? null;
  }

  protected exerciseBusy(exerciseId: string): boolean {
    return this.busy()[exerciseId] ?? false;
  }

  protected exerciseError(exerciseId: string): boolean {
    return this.errors()[exerciseId] ?? false;
  }

  protected selectOption(exerciseId: string, optionIndex: number): void {
    this.selectedOptions.update((options) => ({ ...options, [exerciseId]: optionIndex }));
  }

  protected checkAnswer(grade: CurriculumGradeCatalog, exercise: CatalogExercise): void {
    const optionIndex = this.selectedOption(exercise.id);
    if (optionIndex === null || this.exerciseBusy(exercise.id)) {
      return;
    }
    this.busy.update((state) => ({ ...state, [exercise.id]: true }));
    this.errors.update((state) => ({ ...state, [exercise.id]: false }));
    this.curriculumService
      .answerCatalogExercise(grade.gradeCode, { exerciseId: exercise.id, optionIndex })
      .subscribe({
        next: (result) => {
          this.results.update((state) => ({ ...state, [exercise.id]: result }));
          this.busy.update((state) => ({ ...state, [exercise.id]: false }));
        },
        error: () => {
          this.errors.update((state) => ({ ...state, [exercise.id]: true }));
          this.busy.update((state) => ({ ...state, [exercise.id]: false }));
        },
      });
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.curriculumService.getCycleCatalog().subscribe({
      next: (cycles) => {
        const match = cycles.find((entry) => entry.cycle === this.cycle()) ?? null;
        this.catalog.set(match);
        this.selectedGradeCode.set(match?.grades[0]?.gradeCode ?? null);
        this.expandedChapters.set(new Set<string>());
        this.selectedOptions.set({});
        this.results.set({});
        this.busy.set({});
        this.errors.set({});
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
