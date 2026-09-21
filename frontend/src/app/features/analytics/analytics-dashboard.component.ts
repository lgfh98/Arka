import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from './analytics.service';
import { InventoryService } from '../inventory/inventory.service';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="analytics-page">
      <!-- HEADER -->
      <header class="page-header">
        <div>
          <h2>📊 Analítica Estratégica & Proyecciones CQRS (HU7, HU3)</h2>
          <p class="subtitle">
            Modelos de lectura actualizados de forma asíncrona mediante Domain Events emitidos por el Outbox Engine.
          </p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="refresh()" [disabled]="analyticsService.loading()">
            🔄 Refrescar Proyecciones
          </button>
          <button class="btn btn-primary" (click)="analyticsService.downloadSalesCsv()">
            📥 Exportar Ventas (CSV)
          </button>
          <button class="btn btn-warning" (click)="analyticsService.downloadReplenishmentCsv()">
            📥 Exportar Reposición (CSV)
          </button>
        </div>
      </header>

      <!-- KPI METRIC CARDS -->
      <div class="kpi-grid">
        <div class="kpi-card">
          <span class="kpi-label">Ingresos Totales por Ventas (CQRS)</span>
          <span class="kpi-value text-success">
            \${{ analyticsService.salesReport()?.totalRevenue ?? 0 | number: '1.2-2' }} USD
          </span>
          <span class="kpi-sub">Calculado a partir de órdenes confirmadas</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Unidades Totales Distribuidas</span>
          <span class="kpi-value text-primary">
            {{ analyticsService.salesReport()?.totalUnitsSold ?? 0 }} unid.
          </span>
          <span class="kpi-sub">Volumen físico despachado</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-label">Productos en Alerta de Abastecimiento</span>
          <span class="kpi-value text-danger">
            {{ analyticsService.replenishmentReport()?.totalProductsToReplenish ?? 0 }}
          </span>
          <span class="kpi-sub">Stock &le; Umbral de seguridad (INV-08)</span>
        </div>
      </div>

      <!-- ANALYTICS TABLES GRID -->
      <div class="tables-grid">
        <!-- TOP PRODUCTS RANKING -->
        <div class="table-box">
          <div class="box-header">
            <h3>🏆 Top Productos Más Vendidos</h3>
            <span class="badge badge-confirmed">Proyección Producto</span>
          </div>
          <div class="data-table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Producto</th>
                  <th>Unidades Vendidas</th>
                  <th>Ingresos Generados</th>
                </tr>
              </thead>
              <tbody>
                @for (p of analyticsService.salesReport()?.topProducts; track p.productId; let idx = $index) {
                  <tr>
                    <td class="font-bold">{{ idx + 1 }}</td>
                    <td>
                      <strong>{{ getProductName(p.productId) }}</strong><br />
                      <code class="text-muted">{{ p.productId.substring(0, 8) }}...</code>
                    </td>
                    <td class="font-bold text-primary">{{ p.totalUnitsSold }} unid.</td>
                    <td class="font-bold text-success">\${{ p.totalRevenue | number: '1.2-2' }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" class="empty-cell">Sin órdenes confirmadas aún.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>

        <!-- TOP CUSTOMERS RANKING -->
        <div class="table-box">
          <div class="box-header">
            <h3>🏢 Top Clientes Mayoristas</h3>
            <span class="badge badge-delivered">Proyección Clientes</span>
          </div>
          <div class="data-table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Cliente</th>
                  <th>Órdenes</th>
                  <th>Total Comprado</th>
                </tr>
              </thead>
              <tbody>
                @for (c of analyticsService.salesReport()?.topCustomers; track c.customerId; let idx = $index) {
                  <tr>
                    <td class="font-bold">{{ idx + 1 }}</td>
                    <td>
                      <code>{{ c.customerId }}</code>
                    </td>
                    <td class="font-bold">{{ c.totalOrdersCount }} pedidos</td>
                    <td class="font-bold text-success">\${{ c.totalSpent | number: '1.2-2' }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="4" class="empty-cell">Sin clientes con compras registradas.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- CRITICAL REPLENISHMENT SECTION -->
      <section class="replenishment-section">
        <div class="box-header">
          <div>
            <h3>⚠️ Reporte de Reposición Inmediata de Inventario (HU3)</h3>
            <p class="subtitle">Productos cuyo inventario actual ha alcanzado o cruzado el umbral mínimo de seguridad.</p>
          </div>
          <button class="btn btn-secondary btn-sm" (click)="analyticsService.downloadReplenishmentCsv()">
            Descargar CSV
          </button>
        </div>

        <div class="data-table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Stock Físico Actual</th>
                <th>Umbral Mínimo</th>
                <th>Deficit Estimado</th>
                <th>Última Detección</th>
              </tr>
            </thead>
            <tbody>
              @for (item of analyticsService.replenishmentReport()?.items; track item.productId) {
                <tr>
                  <td>
                    <strong>{{ getProductName(item.productId) }}</strong><br />
                    <code class="text-muted">{{ item.productId }}</code>
                  </td>
                  <td>
                    <span class="badge badge-cancelled">{{ item.currentStock }} unidades</span>
                  </td>
                  <td>{{ item.minimumThreshold }} unidades</td>
                  <td class="font-bold text-danger">
                    {{ item.minimumThreshold - item.currentStock > 0 ? (item.minimumThreshold - item.currentStock) + ' unid. faltantes' : 'En el límite' }}
                  </td>
                  <td>{{ item.updatedAt | date: 'medium' }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="empty-cell">Todos los productos cuentan con niveles óptimos de stock.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .analytics-page {
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
      flex-wrap: wrap;
    }

    /* KPI Grid */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.25rem;
    }

    .kpi-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    .kpi-label {
      font-size: 0.75rem;
      text-transform: uppercase;
      font-weight: 700;
      color: var(--text-muted);
    }

    .kpi-value {
      font-size: 1.75rem;
      font-weight: 800;
    }

    .kpi-sub {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .text-success { color: var(--success); }
    .text-primary { color: var(--primary); }
    .text-danger { color: var(--danger); }

    .tables-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }

    @media (max-width: 900px) {
      .tables-grid {
        grid-template-columns: 1fr;
      }
    }

    .table-box {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .box-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .box-header h3 {
      font-size: 1.15rem;
      font-weight: 700;
    }

    .font-bold {
      font-weight: 700;
    }

    .empty-cell {
      text-align: center;
      padding: 2rem;
      color: var(--text-muted);
    }

    .replenishment-section {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      box-shadow: var(--shadow-sm);
    }
  `]
})
export class AnalyticsDashboardComponent implements OnInit {
  readonly analyticsService = inject(AnalyticsService);
  readonly inventoryService = inject(InventoryService);

  ngOnInit(): void {
    this.refresh();
    this.inventoryService.loadAll().subscribe();
  }

  refresh(): void {
    this.analyticsService.loadAllReports().subscribe();
  }

  getProductName(productId: string): string {
    const prod = this.inventoryService.products().find(p => p.id === productId);
    return prod ? prod.name : `Producto (${productId.substring(0, 8)}...)`;
  }
}
