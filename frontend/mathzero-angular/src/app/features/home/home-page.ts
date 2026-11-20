import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { API_BASE_URL } from '../../core/config/api-config';
import { GatewayHealth, HealthService } from '../../core/api/health.service';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslationKey } from '../../core/i18n/translations';
import { AppButton } from '../../shared/ui/button/app-button';
import { AppCard } from '../../shared/ui/card/app-card';

/**
 * Landing page for Phase 1.
 *
 * It doubles as the acceptance smoke check: it calls the Gateway health
 * endpoint through the public API and renders loading, success and failure
 * states.
 */
@Component({
  selector: 'app-home-page',
  imports: [AppButton, AppCard],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
})
export class HomePage {
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
