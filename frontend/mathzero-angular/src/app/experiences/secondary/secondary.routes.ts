import { Routes } from '@angular/router';

import { experienceGuard } from '../../core/experience/experience.guard';
import { SecondaryShell } from './shell/secondary-shell';

export const SECONDARY_ROUTES: Routes = [
  {
    path: '',
    component: SecondaryShell,
    canActivate: [experienceGuard],
    data: { experience: 'secondary' },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./dashboard/secondary-dashboard').then((m) => m.SecondaryDashboard),
        title: 'Math Lab & Bac Mastery · Tableau de bord',
      },
      {
        path: 'chapters',
        loadComponent: () =>
          import('./chapters/secondary-chapters').then((m) => m.SecondaryChapters),
        title: 'Math Lab & Bac Mastery · Chapitres',
      },
      {
        path: 'lesson',
        loadComponent: () =>
          import('./lesson/secondary-lesson').then((m) => m.SecondaryLesson),
        title: 'Math Lab & Bac Mastery · Leçon',
      },
      {
        path: 'lab',
        loadComponent: () =>
          import('./lab/secondary-lab').then((m) => m.SecondaryLab),
        title: 'Math Lab & Bac Mastery · Laboratoire',
      },
      {
        path: 'exams',
        loadComponent: () =>
          import('./exams/secondary-exams').then((m) => m.SecondaryExams),
        title: 'Math Lab & Bac Mastery · Examens',
      },
      {
        path: 'revision',
        loadComponent: () =>
          import('./revision/secondary-revision').then((m) => m.SecondaryRevision),
        title: 'Math Lab & Bac Mastery · Révision',
      },
    ],
  },
];