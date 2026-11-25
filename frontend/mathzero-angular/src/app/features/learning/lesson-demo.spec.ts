import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Demo } from '../../core/api/learning.service';
import { LessonDemo } from './lesson-demo';

describe('LessonDemo', () => {
  const demo: Demo = {
    kind: 'FRACTIONS',
    title: 'Construire une fraction équivalente',
    frames: [
      { label: 'Partager', narration: 'On partage en deux.', formula: '\\frac{1}{2}', left: '1 / 2', right: '1 sur 2', progress: 50 },
      { label: 'Multiplier', narration: 'On multiplie par deux.', formula: null, left: '1 × 2', right: '2 × 2', progress: 70 },
      { label: 'Vérifier', narration: 'Les valeurs sont égales.', formula: null, left: '1 / 2', right: '2 / 4', progress: 100 },
    ],
  };

  let fixture: ComponentFixture<LessonDemo>;
  let element: HTMLElement;

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.lang = 'fr';
    document.documentElement.dir = 'ltr';

    await TestBed.configureTestingModule({ imports: [LessonDemo] }).compileComponents();
    fixture = TestBed.createComponent(LessonDemo);
    fixture.componentRef.setInput('demo', demo);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  function buttonWith(label: string): HTMLButtonElement {
    const button = Array.from(element.querySelectorAll('button')).find((candidate) =>
      candidate.textContent?.includes(label),
    ) as HTMLButtonElement | undefined;
    if (!button) throw new Error(`Button with label "${label}" not found`);
    return button;
  }

  it('shows the first frame and advances with the next control', () => {
    expect(element.textContent).toContain('Construire une fraction équivalente');
    expect(element.textContent).toContain('On partage en deux.');
    expect(element.textContent).toContain('Partager · 1 / 3');

    buttonWith('Suivant').click();
    fixture.detectChanges();

    expect(element.textContent).toContain('On multiplie par deux.');
    expect(element.textContent).toContain('Multiplier · 2 / 3');
  });

  it('toggles play/pause and replays from the first frame', () => {
    buttonWith('Lire').click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Pause');

    buttonWith('Pause').click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Lire');

    buttonWith('Suivant').click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Multiplier · 2 / 3');

    const replay = buttonWith('Rejouer');
    replay.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Partager · 1 / 3');
  });

  it('shows the multiplication table and highlights the fact used in the current frame', () => {
    fixture.componentRef.setInput('demo', {
      kind: 'MULTIPLICATION',
      title: 'Une multiplication à un chiffre',
      frames: [
        {
          label: 'Consulter la table',
          narration: 'La table de 3 donne 3 × 4 = 12.',
          formula: '3\\times4=12',
          left: '3 × 4',
          right: '12',
          progress: 20,
        },
      ],
    });
    fixture.detectChanges();

    expect(element.textContent).toContain('Table de multiplication de 1 à 10');
    expect(element.querySelectorAll('.times-table tbody td')).toHaveLength(100);
    expect(element.querySelector('.times-table td.active')?.textContent?.trim()).toBe('12');
  });
});
