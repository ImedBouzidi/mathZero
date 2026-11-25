import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppIcon } from '../../design-system/icon/app-icon';
import { IconName } from '../../design-system/icon/icon-paths';

interface ExperienceCard {
  readonly id: 'primary' | 'basic' | 'secondary';
  readonly path: string;
  readonly icon: IconName;
  readonly nameKey:
    | 'landing.primary.name'
    | 'landing.basic.name'
    | 'landing.secondary.name';
  readonly taglineKey:
    | 'landing.primary.tagline'
    | 'landing.basic.tagline'
    | 'landing.secondary.tagline';
  readonly bodyKey:
    | 'landing.primary.body'
    | 'landing.basic.body'
    | 'landing.secondary.body';
  readonly pointKeys: readonly [
    | 'landing.primary.point1'
    | 'landing.basic.point1'
    | 'landing.secondary.point1',
    | 'landing.primary.point2'
    | 'landing.basic.point2'
    | 'landing.secondary.point2',
    | 'landing.primary.point3'
    | 'landing.basic.point3'
    | 'landing.secondary.point3',
  ];
}

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink, AppButton, AppIcon],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.scss',
})
export class LandingPage {
  protected readonly i18n = inject(LanguageService);
  protected readonly auth = inject(AuthService);

  constructor() {
    this.auth.ensureSession().subscribe();
  }

  protected readonly experiences: readonly ExperienceCard[] = [
    {
      id: 'primary',
      path: '/primary',
      icon: 'map',
      nameKey: 'landing.primary.name',
      taglineKey: 'landing.primary.tagline',
      bodyKey: 'landing.primary.body',
      pointKeys: [
        'landing.primary.point1',
        'landing.primary.point2',
        'landing.primary.point3',
      ],
    },
    {
      id: 'basic',
      path: '/basic',
      icon: 'bar-chart',
      nameKey: 'landing.basic.name',
      taglineKey: 'landing.basic.tagline',
      bodyKey: 'landing.basic.body',
      pointKeys: [
        'landing.basic.point1',
        'landing.basic.point2',
        'landing.basic.point3',
      ],
    },
    {
      id: 'secondary',
      path: '/secondary',
      icon: 'flask',
      nameKey: 'landing.secondary.name',
      taglineKey: 'landing.secondary.tagline',
      bodyKey: 'landing.secondary.body',
      pointKeys: [
        'landing.secondary.point1',
        'landing.secondary.point2',
        'landing.secondary.point3',
      ],
    },
  ];

  protected readonly sharedKeys = [
    { icon: 'globe' as IconName, key: 'landing.shared.i18n' as const },
    { icon: 'shield' as IconName, key: 'landing.shared.a11y' as const },
    { icon: 'sigma' as IconName, key: 'landing.shared.math' as const },
    { icon: 'layers' as IconName, key: 'landing.shared.api' as const },
  ];
}
