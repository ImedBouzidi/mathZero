import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppCard } from '../../design-system/card/app-card';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, AppButton, AppCard],
  template: `
    <section class="mx-auto w-full max-w-md px-4 py-14 sm:px-6">
      <app-card title="Créer un compte" heading="h2">
        <form [formGroup]="form" (ngSubmit)="submit()" class="grid gap-4">
          <label class="grid gap-1 text-sm text-ink">
            Nom affiché
            <input formControlName="displayName" autocomplete="name"
              class="focus-ring rounded-control border border-line bg-canvas px-3 py-2" />
          </label>
          <label class="grid gap-1 text-sm text-ink">
            Email
            <input type="email" formControlName="email" autocomplete="email"
              class="focus-ring rounded-control border border-line bg-canvas px-3 py-2" />
          </label>
          <label class="grid gap-1 text-sm text-ink">
            Mot de passe
            <input type="password" formControlName="password" autocomplete="new-password"
              class="focus-ring rounded-control border border-line bg-canvas px-3 py-2" />
          </label>
          @if (error()) {
            <p class="text-sm text-red-600" role="alert">{{ error() }}</p>
          }
          <button appButton type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Création…' : 'Créer mon compte' }}
          </button>
          <a routerLink="/login" class="text-center text-sm font-medium text-accent">
            J’ai déjà un compte
          </a>
        </form>
      </app-card>
    </section>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly i18n = inject(LanguageService);
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected readonly form = new FormGroup({
    displayName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(2)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8)] }),
  });

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl('/onboarding'),
      error: (error: { error?: { message?: string } }) =>
        this.error.set(error.error?.message || 'Inscription impossible'),
      complete: () => this.loading.set(false),
    });
  }
}
