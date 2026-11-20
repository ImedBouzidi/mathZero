import { Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { API_BASE_URL } from '../../core/config/api-config';
import { GatewayHealth, HealthService } from '../../core/api/health.service';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslationKey } from '../../core/i18n/translations';
import { AppButton } from '../../design-system/button/app-button';
import { AppCard } from '../../design-system/card/app-card';
import { AppFormula } from '../../design-system/formula/app-formula';
import { AppIcon } from '../../design-system/icon/app-icon';

/**
 * Public platform-status page. Preserves the Phase 1 health check: loading,
 * success and recoverable error states are part of the acceptance criteria.
 */
@Component({
  selector: 'app-status-page',
  imports: [RouterLink, AppButton, AppCard, AppFormula, AppIcon],
  templateUrl: './status-page.html',
  styleUrl: './status-page.scss',
})
export class StatusPage {
  private readonly healthService = inject(HealthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly i18n = inject(LanguageService);
  protected readonly apiBaseUrl = inject(API_BASE_URL);

  protected readonly loading = signal(true);
  protected readonly health = signal<GatewayHealth | null>(null);

  protected readonly phaseItems: readonly TranslationKey[] = [
    'home.phase.gateway',
    'home.phase.discovery',
    'home.phase.data',
    'home.phase.i18n',
  ];

  constructor() {
    this.refresh();
  }

  protected refresh(): void {
    this.loading.set(true);
    this.health.set(null);

    this.healthService
      .check()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (health) => {
          this.health.set(health);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }
}
