import { Injectable, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

export type Lang = 'tr' | 'en';

const STORAGE_KEY = 'pt-lang';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);

  readonly lang = signal<Lang>('tr');

  init(): void {
    const saved = localStorage.getItem(STORAGE_KEY) as Lang | null;
    this.apply(saved ?? (navigator.language.startsWith('tr') ? 'tr' : 'en'));
  }

  set(lang: Lang): void {
    localStorage.setItem(STORAGE_KEY, lang);
    this.apply(lang);
  }

  private apply(lang: Lang): void {
    this.lang.set(lang);
    this.transloco.setActiveLang(lang);
  }
}
