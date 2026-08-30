import { Injectable, signal } from '@angular/core';

export interface Notification { id: number; message: string; tone: 'success' | 'error' | 'info'; }

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 0;
  readonly notifications = signal<Notification[]>([]);

  show(message: string, tone: Notification['tone'] = 'info'): void {
    const item = { id: ++this.nextId, message, tone };
    this.notifications.update(items => [...items, item]);
    window.setTimeout(() => this.dismiss(item.id), 4500);
  }

  dismiss(id: number): void { this.notifications.update(items => items.filter(item => item.id !== id)); }
}
