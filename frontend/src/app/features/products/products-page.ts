import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideImage, LucidePlus } from '@lucide/angular';

@Component({
  selector: 'app-products-page',
  imports: [RouterLink, TranslocoPipe, LucideImage, LucidePlus],
  templateUrl: './products-page.html',
  styleUrl: './products-page.scss',
})
export class ProductsPage {}
