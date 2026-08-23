import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CatalogApi } from '../../core/api/catalog-api.service';
import { CategorySchemaResponse, ReviewResponse } from '../../core/api/models';
import { ProductApi } from '../../core/api/product-api.service';
import { ProductEvents } from '../../core/api/product-events.service';
import { LanguageService } from '../../core/i18n/language.service';
import { PageTitleService } from '../../core/layout/page-title.service';
import { ReviewPage } from './review-page';

describe('ReviewPage', () => {
  let fixture: ComponentFixture<ReviewPage>;
  let component: any;
  let reviewSubjects: Record<string, Subject<ReviewResponse>>;
  let schemaSubjects: Record<string, Subject<CategorySchemaResponse>>;
  let productApi: any;

  beforeEach(() => {
    reviewSubjects = {};
    schemaSubjects = {};

    productApi = {
      review: vi.fn((id: string) => (reviewSubjects[id] ??= new Subject())),
      imageUrl: vi.fn(() => 'http://localhost/image'),
      approve: vi.fn(),
      reject: vi.fn(),
      retag: vi.fn(),
      regenerateContent: vi.fn(),
      updateContent: vi.fn(),
    };

    const catalogApi = {
      tree: vi.fn(() => of([])),
      schema: vi.fn((code: string) => (schemaSubjects[code] ??= new Subject())),
    };

    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('no network in tests'))));

    TestBed.configureTestingModule({
      imports: [ReviewPage],
      providers: [
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'p1' })) } },
        { provide: ProductApi, useValue: productApi },
        { provide: CatalogApi, useValue: catalogApi },
        { provide: ProductEvents, useValue: { events$: new Subject() } },
        { provide: PageTitleService, useValue: { override: signal<string | null>(null) } },
        { provide: LanguageService, useValue: { lang: signal('en') } },
        { provide: TranslocoService, useValue: { translate: (key: string) => key } },
      ],
    });

    // The race lives in the class logic; the template is not under test here
    TestBed.overrideComponent(ReviewPage, { set: { template: '' } });

    fixture = TestBed.createComponent(ReviewPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('discards a late schema response for a previously selected category', () => {
    reviewSubjects['p1'].next(review('p1', 'tshirt'));

    // The proposed category's schema request is in flight...
    expect(schemaSubjects['tshirt'].observed).toBe(true);

    // ...when the reviewer switches to another category
    component.onCategorySelected('jeans');

    // switchMap must have cancelled the stale request
    expect(schemaSubjects['tshirt'].observed).toBe(false);

    // Responses arrive out of order: jeans first, stale tshirt after
    schemaSubjects['jeans'].next(schema('jeans'));
    schemaSubjects['tshirt'].next(schema('tshirt'));

    expect(component.schema().categoryCode).toBe('jeans');
    expect(component.selectedCategory()).toBe('jeans');
  });

  it('discards a late review response for a previously loaded product', () => {
    component.load('p2');

    reviewSubjects['p2'].next(review('p2', null));
    reviewSubjects['p1'].next(review('p1', 'tshirt'));

    expect(component.review().id).toBe('p2');
  });

  it('keeps loading schemas after a failed request', () => {
    reviewSubjects['p1'].next(review('p1', 'tshirt'));

    schemaSubjects['tshirt'].error(new Error('500'));

    component.onCategorySelected('jeans');
    schemaSubjects['jeans'].next(schema('jeans'));

    expect(component.schema().categoryCode).toBe('jeans');
  });

  it('applies pending proposal values only for the matching schema', () => {
    reviewSubjects['p1'].next(reviewWithAttributes('p1', 'tshirt', { color: ['black'] }));

    component.onCategorySelected('jeans');
    schemaSubjects['jeans'].next(schema('jeans'));

    // A manual switch discards the proposal's values instead of grafting
    // them onto a foreign schema
    expect(component.values()).toEqual({});
  });

  it('flags a 409 conflict and reloads the review', () => {
    productApi.approve.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    reviewSubjects['p1'].next(review('p1', 'tshirt'));
    schemaSubjects['tshirt'].next(schema('tshirt'));

    const reviewCallsBefore = productApi.review.mock.calls.length;

    component.approve();

    expect(component.conflict()).toBe(true);
    expect(component.submitting()).toBe(false);
    expect(productApi.review.mock.calls.length).toBe(reviewCallsBefore + 1);
  });

  function review(id: string, categoryCode: string | null): ReviewResponse {
    return reviewWithAttributes(id, categoryCode, {});
  }

  function reviewWithAttributes(
    id: string,
    categoryCode: string | null,
    attributes: Record<string, unknown>,
  ): ReviewResponse {
    return {
      id,
      status: 'PENDING_REVIEW',
      createdAt: '2026-01-01T00:00:00Z',
      titleTr: null,
      titleEn: null,
      descriptionTr: null,
      descriptionEn: null,
      proposal: categoryCode
        ? {
            proposedCategory: { code: categoryCode, nameTr: categoryCode, nameEn: categoryCode },
            attributes,
            confidences: {},
            createdAt: '2026-01-01T00:00:10Z',
          }
        : null,
    } as unknown as ReviewResponse;
  }

  function schema(code: string): CategorySchemaResponse {
    return {
      categoryCode: code,
      version: 1,
      schema: { attributes: [] },
    } as unknown as CategorySchemaResponse;
  }
});
