import { Injectable, inject, signal } from '@angular/core';
import { ProductApi } from '../../core/api/product-api.service';
import { ProductEvents, ProductStatusEvent } from '../../core/api/product-events.service';
import { ProductStatus } from '../../core/api/models';

export type UploadItemStatus = 'UPLOADING' | 'ERROR' | ProductStatus;

export interface UploadItem {
  localId: number;
  fileName: string;
  productId: string | null;
  status: UploadItemStatus;
}

/**
 * Uploads files and follows each product's pipeline on the shared SSE stream
 * until it reaches review. Root-scoped on purpose: the list survives
 * navigating away and back.
 */
@Injectable({ providedIn: 'root' })
export class UploadManager {
  private readonly api = inject(ProductApi);

  readonly items = signal<UploadItem[]>([]);

  private nextLocalId = 0;

  constructor() {
    inject(ProductEvents).events$.subscribe((event) => this.onStatus(event));
  }

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
      next: (product) => this.patch(localId, { productId: product.id, status: product.status }),
      error: () => this.patch(localId, { status: 'ERROR' }),
    });
  }

  private onStatus(event: ProductStatusEvent): void {
    const item = this.items().find((candidate) => candidate.productId === event.productId);

    if (item) {
      this.patch(item.localId, { status: event.status });
    }
  }

  private patch(localId: number, patch: Partial<UploadItem>): void {
    this.items.update((items) =>
      items.map((item) => (item.localId === localId ? { ...item, ...patch } : item)),
    );
  }
}
