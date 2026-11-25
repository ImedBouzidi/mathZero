import { Component, computed, inject, signal } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';

import {
  Curriculum,
  CurriculumChapter,
  CatalogExercise,
  CurriculumGradeCatalog,
  CurriculumNode,
  CurriculumPage,
  CurriculumService,
  CurriculumSkill,
} from '../../core/api/curriculum.service';
import { EducationCycle } from '../../core/experience/education-cycle';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslationKey } from '../../core/i18n/translations';

interface CycleTab {
  readonly id: EducationCycle;
  readonly labelKey: TranslationKey;
}

@Component({
  selector: 'app-curriculum-explorer',
  templateUrl: './curriculum-explorer.html',
  styleUrl: './curriculum-explorer.scss',
})
export class CurriculumExplorer {
  protected readonly i18n = inject(LanguageService);
  private readonly curriculumService = inject(CurriculumService);

  protected readonly curricula = signal<Curriculum[]>([]);
  protected readonly levels = signal<CurriculumNode[]>([]);
  protected readonly grades = signal<CurriculumNode[]>([]);
  protected readonly specializations = signal<CurriculumNode[]>([]);
  protected readonly subjects = signal<CurriculumNode[]>([]);
  protected readonly chapters = signal<CurriculumChapter[]>([]);
  protected readonly skills = signal<CurriculumSkill[]>([]);
  protected readonly gradeCatalog = signal<CurriculumGradeCatalog | null>(null);
  protected readonly catalogAnswers = signal<Record<string, { correct: boolean; text: string }>>({});

