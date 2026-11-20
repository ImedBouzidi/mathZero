import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { LanguageSwitch } from '../../core/i18n/language-switch';
import { LanguageService } from '../../core/i18n/language.service';

/**
 * Responsive application shell shared by learner-facing routes.
 *
 * The shell owns the header, primary navigation, language switch and footer.
 * Feature routes render inside the <main> landmark.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, LanguageSwitch],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  protected readonly i18n = inject(LanguageService);
}
