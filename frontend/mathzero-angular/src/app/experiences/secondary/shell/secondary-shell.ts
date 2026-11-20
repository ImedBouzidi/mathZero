import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ExperienceService } from '../../../core/experience/experience.service';
import { LanguageSwitch } from '../../../core/i18n/language-switch';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { IconName } from '../../../design-system/icon/icon-paths';

interface SecondaryNavItem {
  readonly path: string;
  readonly labelKey:
    | 'secondary.nav.overview'
    | 'secondary.nav.chapters'
    | 'secondary.nav.lab'
    | 'secondary.nav.exams'
    | 'secondary.nav.revision'
    | 'secondary.nav.profile';
  readonly icon: IconName;
  readonly exact: boolean;
}

@Component({
  selector: 'app-secondary-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, LanguageSwitch, AppIcon],
  templateUrl: './secondary-shell.html',
  styleUrl: './secondary-shell.scss',
  host: { 'data-experience': 'secondary' },
})
export class SecondaryShell {
  private readonly experience = inject(ExperienceService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly i18n = inject(LanguageService);
  protected readonly profile = inject(ProfileService);
  protected readonly menuOpen = signal(false);

  protected readonly navItems: readonly SecondaryNavItem[] = [
    { path: '/secondary', labelKey: 'secondary.nav.overview', icon: 'grid', exact: true },
    {
      path: '/secondary/chapters',
      labelKey: 'secondary.nav.chapters',
      icon: 'book-open',
      exact: false,
    },
    { path: '/secondary/lab', labelKey: 'secondary.nav.lab', icon: 'flask', exact: false },
    { path: '/secondary/exams', labelKey: 'secondary.nav.exams', icon: 'file-text', exact: false },
    {
      path: '/secondary/revision',
      labelKey: 'secondary.nav.revision',
      icon: 'refresh',
      exact: false,
    },
    { path: '/profile', labelKey: 'secondary.nav.profile', icon: 'user', exact: false },
  ];

  constructor() {
    this.experience.activate('secondary');
    this.destroyRef.onDestroy(() => this.experience.deactivate());
  }

  @HostListener('document:keydown.escape')
  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }
}
