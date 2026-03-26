import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { ChatService } from '../../../core/services/chat.service';
import { User } from '../../../core/models/user.model';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ChatRoom } from '../../../core/models/chat-room.model';

@Component({
  selector: 'app-create-group-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="overlay" (click)="close()">
      <div class="dialog" (click)="$event.stopPropagation()">
        <div class="dialog-header">
          <h3>Create Group Chat</h3>
          <button class="close-btn" (click)="close()">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>Group Name</label>
            <input type="text" [(ngModel)]="groupName" placeholder="Enter group name">
          </div>
          <div class="form-group">
            <label>Add Members</label>
            <input type="text"
              [(ngModel)]="searchQuery"
              (ngModelChange)="onSearch()"
              placeholder="Search users...">
          </div>
          @if (searchResults.length > 0) {
            <div class="search-results">
              @for (user of searchResults; track user.id) {
                <div class="user-item" (click)="toggleMember(user)"
                  [class.selected]="isSelected(user)">
                  <div class="avatar">{{ user.username.charAt(0) | uppercase }}</div>
                  <span>{{ user.displayName || user.username }}</span>
                  @if (isSelected(user)) {
                    <span class="check">✓</span>
                  }
                </div>
              }
            </div>
          }
          @if (selectedMembers.length > 0) {
            <div class="selected-members">
              <label>Selected ({{ selectedMembers.length }})</label>
              <div class="chips">
                @for (m of selectedMembers; track m.id) {
                  <span class="chip">
                    {{ m.displayName || m.username }}
                    <button (click)="toggleMember(m)">&times;</button>
                  </span>
                }
              </div>
            </div>
          }
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" (click)="close()">Cancel</button>
          <button class="btn-create"
            [disabled]="!groupName.trim() || selectedMembers.length < 1"
            (click)="createGroup()">Create</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
    }
    .dialog {
      background: #fff;
      border-radius: 12px;
      width: 420px;
      max-height: 80vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 8px 24px rgba(0,0,0,0.2);
    }
    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid #eee;
    }
    .dialog-header h3 { margin: 0; font-size: 16px; }
    .close-btn { background: none; border: none; font-size: 22px; cursor: pointer; color: #999; }
    .dialog-body { padding: 16px 20px; overflow-y: auto; flex: 1; }
    .form-group { margin-bottom: 12px; }
    .form-group label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 4px; color: #555; }
    .form-group input {
      width: 100%;
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
      box-sizing: border-box;
    }
    .search-results { max-height: 160px; overflow-y: auto; border: 1px solid #eee; border-radius: 8px; }
    .user-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 12px;
      cursor: pointer;
    }
    .user-item:hover { background: #f5f5f5; }
    .user-item.selected { background: #e8f5e9; }
    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--primary, #128c7e);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 14px;
    }
    .check { margin-left: auto; color: #4caf50; font-weight: bold; }
    .selected-members { margin-top: 12px; }
    .selected-members label { font-size: 13px; font-weight: 600; color: #555; }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
    .chip {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      background: #e0f2f1;
      border-radius: 16px;
      font-size: 13px;
    }
    .chip button { background: none; border: none; cursor: pointer; font-size: 14px; color: #666; }
    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      padding: 12px 20px;
      border-top: 1px solid #eee;
    }
    .btn-cancel { padding: 8px 16px; border: 1px solid #ddd; border-radius: 8px; background: #fff; cursor: pointer; }
    .btn-create {
      padding: 8px 20px;
      border: none;
      border-radius: 8px;
      background: var(--primary, #128c7e);
      color: #fff;
      cursor: pointer;
      font-weight: 600;
    }
    .btn-create:disabled { opacity: 0.5; cursor: not-allowed; }
  `]
})
export class CreateGroupDialogComponent {
  @Output() closed = new EventEmitter<void>();
  @Output() created = new EventEmitter<ChatRoom>();

  groupName = '';
  searchQuery = '';
  searchResults: User[] = [];
  selectedMembers: User[] = [];

  constructor(
    private userService: UserService,
    private chatService: ChatService
  ) {}

  onSearch(): void {
    if (this.searchQuery.length < 2) {
      this.searchResults = [];
      return;
    }
    this.userService.searchUsers(this.searchQuery).subscribe({
      next: (res: ApiResponse<User[]>) => this.searchResults = res.data,
      error: () => this.searchResults = []
    });
  }

  toggleMember(user: User): void {
    const idx = this.selectedMembers.findIndex(m => m.id === user.id);
    if (idx >= 0) {
      this.selectedMembers.splice(idx, 1);
    } else {
      this.selectedMembers.push(user);
    }
  }

  isSelected(user: User): boolean {
    return this.selectedMembers.some(m => m.id === user.id);
  }

  createGroup(): void {
    if (!this.groupName.trim() || this.selectedMembers.length < 1) return;
    this.chatService.createRoom({
      name: this.groupName.trim(),
      memberIds: this.selectedMembers.map(m => m.id),
      type: 'GROUP'
    }).subscribe({
      next: (res: ApiResponse<ChatRoom>) => {
        this.created.emit(res.data);
        this.close();
      }
    });
  }

  close(): void {
    this.closed.emit();
  }
}
