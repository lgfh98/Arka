import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CartService } from './cart.service';
import { OrderService } from '../ordering/order.service';
import { InventoryService } from '../inventory/inventory.service';
import { CustomerSessionService } from '../../core/services/customer-session.service';
import { CreateOrderRequest } from '../../core/models/order.model';

@Component({
  selector: 'app-cart-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cart-page">
      <!-- HEADER -->
      <header class="page-header">
        <div>
          <h2>🛒 Carrito de Compras B2B & Detección de Abandono (HU8)</h2>
          <p class="subtitle">
            Gestión de pedidos en borrador, cálculo automático de subtotales y recuperación de carritos abandonados.
          </p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="refresh()" [disabled]="cartService.loading()">
            🔄 Refrescar
          </button>
          <button class="btn btn-warning" (click)="detectAbandonedNow()" [disabled]="cartService.loading()">
            ⚡ Forzar Detección de Abandono (0 min)
          </button>
        </div>
      </header>

      <!-- ACTIVE CART SECTION -->
      <section class="active-cart-section">
        <div class="section-title-bar">
          <h3>
            Carrito Activo: <span class="highlight">{{ session.activeCustomer().name }}</span>
          </h3>
          <span class="badge" [class.badge-confirmed]="cartService.cart()?.status === 'ACTIVE'">
            {{ cartService.cart()?.status || 'VACÍO' }}
          </span>
        </div>

        @if (cartService.cart()?.items && cartService.cart()!.items.length > 0) {
          <div class="cart-grid">
            <div class="data-table-container cart-table-box">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Precio Unitario</th>
                    <th>Cantidad</th>
                    <th>Subtotal</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of cartService.cart()!.items; track item.productId) {
                    <tr>
                      <td>
                        <strong>{{ getProductName(item.productId) }}</strong><br />
                        <code class="text-muted">{{ item.productId.substring(0, 8) }}...</code>
                      </td>
                      <td>\${{ item.unitPrice | number: '1.2-2' }}</td>
                      <td>
                        <span class="qty-pill">{{ item.quantity }} unid.</span>
                      </td>
                      <td class="font-bold">\${{ item.subtotal | number: '1.2-2' }}</td>
                      <td>
                        <button
                          class="btn btn-danger btn-sm"
                          (click)="removeItem(item.productId)"
                          title="Eliminar producto del carrito"
                        >
                          🗑️
                        </button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <!-- CART SUMMARY CARD -->
            <div class="cart-summary-card">
              <h4>Resumen del Pedido</h4>
              <div class="summary-line">
                <span>Total de Ítems:</span>
                <strong>{{ cartService.cartItemCount() }} unidades</strong>
              </div>
              <div class="summary-line">
                <span>Última Actividad:</span>
                <span>{{ cartService.cart()?.lastActivityAt | date: 'HH:mm:ss' }}</span>
              </div>
              <div class="summary-divider"></div>
              <div class="summary-total">
                <span>Total a Pagar:</span>
                <span class="total-price">\${{ cartService.cartTotal() | number: '1.2-2' }} USD</span>
              </div>

              <div class="summary-actions">
                <button
                  class="btn btn-primary btn-full"
                  (click)="convertCartToOrder()"
                  [disabled]="cartService.loading()"
                >
                  🚀 Convertir a Pedido B2B (HU4)
                </button>
                <button class="btn btn-secondary btn-full" (click)="goToCatalog()">
                  ➕ Seguir Agregando Productos
                </button>
              </div>
            </div>
          </div>
        } @else {
          <div class="empty-cart-card">
            <span class="empty-icon">🛒</span>
            <h4>El carrito de {{ session.activeCustomer().name }} está vacío</h4>
            <p>Agrega productos desde el catálogo mayorista para iniciar una cotización o pedido.</p>
            <button class="btn btn-primary" (click)="goToCatalog()" style="margin-top: 1rem;">
              Ver Catálogo de Productos
            </button>
          </div>
        }
      </section>

      <!-- ABANDONED CARTS SECTION -->
      <section class="abandoned-carts-section">
        <div class="section-title-bar">
          <div>
            <h3>📦 Carritos Abandonados Detectados para Recuperación (HU8)</h3>
            <p class="subtitle">
              Carritos que superaron el umbral de inactividad. El Transactional Outbox dispara recordatorios automáticos por email.
            </p>
          </div>
          <button class="btn btn-secondary btn-sm" (click)="loadAbandoned()">
            🔄 Recargar Abandonados
          </button>
        </div>

        <div class="data-table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID Carrito</th>
                <th>Cliente</th>
                <th>Estado</th>
                <th>Ítems</th>
                <th>Total en Carrito</th>
                <th>Última Actividad Registrada</th>
              </tr>
            </thead>
            <tbody>
              @for (ab of cartService.abandonedCarts(); track ab.id) {
                <tr>
                  <td><code>#{{ ab.id.substring(0, 8) }}</code></td>
                  <td>
                    <strong>{{ getCustomerName(ab.customerId) }}</strong><br />
                    <code class="text-muted">{{ ab.customerId.substring(0, 8) }}...</code>
                  </td>
                  <td>
                    <span class="badge badge-cancelled">ABANDONADO</span>
                  </td>
                  <td>{{ ab.items.length }} tipos de producto</td>
                  <td class="font-bold">\${{ ab.total | number: '1.2-2' }}</td>
                  <td>{{ ab.lastActivityAt | date: 'medium' }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No hay carritos abandonados actualmente. Presiona el botón <strong>"Forzar Detección de Abandono (0 min)"</strong> para probar la lógica de inactividad.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .cart-page {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .page-header h2 {
      font-size: 1.6rem;
      font-weight: 800;
      letter-spacing: -0.02em;
    }

    .subtitle {
      color: var(--text-muted);
      font-size: 0.9rem;
      margin-top: 0.25rem;
    }

    .header-actions {
      display: flex;
      gap: 0.75rem;
    }

    .section-title-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .highlight {
      color: var(--primary);
    }

    .cart-grid {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 1.5rem;
      align-items: start;
    }

    @media (max-width: 900px) {
      .cart-grid {
        grid-template-columns: 1fr;
      }
    }

    .qty-pill {
      background-color: var(--bg-surface-elevated);
      padding: 0.2rem 0.5rem;
      border-radius: var(--radius-sm);
      font-weight: 600;
    }

    .font-bold {
      font-weight: 700;
    }

    .cart-summary-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.5rem;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .cart-summary-card h4 {
      font-size: 1.15rem;
      font-weight: 700;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 0.75rem;
    }

    .summary-line {
      display: flex;
      justify-content: space-between;
      font-size: 0.9rem;
      color: var(--text-muted);
    }

    .summary-line strong {
      color: var(--text-main);
    }

    .summary-divider {
      height: 1px;
      background-color: var(--border-color);
      margin: 0.25rem 0;
    }

    .summary-total {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      font-size: 1.1rem;
      font-weight: 700;
    }

    .total-price {
      font-size: 1.5rem;
      font-weight: 800;
      color: var(--primary);
    }

    .summary-actions {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-top: 0.5rem;
    }

    .btn-full {
      width: 100%;
    }

    .empty-cart-card {
      background-color: var(--bg-surface);
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-lg);
      padding: 3rem;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }

    .empty-icon {
      font-size: 3rem;
    }

    .empty-cart-card h4 {
      font-size: 1.25rem;
      font-weight: 700;
    }

    .empty-cart-card p {
      color: var(--text-muted);
      font-size: 0.9rem;
    }
  `]
})
export class CartViewComponent implements OnInit {
  readonly cartService = inject(CartService);
  readonly orderService = inject(OrderService);
  readonly inventoryService = inject(InventoryService);
  readonly session = inject(CustomerSessionService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.refresh();
    this.inventoryService.loadAll().subscribe();
  }

  refresh(): void {
    const custId = this.session.activeCustomer().id;
    this.cartService.loadCustomerCart(custId).subscribe();
    this.loadAbandoned();
  }

  loadAbandoned(): void {
    this.cartService.loadAbandonedCarts().subscribe();
  }

  detectAbandonedNow(): void {
    this.cartService.detectAbandonedCarts(0).subscribe();
  }

  removeItem(productId: string): void {
    this.cartService.removeItem(productId).subscribe();
  }

  goToCatalog(): void {
    this.router.navigate(['/inventory']);
  }

  getProductName(productId: string): string {
    const prod = this.inventoryService.products().find(p => p.id === productId);
    return prod ? prod.name : `Producto (${productId.substring(0, 8)}...)`;
  }

  getCustomerName(customerId: string): string {
    const found = this.session.presetCustomers().find(c => c.id === customerId);
    return found ? found.name : `Cliente (${customerId.substring(0, 8)}...)`;
  }

  convertCartToOrder(): void {
    const currentCart = this.cartService.cart();
    if (!currentCart || currentCart.items.length === 0) return;

    const request: CreateOrderRequest = {
      customerId: currentCart.customerId,
      items: currentCart.items.map(i => ({
        productId: i.productId,
        quantity: i.quantity,
        unitPrice: i.unitPrice
      }))
    };

    this.orderService.createOrder(request).subscribe({
      next: () => {
        this.router.navigate(['/orders']);
      }
    });
  }
}
