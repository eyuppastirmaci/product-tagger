import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { ProductStatus } from './models';

export interface ProductStatusEvent {
  productId: string;
  status: ProductStatus;
  descriptionsReady: boolean;
}

/**
 * Single app-wide SSE connection carrying every product's status changes.
 * Consumers filter by productId; one connection keeps the app far below the
 * browser's per-origin connection limit no matter how many uploads run.
 */
@Injectable({ providedIn: 'root' })
export class ProductEvents {
  private source: EventSource | null = null;
  private readonly subject = new Subject<ProductStatusEvent>();

  readonly events$: Observable<ProductStatusEvent> = this.subject.asObservable();

  /** Idempotent; the browser reconnects dropped connections on its own. */
  connect(): void {
    if (this.source) {
      return;
    }

    this.source = new EventSource('/api/products/events');

    this.source.addEventListener('status', (event) => {
      this.subject.next(JSON.parse((event as MessageEvent).data) as ProductStatusEvent);
    });
  }

  disconnect(): void {
    this.source?.close();
    this.source = null;
  }
}
