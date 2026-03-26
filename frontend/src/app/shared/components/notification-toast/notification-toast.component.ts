import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService, Notification } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toasts; track toast.id) {
        <div class="toast" [class.fadeOut]="toast.fading" (click)="dismiss(toast)">
          <div class="toast-icon">{{ getIcon(toast.type) }}</div>
          <div class="toast-body">
            <div class="toast-title">{{ toast.title }}</div>
            <div class="toast-message">{{ toast.body }}</div>
          </div>
          <button class="toast-close" (click)="dismiss(toast)">&times;</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 16px;
      right: 16px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 360px;
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 12px 16px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      animation: slideIn 0.3s ease-out;
      cursor: pointer;
      border-left: 4px solid var(--primary, #128c7e);
    }
    .toast.fadeOut {
      animation: slideOut 0.3s ease-in forwards;
    }
    .toast-icon { font-size: 20px; }
    .toast-body { flex: 1; min-width: 0; }
    .toast-title { font-weight: 600; font-size: 14px; margin-bottom: 2px; }
    .toast-message {
      font-size: 13px;
      color: #666;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .toast-close {
      background: none;
      border: none;
      font-size: 18px;
      color: #999;
      cursor: pointer;
      padding: 0 4px;
    }
    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
      from { transform: translateX(0); opacity: 1; }
      to { transform: translateX(100%); opacity: 0; }
    }
  `]
})
export class NotificationToastComponent implements OnInit, OnDestroy {
  toasts: (Notification & { fading?: boolean })[] = [];
  private sub: Subscription | null = null;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.sub = this.notificationService.getNotifications().subscribe(
      (event: { body: Notification }) => {
        const notification = event.body;
        this.toasts.push(notification);
        // Auto-dismiss after 5s
        setTimeout(() => this.dismiss(notification), 5000);
      }
    );
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  dismiss(toast: Notification & { fading?: boolean }): void {
    toast.fading = true;
    setTimeout(() => {
      this.toasts = this.toasts.filter(t => t.id !== toast.id);
    }, 300);
  }

  getIcon(type: string): string {
    switch (type) {
      case 'NEW_MESSAGE': return '💬';
      case 'GROUP_INVITE': return '👥';
      case 'USER_ONLINE': return '🟢';
      default: return 'ℹ️';
    }
  }
}
