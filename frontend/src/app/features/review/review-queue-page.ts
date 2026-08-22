import { Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-review-queue-page',
  imports: [TranslocoPipe],
  template: `<p class="placeholder">{{ 'review.placeholder' | transloco }}</p>`,
  styles: `
    .placeholder {
      font-size: 12.5px;
      color: var(--color-text-dim);
    }
  `,
})
export class ReviewQueuePage {}
