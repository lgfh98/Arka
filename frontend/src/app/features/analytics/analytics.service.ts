import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, tap } from 'rxjs';
import {
  SalesReportResponse,
  ReplenishmentReportResponse
} from '../../core/models/analytics.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  readonly salesReport = signal<SalesReportResponse | null>(null);
  readonly replenishmentReport = signal<ReplenishmentReportResponse | null>(null);
  readonly loading = signal<boolean>(false);

  loadAllReports(): Observable<[SalesReportResponse, ReplenishmentReportResponse]> {
    this.loading.set(true);
    return forkJoin([
      this.http.get<SalesReportResponse>('/api/analytics/reports/sales?format=json'),
      this.http.get<ReplenishmentReportResponse>('/api/analytics/reports/replenishment?format=json')
    ]).pipe(
      tap({
        next: ([sales, repl]) => {
          this.salesReport.set(sales);
          this.replenishmentReport.set(repl);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }

  downloadSalesCsv(): void {
    this.http.get('/api/analytics/reports/sales?format=csv', { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'reporte_ventas.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  downloadReplenishmentCsv(): void {
    this.http.get('/api/analytics/reports/replenishment?format=csv', { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'reporte_abastecimiento.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }
}
