import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ResourceCycle, ResourceService, ResourceView } from '../../core/api/resource.service';
import { lessonPath } from '../../core/experience/education-cycle';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslationKey } from '../../core/i18n/translations';
import { AppCard } from '../../design-system/card/app-card';
import { AppEmptyState } from '../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../design-system/icon/app-icon';

interface CycleSection {
  readonly id: ResourceCycle;
  readonly labelKey: TranslationKey;
  readonly bodyKey: TranslationKey;
}

@Component({
  selector: 'app-resources-page',
  imports: [RouterLink, AppCard, AppEmptyState, AppIcon],
  template: `
    <section class="mx-auto w-full max-w-[var(--content-max)] px-4 py-12 sm:px-6 sm:py-16">
      <div class="mb-10 text-center">
        <h1 class="text-3xl font-semibold tracking-tight text-ink sm:text-4xl">
          {{ i18n.translate('resources.title') }}
        </h1>
        <p class="mx-auto mt-3 max-w-xl text-sm leading-6 text-muted sm:text-base">
          {{ i18n.translate('resources.subtitle') }}
        </p>
      </div>

      @if (loading()) {
        <p class="text-center text-sm text-muted" role="status" aria-live="polite">
          {{ i18n.translate('common.loading') }}
        </p>
      }

      @if (error()) {
        <div class="mb-8 flex flex-col items-start justify-between gap-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 sm:flex-row sm:items-center" role="alert">
          <p class="text-sm text-red-800">{{ i18n.translate('resources.loadError') }}</p>
          <button
            class="focus-ring rounded-control border border-red-200 px-3 py-2 text-sm font-semibold text-red-800 hover:bg-red-100"
            type="button"
            (click)="load()"
          >
            {{ i18n.translate('common.retry') }}
          </button>
        </div>
      }

      @if (!loading()) {
        <div class="space-y-8">
          @for (cycle of cycles; track cycle.id) {
            <app-card [title]="i18n.translate(cycle.labelKey)" [subtitle]="i18n.translate(cycle.bodyKey)" heading="h2">
              @if (resourcesFor(cycle.id).length === 0) {
                <app-empty-state
                  icon="book-open"
                  [title]="i18n.translate('resources.empty.title')"
                  [body]="i18n.translate('resources.empty.body')"
                />
              } @else {
                <ul class="space-y-3">
                  @for (resource of resourcesFor(cycle.id); track resource.id) {
                    <li class="rounded-2xl border border-line bg-canvas p-4 transition-colors hover:bg-subtle">
                      <div class="flex flex-wrap items-start justify-between gap-3">
                        <div class="min-w-0">
                          <h3 class="text-sm font-semibold text-ink">
                            <a [href]="resource.url" target="_blank" rel="noopener">{{ resource.title }}</a>
                          </h3>
                          @if (resource.description) {
                            <p class="mt-1 text-xs leading-5 text-muted">{{ resource.description }}</p>
                          }
                          <p class="mt-2 text-[0.65rem] font-semibold uppercase tracking-wider text-accent">
                            {{ resource.resourceType }} · {{ resource.licenceName }}
                          </p>
                          @if (animatedLessonPath(resource); as path) {
                            <a
                              [routerLink]="path"
                              class="focus-ring mt-3 inline-flex items-center gap-1 rounded-control bg-accent px-3 py-1.5 text-xs font-semibold text-white hover:opacity-90"
                            >
                              {{ i18n.translate('resources.openLesson') }}
                              <app-icon name="arrow-right" [size]="12" />
                            </a>
                          }
                        </div>
                        <a
                          [href]="resource.url"
                          target="_blank"
                          rel="noopener"
                          class="focus-ring inline-flex shrink-0 items-center gap-1 rounded-control border border-line px-2 py-1 text-xs font-medium text-accent hover:bg-accent-soft"
                        >
                          {{ i18n.translate('common.open') }}
                          <app-icon name="external-link" [size]="12" />
                        </a>
                      </div>
                    </li>
                  }
                </ul>
              }
            </app-card>
          }
        </div>
      }
    </section>
  `,
})
export class ResourcesPage {
  private readonly resourceService = inject(ResourceService);
  protected readonly i18n = inject(LanguageService);

  protected readonly cycles: readonly CycleSection[] = [
    {
      id: 'PRIMARY',
      labelKey: 'resources.level.primary',
      bodyKey: 'resources.level.primary.body',
    },
    {
      id: 'BASIC',
      labelKey: 'resources.level.basic',
      bodyKey: 'resources.level.basic.body',
    },
    {
      id: 'SECONDARY',
      labelKey: 'resources.level.secondary',
      bodyKey: 'resources.level.secondary.body',
    },
  ];

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  private readonly resources = signal<Record<ResourceCycle, readonly ResourceView[]>>({
    PRIMARY: [],
    BASIC: [],
    SECONDARY: [],
  });

  constructor() {
    this.load();
  }

  protected resourcesFor(cycle: ResourceCycle): readonly ResourceView[] {
    return this.resources()[cycle];
  }

  protected animatedLessonPath(resource: ResourceView): string | null {
    return lessonPath(resource.cycle, resource.id);
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(false);
    forkJoin({
      PRIMARY: this.resourceService.listApproved({ cycle: 'PRIMARY', size: 100 }),
      BASIC: this.resourceService.listApproved({ cycle: 'BASIC', size: 100 }),
      SECONDARY: this.resourceService.listApproved({ cycle: 'SECONDARY', size: 100 }),
    }).subscribe({
      next: (pages) => {
        this.resources.set({
          PRIMARY: pages.PRIMARY.items,
          BASIC: pages.BASIC.items,
          SECONDARY: pages.SECONDARY.items,
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
