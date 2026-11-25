import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../config/api-config';

interface ApiEnvelope<T> {
  readonly data: T;
  readonly meta: { readonly requestId: string };
}

export interface LessonStep {
  readonly kind: 'intro' | 'concept' | 'example' | 'source' | 'practice' | string;
  readonly title: string;
  readonly body: string;
  readonly formula: string | null;
}

export interface DemoFrame {
  readonly label: string;
  readonly narration: string;
  readonly formula: string | null;
  readonly left: string;
  readonly right: string;
  readonly progress: number;
}

export interface Demo {
  readonly kind: string;
  readonly title: string;
  readonly frames: readonly DemoFrame[];
}

export interface LessonExercise {
  readonly id: string;
  readonly prompt: string;
  readonly options: readonly string[];
  readonly hint: string;
}

export interface Lesson {
  readonly resourceId: string;
  readonly title: string;
  readonly summary: string | null;
  readonly language: string;
  readonly cycle: string | null;
  readonly sourceUrl: string;
  readonly licenceName: string;
  readonly attributionText: string;
  readonly steps: readonly LessonStep[];
  readonly demo: Demo | null;
  readonly exercises: readonly LessonExercise[];
}

export interface AnswerRequest {
  readonly questionId: string;
  readonly optionIndex: number;
}

export interface AnswerResult {
  readonly correct: boolean;
  readonly correctAnswer: string;
  readonly explanation: string;
  readonly position: number;
  readonly total: number;
  readonly solution: Demo | null;
}

@Injectable({ providedIn: 'root' })
export class LearningService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getLesson(resourceId: string): Observable<Lesson> {
    return this.http
      .get<ApiEnvelope<Lesson>>(`${this.baseUrl}/learning/lessons/${encodeURIComponent(resourceId)}`)
      .pipe(map((response) => response.data));
  }

  answer(resourceId: string, request: AnswerRequest): Observable<AnswerResult> {
    return this.http
      .post<ApiEnvelope<AnswerResult>>(
        `${this.baseUrl}/learning/lessons/${encodeURIComponent(resourceId)}/answers`,
        request,
      )
      .pipe(map((response) => response.data));
  }
}
