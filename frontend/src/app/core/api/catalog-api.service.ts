import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CategorySchemaResponse, CategoryTree } from './models';

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/categories';

  tree(): Observable<CategoryTree[]> {
    return this.http.get<CategoryTree[]>(this.base);
  }

  schema(categoryCode: string): Observable<CategorySchemaResponse> {
    return this.http.get<CategorySchemaResponse>(`${this.base}/${categoryCode}/schema`);
  }
}
