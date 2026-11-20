import { Routes } from '@angular/router';

import { experienceGuard } from './core/experience/experience.guard';

/**
 * Top-level routing.
 *
 * The landing page (with experience selection) is mounted at "".
 * Authenticated learners are redirected to their registered experience by
 * the experienceGuard. Unauthenticated users enter preview mode.
 *
 * Three experience shells are lazy‑loaded. Each owns its own navigation,
 * theme and dashboard surface.
 *
 *   /primary    → Math Adventure (primary)
 *   /basic      → Math Challenge (basic)
 *   /secondary  → Math Lab & Bac Mastery (secondary)
 *
 * Status and onboarding remain under the neutral BrandShell.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/landing/landing-page').then((m) => m.LandingPage),
    title: 'Mathematics Zero to Hero',
  },
  {
    path: 'status',
    loadComponent: () => import('./features/status/status-page').then((m) => m.StatusPage),
    title: 'État de la plateforme · Mathematics Zero to Hero',
  },
  {
    path: 'onboarding',
    loadComponent: () => import('./features/onboarding/onboarding-page').then((m) => m.OnboardingPage),
    title: 'Choisir ton niveau · Mathematics Zero to Hero',
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/account/profile-page').then((m) => m.ProfilePage),
    title: 'Profil · Mathematics Zero to Hero',
  },
  {
    path: 'settings',
    loadComponent: () => import('./features/account/settings-page').then((m) => m.SettingsPage),
    title: 'Paramètres · Mathematics Zero to Hero',
  },
  {
    path: 'primary',
    canActivate: [experienceGuard],
    data: { experience: 'primary' },
    loadComponent: () => import('./experiences/primary/shell/primary-shell').then((m) => m.PrimaryShell),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./experiences/primary/dashboard/primary-dashboard').then((m) => m.PrimaryDashboard), title: 'Carte · Math Adventure' },
      { path: 'lessons', loadComponent: () => import('./experiences/primary/lessons/primary-lesson').then((m) => m.PrimaryLesson), title: 'Leçon · Math Adventure' },
      { path: 'exercises', loadComponent: () => import('./experiences/primary/exercises/primary-exercises').then((m) => m.PrimaryExercises), title: 'Missions · Math Adventure' },
      { path: 'progress', loadComponent: () => import('./experiences/primary/progress/primary-progress').then((m) => m.PrimaryProgress), title: 'Mon avancement · Math Adventure' },
      { path: 'rewards', loadComponent: () => import('./experiences/primary/rewards/primary-rewards').then((m) => m.PrimaryRewards), title: 'Récompenses · Math Adventure' },
    ],
  },
  {
    path: 'basic',
    canActivate: [experienceGuard],
    data: { experience: 'basic' },
    loadComponent: () => import('./experiences/basic/shell/basic-shell').then((m) => m.BasicShell),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./experiences/basic/dashboard/basic-dashboard').then((m) => m.BasicDashboard), title: 'Tableau de bord · Math Challenge' },
      { path: 'lessons', loadComponent: () => import('./experiences/basic/lessons/basic-lessons').then((m) => m.BasicLessons), title: 'Chapitres · Math Challenge' },
      { path: 'lesson/:lessonId', loadComponent: () => import('./experiences/basic/lessons/basic-lesson').then((m) => m.BasicLesson), title: 'Leçon · Math Challenge' },
      { path: 'exercises', loadComponent: () => import('./experiences/basic/exercises/basic-exercises').then((m) => m.BasicExercises), title: 'Exercices · Math Challenge' },
      { path: 'challenges', loadComponent: () => import('./experiences/basic/challenges/basic-challenges').then((m) => m.BasicChallenges), title: 'Défis · Math Challenge' },
      { path: 'progress', loadComponent: () => import('./experiences/basic/progress/basic-progress').then((m) => m.BasicProgress), title: 'Progrès · Math Challenge' },
    ],
  },
  {
    path: 'secondary',
    canActivate: [experienceGuard],
    data: { experience: 'secondary' },
    loadComponent: () => import('./experiences/secondary/shell/secondary-shell').then((m) => m.SecondaryShell),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./experiences/secondary/dashboard/secondary-dashboard').then((m) => m.SecondaryDashboard), title: 'Math Lab & Bac Mastery · Tableau de bord' },
      { path: 'chapters', loadComponent: () => import('./experiences/secondary/chapters/secondary-chapters').then((m) => m.SecondaryChapters), title: 'Math Lab & Bac Mastery · Chapitres' },
      { path: 'lesson', loadComponent: () => import('./experiences/secondary/lesson/secondary-lesson').then((m) => m.SecondaryLesson), title: 'Math Lab & Bac Mastery · Leçon' },
      { path: 'lab', loadComponent: () => import('./experiences/secondary/lab/secondary-lab').then((m) => m.SecondaryLab), title: 'Math Lab & Bac Mastery · Laboratoire' },
      { path: 'exams', loadComponent: () => import('./experiences/secondary/exams/secondary-exams').then((m) => m.SecondaryExams), title: 'Math Lab & Bac Mastery · Examens' },
      { path: 'revision', loadComponent: () => import('./experiences/secondary/revision/secondary-revision').then((m) => m.SecondaryRevision), title: 'Math Lab & Bac Mastery · Révision' },
    ],
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
    title: 'Page introuvable · Mathematics Zero to Hero',
  },
];