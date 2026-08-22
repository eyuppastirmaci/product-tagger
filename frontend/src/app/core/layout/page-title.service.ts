import { Injectable, signal } from '@angular/core';

/**
 * Lets a screen replace the breadcrumb's translated route title with a dynamic
 * one (e.g. the product title on the review screen). Screens must clear it on
 * destroy.
 */
@Injectable({ providedIn: 'root' })
export class PageTitleService {
  readonly override = signal<string | null>(null);
}