  protected readonly selectedCurriculum = signal<Curriculum | null>(null);
  protected readonly selectedLevel = signal<CurriculumNode | null>(null);
  protected readonly selectedGrade = signal<CurriculumNode | null>(null);
  protected readonly selectedSpecialization = signal<CurriculumNode | null>(null);
  protected readonly selectedSubject = signal<CurriculumNode | null>(null);
  protected readonly selectedChapter = signal<CurriculumChapter | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);

  protected readonly cycles: readonly CycleTab[] = [
    { id: 'primary', labelKey: 'landing.primary.tagline' },
    { id: 'basic', labelKey: 'landing.basic.tagline' },
    { id: 'secondary', labelKey: 'landing.secondary.tagline' },
  ];
  protected readonly selectedCycle = signal<EducationCycle | null>(null);
  protected readonly filteredLevels = computed(() => {
    const cycle = this.selectedCycle();
    if (!cycle) {
      return this.levels();
    }
    return this.levels().filter((level) => this.cycleForLevel(level) === cycle);
  });

  constructor() {
    this.loadCurricula();
  }

  protected label(nameFr: string, nameAr: string): string {
    return this.i18n.language() === 'ar' ? nameAr : nameFr;
  }

  protected chapterLabel(chapter: CurriculumChapter): string {
    return this.i18n.language() === 'ar' ? chapter.titleAr : chapter.titleFr;
  }

  protected skillTitle(skill: CurriculumSkill): string {
    return this.i18n.language() === 'ar' ? skill.titleAr : skill.titleFr;
  }

  protected skillObjective(skill: CurriculumSkill): string {
    return this.i18n.language() === 'ar' ? skill.objectiveAr : skill.objectiveFr;
  }

  protected loadCurricula(): void {
    this.load(this.curriculumService.getCurricula(), (items) => this.curricula.set(items));
  }

  protected selectCurriculum(curriculum: Curriculum): void {
    this.selectedCurriculum.set(curriculum);
    this.selectedLevel.set(null);
    this.selectedGrade.set(null);
    this.selectedSpecialization.set(null);
    this.selectedSubject.set(null);
    this.selectedChapter.set(null);
    this.selectedCycle.set(null);
    this.levels.set([]);
    this.clearGradeContents();
    this.chapters.set([]);
    this.skills.set([]);
    this.gradeCatalog.set(null);
    this.catalogAnswers.set({});
    this.loadLevels(curriculum.currentVersion.id);
  }

  protected selectCycle(cycle: EducationCycle): void {
    if (this.selectedCycle() === cycle) {
      return;
    }
    this.selectedCycle.set(cycle);
    this.selectedLevel.set(null);
    this.selectedGrade.set(null);
    this.selectedSpecialization.set(null);
    this.selectedSubject.set(null);
    this.selectedChapter.set(null);
    this.clearGradeContents();
    this.chapters.set([]);
    this.skills.set([]);
    this.gradeCatalog.set(null);
    this.catalogAnswers.set({});
  }

  protected cycleForLevel(level: CurriculumNode): EducationCycle | null {
    const haystack = `${level.code} ${level.nameFr} ${level.nameAr}`.toLowerCase();
    if (/(primary|primaire|ابتدائ)/.test(haystack)) {
      return 'primary';
    }
    if (/(secondary|secondaire|lyc|ثانو)/.test(haystack)) {
      return 'secondary';
    }
    if (/(basic|base|coll|moyen|prépar|prepar|أساس|اعداد|إعداد)/.test(haystack)) {
      return 'basic';
    }
    return null;
  }

  protected selectLevel(level: CurriculumNode): void {
    this.selectedLevel.set(level);
    this.selectedGrade.set(null);
    this.selectedSpecialization.set(null);
    this.selectedSubject.set(null);
    this.selectedChapter.set(null);
    this.clearGradeContents();
    this.chapters.set([]);
    this.skills.set([]);
    this.load(this.curriculumService.getGrades(level.id), (items) => this.grades.set(items));
  }

  protected selectGrade(grade: CurriculumNode): void {
    this.selectedGrade.set(grade);
    this.selectedSpecialization.set(null);
    this.selectedSubject.set(null);
    this.selectedChapter.set(null);
    this.chapters.set([]);
    this.skills.set([]);
    this.loadGradeContents(grade.id);
    this.catalogAnswers.set({});
    this.loading.set(true);
    this.error.set(false);
    this.curriculumService.getGradeCatalog(grade.code).subscribe({
      next: (catalog) => {
        this.gradeCatalog.set(catalog);
        this.loading.set(false);
      },
      error: () => {
        this.gradeCatalog.set(null);
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  protected answerCatalogExercise(
    exercise: CatalogExercise,
    optionIndex: number,
  ): void {
    const grade = this.selectedGrade();
    if (!grade) return;
    this.curriculumService
      .answerCatalogExercise(grade.code, {
        exerciseId: exercise.id,
        optionIndex,
      })
      .subscribe({
        next: (result) => {
          this.catalogAnswers.update((answers) => ({
            ...answers,
            [exercise.id]: {
              correct: result.correct,
              text: result.correct
                ? result.correctAnswer
                : `${result.correctAnswer} — ${result.explanation}`,
            },
          }));
        },
        error: () => {
          this.catalogAnswers.update((answers) => ({
            ...answers,
            [exercise.id]: { correct: false, text: 'Réponse indisponible.' },
          }));
        },
      });
  }

  protected selectSpecialization(specialization: CurriculumNode | null): void {
    this.selectedSpecialization.set(specialization);
  }

  protected selectSubject(subject: CurriculumNode): void {
    this.selectedSubject.set(subject);
    this.selectedChapter.set(null);
    this.skills.set([]);
    this.load(this.curriculumService.getChapters(subject.id), (items) => this.chapters.set(items));
  }

  protected selectChapter(chapter: CurriculumChapter): void {
    this.selectedChapter.set(chapter);
    this.load(this.curriculumService.getSkills(chapter.id), (items) => this.skills.set(items));
  }

  protected retry(): void {
    if (this.selectedChapter()) {
      this.load(this.curriculumService.getSkills(this.selectedChapter()!.id), (items) =>
        this.skills.set(items),
      );
    } else if (this.selectedSubject()) {
      this.load(this.curriculumService.getChapters(this.selectedSubject()!.id), (items) =>
        this.chapters.set(items),
      );
    } else if (this.selectedGrade()) {
      this.loadGradeContents(this.selectedGrade()!.id);
    } else if (this.selectedLevel()) {
      this.load(this.curriculumService.getGrades(this.selectedLevel()!.id), (items) =>
        this.grades.set(items),
      );
    } else if (this.selectedCurriculum()) {
      this.loadLevels(this.selectedCurriculum()!.currentVersion.id);
    } else {
      this.loadCurricula();
    }
  }

  private loadLevels(versionId: string): void {
    this.load(this.curriculumService.getLevels(versionId), (items) => {
      this.levels.set(items);
      const available = this.cycles.find((cycle) =>
        items.some((level) => this.cycleForLevel(level) === cycle.id),
      );
      this.selectedCycle.set(available?.id ?? null);
    });
  }

  private loadGradeContents(gradeId: string): void {
    this.loading.set(true);
    this.error.set(false);
    forkJoin({
      specializations: this.curriculumService.getSpecializations(gradeId),
      subjects: this.curriculumService.getSubjects(gradeId),
    }).subscribe({
      next: ({ specializations, subjects }) => {
        this.specializations.set(specializations.items);
        this.subjects.set(subjects.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  private clearGradeContents(): void {
    this.grades.set([]);
    this.specializations.set([]);
    this.subjects.set([]);
  }

  private load<T>(request: Observable<CurriculumPage<T>>, update: (items: T[]) => void): void {
    this.loading.set(true);
    this.error.set(false);
    request.subscribe({
      next: (page) => {
        update(page.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
