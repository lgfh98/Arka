import { Injectable, inject, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Cart, AddCartItemRequest } from '../../core/models/cart.model';
import { CustomerSessionService } from '../../core/services/customer-session.service';
import { ToastService } from '../../core/services/toast.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(CustomerSessionService);
  private readonly toast = inject(ToastService);

  readonly cart = signal<Cart | null>(null);
  readonly abandonedCarts = signal<Cart[]>([]);
  readonly loading = signal<boolean>(false);

  readonly cartItemCount = computed(() => {
    const c = this.cart();
    if (!c || !c.items) return 0;
    return c.items.reduce((acc, item) => acc + item.quantity, 0);
  });

  readonly cartTotal = computed(() => {
    return this.cart()?.total ?? 0;
  });

  constructor() {
    // Whenever active customer changes, reload their cart
    effect(() => {
      const customer = this.session.activeCustomer();
      if (customer && customer.id) {
        this.loadCustomerCart(customer.id).subscribe();
      }
    });
  }

  loadCustomerCart(customerId: string): Observable<Cart> {
    this.loading.set(true);
    return this.http.get<Cart>(`/api/carts/${customerId}`).pipe(
      tap({
        next: (c) => {
          this.cart.set(c);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  addItem(productId: string, quantity: number, unitPrice: number): Observable<Cart> {
    const customerId = this.session.activeCustomer().id;
    const request: AddCartItemRequest = { productId, quantity, unitPrice };
    this.loading.set(true);

    return this.http.post<Cart>(`/api/carts/${customerId}/items`, request).pipe(
      tap({
        next: (updatedCart) => {
          this.cart.set(updatedCart);
          this.toast.success('Carrito Actualizado', `${quantity} unid. agregadas correctamente.`);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  removeItem(productId: string): Observable<Cart> {
    const customerId = this.session.activeCustomer().id;
    this.loading.set(true);

    return this.http.delete<Cart>(`/api/carts/${customerId}/items/${productId}`).pipe(
      tap({
        next: (updatedCart) => {
          this.cart.set(updatedCart);
          this.toast.info('Producto Eliminado', 'Ítem removido del carrito.');
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  loadAbandonedCarts(): Observable<Cart[]> {
    this.loading.set(true);
    return this.http.get<Cart[]>('/api/carts/abandoned').pipe(
      tap({
        next: (list) => {
          this.abandonedCarts.set(list);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  detectAbandonedCarts(minutes: number = 0): Observable<{ abandonedCount: number; thresholdMinutes: number }> {
    this.loading.set(true);
    return this.http.post<{ abandonedCount: number; thresholdMinutes: number }>(
      `/api/carts/detect-abandoned?minutes=${minutes}`,
      {}
    ).pipe(
      tap({
        next: (res) => {
          this.toast.warning(
            'Detección de Abandono Completada',
            `Se detectaron y marcaron ${res.abandonedCount} carrito(s) abandonado(s).`
          );
          this.loadAbandonedCarts().subscribe();
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }
}
