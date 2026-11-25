import { Routes } from '@angular/router';

import { experienceGuard } from './core/experience/experience.guard';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';

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
    loadComponent: () => import('./shared/layout/brand-shell/brand-shell').then((m) => m.BrandShell),
    children: [
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
        path: 'curriculum',
        loadComponent: () =>
          import('./features/curriculum/curriculum-explorer').then((m) => m.CurriculumExplorer),
        title: 'Programme scolaire · Mathematics Zero to Hero',
      },
      {
        path: 'resources',
        loadComponent: () =>
          import('./features/resources/resources-page').then((m) => m.ResourcesPage),
        title: 'Ressources pédagogiques · Mathematics Zero to Hero',
      },
      {
        path: 'admin/login',
        loadComponent: () => import('./features/account/admin-login-page').then((m) => m.AdminLoginPage),
        title: 'Connexion administrateur · Mathematics Zero to Hero',
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/admin-dashboard').then((m) => m.AdminDashboard),
        title: 'Administration · Mathematics Zero to Hero',
      },
      {
        path: 'login',
        loadComponent: () => import('./features/account/login-page').then((m) => m.LoginPage),
        title: 'Connexion · Mathematics Zero to Hero',
      },
      {
        path: 'register',
        loadComponent: () => import('./features/account/register-page').then((m) => m.RegisterPage),
        title: 'Créer un compte · Mathematics Zero to Hero',
      },
      {
        path: 'onboarding',
        canActivate: [authGuard],
        loadComponent: () => import('./features/onboarding/onboarding-page').then((m) => m.OnboardingPage),
        title: 'Choisir ton niveau · Mathematics Zero to Hero',
      },
      {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () => import('./features/account/profile-page').then((m) => m.ProfilePage),
        title: 'Profil · Mathematics Zero to Hero',
      },
      {
        path: 'settings',
        canActivate: [authGuard],
        loadComponent: () => import('./features/account/settings-page').then((m) => m.SettingsPage),
        title: 'Paramètres · Mathematics Zero to Hero',
      },
    ],
  },
  {
    path: 'primary',
    canActivate: [experienceGuard],
    data: { experience: 'primary' },
    loadComponent: () => import('./experiences/primary/shell/primary-shell').then((m) => m.PrimaryShell),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./experiences/primary/dashboard/primary-dashboard').then((m) => m.PrimaryDashboard), title: 'Carte · Math Adventure' },
      { path: 'lessons/:resourceId', loadComponent: () => import('./features/learning/lesson-player').then((m) => m.LessonPlayer), title: 'Leçon animée · Math Adventure' },
      { path: 'lessons', loadComponent: () => import('./experiences/primary/lessons/primary-lesson').then((m) => m.PrimaryLesson), title: 'Chapitres · Math Adventure' },
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
      { path: 'lesson/:resourceId', loadComponent: () => import('./features/learning/lesson-player').then((m) => m.LessonPlayer), title: 'Leçon animée · Math Challenge' },
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
      { path: 'lesson/:resourceId', loadComponent: () => import('./features/learning/lesson-player').then((m) => m.LessonPlayer), title: 'Math Lab & Bac Mastery · Leçon animée' },
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