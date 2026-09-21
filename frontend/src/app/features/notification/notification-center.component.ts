import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService } from './notification.service';
import { CustomerSessionService } from '../../core/services/customer-session.service';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="notifications-page">
      <!-- HEADER -->
      <header class="page-header">
        <div>
          <h2>🔔 Centro de Notificaciones Multicanal (Idempotent Consumer)</h2>
          <p class="subtitle">
            Bandeja de auditoría de mensajes y correos transaccionales generados reactivamente por eventos de dominio.
          </p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="refresh()" [disabled]="notificationService.loading()">
            🔄 Refrescar
          </button>
        </div>
      </header>

      <!-- FILTER CONTROLS -->
      <div class="filters-bar">
        <label class="filter-option">
          <input
            type="radio"
            name="filterMode"
            [value]="false"
            [(ngModel)]="filterByActiveOnly"
            (ngModelChange)="refresh()"
          />
          Ver Todas las Notificaciones del Sistema
        </label>
        <label class="filter-option">
          <input
            type="radio"
            name="filterMode"
            [value]="true"
            [(ngModel)]="filterByActiveOnly"
            (ngModelChange)="refresh()"
          />
          Solo Cliente Activo ({{ session.activeCustomer().email }})
        </label>
      </div>

      <!-- NOTIFICATIONS FEED -->
      <div class="notifications-feed">
        @for (notif of notificationService.notifications(); track notif.id) {
          <div class="notif-card">
            <div class="notif-header">
              <div class="notif-title-group">
                <span class="channel-badge">{{ notif.channel }}</span>
                <span class="event-tag">{{ notif.eventType }}</span>
                <h4 class="notif-subject">{{ notif.subject }}</h4>
              </div>
              <span class="notif-time">{{ notif.sentAt | date: 'medium' }}</span>
            </div>

            <div class="notif-meta">
              <span><strong>Destinatario:</strong> {{ notif.recipient }}</span>
              <span><strong>ID Registro:</strong> <code>{{ notif.id }}</code></span>
            </div>

            <div class="notif-message-box">
              <pre>{{ notif.message }}</pre>
            </div>
          </div>
        } @empty {
          <div class="empty-notif-card">
            <span class="empty-icon">📭</span>
            <h4>No hay notificaciones registradas</h4>
            <p>
              Las notificaciones se generan automáticamente cuando confirmas, despachas o entregas una orden, o cuando se detecta un carrito abandonado.
            </p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .notifications-page {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .page-header h2 {
      font-size: 1.6rem;
      font-weight: 800;
      letter-spacing: -0.02em;
    }

    .subtitle {
      color: var(--text-muted);
      font-size: 0.9rem;
      margin-top: 0.25rem;
    }

    .filters-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 1.5rem;
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 0.75rem 1.25rem;
    }

    .filter-option {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
      cursor: pointer;
      color: var(--text-main);
    }

    .notifications-feed {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .notif-card {
      background-color: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .notif-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .notif-title-group {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .channel-badge {
      background-color: var(--primary-light);
      color: var(--primary);
      font-size: 0.7rem;
      font-weight: 800;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      text-transform: uppercase;
    }

    .event-tag {
      background-color: var(--accent-purple-light);
      color: var(--accent-purple);
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
    }

    .notif-subject {
      font-size: 1.05rem;
      font-weight: 700;
      color: var(--text-main);
      margin-left: 0.25rem;
    }

    .notif-time {
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    .notif-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 1.5rem;
      font-size: 0.8rem;
      color: var(--text-muted);
    }

    .notif-meta code {
      color: var(--primary);
    }

    .notif-message-box {
      background-color: var(--bg-surface-elevated);
      border-radius: var(--radius-sm);
      padding: 1rem;
      border: 1px solid var(--border-color);
    }

    .notif-message-box pre {
      font-family: var(--font-family);
      font-size: 0.85rem;
      white-space: pre-wrap;
      word-break: break-word;
      color: var(--text-main);
    }

    .empty-notif-card {
      background-color: var(--bg-surface);
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-lg);
      padding: 3rem;
      text-align: center;
      color: var(--text-muted);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }

    .empty-icon {
      font-size: 3rem;
    }
  `]
})
export class NotificationCenterComponent implements OnInit {
  readonly notificationService = inject(NotificationService);
  readonly session = inject(CustomerSessionService);

  filterByActiveOnly: boolean = false;

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    const recipient = this.filterByActiveOnly ? this.session.activeCustomer().email : undefined;
    this.notificationService.loadNotifications(recipient).subscribe();
  }
}
