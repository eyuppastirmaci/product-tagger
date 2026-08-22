import { Component, computed, inject, input } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

// Confidence tiers per the design handoff: >=85 high, 60-85 medium, <60 low.
@Component({
  selector: 'pt-confidence-badge',
  templateUrl: './confidence-badge.html',
  styleUrl: './confidence-badge.scss',
})
export class ConfidenceBadge {
  private readonly transloco = inject(TranslocoService);

  /** 0..1 confidence; null/undefined means the model made no suggestion. */
  readonly confidence = input.required<number | null | undefined>();

  protected readonly tier = computed(() => {
    const value = this.confidence();

    if (value == null) {
      return 'none';
    }

    return value >= 0.85 ? 'high' : value >= 0.6 ? 'medium' : 'low';
  });

  protected readonly label = computed(() => {
    const value = this.confidence();

    return value == null ? '—' : `${Math.round(value * 100)}%`;
  });

  protected readonly tooltip = computed(() => {
    const tier = this.tier();

    return tier === 'none'
      ? this.transloco.translate('review.attributes.noSuggestionTooltip')
      : this.transloco.translate(`review.confidence.${tier}`);
  });
}
