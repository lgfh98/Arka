export type CartStatus = 'ACTIVE' | 'ABANDONED' | 'CONVERTED';

export interface CartItem {
  productId: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Cart {
  id: string;
  customerId: string;
  status: CartStatus;
  total: number;
  lastActivityAt: string;
  items: CartItem[];
}

export interface AddCartItemRequest {
  productId: string;
  quantity: number;
  unitPrice: number;
}
