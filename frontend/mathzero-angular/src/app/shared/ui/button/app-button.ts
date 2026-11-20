import { Directive, input } from '@angular/core';

export type AppButtonVariant = 'primary' | 'secondary' | 'ghost';
export type AppButtonSize = 'md' | 'sm';

/**
 * Accessible button styling primitive for native `button` and `a` elements.
 * Native elements keep their semantics; only presentation is shared.
 */
@Directive({
  selector: 'button[appButton], a[appButton]',
  host: {
    class: 'btn',
    '[class.btn--primary]': "variant() === 'primary'",
    '[class.btn--secondary]': "variant() === 'secondary'",
    '[class.btn--ghost]': "variant() === 'ghost'",
    '[class.btn--sm]': "size() === 'sm'",
  },
})
export class AppButton {
  readonly variant = input<AppButtonVariant>('primary');
  readonly size = input<AppButtonSize>('md');
}
