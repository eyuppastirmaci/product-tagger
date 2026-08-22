import { Component, computed, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideInbox } from '@lucide/angular';
import { ProductCounts, ProductStatus } from '../../core/api/models';
import { ProductApi } from '../../core/api/product-api.service';
import { LanguageService } from '../../core/i18n/language.service';
import { RelativeTimePipe } from '../../shared/format/relative-time.pipe';
import { ProductTable } from '../products/product-table';

const QUEUE_STATUSES: ProductStatus[] = ['PENDING_REVIEW', 'FAILED'];

@Component({
  selector: 'app-review-queue-page',
  imports: [TranslocoPipe, RelativeTimePipe, ProductTable, LucideInbox],
  templateUrl: './review-queue-page.html',
  styleUrl: './review-queue-page.scss',
})
export class ReviewQueuePage {
  private readonly api = inject(ProductApi);

  protected readonly language = inject(LanguageService);

  protected readonly queueStatuses = QUEUE_STATUSES;
  protected readonly counts = signal<ProductCounts | null>(null);

  protected readonly queueCount = computed(() => {
    const counts = this.counts();

    if (!counts) {
      return 0;
    }

    return (counts.byStatus['PENDING_REVIEW'] ?? 0) + (counts.byStatus['FAILED'] ?? 0);
  });

  constructor() {
    this.api.counts().subscribe((counts) => this.counts.set(counts));
  }
}
