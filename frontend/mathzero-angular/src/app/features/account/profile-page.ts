import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { EXPERIENCE_LIST } from '../../core/experience/education-cycle';
import { LanguageService } from '../../core/i18n/language.service';
import { ProfileService } from '../../core/profile/profile.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppCard } from '../../design-system/card/app-card';
import { AppIcon } from '../../design-system/icon/app-icon';
import { AppPageHeader } from '../../design-system/page-header/app-page-header';

@Component({
  selector: 'app-profile-page',
  imports: [RouterLink, AppButton, AppCard, AppIcon, AppPageHeader],
  template: `
    <section class="mx-auto w-full max-w-5xl px-4 py-12 sm:px-6 sm:py-16">
      <app-page-header
        [eyebrow]="i18n.translate('account.profile.title')"
        [title]="i18n.translate('account.profile.title')"
        [description]="i18n.translate('account.profile.subtitle')"
      />

      <div class="mt-8 grid gap-5 md:grid-cols-2">
        <app-card [title]="i18n.translate('account.profile.guest.title')" heading="h2">
          <div class="flex items-start gap-3">
            <span class="grid size-11 flex-none place-items-center rounded-full bg-subtle text-muted">
              <app-icon name="user" [size]="22" />
            </span>
            <div>
              <p class="text-sm leading-6 text-muted">
                {{ i18n.translate('account.profile.guest.body') }}
              </p>
              @if (auth.isAuthenticated()) {
                <button appButton type="button" variant="secondary" size="sm" class="mt-4" (click)="logout()">
                  Se déconnecter
                </button>
              } @else {
                <a routerLink="/login" appButton variant="secondary" size="sm" class="mt-4">
                {{ i18n.translate('account.profile.signIn') }}
                <app-icon name="lock" [size]="15" />
                </a>
              }
            </div>
          </div>
        </app-card>

        <app-card [title]="i18n.translate('account.profile.experience')" heading="h2">
          <dl class="space-y-3 text-sm">
            <div class="flex items-baseline justify-between gap-4 border-b border-line pb-3">
              <dt class="text-muted">{{ i18n.translate('account.profile.grade') }}</dt>
              <dd class="font-medium text-ink">
                {{ profile.gradeLabel() ?? i18n.translate('account.profile.notSet') }}
              </dd>
            </div>
            <div class="flex items-baseline justify-between gap-4 border-b border-line pb-3">
              <dt class="text-muted">{{ i18n.translate('account.profile.language') }}</dt>
              <dd class="font-medium text-ink">
                {{ i18n.language() === 'fr' ? i18n.translate('language.fr') : i18n.translate('language.ar') }}
              </dd>
            </div>
            <div class="flex items-baseline justify-between gap-4">
              <dt class="text-muted">{{ i18n.translate('account.profile.experience') }}</dt>
              <dd class="font-medium text-ink">
                {{ i18n.translate('common.preview') }}
              </dd>
            </div>
          </dl>
        </app-card>
      </div>

      <div class="mt-5">
        <app-card [title]="i18n.translate('account.profile.preview')" heading="h2">
          <div class="grid gap-3 sm:grid-cols-3">
            @for (experience of experiences; track experience.id) {
              <a
                [routerLink]="experience.basePath"
                class="focus-ring interactive-card rounded-surface border border-line bg-canvas p-4"
              >
                <p class="text-xs font-semibold uppercase tracking-[0.08em] text-accent">
                  {{ i18n.translate(experience.taglineKey) }}
                </p>
                <p class="mt-1 text-sm font-semibold text-ink">
                  {{ i18n.translate(experience.nameKey) }}
                </p>
                <span class="mt-3 inline-flex items-center gap-1 text-xs font-medium text-muted">
                  {{ i18n.translate('common.open') }}
                  <app-icon name="arrow-right" [size]="14" />
                </span>
              </a>
            }
          </div>
        </app-card>
      </div>

      <div class="mt-6 flex flex-wrap gap-3">
        <a routerLink="/settings" appButton variant="secondary">
          <app-icon name="settings" [size]="16" />
          {{ i18n.translate('common.settings') }}
        </a>
        <a routerLink="/" appButton variant="ghost">
          {{ i18n.translate('common.home') }}
        </a>
      </div>
    </section>
  `,
})
export class ProfilePage {
  protected readonly i18n = inject(LanguageService);
  protected readonly profile = inject(ProfileService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly experiences = EXPERIENCE_LIST;

  protected logout(): void {
    this.auth.logout().subscribe({
      complete: () => void this.router.navigateByUrl('/'),
    });
  }
}
