import { Routes } from '@angular/router';

import { experienceGuard } from '../../core/experience/experience.guard';
import { PrimaryShell } from './shell/primary-shell';

export const PRIMARY_ROUTES: Routes = [
  {
    path: '',
    component: PrimaryShell,
    canActivate: [experienceGuard],
    data: { experience: 'primary' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./dashboard/primary-dashboard').then((m) => m.PrimaryDashboard),
        title: 'Carte · Math Adventure',
      },
      {
        path: 'lessons',
        loadComponent: () => import('./lessons/primary-lesson').then((m) => m.PrimaryLesson),
        title: 'Leçon · Math Adventure',
      },
      {
        path: 'lessons/:lessonId',
        loadComponent: () => import('./lessons/primary-lesson').then((m) => m.PrimaryLesson),
        title: 'Leçon · Math Adventure',
      },
      {
        path: 'exercises',
        loadComponent: () =>
          import('./exercises/primary-exercises').then((m) => m.PrimaryExercises),
        title: 'Missions · Math Adventure',
      },
      {
        path: 'progress',
        loadComponent: () => import('./progress/primary-progress').then((m) => m.PrimaryProgress),
        title: 'Mon avancement · Math Adventure',
      },
      {
        path: 'rewards',
        loadComponent: () => import('./rewards/primary-rewards').then((m) => m.PrimaryRewards),
        title: 'Récompenses · Math Adventure',
      },
    ],
  },
];
