import { Component, HostListener, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { FormsModule } from '@angular/forms';
import {
  LucideCheck,
  LucideCopy,
  LucideInfo,
  LucideMaximize2,
  LucideRefreshCw,
  LucideRotateCw,
  LucideSparkles,
  LucideX,
} from '@lucide/angular';
import { HttpErrorResponse } from '@angular/common/http';
import { EMPTY, Subject, Subscription, catchError, map, switchMap } from 'rxjs';
import { CatalogApi } from '../../core/api/catalog-api.service';
import {
  AttributeValues,
  CategorySchemaResponse,
  CategoryTree,
  Confidences,
  ReviewResponse,
  SchemaAttribute,
} from '../../core/api/models';
import { ProductApi } from '../../core/api/product-api.service';
import { ProductEvents } from '../../core/api/product-events.service';
import { LanguageService } from '../../core/i18n/language.service';
import { PageTitleService } from '../../core/layout/page-title.service';
import { LocaleDatePipe } from '../../shared/format/locale-date.pipe';
import { RelativeTimePipe } from '../../shared/format/relative-time.pipe';
import { ConfidenceBadge } from '../../shared/ui/confidence-badge';
import { Select, SelectOption } from '../../shared/ui/select';
import { StatusBadge } from '../../shared/ui/status-badge';
import { Textarea } from '../../shared/ui/textarea';
import { AttributeForm } from './attribute-form';

interface ImageMeta {
  width: number;
  height: number;
  format: string | null;
  sizeBytes: number | null;
}

interface LeafCategory {
  code: string;
  nameTr: string;
  nameEn: string;
}

const LOW_CONFIDENCE = 0.6;

@Component({
  selector: 'app-review-page',
  imports: [
    FormsModule,
    TranslocoPipe,
    LocaleDatePipe,
    RelativeTimePipe,
    StatusBadge,
    ConfidenceBadge,
    Select,
    Textarea,
    AttributeForm,
    LucideInfo,
    LucideCopy,
    LucideCheck,
    LucideMaximize2,
    LucideRefreshCw,
    LucideRotateCw,
    LucideSparkles,
    LucideX,
  ],
  templateUrl: './review-page.html',
  styleUrl: './review-page.scss',
})
export class ReviewPage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ProductApi);
  private readonly productEvents = inject(ProductEvents);
  private readonly catalog = inject(CatalogApi);
  private readonly pageTitle = inject(PageTitleService);
  private readonly transloco = inject(TranslocoService);

  protected readonly language = inject(LanguageService);

  protected readonly review = signal<ReviewResponse | null>(null);
  protected readonly leafCategories = signal<LeafCategory[]>([]);
  protected readonly selectedCategory = signal<string | null>(null);
  protected readonly schema = signal<CategorySchemaResponse | null>(null);
  protected readonly values = signal<AttributeValues>({});
  protected readonly errors = signal<Record<string, boolean>>({});
  protected readonly submitting = signal(false);
  protected readonly imageMeta = signal<ImageMeta | null>(null);
  protected readonly detailsOpen = signal(false);
  protected readonly copied = signal(false);

  protected readonly descriptionTr = signal<string | null>(null);
  protected readonly descriptionEn = signal<string | null>(null);
  protected readonly generating = signal(false);
  protected readonly conflict = signal(false);

  // Out-of-order responses: review and schema requests funnel through
  // switchMap pipelines, so a late response for a previous product or
  // category is cancelled instead of overwriting newer state
  private readonly reviewLoads$ = new Subject<string>();
  private readonly schemaLoads$ = new Subject<string>();

  // Proposal values wait here until the proposed category's schema has loaded
  private pendingInitialValues: AttributeValues | null = null;
  private eventsSubscription: Subscription | null = null;
  private generationTimer: ReturnType<typeof setTimeout> | null = null;
  private autosaveTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly productId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id')!)),
  );

  constructor() {
    this.catalog.tree().subscribe((tree) => this.leafCategories.set(this.flattenLeaves(tree)));

    this.reviewLoads$
      .pipe(
        switchMap((id) => this.api.review(id).pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(),
      )
      .subscribe((review) => this.applyReview(review));

    this.schemaLoads$
      .pipe(
        switchMap((code) => this.catalog.schema(code).pipe(catchError(() => EMPTY))),
        takeUntilDestroyed(),
      )
      .subscribe((schema) => this.applySchema(schema));

    effect(() => {
      const id = this.productId();

      if (id) {
        this.load(id);
      }
    });

    effect(() => {
      this.pageTitle.override.set(this.title());
    });

    // Follows a retag live: every pipeline transition of this product reloads
    // the screen; APPROVED is left to the description watcher
    this.productEvents.events$.pipe(takeUntilDestroyed()).subscribe((event) => {
      const id = this.productId();

      if (id && event.productId === id && event.status !== 'APPROVED') {
        this.load(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.pageTitle.override.set(null);
    this.stopWatching();

    if (this.autosaveTimer) {
      clearTimeout(this.autosaveTimer);
    }
  }

  protected readonly descriptionsReady = computed(() => this.review()?.descriptionTr != null);

  /** AI showcase title once generated; otherwise derived from the proposal. */
  protected readonly title = computed(() => {
    const review = this.review();

    if (!review) {
      return null;
    }

    const aiTitle = this.language.lang() === 'tr' ? review.titleTr : review.titleEn;

    return aiTitle ?? this.derivedTitle(review);
  });

  protected readonly imageUrl = computed(() => {
    const id = this.productId();

    return id ? this.api.imageUrl(id, 'processed') : '';
  });

  // Confidences only apply while the proposed category is still selected
  protected readonly activeConfidences = computed<Confidences | null>(() => {
    const review = this.review();

    if (!review?.proposal?.confidences) {
      return null;
    }

    return this.selectedCategory() === review.proposal.proposedCategory?.code
      ? review.proposal.confidences
      : null;
  });

  protected readonly categoryConfidence = computed(() => {
    const confidences = this.activeConfidences();

    if (!confidences) {
      return null;
    }

    const levels = Object.entries(confidences)
      .filter(([key]) => key.startsWith('category.'))
      .map(([, value]) => value);

    return levels.length === 0 ? null : Math.min(...levels);
  });

  protected readonly lowConfidenceCount = computed(() => {
    const confidences = this.activeConfidences();
    const definitions = this.schema()?.schema.attributes ?? [];

    if (!confidences) {
      return 0;
    }

    return definitions.filter((definition) => {
      const confidence = confidences[definition.key];

      return confidence != null && confidence < LOW_CONFIDENCE;
    }).length;
  });

  protected readonly canAct = computed(() => {
    const status = this.review()?.status;

    return status === 'PENDING_REVIEW' || status === 'FAILED';
  });

  // Approximation until a real duration is tracked: upload -> proposal time
  protected readonly processingSeconds = computed(() => {
    const review = this.review();

    if (!review?.proposal) {
      return null;
    }

    const millis = new Date(review.proposal.createdAt).getTime() - new Date(review.createdAt).getTime();

    return millis > 0 ? millis / 1000 : null;
  });

  protected readonly shortId = computed(() => {
    const id = this.productId();

    return id ? `${id.slice(0, 8)}…${id.slice(-4)}` : '';
  });

  protected readonly categoryOptions = computed<SelectOption[]>(() => {
    const lang = this.language.lang();

    return this.leafCategories().map((category) => ({
      value: category.code,
      label: lang === 'tr' ? category.nameTr : category.nameEn,
    }));
  });

  protected onCategorySelected(code: string | null): void {
    if (!code || code === this.selectedCategory()) {
      return;
    }

    // A manual category switch resets everything the proposal had filled in
    this.pendingInitialValues = null;
    this.values.set({});
    this.errors.set({});
    this.selectedCategory.set(code);
    this.loadSchema(code);
  }

  protected approve(): void {
    const id = this.productId();
    const category = this.selectedCategory();

    if (!id || !category || !this.validate()) {
      return;
    }

    this.submitting.set(true);
    this.conflict.set(false);

    this.api.approve(id, { categoryCode: category, attributes: this.cleanedValues() }).subscribe({
      next: () => {
        this.submitting.set(false);
        // Generation starts right after approval; the flag keeps the info box
        // on "generating" until the texts land over SSE
        this.generating.set(true);
        this.load(id);
        this.watchDescriptions(id);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.handleConflict(error);
      },
    });
  }

  protected onDescriptionChange(lang: 'tr' | 'en', value: string | null): void {
    if (lang === 'tr') {
      this.descriptionTr.set(value);
    } else {
      this.descriptionEn.set(value);
    }

    this.scheduleAutosave();
  }

  protected regenerate(): void {
    const id = this.productId();

    if (!id) {
      return;
    }

    this.api.regenerateContent(id).subscribe(() => {
      this.generating.set(true);
      this.load(id);
      this.watchDescriptions(id);
    });
  }

  protected reject(): void {
    const id = this.productId();

    if (id) {
      this.conflict.set(false);

      this.api.reject(id).subscribe({
        next: () => this.load(id),
        error: (error: HttpErrorResponse) => this.handleConflict(error),
      });
    }
  }

  protected retag(): void {
    const id = this.productId();

    if (id) {
      this.conflict.set(false);

      this.api.retag(id).subscribe({
        next: () => this.load(id),
        error: (error: HttpErrorResponse) => this.handleConflict(error),
      });
    }
  }

  /** A 409 means another reviewer beat us to a decision: flag it and reload. */
  private handleConflict(error: HttpErrorResponse): void {
    if (error.status !== 409) {
      return;
    }

    this.conflict.set(true);

    const id = this.productId();

    if (id) {
      this.load(id);
    }
  }

  protected formatSeconds(seconds: number): string {
    return new Intl.NumberFormat(this.language.lang(), {
      style: 'unit',
      unit: 'second',
      maximumFractionDigits: 1,
    }).format(seconds);
  }

  protected toggleDetails(event: MouseEvent): void {
    event.stopPropagation();
    this.detailsOpen.update((open) => !open);
  }

  @HostListener('document:click')
  protected closeDetails(): void {
    this.detailsOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  protected closeOnEscape(): void {
    this.detailsOpen.set(false);
  }

  protected async copyId(): Promise<void> {
    const id = this.productId();

    if (!id) {
      return;
    }

    await navigator.clipboard.writeText(id);

    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 1500);
  }

  protected onImageLoad(event: Event): void {
    const image = event.target as HTMLImageElement;

    this.imageMeta.update((meta) => ({
      width: image.naturalWidth,
      height: image.naturalHeight,
      format: meta?.format ?? null,
      sizeBytes: meta?.sizeBytes ?? null,
    }));
  }

  protected openOriginal(): void {
    const id = this.productId();

    if (id) {
      window.open(this.api.imageUrl(id, 'original'), '_blank');
    }
  }

  private load(id: string): void {
    this.reviewLoads$.next(id);
    this.loadImageHeaders(id);
  }

  private applyReview(review: ReviewResponse): void {
    this.review.set(review);
    this.errors.set({});
    this.descriptionTr.set(review.descriptionTr);
    this.descriptionEn.set(review.descriptionEn);

    if (review.descriptionTr != null) {
      this.generating.set(false);
    }

    const proposedCode = review.proposal?.proposedCategory?.code ?? null;

    this.selectedCategory.set(proposedCode);
    this.pendingInitialValues = review.proposal?.attributes ?? null;

    // Same category as the loaded schema: swap the values in place so the
    // form does not unmount and flicker on reloads (e.g. after a retag)
    if (proposedCode && proposedCode === this.schema()?.categoryCode) {
      this.values.set(this.pendingInitialValues ?? {});
      this.pendingInitialValues = null;
    } else {
      this.values.set({});
      this.schema.set(null);

      if (proposedCode) {
        this.loadSchema(proposedCode);
      }
    }
  }

  // Debounced PATCH so every keystroke does not hit the API
  private scheduleAutosave(): void {
    if (this.autosaveTimer) {
      clearTimeout(this.autosaveTimer);
    }

    this.autosaveTimer = setTimeout(() => this.saveContent(), 800);
  }

  private saveContent(): void {
    const id = this.productId();
    const review = this.review();

    if (!id || !review || review.status !== 'APPROVED') {
      return;
    }

    this.api.updateContent(id, {
      titleTr: review.titleTr,
      titleEn: review.titleEn,
      descriptionTr: this.descriptionTr(),
      descriptionEn: this.descriptionEn(),
    }).subscribe();
  }

  /** Watches the shared SSE stream until the generated texts land, then reloads. */
  private watchDescriptions(id: string): void {
    this.stopWatching();

    this.eventsSubscription = this.productEvents.events$.subscribe((event) => {
      if (event.productId === id && event.descriptionsReady) {
        this.stopWatching();
        this.load(id);
      }
    });

    // Safety valve: a permanently failed generation emits no event, so fall
    // back to the Regenerate affordance instead of spinning forever
    this.generationTimer = setTimeout(() => {
      this.stopWatching();
      this.generating.set(false);
    }, 90_000);
  }

  private stopWatching(): void {
    this.eventsSubscription?.unsubscribe();
    this.eventsSubscription = null;

    if (this.generationTimer) {
      clearTimeout(this.generationTimer);
      this.generationTimer = null;
    }
  }

  private loadSchema(code: string): void {
    this.schemaLoads$.next(code);
  }

  private applySchema(schema: CategorySchemaResponse): void {
    this.schema.set(schema);

    if (this.pendingInitialValues && this.selectedCategory() === schema.categoryCode) {
      this.values.set(this.pendingInitialValues);
      this.pendingInitialValues = null;
    }
  }

  /** Required fields must have a value; all violations are marked at once. */
  private validate(): boolean {
    const definitions = this.schema()?.schema.attributes ?? [];
    const values = this.values();
    const errors: Record<string, boolean> = {};

    for (const definition of definitions) {
      if (!definition.required) {
        continue;
      }

      const value = values[definition.key];
      const empty = value == null || value === '' || (Array.isArray(value) && value.length === 0);

      if (empty) {
        errors[definition.key] = true;
      }
    }

    this.errors.set(errors);

    return Object.keys(errors).length === 0;
  }

  private cleanedValues(): AttributeValues {
    return Object.fromEntries(
      Object.entries(this.values()).filter(([, value]) => {
        return value != null && value !== '' && !(Array.isArray(value) && value.length === 0);
      }),
    );
  }

  private loadImageHeaders(id: string): Promise<void> | void {
    return fetch(this.api.imageUrl(id, 'original'), { method: 'HEAD' })
      .then((response) => {
        const contentType = response.headers.get('content-type');
        const contentLength = response.headers.get('content-length');

        this.imageMeta.update((meta) => ({
          width: meta?.width ?? 0,
          height: meta?.height ?? 0,
          format: contentType ? contentType.replace('image/', '').toUpperCase() : null,
          sizeBytes: contentLength ? Number(contentLength) : null,
        }));
      })
      .catch(() => {
        // Meta line degrades gracefully without headers
      });
  }

  private derivedTitle(review: ReviewResponse): string {
    const proposal = review.proposal;

    if (!proposal?.proposedCategory) {
      return this.transloco.translate('review.breadcrumb');
    }

    const categoryName = this.language.lang() === 'tr'
      ? proposal.proposedCategory.nameTr
      : proposal.proposedCategory.nameEn;

    const definitions = this.schema()?.schema.attributes ?? [];

    const values = Object.entries(proposal.attributes ?? {})
      .flatMap(([key, value]) => (Array.isArray(value) ? value.map((entry) => [key, entry]) : [[key, value]]))
      .filter((pair): pair is [string, string] => typeof pair[1] === 'string')
      .slice(0, 2)
      .map(([key, code]) => this.valueLabel(definitions, key, code));

    return [...values, categoryName].join(' ');
  }

  // Schema labels when available; prettified code as the fallback
  private valueLabel(definitions: SchemaAttribute[], key: string, code: string): string {
    const entry = definitions
      .find((definition) => definition.key === key)?.values
      ?.find((value) => value.value === code);

    if (entry) {
      return this.language.lang() === 'tr' ? entry.label_tr : entry.label_en;
    }

    return code
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private flattenLeaves(tree: CategoryTree[]): LeafCategory[] {
    return tree.flatMap((node) =>
      node.leaf
        ? [{ code: node.code, nameTr: node.nameTr, nameEn: node.nameEn }]
        : this.flattenLeaves(node.children),
    );
  }
}
