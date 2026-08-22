import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideImage, LucidePlus } from '@lucide/angular';

@Component({
  selector: 'app-products-page',
  imports: [RouterLink, TranslocoPipe, LucideImage, LucidePlus],
  template: `
    <div class="empty-state">
      <div class="icon-box">
        <svg lucideImage [size]="19" [strokeWidth]="1.6"></svg>
      </div>
      <h2>{{ 'products.empty.title' | transloco }}</h2>
      <p>{{ 'products.empty.description' | transloco }}</p>
      <a class="primary-button" routerLink="/upload">
        <svg lucidePlus [size]="13" [strokeWidth]="2.2"></svg>
        {{ 'products.empty.action' | transloco }}
      </a>
    </div>
  `,
  styles: `
    .empty-state {
      max-width: 320px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 14px;
      text-align: center;
    }

    .icon-box {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      color: var(--color-text-dim);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    h2 {
      margin: 0;
      font-size: 13.5px;
      font-weight: 600;
      letter-spacing: -0.012em;
    }

    p {
      margin: 0;
      font-size: 12.5px;
      line-height: 1.5;
      color: var(--color-text-muted);
    }

    .primary-button {
      height: 30px;
      padding: 0 13px;
      border-radius: 8px;
      background: var(--color-primary);
      color: var(--color-on-primary);
      font-size: 12.5px;
      font-weight: 500;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 7px;
      transition: background-color 120ms ease;

      &:hover {
        background: var(--color-primary-hover);
      }
    }
  `,
})
export class ProductsPage {}
