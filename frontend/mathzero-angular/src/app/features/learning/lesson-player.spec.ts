import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '../../core/config/api-config';
import { LessonPlayer } from './lesson-player';

describe('LessonPlayer', () => {
  const baseUrl = 'http://localhost:8090/api/v1';
  let fixture: ComponentFixture<LessonPlayer>;
  let httpMock: HttpTestingController;
  let element: HTMLElement;

  const lesson = {
    resourceId: 'resource-1',
    title: 'Les fractions',
    summary: 'Comprendre les fractions',
    language: 'fr',
    cycle: 'PRIMARY',
    sourceUrl: 'https://fr.wikipedia.org/wiki/Fraction',
    licenceName: 'Creative Commons Attribution-ShareAlike 4.0',
    attributionText: 'Wikipedia contributors, Les fractions (CC BY-SA 4.0)',
    steps: [
      { kind: 'intro', title: 'Les fractions', body: 'Une fraction représente une partie d’un tout.', formula: '\\frac{a}{b}' },
      { kind: 'demo', title: 'Démonstration animée', body: 'Suivez la résolution.', formula: null },
      { kind: 'source', title: 'Source et droits', body: 'Contenu sous licence CC BY-SA 4.0.', formula: null },
    ],
    demo: {
      kind: 'FRACTIONS',
      title: 'Construire une fraction équivalente',
      frames: [
        { label: 'Partager', narration: 'On partage en deux.', formula: '\\frac{1}{2}', left: '1 / 2', right: '1 sur 2', progress: 50 },
        { label: 'Vérifier', narration: 'Les valeurs sont égales.', formula: null, left: '1 / 2', right: '2 / 4', progress: 100 },
      ],
    },
    exercises: [
      { id: 'q1', prompt: 'Combien de quarts dans un entier ?', options: ['4', '2', '3', '8'], hint: 'Choisissez la bonne réponse.' },
    ],
  };

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({
      imports: [LessonPlayer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_BASE_URL, useValue: baseUrl },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonPlayer);
    fixture.componentRef.setInput('resourceId', 'resource-1');
    httpMock = TestBed.inject(HttpTestingController);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function buttonWith(label: string, preferLast = false): HTMLButtonElement {
    const matches = Array.from(element.querySelectorAll('button')).filter((candidate) =>
      candidate.textContent?.includes(label),
    ) as HTMLButtonElement[];
    const button = preferLast ? matches.at(-1) : matches[0];
    if (!button) throw new Error(`Button with label "${label}" not found`);
    return button;
  }

  it('plays the animated steps then the quiz and reveals the answer only after submission', async () => {
    httpMock.expectOne(`${baseUrl}/learning/lessons/resource-1`).flush({
      data: lesson,
      meta: { requestId: 'lesson-1' },
    });
    fixture.detectChanges();
    expect(element.textContent).toContain('Les fractions');
    expect(element.textContent).toContain('Une fraction représente une partie d’un tout.');

    buttonWith('Suivant', true).click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Démonstration animée');
    expect(element.textContent).toContain('On partage en deux.');

    buttonWith('Suivant', true).click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Source et droits');

    buttonWith('Passer aux exercices').click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Combien de quarts dans un entier ?');
    expect(element.textContent).not.toContain('Bravo');

    const options = Array.from(element.querySelectorAll('button.option')) as HTMLButtonElement[];
    options[0]!.click();
    fixture.detectChanges();
    buttonWith('Vérifier').click();

    const answer = httpMock.expectOne(
      (request) =>
        request.url === `${baseUrl}/learning/lessons/resource-1/answers` && request.method === 'POST',
    );
    expect(answer.request.body).toEqual({ questionId: 'q1', optionIndex: 0 });
    answer.flush({
      data: {
        correct: true,
        correctAnswer: '4',
        explanation: 'Un entier contient 4 quarts.',
        position: 1,
        total: 1,
        solution: {
          kind: 'SOLUTION',
          title: 'Méthode pas à pas',
          frames: [
            { label: 'Raisonner', narration: 'Un entier contient 4 quarts.', formula: null, left: '1', right: '4', progress: 100 },
          ],
        },
      },
      meta: { requestId: 'answer-1' },
    });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.textContent).toContain('Bravo');
    expect(element.textContent).toContain('Méthode animée');
    expect(element.textContent).toContain('Un entier contient 4 quarts.');

    buttonWith('Terminer').click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Leçon terminée !');
    expect(element.textContent).toContain('Score');
  });
});
