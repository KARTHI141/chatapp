import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ChatListComponent } from '../chat-list/chat-list.component';
import { ChatWindowComponent } from '../chat-window/chat-window.component';
import { NotificationToastComponent } from '../../../shared/components/notification-toast/notification-toast.component';
import { CreateGroupDialogComponent } from '../../../shared/components/create-group-dialog/create-group-dialog.component';
import { ChatService } from '../../../core/services/chat.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChatRoom } from '../../../core/models/chat-room.model';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [CommonModule, ChatListComponent, ChatWindowComponent, NotificationToastComponent, CreateGroupDialogComponent],
  templateUrl: './chat-layout.component.html',
  styles: [`
    .chat-layout {
      display: flex;
      height: 100vh;
      max-width: 1400px;
      margin: 0 auto;
      background: var(--bg-white);
      box-shadow: 0 0 20px rgba(0,0,0,0.1);
    }
    .sidebar {
      width: 380px;
      border-right: 1px solid var(--border);
      display: flex;
      flex-direction: column;
    }
    .sidebar-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: var(--primary);
      color: white;
    }
    .sidebar-header h2 {
      font-size: 18px;
      font-weight: 600;
    }
    .header-actions {
      display: flex;
      gap: 12px;
    }
    .header-actions button {
      background: none;
      color: white;
      font-size: 13px;
      opacity: 0.9;
      border: none;
      cursor: pointer;
    }
    .header-actions button:hover { opacity: 1; }
    .main-area {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    .no-chat {
      flex: 1;
      display: flex;
      justify-content: center;
      align-items: center;
      color: var(--text-secondary);
      font-size: 16px;
      background: var(--bg-chat);
    }
  `]
})
export class ChatLayoutComponent implements OnInit, OnDestroy {
  activeRoom: ChatRoom | null = null;
  currentUsername = '';
  showGroupDialog = false;

  constructor(
    private chatService: ChatService,
    private wsService: WebSocketService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentUsername = user.username;
    }

    this.wsService.connect();
    this.chatService.loadRooms().subscribe();

    this.chatService.activeRoom$.subscribe((room: ChatRoom | null) => {
      this.activeRoom = room;
    });
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }

  onRoomSelected(room: ChatRoom): void {
    this.chatService.setActiveRoom(room);
  }

  logout(): void {
    this.authService.logout();
    this.wsService.disconnect();
    this.router.navigate(['/auth/login']);
  }

  openGroupDialog(): void {
    this.showGroupDialog = true;
  }

  onGroupCreated(room: ChatRoom): void {
    this.showGroupDialog = false;
    this.chatService.setActiveRoom(room);
  }
}
