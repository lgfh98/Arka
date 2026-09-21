import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, tap } from 'rxjs';
import {
  Product,
  InventoryItem,
  ProductWithInventory,
  CreateProductRequest,
  AdjustStockRequest
} from '../../core/models/product.model';
import { ToastService } from '../../core/services/toast.service';

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  readonly products = signal<Product[]>([]);
  readonly inventoryMap = signal<Record<string, InventoryItem>>({});
  readonly lowStockItems = signal<InventoryItem[]>([]);
  readonly loading = signal<boolean>(false);
  readonly selectedCategory = signal<string>('TODOS');
  readonly searchTerm = signal<string>('');

  readonly categories = computed(() => {
    const list = this.products().map(p => p.category);
    return ['TODOS', ...Array.from(new Set(list))];
  });

  readonly productsWithInventory = computed<ProductWithInventory[]>(() => {
    const prods = this.products();
    const inv = this.inventoryMap();
    const cat = this.selectedCategory();
    const term = this.searchTerm().toLowerCase().trim();

    return prods
      .filter(p => {
        const matchesCat = cat === 'TODOS' || p.category.toLowerCase() === cat.toLowerCase();
        const matchesTerm = !term || p.name.toLowerCase().includes(term) || p.description.toLowerCase().includes(term);
        return matchesCat && matchesTerm;
      })
      .map(p => ({
        ...p,
        inventory: inv[p.id]
      }));
  });

  loadAll(): Observable<[Product[], InventoryItem[], InventoryItem[]]> {
    this.loading.set(true);
    return forkJoin([
      this.http.get<Product[]>('/api/products'),
      this.http.get<InventoryItem[]>('/api/inventory'),
      this.http.get<InventoryItem[]>('/api/inventory/low-stock')
    ]).pipe(
      tap({
        next: ([prods, invList, lowStock]) => {
          this.products.set(prods);

          const map: Record<string, InventoryItem> = {};
          for (const item of invList) {
            map[item.productId] = item;
          }
          this.inventoryMap.set(map);
          this.lowStockItems.set(lowStock);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        }
      })
    );
  }

  createProduct(request: CreateProductRequest): Observable<Product> {
    this.loading.set(true);
    return this.http.post<Product>('/api/products', request).pipe(
      tap({
        next: (created) => {
          this.toast.success('Producto Registrado', `"${created.name}" fue añadido exitosamente al catálogo.`);
          this.loadAll().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }

  adjustStock(productId: string, request: AdjustStockRequest): Observable<InventoryItem> {
    this.loading.set(true);
    return this.http.put<InventoryItem>(`/api/inventory/product/${productId}/stock`, request).pipe(
      tap({
        next: (updated) => {
          this.toast.success('Stock Actualizado', `Nuevo stock físico: ${updated.physicalStock} unidades.`);
          this.loadAll().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }
}
