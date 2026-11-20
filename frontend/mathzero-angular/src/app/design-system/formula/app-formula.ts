import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import * as katex from 'katex';

/**
 * KaTeX formula renderer.
 *
 * Math is always isolated LTR, even inside Arabic RTL pages. KaTeX also emits
 * MathML so screen readers can access the expression.
 */
@Component({
  selector: 'app-formula',
  template: `
    <span
      [class]="display() ? 'block max-w-full overflow-x-auto py-1' : 'inline-block max-w-full'"
      dir="ltr"
      [attr.aria-label]="label() || null"
      [innerHTML]="html()"
    ></span>
  `,
})
export class AppFormula {
  private readonly sanitizer = inject(DomSanitizer);

  readonly expression = input.required<string>();
  readonly display = input(false);
  readonly label = input<string>();

  protected readonly html = computed<SafeHtml>(() => {
    const rendered = katex.renderToString(this.expression(), {
      displayMode: this.display(),
      throwOnError: false,
      output: 'htmlAndMathml',
      strict: false,
      trust: false,
    });

    return this.sanitizer.bypassSecurityTrustHtml(rendered);
  });
}
