import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.ensureSession().pipe(
    map((authenticated) => {
      if (authenticated && auth.user()?.roles.includes('ADMIN')) {
        return true;
      }
      return router.createUrlTree(authenticated ? ['/'] : ['/login'], {
        queryParams: authenticated ? undefined : { returnUrl: state.url },
      });
    }),
  );
};
