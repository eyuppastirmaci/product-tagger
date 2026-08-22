import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'pt-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>('light');

  private readonly mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

  init(): void {
    const saved = localStorage.getItem(STORAGE_KEY) as Theme | null;
    this.apply(saved ?? (this.mediaQuery.matches ? 'dark' : 'light'));

    // Follow the OS preference only while the user has not chosen explicitly
    this.mediaQuery.addEventListener('change', () => {
      if (!localStorage.getItem(STORAGE_KEY)) {
        this.apply(this.mediaQuery.matches ? 'dark' : 'light');
      }
    });
  }

  set(theme: Theme): void {
    localStorage.setItem(STORAGE_KEY, theme);
    this.apply(theme);
  }

  private apply(theme: Theme): void {
    this.theme.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
  }
}
