import { Component, DestroyRef, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ExperienceService } from '../../../core/experience/experience.service';
import { LanguageSwitch } from '../../../core/i18n/language-switch';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { IconName } from '../../../design-system/icon/icon-paths';

interface PrimaryNavItem {
  readonly path: string;
  readonly labelKey:
    | 'primary.nav.map'
    | 'primary.nav.chapters'
    | 'primary.nav.missions'
    | 'primary.nav.rewards'
    | 'primary.nav.me';
  readonly icon: IconName;
  readonly exact: boolean;
}

@Component({
  selector: 'app-primary-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, LanguageSwitch, AppIcon],
  templateUrl: './primary-shell.html',
  styleUrl: './primary-shell.scss',
  host: { 'data-experience': 'primary' },
})
export class PrimaryShell {
  private readonly experience = inject(ExperienceService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly i18n = inject(LanguageService);
  protected readonly profile = inject(ProfileService);

  protected readonly navItems: readonly PrimaryNavItem[] = [
    { path: '/primary', labelKey: 'primary.nav.map', icon: 'map', exact: true },
    {
      path: '/primary/lessons',
      labelKey: 'primary.nav.chapters',
      icon: 'book-open',
      exact: false,
    },
    {
      path: '/primary/exercises',
      labelKey: 'primary.nav.missions',
      icon: 'target',
      exact: false,
    },
    {
      path: '/primary/rewards',
      labelKey: 'primary.nav.rewards',
      icon: 'star',
      exact: false,
    },
    { path: '/profile', labelKey: 'primary.nav.me', icon: 'user', exact: false },
  ];

  constructor() {
    this.experience.activate('primary');
    this.destroyRef.onDestroy(() => this.experience.deactivate());
  }
}
