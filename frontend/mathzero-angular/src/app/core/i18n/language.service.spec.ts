import { TestBed } from '@angular/core/testing';

import { LanguageService } from './language.service';

describe('LanguageService', () => {
  let service: LanguageService;

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';
    TestBed.configureTestingModule({});
    service = TestBed.inject(LanguageService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('defaults to French with left-to-right direction', () => {
    expect(service.language()).toBe('fr');
    expect(service.direction()).toBe('ltr');
    expect(service.isRtl()).toBe(false);
  });

  it('switches to Arabic, updates the document and persists the choice', () => {
    service.setLanguage('ar');
    TestBed.tick();

    expect(service.language()).toBe('ar');
    expect(service.direction()).toBe('rtl');
    expect(document.documentElement.lang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
    expect(localStorage.getItem('mathzero.language')).toBe('ar');
  });

  it('translates shell labels in the active language', () => {
    expect(service.translate('nav.home')).toBe('Accueil');

    service.setLanguage('ar');

    expect(service.translate('nav.home')).toBe('الرئيسية');
  });

  it('restores a stored language preference', () => {
    localStorage.setItem('mathzero.language', 'ar');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});

    const restored = TestBed.inject(LanguageService);

    expect(restored.language()).toBe('ar');
  });
});
