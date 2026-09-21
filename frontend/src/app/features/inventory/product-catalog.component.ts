import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService } from './inventory.service';
import { CartService } from '../cart/cart.service';
import { CustomerSessionService } from '../../core/services/customer-session.service';
import { ProductWithInventory, CreateProductRequest, AdjustStockRequest } from '../../core/models/product.model';

@Component({
  selector: 'app-product-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="catalog-page">
      <!-- HEADER AND CONTROLS -->
      <header class="catalog-header">
        <div>
          <h2>🏭 Catálogo & Inventario Mayorista</h2>
          <p class="subtitle">
            Control de stock en tiempo real, reserva preventiva anti-sobreventa y gestión de atributos de PC.
          </p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="refresh()" [disabled]="inventoryService.loading()">
            🔄 Refrescar
          </button>
          <button class="btn btn-primary" (click)="openCreateModal()">
            ➕ Registrar Nuevo Producto (HU1)
          </button>
        </div>
      </header>

      <!-- KPI METRICS SUMMARY -->
      <div class="kpi-grid">
        <div class="kpi-card">
          <span class="kpi-label">Total Productos en Catálogo</span>
          <span class="kpi-value">{{ inventoryService.products().length }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Ítems con Stock Crítico (Alerta)</span>
          <span class="kpi-value text-danger">{{ inventoryService.lowStockItems().length }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Cliente B2B Activo</span>
          <span class="kpi-value highlight">{{ session.activeCustomer().name }}</span>
        </div>
      </div>

      <!-- FILTER & SEARCH BAR -->
      <div class="filters-bar">
        <div class="search-box">
          <input
            type="text"
            class="form-input"
            placeholder="🔍 Buscar por nombre o descripción..."
            [ngModel]="inventoryService.searchTerm()"
            (ngModelChange)="inventoryService.searchTerm.set($event)"
          />
        </div>

        <div class="categories-tabs">
          @for (cat of inventoryService.categories(); track cat) {
            <button
              class="tab-btn"
              [class.active]="inventoryService.selectedCategory() === cat"
              (click)="inventoryService.selectedCategory.set(cat)"
            >
              {{ cat }}
            </button>
          }
        </div>
      </div>

      <!-- PRODUCTS GRID -->
      @if (inventoryService.loading() && inventoryService.products().length === 0) {
        <div class="loading-state">
          <div class="spinner"></div>
          <p>Cargando catálogo e inventario desde el backend...</p>
        </div>
      } @else {
        <div class="products-grid">
          @for (product of inventoryService.productsWithInventory(); track product.id) {
            <div class="product-card" [class.border-critical]="product.inventory?.lowStock">
              <div class="card-header">
                <span class="category-badge">{{ product.category }}</span>
                @if (product.inventory?.lowStock) {
                  <span class="badge badge-low-stock">⚠️ Stock Bajo (HU3)</span>
                }
              </div>

              <h3 class="product-title">{{ product.name }}</h3>
              <p class="product-desc">{{ product.description }}</p>

              <!-- ATTRIBUTES TAGS -->
              <div class="attributes-chips">
                @for (entry of getAttributesList(product.attributes); track entry.key) {
                  <span class="attr-chip">
                    <strong>{{ entry.key }}:</strong> {{ entry.val }}
                  </span>
                }
              </div>

              <!-- STOCK METRICS -->
              <div class="stock-info-box">
                <div class="stock-metric">
                  <span class="stock-num">{{ product.inventory?.physicalStock ?? 0 }}</span>
                  <span class="stock-lbl">Físico</span>
                </div>
                <div class="stock-divider">-</div>
                <div class="stock-metric">
                  <span class="stock-num text-warning">{{ product.inventory?.reservedStock ?? 0 }}</span>
                  <span class="stock-lbl">Reservado</span>
                </div>
                <div class="stock-divider">=</div>
                <div class="stock-metric">
                  <span class="stock-num text-success">{{ product.inventory?.availableStock ?? 0 }}</span>
                  <span class="stock-lbl">Disponible</span>
                </div>
                <div class="stock-threshold">
                  Umbral mín: <strong>{{ product.inventory?.minimumThreshold ?? 0 }}</strong>
                </div>
              </div>

              <!-- PRICE & ACTIONS -->
              <div class="card-footer">
                <div class="price-display">
                  <span class="currency">USD</span>
                  <span class="amount">\${{ product.price | number: '1.2-2' }}</span>
                </div>

                <div class="actions-group">
                  <div class="add-cart-inline">
                    <input
                      type="number"
                      min="1"
                      [max]="product.inventory?.availableStock || 999"
                      [(ngModel)]="quantities[product.id]"
                      class="qty-input"
                    />
                    <button
                      class="btn btn-primary btn-sm"
                      (click)="addToCart(product)"
                      [disabled]="!product.inventory || product.inventory.availableStock <= 0"
                    >
                      🛒 Agregar
                    </button>
                  </div>

                  <button class="btn btn-secondary btn-sm" (click)="openAdjustModal(product)">
                    ⚙️ Stock
                  </button>
                </div>
              </div>
            </div>
          } @empty {
            <div class="empty-state">
              <p>No se encontraron productos coincidentes con los filtros.</p>
            </div>
          }
        </div>
      }

      <!-- MODAL: REGISTRAR PRODUCTO (HU1) -->
      @if (showCreateModal()) {
        <div class="modal-backdrop" (click)="closeCreateModal()">
          <div class="modal-dialog" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>📦 Registrar Nuevo Producto (HU1)</h3>
              <button class="modal-close" (click)="closeCreateModal()">&times;</button>
            </div>
            <form (ngSubmit)="submitCreateProduct()">
              <div class="modal-body">
                <div class="form-group">
                  <label class="form-label">Nombre del Producto *</label>
                  <input type="text" class="form-input" [(ngModel)]="newProduct.name" name="name" required placeholder="Ej. Tarjeta Gráfica RTX 4070" />
                </div>
                <div class="form-group">
                  <label class="form-label">Categoría *</label>
                  <input type="text" class="form-input" [(ngModel)]="newProduct.category" name="category" required placeholder="Ej. Componentes, Periféricos, Audio" />
                </div>
                <div class="form-row">
                  <div class="form-group col">
                    <label class="form-label">Precio Mayorista (USD) *</label>
                    <input type="number" step="0.01" class="form-input" [(ngModel)]="newProduct.price" name="price" required placeholder="599.99" />
                  </div>
                  <div class="form-group col">
                    <label class="form-label">Stock Inicial Físico *</label>
                    <input type="number" class="form-input" [(ngModel)]="newProduct.initialStock" name="initialStock" required placeholder="25" />
                  </div>
                  <div class="form-group col">
                    <label class="form-label">Umbral de Alerta *</label>
                    <input type="number" class="form-input" [(ngModel)]="newProduct.minimumThreshold" name="minimumThreshold" required placeholder="5" />
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label">Descripción Técnica</label>
                  <textarea class="form-textarea" rows="2" [(ngModel)]="newProduct.description" name="description" placeholder="Detalles de especificaciones..."></textarea>
                </div>
                <div class="form-group">
                  <label class="form-label">Atributos PC (Marca, Conexión, Interfaz)</label>
                  <div class="form-row">
                    <input type="text" class="form-input col" [(ngModel)]="attrKey" name="attrKey" placeholder="Propiedad (ej: marca)" />
                    <input type="text" class="form-input col" [(ngModel)]="attrVal" name="attrVal" placeholder="Valor (ej: ASUS)" />
                    <button type="button" class="btn btn-secondary btn-sm" (click)="addAttribute()">+ Añadir</button>
                  </div>
                  <div class="attributes-preview">
                    @for (item of getAttributesList(newProduct.attributes); track item.key) {
                      <span class="attr-chip">
                        {{ item.key }}: {{ item.val }}
                        <a (click)="removeAttribute(item.key)" style="cursor:pointer;margin-left:4px;">✕</a>
                      </span>
                    }
                  </div>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" (click)="closeCreateModal()">Cancelar</button>
                <button type="submit" class="btn btn-primary">Registrar en Inventario</button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- MODAL: AJUSTAR STOCK (HU2) -->
      @if (showAdjustModal() && selectedProductForAdjust()) {
        <div class="modal-backdrop" (click)="closeAdjustModal()">
          <div class="modal-dialog" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>⚙️ Ajustar Stock Físico (HU2 - Auditoría)</h3>
              <button class="modal-close" (click)="closeAdjustModal()">&times;</button>
            </div>
            <form (ngSubmit)="submitAdjustStock()">
              <div class="modal-body">
                <p style="margin-bottom: 1rem; font-size: 0.9rem;">
                  Producto: <strong>{{ selectedProductForAdjust()?.name }}</strong><br />
                  Stock Físico Actual: <strong>{{ selectedProductForAdjust()?.inventory?.physicalStock ?? 0 }}</strong> |
                  Reservado: <strong>{{ selectedProductForAdjust()?.inventory?.reservedStock ?? 0 }}</strong>
                </p>
                <div class="form-group">
                  <label class="form-label">Nuevo Stock Físico *</label>
                  <input type="number" min="0" class="form-input" [(ngModel)]="adjustRequest.newStock" name="newStock" required />
                </div>
                <div class="form-group">
                  <label class="form-label">Motivo Obligatorio de Auditoría * (INV-02)</label>
                  <textarea
                    class="form-textarea"
                    rows="3"
                    [(ngModel)]="adjustRequest.reason"
                    name="reason"
                    required
                    placeholder="Ej. Ingreso de conteo físico trimestral o recepción de embarque #LOT-2026-B"
                  ></textarea>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" (click)="closeAdjustModal()">Cancelar</button>
                <button type="submit" class="btn btn-warning">Confirmar Ajuste Auditado</button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .catalog-page {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .catalog-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .catalog-header h2 {
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

    /* KPI Grid */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
    }

    .kpi-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1rem 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      box-shadow: var(--shadow-sm);
    }

    .kpi-label {
      font-size: 0.75rem;
      text-transform: uppercase;
      font-weight: 700;
      color: var(--text-muted);
    }

    .kpi-value {
      font-size: 1.5rem;
      font-weight: 800;
      color: var(--text-main);
    }

    .highlight {
      font-size: 1.15rem;
      color: var(--primary);
    }

    .text-danger {
      color: var(--danger);
    }

    /* Filters Bar */
    .filters-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      align-items: center;
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 0.75rem 1rem;
    }

    .search-box {
      flex: 1;
      min-width: 250px;
    }

    .categories-tabs {
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

    /* Products Grid */
    .products-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 1.25rem;
    }

    .product-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      box-shadow: var(--shadow-sm);
      position: relative;
    }

    .border-critical {
      border-color: var(--danger);
      box-shadow: 0 0 0 1px var(--danger);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .category-badge {
      font-size: 0.7rem;
      font-weight: 700;
      color: var(--primary);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .product-title {
      font-size: 1.1rem;
      font-weight: 700;
      line-height: 1.3;
    }

    .product-desc {
      font-size: 0.85rem;
      color: var(--text-muted);
      min-height: 2.5rem;
    }

    .attributes-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }

    .attr-chip {
      background-color: var(--bg-surface-elevated);
      border: 1px solid var(--border-color);
      padding: 0.15rem 0.45rem;
      font-size: 0.7rem;
      border-radius: 4px;
      color: var(--text-muted);
    }

    .stock-info-box {
      background-color: var(--bg-surface-elevated);
      border-radius: var(--radius-sm);
      padding: 0.75rem;
      display: flex;
      align-items: center;
      justify-content: space-around;
      position: relative;
    }

    .stock-metric {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .stock-num {
      font-size: 1.15rem;
      font-weight: 800;
    }

    .stock-lbl {
      font-size: 0.65rem;
      text-transform: uppercase;
      color: var(--text-muted);
    }

    .stock-divider {
      font-weight: bold;
      color: var(--text-muted);
    }

    .stock-threshold {
      position: absolute;
      bottom: -0.4rem;
      right: 0.5rem;
      font-size: 0.65rem;
      color: var(--text-muted);
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: auto;
      padding-top: 0.75rem;
      border-top: 1px solid var(--border-color);
    }

    .price-display {
      display: flex;
      align-items: baseline;
      gap: 0.25rem;
    }

    .currency {
      font-size: 0.75rem;
      font-weight: 700;
      color: var(--text-muted);
    }

    .amount {
      font-size: 1.35rem;
      font-weight: 800;
      color: var(--text-main);
    }

    .actions-group {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .add-cart-inline {
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }

    .qty-input {
      width: 48px;
      padding: 0.35rem;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-sm);
      background-color: var(--bg-surface);
      color: var(--text-main);
      font-size: 0.85rem;
      text-align: center;
    }

    .form-row {
      display: flex;
      gap: 0.75rem;
    }

    .col {
      flex: 1;
    }

    .attributes-preview {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
      margin-top: 0.5rem;
    }

    .loading-state, .empty-state {
      padding: 3rem;
      text-align: center;
      color: var(--text-muted);
    }

    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid var(--border-color);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 1rem;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class ProductCatalogComponent implements OnInit {
  readonly inventoryService = inject(InventoryService);
  readonly cartService = inject(CartService);
  readonly session = inject(CustomerSessionService);

  quantities: Record<string, number> = {};

  showCreateModal = signal<boolean>(false);
  showAdjustModal = signal<boolean>(false);
  selectedProductForAdjust = signal<ProductWithInventory | null>(null);

  newProduct: CreateProductRequest = {
    name: '',
    description: '',
    price: 0,
    category: '',
    attributes: {},
    initialStock: 10,
    minimumThreshold: 5
  };

  adjustRequest: AdjustStockRequest = {
    newStock: 0,
    reason: ''
  };

  attrKey: string = '';
  attrVal: string = '';

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.inventoryService.loadAll().subscribe({
      next: ([products]) => {
        for (const p of products) {
          if (!this.quantities[p.id]) {
            this.quantities[p.id] = 1;
          }
        }
      }
    });
  }

  getAttributesList(attributes?: Record<string, string>): { key: string; val: string }[] {
    if (!attributes) return [];
    return Object.entries(attributes).map(([key, val]) => ({ key, val }));
  }

  addToCart(product: ProductWithInventory): void {
    const qty = this.quantities[product.id] || 1;
    this.cartService.addItem(product.id, qty, product.price).subscribe();
  }

  openCreateModal(): void {
    this.newProduct = {
      name: '',
      description: '',
      price: 0,
      category: '',
      attributes: {},
      initialStock: 20,
      minimumThreshold: 5
    };
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  addAttribute(): void {
    if (this.attrKey.trim() && this.attrVal.trim()) {
      this.newProduct.attributes[this.attrKey.trim()] = this.attrVal.trim();
      this.attrKey = '';
      this.attrVal = '';
    }
  }

  removeAttribute(key: string): void {
    delete this.newProduct.attributes[key];
  }

  submitCreateProduct(): void {
    this.inventoryService.createProduct(this.newProduct).subscribe({
      next: () => {
        this.closeCreateModal();
      }
    });
  }

  openAdjustModal(product: ProductWithInventory): void {
    this.selectedProductForAdjust.set(product);
    this.adjustRequest = {
      newStock: product.inventory?.physicalStock ?? 0,
      reason: ''
    };
    this.showAdjustModal.set(true);
  }

  closeAdjustModal(): void {
    this.showAdjustModal.set(false);
    this.selectedProductForAdjust.set(null);
  }

  submitAdjustStock(): void {
    const prod = this.selectedProductForAdjust();
    if (!prod) return;

    this.inventoryService.adjustStock(prod.id, this.adjustRequest).subscribe({
      next: () => {
        this.closeAdjustModal();
      }
    });
  }
}
