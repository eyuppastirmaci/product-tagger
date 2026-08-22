import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../i18n/language.service';

@Component({
  selector: 'app-language-toggle',
  imports: [TranslocoPipe],
  template: `
    <div class="lang-toggle" role="group" [attr.aria-label]="'shell.language.label' | transloco">
      <button type="button"
              [class.selected]="language.lang() === 'tr'"
              (click)="language.set('tr')">TR</button>
      <button type="button"
              [class.selected]="language.lang() === 'en'"
              (click)="language.set('en')">EN</button>
    </div>
  `,
  styles: `
    .lang-toggle {
      display: flex;
      gap: 1px;
      background: var(--color-surface-alt);
      border: 1px solid var(--color-border);
      border-radius: 8px;
      padding: 2px;
    }

    button {
      min-width: 28px;
      height: 22px;
      padding: 0 6px;
      border-radius: 6px;
      border: 1px solid transparent;
      background: transparent;
      color: var(--color-text-dim);
      font-family: var(--font-mono);
      font-size: 10.5px;
      font-weight: 500;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      &.selected {
        background: var(--color-surface);
        border-color: var(--color-border);
        color: var(--color-text);
      }
    }
  `,
})
export class LanguageToggle {
  protected readonly language = inject(LanguageService);
}
