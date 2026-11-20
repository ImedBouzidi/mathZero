import { Directive, input } from '@angular/core';

export type AppButtonVariant = 'primary' | 'secondary' | 'ghost' | 'reward' | 'danger';
export type AppButtonSize = 'md' | 'sm' | 'lg';

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
    '[class.btn--reward]': "variant() === 'reward'",
    '[class.btn--danger]': "variant() === 'danger'",
    '[class.btn--sm]': "size() === 'sm'",
    '[class.btn--lg]': "size() === 'lg'",
    '[class.btn--block]': 'block()',
  },
})
export class AppButton {
  readonly variant = input<AppButtonVariant>('primary');
  readonly size = input<AppButtonSize>('md');
  readonly block = input(false);
}
