import { computed, effect, Injectable, signal } from '@angular/core';

import { Language, TextDirection, TRANSLATIONS, TranslationKey } from './translations';

const STORAGE_KEY = 'mathzero.language';
const DEFAULT_LANGUAGE: Language = 'fr';

function isLanguage(value: string | null): value is Language {
  return value === 'fr' || value === 'ar';
}

function readStoredLanguage(): Language {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return isLanguage(stored) ? stored : DEFAULT_LANGUAGE;
  } catch {
    return DEFAULT_LANGUAGE;
  }
}

/**
 * Minimal bilingual state for the application shell.
 *
 * The Arabic experience is a first-class requirement, so document language and
 * direction are driven from a single signal. A full translation library will
 * replace the small shell dictionary when lesson content is introduced.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly languageState = signal<Language>(readStoredLanguage());

  readonly language = this.languageState.asReadonly();
  readonly direction = computed<TextDirection>(() =>
    this.languageState() === 'ar' ? 'rtl' : 'ltr',
  );
  readonly isRtl = computed(() => this.direction() === 'rtl');
  private readonly dictionary = computed(() => TRANSLATIONS[this.languageState()]);

  constructor() {
    effect(() => this.applyToDocument(this.languageState(), this.direction()));
  }

  setLanguage(language: Language): void {
    this.languageState.set(language);
    try {
      localStorage.setItem(STORAGE_KEY, language);
    } catch {
      // Storage can be unavailable in private browsing; the in-memory choice still applies.
    }
  }

  toggle(): void {
    this.setLanguage(this.languageState() === 'fr' ? 'ar' : 'fr');
  }

  translate(key: TranslationKey): string {
    return this.dictionary()[key];
  }

  private applyToDocument(language: Language, direction: TextDirection): void {
    const root = document.documentElement;
    root.lang = language;
    root.dir = direction;
  }
}
