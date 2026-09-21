import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { NotificationRecord } from '../../core/models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly http = inject(HttpClient);

  readonly notifications = signal<NotificationRecord[]>([]);
  readonly loading = signal<boolean>(false);

  loadNotifications(recipient?: string): Observable<NotificationRecord[]> {
    this.loading.set(true);
    const url = recipient && recipient.trim()
      ? `/api/notifications?recipient=${encodeURIComponent(recipient.trim())}`
      : '/api/notifications';

    return this.http.get<NotificationRecord[]>(url).pipe(
      tap({
        next: (list) => {
          this.notifications.set(list);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      })
    );
  }
}
