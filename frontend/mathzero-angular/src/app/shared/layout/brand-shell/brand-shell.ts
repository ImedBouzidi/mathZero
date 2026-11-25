import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LanguageService } from '../../../core/i18n/language.service';
import { AuthService } from '../../../core/auth/auth.service';

/**
 * Neutral brand shell for landing, status, onboarding and account pages.
 * Experience routes provide their own shells.
 */
@Component({
  selector: 'app-brand-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './brand-shell.html',
  styleUrl: './brand-shell.scss',
})
export class BrandShell {
  protected readonly i18n = inject(LanguageService);
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected logout(): void {
    const destination = this.auth.user()?.roles.includes('ADMIN') ? '/login' : '/';
    this.auth.logout().subscribe({
      complete: () => void this.router.navigateByUrl(destination),
    });
  }
}