import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppBadge } from '../../../design-system/badge/app-badge';
import { AppButton } from '../../../design-system/button/app-button';
import { AppCard } from '../../../design-system/card/app-card';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';

@Component({
  selector: 'app-basic-dashboard',
  imports: [RouterLink, AppBadge, AppButton, AppCard, AppEmptyState, AppIcon],
  templateUrl: './basic-dashboard.html',
  styleUrl: './basic-dashboard.scss',
})
export class BasicDashboard {
  protected readonly i18n = inject(LanguageService);
}
