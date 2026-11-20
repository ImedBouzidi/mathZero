import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LanguageService } from '../../../core/i18n/language.service';

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
}