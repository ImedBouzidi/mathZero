import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppCard } from '../../design-system/card/app-card';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, AppButton, AppCard],
  template: `
    <section class="mx-auto w-full max-w-md px-4 py-14 sm:px-6">
      <app-card title="Connexion" heading="h2">
        <form [formGroup]="form" (ngSubmit)="submit()" class="grid gap-4">
          <label class="grid gap-1 text-sm text-ink">
            Email
            <input
              type="email"
              formControlName="email"
              autocomplete="email"
              class="focus-ring rounded-control border border-line bg-canvas px-3 py-2"
            />
          </label>
          <label class="grid gap-1 text-sm text-ink">
            Mot de passe
            <span class="relative">
              <input
                [attr.type]="passwordVisible() ? 'text' : 'password'"
                formControlName="password"
                autocomplete="current-password"
                class="focus-ring w-full rounded-control border border-line bg-canvas px-3 py-2 pe-11"
              />
              <button
                type="button"
                class="focus-ring absolute end-2 top-1/2 -translate-y-1/2 rounded-control px-2 py-1 text-xs font-semibold text-accent"
                [attr.aria-label]="passwordVisible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
                (click)="passwordVisible.set(!passwordVisible())"
              >
                {{ passwordVisible() ? 'Masquer' : 'Afficher' }}
              </button>
            </span>
          </label>
          @if (error()) {
            <p class="text-sm text-red-600" role="alert">{{ error() }}</p>
          }
          <button appButton type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Connexion…' : 'Se connecter' }}
          </button>
          <a routerLink="/register" class="text-center text-sm font-medium text-accent">
            Créer un compte
          </a>
        </form>
      </app-card>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly i18n = inject(LanguageService);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly passwordVisible = signal(false);

  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/onboarding';
        void this.router.navigateByUrl(returnUrl);
      },
      error: (error: HttpErrorResponse) => {
        const body = error.error as { code?: string; message?: string } | null;
        if (body?.code === 'INVALID_CREDENTIALS' || error.status === 401) {
          this.error.set('Adresse e-mail ou mot de passe incorrect.');
        } else if (body?.code === 'CSRF_INVALID' || error.status === 403) {
          this.error.set('La validation de sécurité a échoué. Rechargez la page puis réessayez.');
        } else {
          this.error.set(body?.message || 'Connexion impossible');
        }
      },
      complete: () => this.loading.set(false),
    });
  }
}
