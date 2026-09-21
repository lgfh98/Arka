import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ThemeService } from './core/services/theme.service';
import { CustomerSessionService } from './core/services/customer-session.service';
import { CartService } from './features/cart/cart.service';
import { ToastService } from './core/services/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  readonly themeService = inject(ThemeService);
  readonly sessionService = inject(CustomerSessionService);
  readonly cartService = inject(CartService);
  readonly toastService = inject(ToastService);

  customCustomerIdInput = signal<string>('');
  showCustomCustomerModal = signal<boolean>(false);

  onCustomerChange(customerId: string): void {
    if (customerId === 'CUSTOM') {
      this.showCustomCustomerModal.set(true);
      return;
    }
    const found = this.sessionService.presetCustomers().find(c => c.id === customerId);
    if (found) {
      this.sessionService.selectCustomer(found);
    }
  }

  applyCustomCustomer(): void {
    const val = this.customCustomerIdInput().trim();
    if (val) {
      this.sessionService.setCustomCustomerId(val);
      this.customCustomerIdInput.set('');
      this.showCustomCustomerModal.set(false);
    }
  }

  closeCustomCustomerModal(): void {
    this.showCustomCustomerModal.set(false);
  }

  getValidationErrors(errors?: Record<string, string>): { field: string; msg: string }[] {
    if (!errors) return [];
    return Object.entries(errors).map(([field, msg]) => ({ field, msg }));
  }
}
