export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'DISPATCHED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  productId: string;
  quantity: number;
  unitPrice: number;
  subtotal?: number;
}

export interface PurchaseOrder {
  id: string;
  customerId: string;
  status: OrderStatus;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
}

export interface OrderItemCommand {
  productId: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateOrderRequest {
  customerId: string;
  items: OrderItemCommand[];
}

export interface ModifyOrderRequest {
  items: OrderItemCommand[];
}
