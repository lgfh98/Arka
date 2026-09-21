import { Injectable, signal } from '@angular/core';
import { CustomerSeed, PRESET_CUSTOMERS } from '../models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerSessionService {
  readonly activeCustomer = signal<CustomerSeed>(PRESET_CUSTOMERS[0]);
  readonly presetCustomers = signal<CustomerSeed[]>(PRESET_CUSTOMERS);

  selectCustomer(customer: CustomerSeed): void {
    this.activeCustomer.set(customer);
  }

  setCustomCustomerId(id: string): void {
    const trimmed = id.trim();
    if (!trimmed) return;
    const existing = this.presetCustomers().find(c => c.id.toLowerCase() === trimmed.toLowerCase());
    if (existing) {
      this.activeCustomer.set(existing);
    } else {
      const custom: CustomerSeed = {
        id: trimmed,
        name: `Cliente Especial (${trimmed.substring(0, 8)}...)`,
        city: 'Latinoamérica B2B',
        email: `${trimmed}@arka-client.com`
      };
      this.activeCustomer.set(custom);
    }
  }
}
