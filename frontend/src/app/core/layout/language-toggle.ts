import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../i18n/language.service';

@Component({
  selector: 'app-language-toggle',
  imports: [TranslocoPipe],
  templateUrl: './language-toggle.html',
  styleUrl: './language-toggle.scss',
})
export class LanguageToggle {
  protected readonly language = inject(LanguageService);
}
