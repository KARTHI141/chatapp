import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../core/services/chat.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChatRoom, ChatRoomType } from '../../../core/models/chat-room.model';
import { User } from '../../../core/models/user.model';
import { ApiResponse } from '../../../core/models/api-response.model';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TimeAgoPipe],
  templateUrl: './chat-list.component.html',
  styles: [`
    .chat-list {
      flex: 1;
      overflow-y: auto;
    }
    .search-bar {
      padding: 8px 12px;
      background: var(--bg-light);
    }
    .search-bar input {
      width: 100%;
      padding: 8px 12px;
      border: none;
      border-radius: 20px;
      background: var(--bg-white);
      font-size: 13px;
    }
    .room-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      cursor: pointer;
      border-bottom: 1px solid var(--border);
      transition: background 0.15s;
    }
    .room-item:hover { background: var(--bg-light); }
    .room-item.active { background: #f0f0f0; }
    .avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: var(--primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 18px;
      margin-right: 12px;
      flex-shrink: 0;
    }
    .room-info {
      flex: 1;
      min-width: 0;
    }
    .room-info .name {
      font-weight: 600;
      font-size: 15px;
      margin-bottom: 2px;
    }
    .room-info .last-msg {
      color: var(--text-secondary);
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .room-meta {
      text-align: right;
      flex-shrink: 0;
    }
    .room-meta .time {
      font-size: 11px;
      color: var(--text-secondary);
    }
    .unread-badge {
      background: var(--secondary);
      color: white;
      font-size: 11px;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 10px;
      margin-top: 4px;
      display: inline-block;
    }
    .search-results {
      padding: 8px;
    }
    .search-results .user-item {
      display: flex;
      align-items: center;
      padding: 10px 12px;
      cursor: pointer;
      border-radius: 8px;
    }
    .search-results .user-item:hover { background: var(--bg-light); }
  `]
})
export class ChatListComponent implements OnInit {
  @Output() roomSelected = new EventEmitter<ChatRoom>();

  rooms: ChatRoom[] = [];
  searchQuery = '';
  searchResults: User[] = [];
  activeRoomId: string | null = null;
  searching = false;

  constructor(
    private chatService: ChatService,
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.chatService.rooms$.subscribe((rooms: ChatRoom[]) => this.rooms = rooms);
  }

  selectRoom(room: ChatRoom): void {
    this.activeRoomId = room.id;
    this.roomSelected.emit(room);
  }

  onSearch(): void {
    if (this.searchQuery.length < 2) {
      this.searchResults = [];
      this.searching = false;
      return;
    }
    this.searching = true;
    this.userService.searchUsers(this.searchQuery).subscribe({
      next: (res: ApiResponse<User[]>) => this.searchResults = res.data,
      error: () => this.searchResults = []
    });
  }

  startChatWithUser(user: User): void {
    this.chatService.createRoom({
      memberIds: [user.id],
      type: 'PRIVATE'
    }).subscribe({
      next: (res: ApiResponse<ChatRoom>) => {
        this.selectRoom(res.data);
        this.searchQuery = '';
        this.searchResults = [];
        this.searching = false;
      }
    });
  }

  getRoomDisplayName(room: ChatRoom): string {
    if (room.type === ChatRoomType.GROUP) {
      return room.name || 'Group Chat';
    }
    return room.name || `Room ${room.id.substring(0, 6)}`;
  }

  getInitial(room: ChatRoom): string {
    const name = this.getRoomDisplayName(room);
    return name.charAt(0).toUpperCase();
  }
}
