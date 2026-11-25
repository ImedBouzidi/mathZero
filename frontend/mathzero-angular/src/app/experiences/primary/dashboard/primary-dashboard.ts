import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';
import { LevelResources } from '../../../shared/ui/level-resources/level-resources';

@Component({
  selector: 'app-primary-dashboard',
  imports: [RouterLink, AppButton, AppCard, AppEmptyState, AppIcon, LevelResources],
  templateUrl: './primary-dashboard.html',
  styleUrl: './primary-dashboard.scss',
})
export class PrimaryDashboard {
  protected readonly i18n = inject(LanguageService);
}
