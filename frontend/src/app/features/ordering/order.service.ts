import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  PurchaseOrder,
  CreateOrderRequest,
  ModifyOrderRequest,
  OrderStatus
} from '../../core/models/order.model';
import { ToastService } from '../../core/services/toast.service';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  readonly orders = signal<PurchaseOrder[]>([]);
  readonly loading = signal<boolean>(false);
  readonly selectedOrder = signal<PurchaseOrder | null>(null);
  readonly filterStatus = signal<string>('TODOS');
  readonly filterByActiveCustomerOnly = signal<boolean>(false);

  readonly filteredOrders = computed(() => {
    const list = this.orders();
    const st = this.filterStatus();

    return list.filter(o => {
      const matchStatus = st === 'TODOS' || o.status === st;
      return matchStatus;
    });
  });

  loadOrders(customerId?: string): Observable<PurchaseOrder[]> {
    this.loading.set(true);
    const url = customerId ? `/api/orders?customerId=${customerId}` : '/api/orders';

    return this.http.get<PurchaseOrder[]>(url).pipe(
      tap({
        next: (list) => {
          this.orders.set(list);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  getOrderById(id: string): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.get<PurchaseOrder>(`/api/orders/${id}`).pipe(
      tap({
        next: (order) => {
          this.selectedOrder.set(order);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  createOrder(request: CreateOrderRequest): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.post<PurchaseOrder>('/api/orders', request).pipe(
      tap({
        next: (created) => {
          this.toast.success(
            'Pedido B2B Radicado (PENDIENTE)',
            `Orden #${created.id.substring(0, 8)} creada. Reserva preventiva de stock activada vía Outbox.`
          );
          this.loadOrders().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }

  modifyOrder(id: string, request: ModifyOrderRequest): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.put<PurchaseOrder>(`/api/orders/${id}/items`, request).pipe(
      tap({
        next: (modified) => {
          this.toast.success(
            'Pedido Modificado',
            `Ítems del pedido #${id.substring(0, 8)} actualizados con ajuste de stock en Outbox.`
          );
          this.selectedOrder.set(modified);
          this.loadOrders().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }

  confirmOrder(id: string): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.post<PurchaseOrder>(`/api/orders/${id}/confirm`, {}).pipe(
      tap({
        next: (order) => {
          this.toast.success(
            'Orden Confirmada',
            `Pedido #${id.substring(0, 8)} confirmado. Stock físico descontado y proyecciones CQRS actualizadas.`
          );
          this.selectedOrder.set(order);
          this.loadOrders().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }

  dispatchOrder(id: string): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.post<PurchaseOrder>(`/api/orders/${id}/dispatch`, {}).pipe(
      tap({
        next: (order) => {
          this.toast.info(
            'Orden Despachada',
            `Pedido #${id.substring(0, 8)} despachado en ruta hacia almacén de destino.`
          );
          this.selectedOrder.set(order);
          this.loadOrders().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }

  deliverOrder(id: string): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.post<PurchaseOrder>(`/api/orders/${id}/deliver`, {}).pipe(
      tap({
        next: (order) => {
          this.toast.success(
            'Orden Entregada',
            `Pedido #${id.substring(0, 8)} entregado a satisfacción al cliente mayorista.`
          );
          this.selectedOrder.set(order);
          this.loadOrders().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }

  cancelOrder(id: string): Observable<PurchaseOrder> {
    this.loading.set(true);
    return this.http.post<PurchaseOrder>(`/api/orders/${id}/cancel`, {}).pipe(
      tap({
        next: (order) => {
          this.toast.warning(
            'Orden Cancelada',
            `Pedido #${id.substring(0, 8)} cancelado y reservas liberadas.`
          );
          this.selectedOrder.set(order);
          this.loadOrders().subscribe();
        },
        error: () => this.loading.set(false)
      })
    );
  }
}
