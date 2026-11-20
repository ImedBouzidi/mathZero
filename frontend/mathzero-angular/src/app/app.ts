import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LanguageService } from './core/i18n/language.service';

/**
 * Root application component.
 *
 * BrandShell is the shared layout for all unauthenticated / landing /
 * status / onboarding / profile / settings pages. It provides the
 * global header, footer, language switch and a single <router-outlet />.
 *
 * Authenticated learners' cycle is handled by the experienceGuard:
 * once a profile is mounted, mismatched deep links are redirected to
 * the learner's own experience, and each experience shell mounts its
 * own theme via [data-experience].
 *
 * When the profile is null the user stays in preview mode — the
 * landing page is shown first, then any route is allowed.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  template: `<router-outlet />`,
})
export class App {
  protected readonly i18n = inject(LanguageService);
}