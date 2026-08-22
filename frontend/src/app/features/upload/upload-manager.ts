import { Injectable, inject, signal } from '@angular/core';
import { ProductApi } from '../../core/api/product-api.service';
import { ProductStatus } from '../../core/api/models';
import { ReviewStore } from '../../core/state/review-store';

export type UploadItemStatus = 'UPLOADING' | 'ERROR' | ProductStatus;

export interface UploadItem {
  localId: number;
  fileName: string;
  productId: string | null;
  status: UploadItemStatus;
}

// The pipeline stops being interesting for the upload list at these states
const TERMINAL_STATUSES: ProductStatus[] = ['PENDING_REVIEW', 'APPROVED', 'REJECTED', 'FAILED'];

/**
 * Uploads files and follows each product's pipeline over SSE until it reaches
 * review. Root-scoped on purpose: the list survives navigating away and back.
 */
@Injectable({ providedIn: 'root' })
export class UploadManager {
  private readonly api = inject(ProductApi);
  private readonly reviewStore = inject(ReviewStore);

  readonly items = signal<UploadItem[]>([]);

  private nextLocalId = 0;
  private readonly sources = new Map<number, EventSource>();

  uploadAll(files: File[]): void {
    files.forEach((file) => this.uploadOne(file));
  }

  private uploadOne(file: File): void {
    const localId = this.nextLocalId++;

    this.items.update((items) => [
      { localId, fileName: file.name, productId: null, status: 'UPLOADING' as const },
      ...items,
    ]);

    this.api.upload(file).subscribe({
      next: (product) => {
        this.patch(localId, { productId: product.id, status: product.status });
        this.watch(localId, product.id);
      },
      error: () => this.patch(localId, { status: 'ERROR' }),
    });
  }

  private watch(localId: number, productId: string): void {
    const source = new EventSource(`/api/products/${productId}/events`);

    source.addEventListener('status', (event) => {
      const payload = JSON.parse((event as MessageEvent).data) as { status: ProductStatus };

      this.patch(localId, { status: payload.status });

      if (TERMINAL_STATUSES.includes(payload.status)) {
        this.stopWatching(localId);
        this.reviewStore.refresh();
      }
    });

    source.onerror = () => this.stopWatching(localId);

    this.sources.set(localId, source);
  }

  private stopWatching(localId: number): void {
    this.sources.get(localId)?.close();
    this.sources.delete(localId);
  }

  private patch(localId: number, patch: Partial<UploadItem>): void {
    this.items.update((items) =>
      items.map((item) => (item.localId === localId ? { ...item, ...patch } : item)),
    );
  }
}
