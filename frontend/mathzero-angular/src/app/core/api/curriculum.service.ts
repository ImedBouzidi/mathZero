import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';

export interface CurriculumPage<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CurriculumVersion {
  id: string;
  code: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  sourceReference?: string | null;
  reviewer?: string | null;
}

export interface Curriculum {
  id: string;
  code: string;
  nameFr: string;
  nameAr: string;
  currentVersion: CurriculumVersion;
}

export interface CurriculumNode {
  id: string;
  code: string;
  nameFr: string;
  nameAr: string;
  displayOrder: number;
}

export interface CurriculumChapter {
  id: string;
  code: string;
  titleFr: string;
  titleAr: string;
  displayOrder: number;
}

export interface CurriculumSkill {
  id: string;
  code: string;
  titleFr: string;
  titleAr: string;
  objectiveFr: string;
  objectiveAr: string;
  prerequisites: string[];
}

export interface CatalogStep {
  kind: string;
  title: string;
  body: string;
  formula: string | null;
}

export interface CatalogExercise {
  id: string;
  prompt: string;
  options: string[];
  hint: string;
  promptAr: string;
  optionsAr: string[];
  hintAr: string;
}

export interface CatalogChapter {
  id: string;
  title: string;
  description: string;
  titleAr: string;
  descriptionAr: string;
  steps: CatalogStep[];
  exercises: CatalogExercise[];
}

export interface CurriculumGradeCatalog {
  gradeCode: string;
  gradeName: string;
  gradeNameAr: string;
  cycle: 'PRIMARY' | 'BASIC' | 'SECONDARY';
  draft: boolean;
  reviewNote: string;
  chapters: CatalogChapter[];
}

export interface CurriculumCycleCatalog {
  cycle: 'PRIMARY' | 'BASIC' | 'SECONDARY';
  nameFr: string;
  nameAr: string;
  grades: CurriculumGradeCatalog[];
}

export interface CatalogAnswerRequest {
  exerciseId: string;
  optionIndex: number;
}

export interface CatalogAnswerResult {
  correct: boolean;
  correctAnswer: string;
  explanation: string;
}

interface ApiEnvelope<T> {
  data: T;
  meta: { requestId: string };
}

@Injectable({ providedIn: 'root' })
export class CurriculumService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getCurricula(search?: string): Observable<CurriculumPage<Curriculum>> {
    const params: Record<string, string> = { page: '0', size: '100' };
    if (search?.trim()) {
      params['search'] = search.trim();
    }
    return this.get('/curricula', params);
  }

  getLevels(versionId: string): Observable<CurriculumPage<CurriculumNode>> {
    return this.get(`/versions/${encodeURIComponent(versionId)}/levels`);
  }

  getGrades(levelId: string): Observable<CurriculumPage<CurriculumNode>> {
    return this.get(`/levels/${encodeURIComponent(levelId)}/grades`);
  }

  getSpecializations(gradeId: string): Observable<CurriculumPage<CurriculumNode>> {
    return this.get(`/grades/${encodeURIComponent(gradeId)}/specializations`);
  }

  getSubjects(gradeId: string): Observable<CurriculumPage<CurriculumNode>> {
    return this.get(`/grades/${encodeURIComponent(gradeId)}/subjects`);
  }

  getChapters(subjectId: string): Observable<CurriculumPage<CurriculumChapter>> {
    return this.get(`/subjects/${encodeURIComponent(subjectId)}/chapters`);
  }

  getSkills(chapterId: string): Observable<CurriculumPage<CurriculumSkill>> {
    return this.get(`/chapters/${encodeURIComponent(chapterId)}/skills`);
  }

  getGradeCatalog(gradeCode: string): Observable<CurriculumGradeCatalog> {
    return this.getCatalog(`/learning/curriculum/grades/${encodeURIComponent(gradeCode)}`);
  }

  getCycleCatalog(): Observable<CurriculumCycleCatalog[]> {
    return this.getCatalog('/learning/curriculum/cycles');
  }

  answerCatalogExercise(
    gradeCode: string,
    request: CatalogAnswerRequest,
  ): Observable<CatalogAnswerResult> {
    return this.http
      .post<ApiEnvelope<CatalogAnswerResult>>(
        `${this.baseUrl}/learning/curriculum/grades/${encodeURIComponent(gradeCode)}/answers`,
        request,
      )
      .pipe(map((response) => response.data));
  }

  private getCatalog<T>(path: string): Observable<T> {
    return this.http
      .get<ApiEnvelope<T>>(`${this.baseUrl}${path}`)
      .pipe(map((response) => response.data));
  }

  private get<T>(
    path: string,
    params: Record<string, string> = { page: '0', size: '100' },
  ): Observable<CurriculumPage<T>> {
    return this.http
      .get<ApiEnvelope<CurriculumPage<T>>>(`${this.baseUrl}${path}`, { params })
      .pipe(map((response) => response.data));
  }
}
