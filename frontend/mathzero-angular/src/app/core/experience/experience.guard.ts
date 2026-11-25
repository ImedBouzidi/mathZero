import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';

import { isEducationCycle } from './education-cycle';
import { ProfileService } from '../profile/profile.service';

/**
 * Keeps a signed-in learner inside their curriculum cycle, while allowing
 * animated resource lessons to be opened from the cross-cycle resource catalogue.
 *
 * No profile means preview mode, so the route is allowed. Once the identity and
 * curriculum services populate the profile, a mismatched deep link is redirected
 * to the learner's own experience unless it targets an animated resource lesson.
 */
export const experienceGuard: CanActivateFn = (route, state: RouterStateSnapshot) => {
  const required = route.data['experience'];
  const profile = inject(ProfileService);
  const router = inject(Router);
  const actual = profile.cycle();

  if (
    !isEducationCycle(required) ||
    actual === null ||
    actual === required ||
    isAnimatedLessonRoute(state.url, required)
  ) {
    return true;
  }

  return router.createUrlTree([`/${actual}`]);
};

function isAnimatedLessonRoute(url: string, cycle: string): boolean {
  const [path] = url.split(/[?#]/, 1);
  const segments = path.split('/').filter(Boolean);
  const lessonSegment = cycle === 'primary' ? 'lessons' : 'lesson';

  return (
    segments.length === 3 &&
    segments[0] === cycle &&
    segments[1] === lessonSegment &&
    segments[2] !== ''
  );
}
