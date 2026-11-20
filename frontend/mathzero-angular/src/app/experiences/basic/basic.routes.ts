import { Routes } from '@angular/router';

import { experienceGuard } from '../../core/experience/experience.guard';
import { BasicShell } from './shell/basic-shell';

export const BASIC_ROUTES: Routes = [
  {
    path: '',
    component: BasicShell,
    canActivate: [experienceGuard],
    data: { experience: 'basic' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./dashboard/basic-dashboard').then((m) => m.BasicDashboard),
        title: 'Tableau de bord · Math Challenge',
      },
      {
        path: 'lessons',
        loadComponent: () => import('./lessons/basic-lessons').then((m) => m.BasicLessons),
        title: 'Chapitres · Math Challenge',
      },
      {
        path: 'lessons/:lessonId',
        loadComponent: () => import('./lessons/basic-lesson').then((m) => m.BasicLesson),
        title: 'Leçon · Math Challenge',
      },
      {
        path: 'exercises',
        loadComponent: () =>
          import('./exercises/basic-exercises').then((m) => m.BasicExercises),
        title: 'Exercices · Math Challenge',
      },
      {
        path: 'challenges',
        loadComponent: () =>
          import('./challenges/basic-challenges').then((m) => m.BasicChallenges),
        title: 'Défis · Math Challenge',
      },
      {
        path: 'progress',
        loadComponent: () => import('./progress/basic-progress').then((m) => m.BasicProgress),
        title: 'Progrès · Math Challenge',
      },
    ],
  },
];
