import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminApiService,
  AdminResource,
  ApproveSuggestionRequest,
  CurriculumImportDocument,
  CurriculumImportResult,
  ImportProvider,
  ProviderDescriptor,
  ResourceReviewAction,
  ResourceReview,
  ResourceReviewStatus,
  ResourceSuggestion,
} from './admin-api.service';
import { ResourceCycle } from '../../core/api/resource.service';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslationKey } from '../../core/i18n/translations';

function isImportDocument(value: unknown): value is CurriculumImportDocument {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record['code'] === 'string' &&
    typeof record['nameFr'] === 'string' &&
    typeof record['nameAr'] === 'string' &&
    typeof record['versionCode'] === 'string' &&
    typeof record['effectiveFrom'] === 'string'
  );
}

type SuggestionProviderFilter = ImportProvider | 'ALL';

function errorText(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    return error.error?.message ?? error.message;
  }
  return error instanceof Error ? error.message : 'Unexpected error';
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [FormsModule],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard {
  protected static readonly SUGGESTION_PROVIDERS: readonly ImportProvider[] = [
    'WIKIMEDIA',
    'OPENSTAX',
    'GEOGEBRA',
    'PHET',
  ];

  protected readonly i18n = inject(LanguageService);
  private readonly api = inject(AdminApiService);
  protected readonly suggestionProviders = AdminDashboard.SUGGESTION_PROVIDERS;

  protected readonly curriculumDocument = signal<CurriculumImportDocument | null>(null);
  protected readonly selectedFile = signal('');
  protected readonly preview = signal<CurriculumImportResult | null>(null);
  protected readonly curriculumBusy = signal(false);
  protected readonly curriculumError = signal('');
  protected readonly curriculumMessage = signal('');
  protected readonly publishAfterImport = signal(false);

  protected readonly resources = signal<readonly AdminResource[]>([]);
  protected readonly suggestions = signal<readonly ResourceSuggestion[]>([]);
  protected readonly selectedProvider = signal<SuggestionProviderFilter>('ALL');
  protected readonly suggestionsLoading = signal(true);
  protected readonly suggestionsError = signal(false);
  protected readonly suggestionApprovalBusyUrl = signal<string | null>(null);
  protected readonly reviewHistories = signal<Readonly<Record<string, readonly ResourceReview[]>>>(
    {},
  );
  protected readonly reviewHistoryResourceId = signal<string | null>(null);
  protected readonly reviewHistoryBusyId = signal<string | null>(null);
  protected readonly reviewHistoryError = signal('');
  protected readonly providers = signal<readonly ProviderDescriptor[]>([]);
  protected readonly resourceBusy = signal(false);
  protected readonly resourceError = signal('');
  protected readonly resourceMessage = signal('');

  constructor() {
    this.loadResources();
    this.loadProviders();
    this.loadSuggestions();
  }

  protected loadSuggestions(): void {
    this.suggestionsLoading.set(true);
    this.suggestionsError.set(false);
    this.api.listSuggestions().subscribe({
      next: (items) => {
        this.suggestions.set(items);
        this.suggestionsLoading.set(false);
      },
      error: () => {
        this.suggestionsError.set(true);
        this.suggestionsLoading.set(false);
      },
    });
  }

  protected suggestionsFor(cycle: string): readonly ResourceSuggestion[] {
    const provider = this.selectedProvider();
    return this.suggestions().filter(
      (item) =>
        item.cycle === cycle &&
        (provider === 'ALL' || item.provider === provider),
    );
  }

  protected changeProviderFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (
      value === 'ALL' ||
      AdminDashboard.SUGGESTION_PROVIDERS.includes(value as ImportProvider)
    ) {
      this.selectedProvider.set(value as SuggestionProviderFilter);
    }
  }

  protected providerFilterLabel(value: SuggestionProviderFilter): TranslationKey {
    return `admin.resources.provider.${value}` as TranslationKey;
  }

  protected suggestionKey(suggestion: ResourceSuggestion): string {
    return `${suggestion.provider}:${suggestion.url}`;
  }

  protected suggestionCycleLabel(cycle: string): TranslationKey {
    if (cycle === 'PRIMARY') return 'resources.level.primary';
    if (cycle === 'BASIC') return 'resources.level.basic';
    return 'resources.level.secondary';
  }

  protected suggestionRightsLabel(provider: ImportProvider): TranslationKey {
    return `admin.resources.suggestionRights.${provider}` as TranslationKey;
  }

  protected approveSuggestion(suggestion: ResourceSuggestion): void {
    const key = this.suggestionKey(suggestion);
    if (this.suggestionApprovalBusyUrl()) {
      return;
    }
    const request: ApproveSuggestionRequest = {
      provider: suggestion.provider,
      cycle: suggestion.cycle,
      language: suggestion.language,
      query: suggestion.query,
      resultUrl: suggestion.url,
    };
    this.suggestionApprovalBusyUrl.set(key);
    this.resourceError.set('');
    this.resourceMessage.set('');
    this.api.approveSuggestion(request).subscribe({
      next: (resource) => {
        this.suggestions.update((items) =>
          items.filter((item) => this.suggestionKey(item) !== key),
        );
        this.resources.update((items) =>
          items.some((item) => item.id === resource.id) ? items : [resource, ...items],
        );
        this.suggestionApprovalBusyUrl.set(null);
        this.resourceMessage.set(this.i18n.translate('admin.resources.suggestionApproved'));
      },
      error: (error: unknown) => {
        this.suggestionApprovalBusyUrl.set(null);
        this.resourceError.set(errorText(error));
      },
    });
  }

  protected async selectCurriculumFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.curriculumError.set('');
    this.curriculumMessage.set('');
    this.preview.set(null);
    this.curriculumDocument.set(null);
    this.selectedFile.set('');
    if (!file) {
      return;
    }

    try {
      const parsed: unknown = JSON.parse(await file.text());
      if (!isImportDocument(parsed)) {
        throw new Error(this.i18n.translate('admin.fileShapeError'));
      }
      this.curriculumDocument.set({ ...parsed, publish: false });
      this.selectedFile.set(file.name);
    } catch (error: unknown) {
      this.curriculumError.set(errorText(error));
    }
  }

  protected validateCurriculum(): void {
    const document = this.curriculumDocument();
    if (!document) {
      return;
    }
    this.curriculumBusy.set(true);
    this.curriculumError.set('');
    this.curriculumMessage.set('');
    this.api.validateCurriculum(document).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.curriculumBusy.set(false);
      },
      error: (error: unknown) => {
        this.curriculumError.set(errorText(error));
        this.curriculumBusy.set(false);
      },
    });
  }

  protected setPublishAfterImport(event: Event): void {
    this.publishAfterImport.set((event.target as HTMLInputElement).checked);
  }

  protected importCurriculum(): void {
    const document = this.curriculumDocument();
    if (!document || !this.preview()?.valid) {
      return;
    }
    this.curriculumBusy.set(true);
    this.curriculumError.set('');
    this.curriculumMessage.set('');
    this.api.importCurriculum(document, this.publishAfterImport()).subscribe({
      next: (result) => {
        this.preview.set(result);
        this.curriculumMessage.set(
          result.published
            ? this.i18n.translate('admin.published')
            : this.i18n.translate('admin.imported'),
        );
        this.curriculumBusy.set(false);
      },
      error: (error: unknown) => {
        this.curriculumError.set(errorText(error));
        this.curriculumBusy.set(false);
      },
    });
  }

  protected reviewResource(resource: AdminResource, action: ResourceReviewAction): void {
    this.resourceBusy.set(true);
    this.resourceError.set('');
    this.resourceMessage.set('');
    this.api.reviewResource(resource.id, action).subscribe({
      next: (updated) => {
        this.resources.update((items) =>
          items.map((item) => (item.id === updated.id ? updated : item)),
        );
        this.clearReviewHistory(resource.id);
        if (this.reviewHistoryResourceId() === resource.id) {
          this.reviewHistoryResourceId.set(null);
        }
        this.resourceBusy.set(false);
      },
      error: (error: unknown) => {
        this.resourceError.set(errorText(error));
        this.resourceBusy.set(false);
      },
    });
  }

  protected toggleReviewHistory(resource: AdminResource): void {
    if (this.reviewHistoryResourceId() === resource.id) {
      this.reviewHistoryResourceId.set(null);
      this.reviewHistoryError.set('');
      return;
    }

    this.reviewHistoryResourceId.set(resource.id);
    this.reviewHistoryError.set('');
    if (this.reviewHistories()[resource.id] || this.reviewHistoryBusyId() === resource.id) {
      return;
    }

    this.reviewHistoryBusyId.set(resource.id);
    this.api.reviewHistory(resource.id).subscribe({
      next: (history) => {
        this.reviewHistories.update((histories) => ({ ...histories, [resource.id]: history }));
        if (this.reviewHistoryBusyId() === resource.id) {
          this.reviewHistoryBusyId.set(null);
        }
      },
      error: (error: unknown) => {
        if (this.reviewHistoryBusyId() === resource.id) {
          this.reviewHistoryBusyId.set(null);
        }
        if (this.reviewHistoryResourceId() === resource.id) {
          this.reviewHistoryError.set(errorText(error));
        }
      },
    });
  }

  protected reviewHistoryFor(resourceId: string): readonly ResourceReview[] {
    return this.reviewHistories()[resourceId] ?? [];
  }

  protected reviewDecisionLabel(decision: ResourceReviewAction): string {
    switch (decision) {
      case 'SUBMIT':
        return this.i18n.translate('admin.resources.submit');
      case 'APPROVE':
        return this.i18n.translate('admin.resources.approve');
      case 'REJECT':
        return this.i18n.translate('admin.resources.reject');
    }
  }

  protected reviewedAtLabel(value: string): string {
    return new Date(value).toLocaleString(this.i18n.language());
  }

  protected statusLabel(status: ResourceReviewStatus): string {
    return this.i18n.translate(`admin.status.${status}`);
  }

  protected cycleLabel(cycle: ResourceCycle): string {
    return this.i18n.translate(`resources.level.${cycle.toLowerCase()}` as TranslationKey);
  }

  protected canSubmit(status: ResourceReviewStatus): boolean {
    return status === 'DRAFT' || status === 'REJECTED';
  }

  protected canReview(status: ResourceReviewStatus): boolean {
    return status === 'SUBMITTED';
  }

  protected loadResources(): void {
    this.resourceBusy.set(true);
    this.resourceError.set('');
    this.api.listResources().subscribe({
      next: (resources) => {
        this.resources.set(resources);
        this.resourceBusy.set(false);
      },
      error: (error: unknown) => {
        this.resourceError.set(errorText(error));
        this.resourceBusy.set(false);
      },
    });
  }

  protected loadProviders(): void {
    this.api.listProviders().subscribe({
      next: (providers) => this.providers.set(providers),
      error: (error: unknown) => this.resourceError.set(errorText(error)),
    });
  }

  private clearReviewHistory(resourceId: string): void {
    const histories = { ...this.reviewHistories() };
    delete histories[resourceId];
    this.reviewHistories.set(histories);
  }
}
