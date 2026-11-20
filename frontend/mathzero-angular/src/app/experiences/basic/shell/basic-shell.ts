import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ExperienceService } from '../../../core/experience/experience.service';
import { LanguageSwitch } from '../../../core/i18n/language-switch';
import { LanguageService } from '../../../core/i18n/language.service';
import { ProfileService } from '../../../core/profile/profile.service';
import { AppBadge } from '../../../design-system/badge/app-badge';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { IconName } from '../../../design-system/icon/icon-paths';

interface BasicNavItem {
  readonly path: string;
  readonly labelKey:
    | 'basic.nav.dashboard'
    | 'basic.nav.chapters'
    | 'basic.nav.exercises'
    | 'basic.nav.challenges'
    | 'basic.nav.progress'
    | 'basic.nav.profile';
  readonly icon: IconName;
  readonly exact: boolean;
  readonly mobile: boolean;
}

@Component({
  selector: 'app-basic-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, LanguageSwitch, AppBadge, AppIcon],
  templateUrl: './basic-shell.html',
  styleUrl: './basic-shell.scss',
  host: { 'data-experience': 'basic' },
})
export class BasicShell {
  private readonly experience = inject(ExperienceService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly i18n = inject(LanguageService);
  protected readonly profile = inject(ProfileService);
  protected readonly menuOpen = signal(false);

  protected readonly navItems: readonly BasicNavItem[] = [
    {
      path: '/basic',
      labelKey: 'basic.nav.dashboard',
      icon: 'home',
      exact: true,
      mobile: true,
    },
    {
      path: '/basic/lessons',
      labelKey: 'basic.nav.chapters',
      icon: 'book-open',
      exact: false,
      mobile: true,
    },
    {
      path: '/basic/exercises',
      labelKey: 'basic.nav.exercises',
      icon: 'list-checks',
      exact: false,
      mobile: true,
    },
    {
      path: '/basic/challenges',
      labelKey: 'basic.nav.challenges',
      icon: 'trophy',
      exact: false,
      mobile: false,
    },
    {
      path: '/basic/progress',
      labelKey: 'basic.nav.progress',
      icon: 'bar-chart',
      exact: false,
      mobile: true,
    },
    {
      path: '/profile',
      labelKey: 'basic.nav.profile',
      icon: 'user',
      exact: false,
      mobile: false,
    },
  ];

  protected readonly mobileNav = this.navItems.filter((item) => item.mobile);

  constructor() {
    this.experience.activate('basic');
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
