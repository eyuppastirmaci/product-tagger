import { Component } from '@angular/core';
import { ProductTable } from './product-table';

@Component({
  selector: 'app-products-page',
  imports: [ProductTable],
  templateUrl: './products-page.html',
  styleUrl: './products-page.scss',
})
export class ProductsPage {}
