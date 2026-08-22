import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { TranslocoPipe } from '@jsverse/transloco';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideCircleCheckBig,
  LucideImage,
  LucidePlus,
  LucideRotateCw,
} from '@lucide/angular';
import { CatalogApi } from '../../core/api/catalog-api.service';
import { CategoryTree, ProductCounts, ProductResponse, ProductStatus } from '../../core/api/models';
import { ProductApi } from '../../core/api/product-api.service';
import { ProductEvents } from '../../core/api/product-events.service';
import { LanguageService } from '../../core/i18n/language.service';
import { RelativeTimePipe } from '../../shared/format/relative-time.pipe';
import { ConfidenceBadge } from '../../shared/ui/confidence-badge';
import { StatusBadge } from '../../shared/ui/status-badge';

const PAGE_SIZE = 20;

// URL tokens per the handoff: ?status=pendingReview,failed
const STATUS_TOKENS: Array<{ status: ProductStatus; token: string }> = [
  { status: 'PENDING_REVIEW', token: 'pendingReview' },
  { status: 'APPROVED', token: 'approved' },
  { status: 'REJECTED', token: 'rejected' },
  { status: 'FAILED', token: 'failed' },
];

/**
 * Shared table behind /products and /review; the routes only differ in their
 * default status filter and the surrounding chrome.
 */
@Component({
  selector: 'app-product-table',
  imports: [
    RouterLink,
    TranslocoPipe,
    RelativeTimePipe,
    StatusBadge,
    ConfidenceBadge,
    LucideChevronLeft,
    LucideChevronRight,
    LucideCircleCheckBig,
    LucideImage,
    LucidePlus,
    LucideRotateCw,
  ],
  templateUrl: './product-table.html',
  styleUrl: './product-table.scss',
})
export class ProductTable {
  private readonly api = inject(ProductApi);
  private readonly catalog = inject(CatalogApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly language = inject(LanguageService);

  /** Filter applied when the URL carries no explicit status selection. */
  readonly defaultStatuses = input<ProductStatus[]>([]);
  readonly emptyVariant = input<'products' | 'queue'>('products');

  protected readonly items = signal<ProductResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly loading = signal(true);
  protected readonly counts = signal<ProductCounts | null>(null);

  protected readonly filterOptions = STATUS_TOKENS;
  protected readonly skeletonRows = Array.from({ length: 5 });

  private readonly categoryNames = signal<Map<string, { tr: string; en: string }>>(new Map());

  private readonly queryParams = toSignal(this.route.queryParamMap);

  protected readonly page = computed(() => {
    const raw = Number(this.queryParams()?.get('page') ?? '1');

    return Number.isInteger(raw) && raw > 0 ? raw : 1;
  });

  protected readonly selectedStatuses = computed<ProductStatus[]>(() => {
    const raw = this.queryParams()?.get('status');

    if (raw == null) {
      return this.defaultStatuses();
    }

    return raw
      .split(',')
      .map((token) => STATUS_TOKENS.find((entry) => entry.token === token)?.status)
      .filter((status): status is ProductStatus => status != null);
  });

  protected readonly filtersActive = computed(() => this.selectedStatuses().length > 0);

  constructor() {
    this.catalog.tree().subscribe((tree) => {
      const names = new Map<string, { tr: string; en: string }>();

      this.collectNames(tree, names);
      this.categoryNames.set(names);
    });

    this.refreshCounts();

    effect(() => {
      const statuses = this.selectedStatuses();
      const page = this.page();

      untracked(() => this.fetch(statuses, page));
    });

    // Live refresh: any pipeline status change re-reads the current page and
    // counts quietly; the debounce folds bursts of events into one request
    inject(ProductEvents).events$
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(() => {
        this.fetch(this.selectedStatuses(), this.page(), true);
        this.refreshCounts();
      });
  }

  protected imageUrl(product: ProductResponse): string {
    return this.api.imageUrl(product.id, 'thumbnail');
  }

  protected titleOf(product: ProductResponse): string | null {
    return this.language.lang() === 'tr' ? product.titleTr : product.titleEn;
  }

  protected categoryNameOf(product: ProductResponse): string | null {
    if (!product.categoryCode) {
      return null;
    }

    const names = this.categoryNames().get(product.categoryCode);

    if (!names) {
      return product.categoryCode;
    }

    return this.language.lang() === 'tr' ? names.tr : names.en;
  }

  protected isSelected(status: ProductStatus): boolean {
    return this.selectedStatuses().includes(status);
  }

  protected countOf(status: ProductStatus): number {
    return this.counts()?.byStatus[status] ?? 0;
  }

  protected toggleStatus(status: ProductStatus): void {
    const current = this.selectedStatuses();
    const next = current.includes(status)
      ? current.filter((entry) => entry !== status)
      : [...current, status];

    this.applyStatuses(next);
  }

  protected selectAll(): void {
    this.applyStatuses([]);
  }

  protected clearFilters(): void {
    this.applyStatuses([]);
  }

  protected goToPage(page: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: page > 1 ? page : null },
      queryParamsHandling: 'merge',
    });
  }

  protected openProduct(id: string): void {
    this.router.navigate(['/review', id]);
  }

  protected onRowKeydown(event: KeyboardEvent, id: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.openProduct(id);
    }
  }

  protected retry(event: MouseEvent, id: string): void {
    event.stopPropagation();

    this.api.retag(id).subscribe(() => {
      this.fetch(this.selectedStatuses(), this.page());
      this.refreshCounts();
    });
  }

  protected rangeFrom(): number {
    return this.total() === 0 ? 0 : (this.page() - 1) * PAGE_SIZE + 1;
  }

  protected rangeTo(): number {
    return Math.min(this.page() * PAGE_SIZE, this.total());
  }

  private applyStatuses(statuses: ProductStatus[]): void {
    const tokens = statuses
      .map((status) => STATUS_TOKENS.find((entry) => entry.status === status)?.token)
      .filter((token): token is string => token != null);

    // Filter changes always reset the pagination
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { status: tokens.length > 0 ? tokens.join(',') : '', page: null },
      queryParamsHandling: 'merge',
    });
  }

  // A silent fetch swaps the rows in place without flashing the skeleton
  private fetch(statuses: ProductStatus[], page: number, silent = false): void {
    if (!silent) {
      this.loading.set(true);
    }

    this.api.list({ status: statuses, page: page - 1, size: PAGE_SIZE }).subscribe((result) => {
      this.items.set(result.content);
      this.total.set(result.totalElements);
      this.totalPages.set(result.totalPages);
      this.loading.set(false);
    });
  }

  private refreshCounts(): void {
    this.api.counts().subscribe((counts) => this.counts.set(counts));
  }

  private collectNames(tree: CategoryTree[], names: Map<string, { tr: string; en: string }>): void {
    for (const node of tree) {
      names.set(node.code, { tr: node.nameTr, en: node.nameEn });
      this.collectNames(node.children, names);
    }
  }
}
