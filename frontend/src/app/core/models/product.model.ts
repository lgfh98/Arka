export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  attributes: Record<string, string>;
}

export interface InventoryItem {
  id: string;
  productId: string;
  physicalStock: number;
  reservedStock: number;
  availableStock: number;
  minimumThreshold: number;
  lowStock: boolean;
}

export interface ProductWithInventory extends Product {
  inventory?: InventoryItem;
}

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  category: string;
  attributes: Record<string, string>;
  initialStock: number;
  minimumThreshold: number;
}

export interface AdjustStockRequest {
  newStock: number;
  reason: string;
}
