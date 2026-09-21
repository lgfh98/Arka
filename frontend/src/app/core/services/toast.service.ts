import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  detail: string;
  errors?: Record<string, string>;
  durationMs?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  readonly toasts = signal<ToastMessage[]>([]);

  show(toast: Omit<ToastMessage, 'id'>): void {
    const id = Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = {
      ...toast,
      id,
      durationMs: toast.durationMs ?? 5000
    };

    this.toasts.update(list => [...list, newToast]);

    const duration = newToast.durationMs ?? 5000;
    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  success(title: string, detail: string = ''): void {
    this.show({ type: 'success', title, detail });
  }

  error(title: string, detail: string = '', errors?: Record<string, string>): void {
    this.show({ type: 'error', title, detail, errors, durationMs: 7000 });
  }

  info(title: string, detail: string = ''): void {
    this.show({ type: 'info', title, detail });
  }

  warning(title: string, detail: string = ''): void {
    this.show({ type: 'warning', title, detail });
  }

  remove(id: string): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
