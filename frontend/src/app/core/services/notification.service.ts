import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { WebSocketService } from './websocket.service';

export interface Notification {
  id: string;
  recipientId: number;
  type: string;
  title: string;
  body: string;
  referenceId: string;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private unreadCountSubject = new BehaviorSubject<number>(0);
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private wsService: WebSocketService) {}

  getNotifications(): Observable<any> {
    return this.wsService.subscribe('/user/queue/notifications');
  }

  incrementUnread(): void {
    this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
  }

  clearUnread(): void {
    this.unreadCountSubject.next(0);
  }
}
