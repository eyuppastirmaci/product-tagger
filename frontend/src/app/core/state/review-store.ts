import { Injectable, inject, signal } from '@angular/core';
import { ProductApi } from '../api/product-api.service';

@Injectable({ providedIn: 'root' })
export class ReviewStore {
  private readonly api = inject(ProductApi);

  readonly queueCount = signal(0);

  refresh(): void {
    // Only the total matters for the badge; size 1 keeps the payload minimal
    this.api.list({ status: ['PENDING_REVIEW', 'FAILED'], size: 1 })
      .subscribe((page) => this.queueCount.set(page.totalElements));
  }
}
