export interface ProductSalesProjection {
  productId: string;
  totalUnitsSold: number;
  totalRevenue: number;
  updatedAt: string;
}

export interface CustomerSalesProjection {
  customerId: string;
  totalOrdersCount: number;
  totalSpent: number;
  updatedAt: string;
}

export interface SalesReportResponse {
  totalRevenue: number;
  totalUnitsSold: number;
  topProducts: ProductSalesProjection[];
  topCustomers: CustomerSalesProjection[];
}

export interface ReplenishmentItem {
  productId: string;
  currentStock: number;
  minimumThreshold: number;
  updatedAt: string;
}

export interface ReplenishmentReportResponse {
  totalProductsToReplenish: number;
  items: ReplenishmentItem[];
}
