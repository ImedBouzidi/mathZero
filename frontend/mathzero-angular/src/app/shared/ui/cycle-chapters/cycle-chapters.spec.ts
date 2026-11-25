import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '../../../core/config/api-config';
import { LanguageService } from '../../../core/i18n/language.service';
import { CycleChapters } from './cycle-chapters';

describe('CycleChapters', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<CycleChapters>;
  let httpMock: HttpTestingController;

  const chapter = (id: string, title: string, titleAr: string) => ({
    id,
    title,
    description: `${title} — description`,
    titleAr,
    descriptionAr: `${titleAr} — الوصف`,
    steps: [
      {
        kind: 'intro',
        title: `${title} : introduction`,
        body: `Contenu de ${title}`,
        formula: null,
      },
    ],
    exercises: [
      {
        id: `${id}-Q1`,
        prompt: 'Question',
        options: ['a', 'b'],
        hint: 'Indice',
        promptAr: 'سؤال',
        optionsAr: ['أ', 'ب'],
        hintAr: 'تلميح',
      },
    ],
  });

  const cycles = [
    {
      cycle: 'PRIMARY',
      nameFr: 'Primaire',
      nameAr: 'الابتدائي',
      grades: [
        {
          gradeCode: 'P1',
          gradeName: 'Première année primaire',
          gradeNameAr: 'السنة الأولى ابتدائي',
          cycle: 'PRIMARY',
          draft: true,
          reviewNote: 'Chapitres provisoires',
          chapters: [chapter('P1-NUMBERS', 'Nombres et calcul', 'الأعداد والحساب')],
        },
      ],
    },
    {
      cycle: 'BASIC',
      nameFr: 'Enseignement de base',
      nameAr: 'التعليم الأساسي',
      grades: [
        {
          gradeCode: 'B7',
          gradeName: 'Septième année de l’enseignement de base',
          gradeNameAr: 'السنة السابعة من التعليم الأساسي',
          cycle: 'BASIC',
          draft: false,
          reviewNote: '',
          chapters: [
            chapter('B7-NUMBERS', 'Nombres entiers et fractions', 'الأعداد الصحيحة والكسور'),
          ],
        },
        {
          gradeCode: 'B8',
          gradeName: 'Huitième année de l’enseignement de base',
          gradeNameAr: 'السنة الثامنة من التعليم الأساسي',
          cycle: 'BASIC',
          draft: false,
          reviewNote: '',
          chapters: [chapter('B8-NUMBERS', 'Rationnels et puissances', 'الأعداد النسبية والتمكين')],
        },
      ],
    },
    {
      cycle: 'SECONDARY',
      nameFr: 'Enseignement secondaire',
      nameAr: 'التعليم الثانوي',
      grades: [
        {
          gradeCode: 'S1',
          gradeName: 'Première année secondaire',
          gradeNameAr: 'السنة الأولى ثانوي',
          cycle: 'SECONDARY',
          draft: false,
          reviewNote: '',
          chapters: [chapter('S1-FUNCTIONS', 'Fonctions linéaires', 'الدوال الخطية')],
        },
      ],
    },
  ];

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [CycleChapters],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CycleChapters);
    fixture.componentRef.setInput('cycle', 'BASIC');
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function flushCatalog(): void {
    httpMock.expectOne(`${baseUrl}/learning/curriculum/cycles`).flush({
      data: cycles,
      meta: { requestId: 'cycles-1' },
    });
    fixture.detectChanges();
  }

  it('renders the grades and chapters of the requested cycle', () => {
    flushCatalog();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Septième année de l’enseignement de base');
    expect(text).toContain('Huitième année de l’enseignement de base');
    expect(text).toContain('Nombres entiers et fractions');
    expect(text).not.toContain('Fonctions linéaires');
  });

  it('switches to another grade of the same cycle', () => {
    flushCatalog();

    const tabs = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
    (tabs[1] as HTMLButtonElement).click();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Rationnels et puissances');
  });

  it('renders Arabic grade and chapter labels when the language is Arabic', () => {
    flushCatalog();
    TestBed.inject(LanguageService).setLanguage('ar');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('السنة السابعة من التعليم الأساسي');
    expect(text).toContain('الأعداد الصحيحة والكسور');
  });

  it('reveals the chapter steps and exercises when expanded', () => {
    flushCatalog();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).not.toContain('Contenu de Nombres entiers et fractions');

    (element.querySelector('article button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(element.textContent).toContain('Contenu de Nombres entiers et fractions');
    expect(element.textContent).toContain('Question');
  });

  it('checks a chapter exercise answer through the catalogue endpoint', () => {
    flushCatalog();

    const element = fixture.nativeElement as HTMLElement;
    (element.querySelector('article button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const buttons = Array.from(element.querySelectorAll('article button')) as HTMLButtonElement[];
    (buttons.find((button) => button.textContent?.trim() === 'a') as HTMLButtonElement).click();
    fixture.detectChanges();

    (buttons.find((button) => button.textContent?.includes('Vérifier')) as HTMLButtonElement).click();
    fixture.detectChanges();

    const request = httpMock.expectOne(`${baseUrl}/learning/curriculum/grades/B7/answers`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      exerciseId: 'B7-NUMBERS-Q1',
      optionIndex: 0,
    });
    request.flush({
      data: { correct: true, correctAnswer: 'a', explanation: 'Bien joué' },
      meta: { requestId: 'answer-1' },
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Bravo');
  });

  it('shows a retry action when the catalogue request fails', () => {
    httpMock
      .expectOne(`${baseUrl}/learning/curriculum/cycles`)
      .flush('offline', { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('[role="alert"]')).toBeTruthy();
    expect(element.textContent).toContain('Réessayer');

    (element.querySelector('[role="alert"] button') as HTMLButtonElement).click();
    fixture.detectChanges();
    httpMock.expectOne(`${baseUrl}/learning/curriculum/cycles`).flush({
      data: cycles,
      meta: { requestId: 'cycles-2' },
    });
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')).toBeNull();
  });
});
