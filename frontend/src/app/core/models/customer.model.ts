export interface CustomerSeed {
  id: string;
  name: string;
  city: string;
  email: string;
}

export const PRESET_CUSTOMERS: CustomerSeed[] = [
  {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    name: 'Almacén Mayorista Medellín',
    city: 'Medellín, Colombia',
    email: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa@arka-client.com'
  },
  {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    name: 'Distribuidora Bogotá',
    city: 'Bogotá, Colombia',
    email: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb@arka-client.com'
  }
];
