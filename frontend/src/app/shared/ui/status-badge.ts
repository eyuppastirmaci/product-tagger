import { Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { ProductStatus } from '../../core/api/models';

const STATUS_META: Record<ProductStatus, { key: string; tone: string }> = {
  UPLOADED: { key: 'status.uploaded', tone: 'info' },
  PREPROCESSED: { key: 'status.preprocessed', tone: 'info' },
  TAGGING: { key: 'status.tagging', tone: 'primary' },
  PENDING_REVIEW: { key: 'status.pendingReview', tone: 'warning' },
  APPROVED: { key: 'status.approved', tone: 'success' },
  REJECTED: { key: 'status.rejected', tone: 'neutral' },
  FAILED: { key: 'status.failed', tone: 'danger' },
};

// Raw status enums never reach the screen; the label always comes from i18n.
@Component({
  selector: 'pt-status-badge',
  imports: [TranslocoPipe],
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.scss',
})
export class StatusBadge {
  readonly status = input.required<ProductStatus>();

  protected readonly meta = computed(() => STATUS_META[this.status()]);
}
