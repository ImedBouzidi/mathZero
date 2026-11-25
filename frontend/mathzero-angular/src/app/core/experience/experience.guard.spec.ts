import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet } from '@angular/router';

import { ProfileService } from '../profile/profile.service';
import { experienceGuard } from './experience.guard';

@Component({ standalone: true, imports: [RouterOutlet], template: '<router-outlet />' })
class TestShell {}

@Component({ standalone: true, template: '' })
class TestPage {}

describe('experienceGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestShell, TestPage],
      providers: [
        provideRouter([
          {
            path: 'primary',
            component: TestShell,
            children: [{ path: '', component: TestPage }],
          },
          {
            path: 'secondary',
            component: TestShell,
            canActivate: [experienceGuard],
            data: { experience: 'secondary' },
            children: [
              { path: 'lesson/:resourceId', component: TestPage },
              { path: 'dashboard', component: TestPage },
            ],
          },
        ]),
      ],
    });

    TestBed.inject(ProfileService).setProfile({
      id: 'learner-1',
      displayName: 'Learner',
      gradeId: null,
      gradeLabel: null,
      cycle: 'primary',
      locale: 'fr',
      avatarUrl: null,
    });
  });

  it('allows a learner to open an animated lesson from another cycle', async () => {
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/secondary/lesson/resource-1');

    expect(router.url).toBe('/secondary/lesson/resource-1');
  });

  it('keeps other experience pages restricted to the learner profile cycle', async () => {
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/secondary/dashboard');

    expect(router.url).toBe('/primary');
  });
});
