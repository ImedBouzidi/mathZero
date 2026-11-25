import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { LanguageService } from '../../core/i18n/language.service';
import { AppButton } from '../../design-system/button/app-button';
import { AppCard } from '../../design-system/card/app-card';

@Component({
  selector: 'app-admin-login-page',
  imports: [ReactiveFormsModule, RouterLink, AppButton, AppCard],
  template: `
    <section class="mx-auto grid w-full max-w-md gap-5 px-4 py-14 sm:px-6 sm:py-20">
      <div class="text-center">
        <span class="mx-auto grid size-12 place-items-center rounded-control bg-accent text-xl font-bold text-accent-contrast" aria-hidden="true">
          ∑
        </span>
        <p class="mt-4 text-xs font-semibold uppercase tracking-[0.1em] text-accent">
          {{ i18n.language() === 'fr' ? 'Administration' : 'الإدارة' }}
        </p>
        <h1 class="mt-2 text-3xl font-semibold tracking-tight text-ink">
          {{ i18n.language() === 'fr' ? 'Connexion administrateur' : 'تسجيل دخول المسؤول' }}
        </h1>
      </div>

      <app-card>
        <form [formGroup]="form" (ngSubmit)="submit()" class="grid gap-4">
          <label class="grid gap-1 text-sm font-medium text-ink">
            {{ i18n.language() === 'fr' ? 'Adresse e-mail' : 'البريد الإلكتروني' }}
            <input
              type="email"
              formControlName="email"
              autocomplete="username"
              class="focus-ring rounded-control border border-line bg-canvas px-3 py-2.5 text-base"
              [attr.placeholder]="i18n.language() === 'fr' ? 'admin@mathplay.com' : 'admin@mathplay.com'"
            />
          </label>

          <label class="grid gap-1 text-sm font-medium text-ink">
            {{ i18n.language() === 'fr' ? 'Mot de passe' : 'كلمة المرور' }}
            <span class="relative">
              <input
                [attr.type]="passwordVisible() ? 'text' : 'password'"
                formControlName="password"
                autocomplete="current-password"
                class="focus-ring w-full rounded-control border border-line bg-canvas px-3 py-2.5 pe-11 text-base"
              />
              <button
                type="button"
                class="focus-ring absolute end-2 top-1/2 -translate-y-1/2 rounded-control px-2 py-1 text-xs font-semibold text-accent"
                [attr.aria-label]="passwordVisible()
                  ? (i18n.language() === 'fr' ? 'Masquer le mot de passe' : 'إخفاء كلمة المرور')
                  : (i18n.language() === 'fr' ? 'Afficher le mot de passe' : 'إظهار كلمة المرور')"
                (click)="passwordVisible.set(!passwordVisible())"
              >
                {{ passwordVisible()
                  ? (i18n.language() === 'fr' ? 'Masquer' : 'إخفاء')
                  : (i18n.language() === 'fr' ? 'Afficher' : 'إظهار') }}
              </button>
            </span>
          </label>

          @if (error()) {
            <p class="rounded-control border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {{ error() }}
            </p>
          }

          <button appButton type="submit" variant="primary" [disabled]="form.invalid || loading()">
            {{ loading()
              ? (i18n.language() === 'fr' ? 'Connexion…' : 'جارٍ تسجيل الدخول…')
              : (i18n.language() === 'fr' ? 'Se connecter' : 'تسجيل الدخول') }}
          </button>

          <a routerLink="/" class="text-center text-sm font-medium text-accent">
            {{ i18n.language() === 'fr' ? 'Retour à l’accueil' : 'العودة إلى الرئيسية' }}
          </a>
        </form>
      </app-card>
    </section>
  `,
})
export class AdminLoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly i18n = inject(LanguageService);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly passwordVisible = signal(false);

  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        if (!this.auth.user()?.roles.includes('ADMIN')) {
          this.auth.clearSession();
          this.error.set(
            this.i18n.language() === 'fr'
              ? 'Ce compte n’a pas accès à l’administration.'
              : 'هذا الحساب لا يملك صلاحية الدخول إلى لوحة الإدارة.',
          );
          this.loading.set(false);
          return;
        }

        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/admin';
        void this.router.navigateByUrl(returnUrl);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        const body = error.error as { code?: string; message?: string } | null;
        if (body?.code === 'INVALID_CREDENTIALS' || error.status === 401) {
          this.error.set(
            this.i18n.language() === 'fr'
              ? 'Adresse e-mail ou mot de passe incorrect.'
              : 'البريد الإلكتروني أو كلمة المرور غير صحيحة.',
          );
        } else if (body?.code === 'CSRF_INVALID' || error.status === 403) {
          this.error.set(
            this.i18n.language() === 'fr'
              ? 'La validation de sécurité a échoué. Rechargez la page puis réessayez.'
              : 'فشل التحقق الأمني. أعد تحميل الصفحة وحاول مجددًا.',
          );
        } else {
          this.error.set(
            body?.message ||
              (this.i18n.language() === 'fr'
                ? 'Connexion administrateur impossible.'
                : 'تعذر تسجيل دخول المسؤول.'),
          );
        }
        this.loading.set(false);
      },
    });
  }
}
