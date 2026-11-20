import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { isEducationCycle } from './education-cycle';
import { ProfileService } from '../profile/profile.service';

/**
 * Keeps a signed-in learner inside their curriculum cycle.
 *
 * No profile means preview mode, so the route is allowed. Once the identity and
 * curriculum services populate the profile, a mismatched deep link is redirected
 * to the learner's own experience.
 */
export const experienceGuard: CanActivateFn = (route) => {
  const required = route.data['experience'];
  const profile = inject(ProfileService);
  const router = inject(Router);
  const actual = profile.cycle();

  if (!isEducationCycle(required) || actual === null || actual === required) {
    return true;
  }

  return router.createUrlTree([`/${actual}`]);
};
