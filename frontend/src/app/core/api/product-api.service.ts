import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ApproveRequest,
  ImageVariant,
  PageResponse,
  ProductResponse,
  ProductStatus,
  ReviewResponse,
  UpdateContentRequest,
} from './models';

@Injectable({ providedIn: 'root' })
export class ProductApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/products';

  list(options: { status?: ProductStatus[]; page?: number; size?: number } = {}): Observable<PageResponse<ProductResponse>> {
    let params = new HttpParams()
      .set('page', options.page ?? 0)
      .set('size', options.size ?? 20);

    if (options.status?.length) {
      params = params.set('status', options.status.join(','));
    }

    return this.http.get<PageResponse<ProductResponse>>(this.base, { params });
  }

  get(id: string): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.base}/${id}`);
  }

  review(id: string): Observable<ReviewResponse> {
    return this.http.get<ReviewResponse>(`${this.base}/${id}/review`);
  }

  upload(file: File): Observable<ProductResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ProductResponse>(this.base, form);
  }

  approve(id: string, request: ApproveRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.base}/${id}/approve`, request);
  }

  reject(id: string): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.base}/${id}/reject`, null);
  }

  retag(id: string): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.base}/${id}/retag`, null);
  }

  updateContent(id: string, content: UpdateContentRequest): Observable<ProductResponse> {
    return this.http.patch<ProductResponse>(`${this.base}/${id}/content`, content);
  }

  regenerateContent(id: string): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`${this.base}/${id}/content/regenerate`, null);
  }

  imageUrl(id: string, variant: ImageVariant = 'thumbnail'): string {
    return `${this.base}/${id}/image?variant=${variant}`;
  }
}
