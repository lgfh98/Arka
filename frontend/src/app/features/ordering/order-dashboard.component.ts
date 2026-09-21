import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from './order.service';
import { InventoryService } from '../inventory/inventory.service';
import { CustomerSessionService } from '../../core/services/customer-session.service';
import { PurchaseOrder, OrderStatus, CreateOrderRequest, ModifyOrderRequest } from '../../core/models/order.model';

@Component({
  selector: 'app-order-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="orders-page">
      <!-- HEADER -->
      <header class="page-header">
        <div>
          <h2>📦 Gestión de Órdenes de Compra B2B</h2>
          <p class="subtitle">
            Ciclo de vida transaccional: Radicación (Reserva preventiva vía Outbox) ➡️ Modificación ➡️ Confirmación (Descuento físico) ➡️ Despacho ➡️ Entrega.
          </p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="refresh()" [disabled]="orderService.loading()">
            🔄 Refrescar
          </button>
          <button class="btn btn-primary" (click)="openCreateOrderModal()">
            ➕ Nueva Orden B2B (HU4)
          </button>
        </div>
      </header>

      <!-- STATUS FILTER TABS -->
      <div class="filters-bar">
        <div class="status-tabs">
          @for (st of statusTabs; track st) {
            <button
              class="tab-btn"
              [class.active]="orderService.filterStatus() === st"
              (click)="orderService.filterStatus.set(st)"
            >
              {{ st }}
            </button>
          }
        </div>
        <div class="filter-customer">
          <label>
            <input
              type="checkbox"
              [(ngModel)]="filterByCurrentCustomer"
              (ngModelChange)="onFilterCustomerChange()"
            />
            Solo cliente activo ({{ session.activeCustomer().name.substring(0, 20) }}...)
          </label>
        </div>
      </div>

      <!-- ORDERS TABLE -->
      <div class="data-table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID Orden</th>
              <th>Cliente</th>
              <th>Fecha de Creación</th>
              <th>Ítems</th>
              <th>Total (USD)</th>
              <th>Estado</th>
              <th>Acciones Rápidas</th>
            </tr>
          </thead>
          <tbody>
            @for (order of orderService.filteredOrders(); track order.id) {
              <tr>
                <td>
                  <code class="order-id-badge">#{{ order.id.substring(0, 8) }}</code>
                </td>
                <td>
                  <div class="customer-cell">
                    <span class="customer-name">{{ getCustomerDisplayName(order.customerId) }}</span>
                    <span class="customer-uuid">{{ order.customerId.substring(0, 8) }}...</span>
                  </div>
                </td>
                <td>{{ order.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>
                  <span class="badge" style="background: var(--bg-surface-elevated); color: var(--text-main);">
                    {{ order.items.length }} prod.
                  </span>
                </td>
                <td class="font-bold">\${{ order.totalAmount | number: '1.2-2' }}</td>
                <td>
                  <span class="badge" [ngClass]="getStatusBadgeClass(order.status)">
                    {{ order.status }}
                  </span>
                </td>
                <td>
                  <div class="action-buttons-cell">
                    <button class="btn btn-secondary btn-sm" (click)="viewOrderDetail(order)">
                      👁️ Detalle
                    </button>
                    @if (order.status === 'PENDING') {
                      <button class="btn btn-primary btn-sm" (click)="confirm(order)">
                        ✅ Confirmar (HU6)
                      </button>
                    }
                    @if (order.status === 'CONFIRMED') {
                      <button class="btn btn-warning btn-sm" (click)="dispatch(order)">
                        🚚 Despachar (HU6)
                      </button>
                    }
                    @if (order.status === 'DISPATCHED') {
                      <button class="btn btn-success btn-sm" (click)="deliver(order)">
                        📦 Entregar (HU6)
                      </button>
                    }
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="empty-table-cell">
                  @if (orderService.loading()) {
                    <div class="spinner"></div>
                    <p>Cargando órdenes...</p>
                  } @else {
                    <p>No hay pedidos radicados en esta vista.</p>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- MODAL: DETALLE Y CICLO DE VIDA DE LA ORDEN -->
      @if (showDetailModal() && selectedOrder()) {
        <div class="modal-backdrop" (click)="closeDetailModal()">
          <div class="modal-dialog modal-large" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <div>
                <h3>Detalle de Orden #{{ selectedOrder()?.id }}</h3>
                <span class="badge" [ngClass]="getStatusBadgeClass(selectedOrder()!.status)">
                  {{ selectedOrder()!.status }}
                </span>
              </div>
              <button class="modal-close" (click)="closeDetailModal()">&times;</button>
            </div>

            <div class="modal-body">
              <div class="order-meta-grid">
                <div>
                  <span class="meta-lbl">Cliente B2B:</span>
                  <p class="meta-val">{{ getCustomerDisplayName(selectedOrder()!.customerId) }}</p>
                  <code class="text-muted">{{ selectedOrder()!.customerId }}</code>
                </div>
                <div>
                  <span class="meta-lbl">Fecha de Radicación:</span>
                  <p class="meta-val">{{ selectedOrder()!.createdAt | date: 'medium' }}</p>
                </div>
                <div>
                  <span class="meta-lbl">Última Actualización:</span>
                  <p class="meta-val">{{ selectedOrder()!.updatedAt | date: 'medium' }}</p>
                </div>
                <div>
                  <span class="meta-lbl">Monto Total:</span>
                  <p class="meta-val font-bold text-success">\${{ selectedOrder()!.totalAmount | number: '1.2-2' }}</p>
                </div>
              </div>

              <!-- ITEMS LIST -->
              <h4 style="margin: 1.25rem 0 0.5rem;">Ítems Solicitados</h4>
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cantidad</th>
                    <th>Precio Unitario</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of selectedOrder()!.items; track item.productId) {
                    <tr>
                      <td>
                        <strong>{{ getProductName(item.productId) }}</strong><br />
                        <code class="text-muted">{{ item.productId.substring(0, 8) }}...</code>
                      </td>
                      <td>{{ item.quantity }} unid.</td>
                      <td>\${{ item.unitPrice | number: '1.2-2' }}</td>
                      <td class="font-bold">\${{ (item.subtotal || item.quantity * item.unitPrice) | number: '1.2-2' }}</td>
                    </tr>
                  }
                </tbody>
              </table>

              <!-- LIFECYCLE CONTROLS BOX -->
              <div class="lifecycle-box">
                <span class="meta-lbl">Transiciones de Estado de Dominio (Invariantes INV-04, INV-05):</span>
                <div class="lifecycle-buttons">
                  @if (selectedOrder()!.status === 'PENDING') {
                    <button class="btn btn-secondary btn-sm" (click)="openModifyItemsModal()">
                      ✏️ Modificar Ítems (HU5)
                    </button>
                    <button class="btn btn-primary btn-sm" (click)="confirm(selectedOrder()!)">
                      ✅ Confirmar (Descuenta Físico + Notifica)
                    </button>
                    <button class="btn btn-danger btn-sm" (click)="cancel(selectedOrder()!)">
                      ❌ Cancelar Pedido
                    </button>
                  }
                  @if (selectedOrder()!.status === 'CONFIRMED') {
                    <button class="btn btn-warning btn-sm" (click)="dispatch(selectedOrder()!)">
                      🚚 Despachar Pedido a Ruta
                    </button>
                  }
                  @if (selectedOrder()!.status === 'DISPATCHED') {
                    <button class="btn btn-success btn-sm" (click)="deliver(selectedOrder()!)">
                      📦 Marcar Entregado a Satisfacción
                    </button>
                  }
                  @if (selectedOrder()!.status === 'DELIVERED') {
                    <p class="text-success" style="font-weight: 600;">✔️ Este pedido fue completado y entregado satisfactoriamente.</p>
                  }
                  @if (selectedOrder()!.status === 'CANCELLED') {
                    <p class="text-danger" style="font-weight: 600;">🚫 Este pedido se encuentra anulado.</p>
                  }
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button class="btn btn-secondary" (click)="closeDetailModal()">Cerrar</button>
            </div>
          </div>
        </div>
      }

      <!-- MODAL: MODIFICAR ÍTEMS DE ORDEN PENDIENTE (HU5) -->
      @if (showModifyModal() && selectedOrder()) {
        <div class="modal-backdrop" (click)="closeModifyModal()">
          <div class="modal-dialog" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>✏️ Modificar Pedido #{{ selectedOrder()!.id.substring(0, 8) }} (HU5)</h3>
              <button class="modal-close" (click)="closeModifyModal()">&times;</button>
            </div>
            <form (ngSubmit)="submitModifyOrder()">
              <div class="modal-body">
                <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem;">
                  Ajusta las cantidades de los productos mientras la orden se encuentra en estado <strong>PENDIENTE</strong>.
                  El Transactional Outbox sincronizará la reserva de stock.
                </p>

                @for (item of modifyItemsList; track item.productId; let i = $index) {
                  <div class="form-row align-center" style="margin-bottom: 0.75rem;">
                    <div style="flex: 2;">
                      <strong>{{ getProductName(item.productId) }}</strong><br />
                      <span class="text-muted">\${{ item.unitPrice | number: '1.2-2' }} c/u</span>
                    </div>
                    <div style="flex: 1;">
                      <label class="form-label" style="font-size: 0.75rem;">Cantidad</label>
                      <input
                        type="number"
                        min="1"
                        class="form-input"
                        [(ngModel)]="item.quantity"
                        [name]="'qty_' + i"
                        required
                      />
                    </div>
                    <div>
                      <button
                        type="button"
                        class="btn btn-danger btn-sm"
                        (click)="removeModifyItem(i)"
                        [disabled]="modifyItemsList.length <= 1"
                        title="La orden debe contener al menos 1 producto (INV-06)"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                }
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" (click)="closeModifyModal()">Cancelar</button>
                <button type="submit" class="btn btn-primary">Guardar Modificaciones</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- MODAL: CREAR NUEVA ORDEN DE COMPRA (HU4) -->
      @if (showCreateModal()) {
        <div class="modal-backdrop" (click)="closeCreateOrderModal()">
          <div class="modal-dialog modal-large" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>➕ Radicar Nueva Orden de Compra B2B (HU4)</h3>
              <button class="modal-close" (click)="closeCreateOrderModal()">&times;</button>
            </div>
            <form (ngSubmit)="submitCreateOrder()">
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">Cliente Mayorista Radicador *</label>
                  <select class="form-select" [(ngModel)]="newOrderCustomerId" name="customerId" required>
                    @for (c of session.presetCustomers(); track c.id) {
                      <option [value]="c.id">{{ c.name }} ({{ c.city }})</option>
                    }
                  </select>
                </div>

                <div class="order-items-builder">
                  <div class="builder-header">
                    <h4>Ítems del Pedido (Mínimo 1 - INV-06)</h4>
                    <button type="button" class="btn btn-secondary btn-sm" (click)="addNewOrderItemRow()">
                      + Añadir Producto
                    </button>
                  </div>

                  @for (row of newOrderRows; track $index; let idx = $index) {
                    <div class="form-row align-center" style="margin-bottom: 0.75rem;">
                      <div style="flex: 2;">
                        <label class="form-label">Producto *</label>
                        <select
                          class="form-select"
                          [(ngModel)]="row.productId"
                          [name]="'prod_' + idx"
                          (ngModelChange)="onNewOrderProductSelect(row)"
                          required
                        >
                          <option value="" disabled>Seleccione producto...</option>
                          @for (p of inventoryService.products(); track p.id) {
                            <option [value]="p.id">
                              {{ p.name }} - \${{ p.price | number: '1.2-2' }}
                            </option>
                          }
                        </select>
                      </div>
                      <div style="flex: 1;">
                        <label class="form-label">Cantidad *</label>
                        <input
                          type="number"
                          min="1"
                          class="form-input"
                          [(ngModel)]="row.quantity"
                          [name]="'qty_' + idx"
                          required
                        />
                      </div>
                      <div style="flex: 1;">
                        <label class="form-label">Precio Unitario</label>
                        <input
                          type="number"
                          step="0.01"
                          class="form-input"
                          [(ngModel)]="row.unitPrice"
                          [name]="'price_' + idx"
                          required
                        />
                      </div>
                      <div style="padding-top: 1.25rem;">
                        <button
                          type="button"
                          class="btn btn-danger btn-sm"
                          (click)="removeNewOrderRow(idx)"
                          [disabled]="newOrderRows.length <= 1"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  }

                  <div class="order-calc-total">
                    <span>Total Estimado de la Orden:</span>
                    <strong>\${{ calculateNewOrderTotal() | number: '1.2-2' }} USD</strong>
                  </div>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" (click)="closeCreateOrderModal()">Cancelar</button>
                <button type="submit" class="btn btn-primary" [disabled]="newOrderRows.length === 0">
                  Radicar Orden B2B
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .orders-page {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
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

    .filters-bar {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 0.75rem 1rem;
      gap: 1rem;
    }

    .status-tabs {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .tab-btn {
      background: none;
      border: 1px solid var(--border-color);
      color: var(--text-muted);
      padding: 0.35rem 0.85rem;
      border-radius: var(--radius-sm);
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
    }

    .tab-btn.active, .tab-btn:hover {
      background-color: var(--primary-light);
      border-color: var(--primary);
      color: var(--primary);
    }

    .filter-customer {
      font-size: 0.85rem;
      color: var(--text-main);
    }

    .order-id-badge {
      background-color: var(--bg-surface-elevated);
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-weight: bold;
      color: var(--primary);
    }

    .customer-cell {
      display: flex;
      flex-direction: column;
    }

    .customer-name {
      font-weight: 600;
    }

    .customer-uuid {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .font-bold {
      font-weight: 700;
    }

    .action-buttons-cell {
      display: flex;
      gap: 0.35rem;
      flex-wrap: wrap;
    }

    .modal-large {
      max-width: 750px;
    }

    .order-meta-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
      background-color: var(--bg-surface-elevated);
      border-radius: var(--radius-md);
      padding: 1rem;
      margin-bottom: 1rem;
    }

    .meta-lbl {
      font-size: 0.75rem;
      text-transform: uppercase;
      font-weight: 700;
      color: var(--text-muted);
    }

    .meta-val {
      font-size: 0.95rem;
      margin-top: 0.25rem;
    }

    .lifecycle-box {
      margin-top: 1.5rem;
      background-color: var(--bg-surface-elevated);
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-md);
      padding: 1rem;
    }

    .lifecycle-buttons {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-top: 0.75rem;
    }

    .order-items-builder {
      background-color: var(--bg-surface-elevated);
      border-radius: var(--radius-md);
      padding: 1rem;
      margin-top: 1rem;
    }

    .builder-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .order-calc-total {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      font-size: 1.1rem;
      padding-top: 1rem;
      margin-top: 1rem;
      border-top: 1px solid var(--border-color);
    }

    .align-center {
      align-items: center;
    }

    .empty-table-cell {
      text-align: center;
      padding: 3rem;
      color: var(--text-muted);
    }

    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid var(--border-color);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 0.75rem;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class OrderDashboardComponent implements OnInit {
  readonly orderService = inject(OrderService);
  readonly inventoryService = inject(InventoryService);
  readonly session = inject(CustomerSessionService);

  readonly statusTabs = ['TODOS', 'PENDING', 'CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED'];
  filterByCurrentCustomer: boolean = false;

  showDetailModal = signal<boolean>(false);
  showModifyModal = signal<boolean>(false);
  showCreateModal = signal<boolean>(false);

  selectedOrder = signal<PurchaseOrder | null>(null);

  // Modify items state
  modifyItemsList: { productId: string; quantity: number; unitPrice: number }[] = [];

  // Create order state
  newOrderCustomerId: string = '';
  newOrderRows: { productId: string; quantity: number; unitPrice: number }[] = [];

  ngOnInit(): void {
    this.refresh();
    this.inventoryService.loadAll().subscribe();
  }

  refresh(): void {
    const custId = this.filterByCurrentCustomer ? this.session.activeCustomer().id : undefined;
    this.orderService.loadOrders(custId).subscribe();
  }

  onFilterCustomerChange(): void {
    this.refresh();
  }

  getCustomerDisplayName(customerId: string): string {
    const found = this.session.presetCustomers().find(c => c.id === customerId);
    return found ? found.name : `Cliente (${customerId.substring(0, 8)}...)`;
  }

  getProductName(productId: string): string {
    const prod = this.inventoryService.products().find(p => p.id === productId);
    return prod ? prod.name : `Producto (${productId.substring(0, 8)}...)`;
  }

  getStatusBadgeClass(status: OrderStatus): string {
    switch (status) {
      case 'PENDING': return 'badge-pending';
      case 'CONFIRMED': return 'badge-confirmed';
      case 'DISPATCHED': return 'badge-dispatched';
      case 'DELIVERED': return 'badge-delivered';
      case 'CANCELLED': return 'badge-cancelled';
      default: return '';
    }
  }

  viewOrderDetail(order: PurchaseOrder): void {
    this.orderService.getOrderById(order.id).subscribe({
      next: (fullOrder) => {
        this.selectedOrder.set(fullOrder);
        this.showDetailModal.set(true);
      }
    });
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
  }

  confirm(order: PurchaseOrder): void {
    this.orderService.confirmOrder(order.id).subscribe({
      next: (confirmed) => {
        this.selectedOrder.set(confirmed);
      }
    });
  }

  dispatch(order: PurchaseOrder): void {
    this.orderService.dispatchOrder(order.id).subscribe({
      next: (dispatched) => {
        this.selectedOrder.set(dispatched);
      }
    });
  }

  deliver(order: PurchaseOrder): void {
    this.orderService.deliverOrder(order.id).subscribe({
      next: (delivered) => {
        this.selectedOrder.set(delivered);
      }
    });
  }

  cancel(order: PurchaseOrder): void {
    this.orderService.cancelOrder(order.id).subscribe({
      next: (cancelled) => {
        this.selectedOrder.set(cancelled);
      }
    });
  }

  openModifyItemsModal(): void {
    const order = this.selectedOrder();
    if (!order) return;
    this.modifyItemsList = order.items.map(i => ({
      productId: i.productId,
      quantity: i.quantity,
      unitPrice: i.unitPrice
    }));
    this.showModifyModal.set(true);
  }

  closeModifyModal(): void {
    this.showModifyModal.set(false);
  }

  removeModifyItem(index: number): void {
    if (this.modifyItemsList.length > 1) {
      this.modifyItemsList.splice(index, 1);
    }
  }

  submitModifyOrder(): void {
    const order = this.selectedOrder();
    if (!order) return;

    const request: ModifyOrderRequest = {
      items: this.modifyItemsList
    };

    this.orderService.modifyOrder(order.id, request).subscribe({
      next: (updated) => {
        this.selectedOrder.set(updated);
        this.closeModifyModal();
      }
    });
  }

  openCreateOrderModal(): void {
    this.newOrderCustomerId = this.session.activeCustomer().id;
    this.newOrderRows = [];
    this.addNewOrderItemRow();
    this.showCreateModal.set(true);
  }

  closeCreateOrderModal(): void {
    this.showCreateModal.set(false);
  }

  addNewOrderItemRow(): void {
    const firstProd = this.inventoryService.products()[0];
    this.newOrderRows.push({
      productId: firstProd ? firstProd.id : '',
      quantity: 1,
      unitPrice: firstProd ? firstProd.price : 0
    });
  }

  removeNewOrderRow(index: number): void {
    this.newOrderRows.splice(index, 1);
  }

  onNewOrderProductSelect(row: { productId: string; quantity: number; unitPrice: number }): void {
    const prod = this.inventoryService.products().find(p => p.id === row.productId);
    if (prod) {
      row.unitPrice = prod.price;
    }
  }

  calculateNewOrderTotal(): number {
    return this.newOrderRows.reduce((sum, r) => sum + (r.quantity * r.unitPrice), 0);
  }

  submitCreateOrder(): void {
    const request: CreateOrderRequest = {
      customerId: this.newOrderCustomerId,
      items: this.newOrderRows
    };

    this.orderService.createOrder(request).subscribe({
      next: () => {
        this.closeCreateOrderModal();
      }
    });
  }
}
