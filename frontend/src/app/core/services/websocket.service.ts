import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject, filter } from 'rxjs';
import { environment } from '@env/environment';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);
  private messageSubject = new Subject<{ destination: string; body: any }>();

  constructor(private authService: AuthService) {}

  connect(): void {
    const token = this.authService.getToken();
    if (!token || this.client?.connected) return;

    this.client = new Client({
      brokerURL: environment.wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log('WebSocket connected');
        this.connected$.next(true);
        this.subscribeToPersonalQueue();
      },

      onDisconnect: () => {
        console.log('WebSocket disconnected');
        this.connected$.next(false);
      },

      onStompError: (frame: any) => {
        console.error('STOMP error:', frame.headers['message']);
      }
    });

    this.client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.connected$.next(false);
  }

  isConnected(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  /**
   * Subscribe to a STOMP destination and return an Observable of messages.
   */
  subscribe(destination: string): Observable<any> {
    if (this.client?.connected) {
      this.client.subscribe(destination, (message: IMessage) => {
        this.messageSubject.next({
          destination,
          body: JSON.parse(message.body)
        });
      });
    }

    return this.messageSubject.asObservable().pipe(
      filter((msg: { destination: string; body: any }) => msg.destination === destination)
    );
  }

  /**
   * Send a message to a STOMP destination.
   */
  send(destination: string, body: any): void {
    if (this.client?.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body)
      });
    }
  }

  private subscribeToPersonalQueue(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    // Personal message queue
    this.client?.subscribe(
      `/user/${user.userId}/queue/messages`,
      (message: IMessage) => {
        this.messageSubject.next({
          destination: '/user/queue/messages',
          body: JSON.parse(message.body)
        });
      }
    );

    // Personal notification queue
    this.client?.subscribe(
      `/user/${user.userId}/queue/notifications`,
      (message: IMessage) => {
        this.messageSubject.next({
          destination: '/user/queue/notifications',
          body: JSON.parse(message.body)
        });
      }
    );

    // Online status topic
    this.client?.subscribe('/topic/status', (message: IMessage) => {
      this.messageSubject.next({
        destination: '/topic/status',
        body: JSON.parse(message.body)
      });
    });
  }
}
