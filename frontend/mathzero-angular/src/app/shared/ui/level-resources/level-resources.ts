import { Component, OnChanges, OnInit, SimpleChanges, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { lessonPath } from '../../../core/experience/education-cycle';
import { ResourceCycle, ResourceService, ResourceView } from '../../../core/api/resource.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AppEmptyState } from '../../../design-system/empty-state/app-empty-state';
import { AppIcon } from '../../../design-system/icon/app-icon';

/**
 * Approved resources for a single experience cycle.
 *
 * Each dashboard mounts one instance with its own cycle so a learner only sees
 * material curated for their level. The parent supplies the card surface.
 */
@Component({
  selector: 'app-level-resources',
  imports: [RouterLink, AppEmptyState, AppIcon],
  styleUrl: './level-resources.scss',
  template: `
    @if (loading()) {
      <div
        class="space-y-3"
        role="status"
        aria-live="polite"
        [attr.aria-label]="i18n.translate('common.loading')"
      >
        @for (index of [1, 2, 3]; track index) {
          <div class="flex gap-3 rounded-2xl border border-line bg-canvas p-3">
            <div class="h-10 w-10 shrink-0 animate-pulse rounded-xl bg-subtle"></div>
            <div class="min-w-0 flex-1 space-y-2 py-1">
              <div class="h-3.5 w-2/3 animate-pulse rounded bg-subtle"></div>
              <div class="h-3 w-full animate-pulse rounded bg-subtle"></div>
            </div>
          </div>
        }
      </div>
    } @else if (error()) {
      <div
        class="flex items-center justify-between gap-3 rounded-2xl border border-red-200 bg-red-50 px-4 py-3"
        role="alert"
      >
        <p class="text-sm text-red-800">{{ i18n.translate('resources.loadError') }}</p>
        <button
          class="focus-ring inline-flex shrink-0 items-center gap-2 rounded-control border border-red-200 bg-canvas px-3 py-2 text-xs font-semibold text-red-800 hover:bg-red-100"
          type="button"
          (click)="load()"
        >
          <app-icon name="refresh" [size]="14" />
          {{ i18n.translate('common.retry') }}
        </button>
      </div>
    } @else if (resources().length === 0) {
      <app-empty-state
        icon="book-open"
        [title]="i18n.translate('resources.empty.title')"
        [body]="i18n.translate('resources.empty.body')"
      />
    } @else {
      <div class="mb-3 flex items-center justify-between gap-3">
        <p class="text-xs font-semibold uppercase tracking-wider text-muted">
          {{ i18n.translate('resources.curatedSelection') }}
        </p>
        <span
          class="rounded-full border border-line bg-canvas px-2.5 py-1 text-xs font-semibold text-accent"
        >
          {{ resources().length }}
        </span>
      </div>
      <ul class="level-resources__list">
        @for (resource of visibleResources(); track resource.id) {
          <li
            class="group rounded-2xl border border-line bg-canvas p-3 transition-all hover:border-accent/30 hover:bg-accent-soft/40 hover:shadow-sm"
          >
            <div class="flex items-start gap-3">
              <span
                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent-soft text-accent"
                aria-hidden="true"
              >
                <app-icon [name]="resourceIcon(resource.resourceType)" [size]="18" />
              </span>
              <div class="min-w-0 flex-1">
                <h3 class="truncate text-sm font-semibold text-ink">
                  <a
                    class="focus-ring rounded-sm"
                    [href]="resource.url"
                    target="_blank"
                    rel="noopener"
                  >
                    {{ resource.title }}
                  </a>
                </h3>
                @if (resource.description) {
                  <p class="mt-1 line-clamp-2 text-xs leading-5 text-muted">
                    {{ resource.description }}
                  </p>
                }
                <div class="mt-2 flex flex-wrap items-center gap-1.5">
                  <span
                    class="rounded-full border border-line bg-surface px-2 py-0.5 text-[0.65rem] font-semibold text-muted"
                  >
                    {{ resource.resourceType }}
                  </span>
                  @if (resource.licenceName) {
                    <span class="text-[0.65rem] font-medium text-muted">{{
                      resource.licenceName
                    }}</span>
                  }
                </div>
                @if (animatedLessonPath(resource.id); as path) {
                  <a
                    [routerLink]="path"
                    class="focus-ring mt-3 inline-flex items-center gap-1.5 rounded-control bg-accent px-3 py-1.5 text-xs font-semibold text-white hover:opacity-90"
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
                class="focus-ring inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-line bg-surface text-accent transition-colors group-hover:border-accent/30 group-hover:bg-accent group-hover:text-white"
                [attr.aria-label]="i18n.translate('common.open')"
                [attr.title]="i18n.translate('common.open')"
              >
                <app-icon name="external-link" [size]="14" />
              </a>
            </div>
          </li>
        }
      </ul>
      @if (resources().length > 3) {
        <a
          routerLink="/resources"
          class="focus-ring mt-4 inline-flex items-center gap-1.5 text-sm font-semibold text-accent hover:underline"
        >
          {{ i18n.translate('resources.viewAll') }}
          <app-icon name="arrow-right" [size]="15" />
        </a>
      }
    }
  `,
})
export class LevelResources implements OnInit, OnChanges {
  readonly cycle = input.required<ResourceCycle>();
  protected readonly i18n = inject(LanguageService);
  private readonly resourceService = inject(ResourceService);

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly resources = signal<readonly ResourceView[]>([]);

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['cycle'] && !changes['cycle'].firstChange) {
      this.load();
    }
  }

  protected visibleResources(): readonly ResourceView[] {
    return this.resources().slice(0, 3);
  }

  protected resourceIcon(resourceType: string): 'file-text' | 'play' | 'book-open' {
    const type = resourceType.toUpperCase();
    if (type.includes('VIDEO')) return 'play';
    if (type.includes('ARTICLE') || type.includes('DOCUMENT')) return 'file-text';
    return 'book-open';
  }

  protected animatedLessonPath(resourceId: string): string | null {
    return lessonPath(this.cycle(), resourceId);
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.resourceService.listApproved({ cycle: this.cycle(), size: 100 }).subscribe({
      next: (page) => {
        this.resources.set(page.items);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
