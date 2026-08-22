import { Injectable, inject, signal } from '@angular/core';
import { debounceTime } from 'rxjs';
import { ProductApi } from '../api/product-api.service';
import { ProductEvents } from '../api/product-events.service';

@Injectable({ providedIn: 'root' })
export class ReviewStore {
  private readonly api = inject(ProductApi);

  readonly queueCount = signal(0);

  constructor() {
    // The sidebar badge follows every pipeline status change
    inject(ProductEvents).events$
      .pipe(debounceTime(300))
      .subscribe(() => this.refresh());
  }

  refresh(): void {
    // Only the total matters for the badge; size 1 keeps the payload minimal
    this.api.list({ status: ['PENDING_REVIEW', 'FAILED'], size: 1 })
      .subscribe((page) => this.queueCount.set(page.totalElements));
  }
}
