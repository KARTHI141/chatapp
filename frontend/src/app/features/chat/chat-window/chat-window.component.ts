import { Component, Input, OnChanges, SimpleChanges, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChatRoom } from '../../../core/models/chat-room.model';
import { ChatMessage, MessageType } from '../../../core/models/message.model';
import { TimeAgoPipe } from '../../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule, TimeAgoPipe],
  templateUrl: './chat-window.component.html',
  styles: [`
    .chat-window {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    .chat-header {
      display: flex;
      align-items: center;
      padding: 10px 16px;
      background: var(--bg-light);
      border-bottom: 1px solid var(--border);
    }
    .chat-header .avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--primary);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      margin-right: 12px;
    }
    .chat-header .info { flex: 1; }
    .chat-header .info .name { font-weight: 600; font-size: 15px; }
    .chat-header .info .status { font-size: 12px; color: var(--text-secondary); }
    .chat-header .header-actions { display: flex; gap: 8px; }
    .chat-header .header-actions button {
      background: none;
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 6px 12px;
      cursor: pointer;
      font-size: 13px;
    }
    .chat-header .header-actions button:hover { background: var(--bg-white); }

    .search-bar {
      display: flex;
      padding: 8px 16px;
      background: var(--bg-light);
      border-bottom: 1px solid var(--border);
      gap: 8px;
    }
    .search-bar input {
      flex: 1;
      padding: 8px 12px;
      border: 1px solid var(--border);
      border-radius: 8px;
      font-size: 13px;
    }
    .search-bar button {
      padding: 6px 12px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 13px;
    }

    .messages-area {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      background: var(--bg-chat);
    }
    .message {
      display: flex;
      margin-bottom: 4px;
      position: relative;
    }
    .message.sent { justify-content: flex-end; }
    .message.received { justify-content: flex-start; }
    .bubble {
      max-width: 65%;
      padding: 8px 12px;
      border-radius: 8px;
      font-size: 14px;
      line-height: 1.4;
      position: relative;
    }
    .sent .bubble {
      background: var(--sent-bubble);
      border-top-right-radius: 2px;
    }
    .received .bubble {
      background: var(--received-bubble);
      border-top-left-radius: 2px;
      box-shadow: 0 1px 1px rgba(0,0,0,0.05);
    }
    .bubble .sender-name {
      font-size: 12px;
      font-weight: 600;
      color: var(--primary);
      margin-bottom: 2px;
    }
    .bubble .meta {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 4px;
      margin-top: 2px;
    }
    .bubble .meta .time {
      font-size: 11px;
      color: var(--text-secondary);
    }
    .bubble .meta .edited-tag {
      font-size: 10px;
      color: var(--text-secondary);
      font-style: italic;
    }
    .bubble .meta .status {
      font-size: 12px;
    }
    .bubble .deleted-msg {
      font-style: italic;
      color: var(--text-secondary);
    }

    /* Message actions */
    .message-actions {
      display: none;
      position: absolute;
      top: -4px;
      gap: 2px;
      z-index: 2;
    }
    .sent .message-actions { left: -100px; }
    .received .message-actions { right: -100px; }
    .message:hover .message-actions { display: flex; }
    .message-actions button {
      background: var(--bg-white);
      border: 1px solid var(--border);
      border-radius: 4px;
      padding: 2px 6px;
      cursor: pointer;
      font-size: 14px;
    }
    .message-actions button:hover { background: #eee; }

    /* Reactions */
    .reactions-bar {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-top: 4px;
    }
    .reaction-chip {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      background: var(--bg-white);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 1px 6px;
      font-size: 13px;
      cursor: pointer;
    }
    .reaction-chip.active { border-color: var(--primary); background: #e3f2fd; }
    .reaction-chip .count { font-size: 11px; color: var(--text-secondary); }

    /* Quick reaction picker */
    .reaction-picker {
      display: flex;
      gap: 2px;
      background: var(--bg-white);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 4px 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    .reaction-picker button {
      background: none;
      border: none;
      font-size: 18px;
      cursor: pointer;
      padding: 2px;
      border-radius: 4px;
    }
    .reaction-picker button:hover { background: #f0f0f0; }

    /* Edit mode */
    .edit-input {
      display: flex;
      gap: 4px;
      margin-top: 4px;
    }
    .edit-input input {
      flex: 1;
      padding: 4px 8px;
      border: 1px solid var(--primary);
      border-radius: 4px;
      font-size: 13px;
    }
    .edit-input button {
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
    }

    .input-area {
      display: flex;
      align-items: center;
      padding: 10px 16px;
      background: var(--bg-light);
      border-top: 1px solid var(--border);
      gap: 10px;
    }
    .input-area input {
      flex: 1;
      padding: 10px 16px;
      border: none;
      border-radius: 20px;
      background: var(--bg-white);
      font-size: 14px;
    }
    .send-btn {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: var(--primary);
      color: white;
      font-size: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .send-btn:hover { background: var(--primary-dark); }

    .date-divider {
      text-align: center;
      margin: 12px 0;
    }
    .date-divider span {
      background: rgba(0,0,0,0.06);
      padding: 4px 12px;
      border-radius: 6px;
      font-size: 12px;
      color: var(--text-secondary);
    }
  `]
})
export class ChatWindowComponent implements OnChanges, AfterViewChecked {
  @Input() room!: ChatRoom;
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  messages: ChatMessage[] = [];
  newMessage = '';
  currentUserId: number = 0;
  private shouldScroll = false;

  // Edit state
  editingMessageId: string | null = null;
  editContent = '';

  // Search state
  showSearch = false;
  searchQuery = '';
  searchResults: ChatMessage[] = [];

  // Reaction picker
  showReactionPicker: string | null = null;
  quickReactions = ['👍', '❤️', '😂', '😮', '😢', '🙏'];

  constructor(
    private chatService: ChatService,
    private authService: AuthService
  ) {
    this.currentUserId = this.authService.getCurrentUser()?.userId || 0;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['room']) {
      this.chatService.messages$.subscribe(msgs => {
        this.messages = msgs;
        this.shouldScroll = true;
      });
      this.chatService.markAsRead(this.room.id);
      this.cancelEdit();
      this.closeSearch();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage(): void {
    const content = this.newMessage.trim();
    if (!content) return;

    this.chatService.sendMessage({
      chatRoomId: this.room.id,
      content
    });

    this.newMessage = '';
    this.shouldScroll = true;
  }

  onTyping(): void {
    this.chatService.sendTypingIndicator(this.room.id, true);
  }

  isSent(msg: ChatMessage): boolean {
    return msg.senderId === this.currentUserId;
  }

  getStatusIcon(msg: ChatMessage): string {
    switch (msg.status) {
      case 'SENT': return '✓';
      case 'DELIVERED': return '✓✓';
      case 'READ': return '✓✓';
      default: return '';
    }
  }

  // --- Edit ---

  startEdit(msg: ChatMessage): void {
    this.editingMessageId = msg.id;
    this.editContent = msg.content;
  }

  cancelEdit(): void {
    this.editingMessageId = null;
    this.editContent = '';
  }

  saveEdit(messageId: string): void {
    const content = this.editContent.trim();
    if (!content) return;
    this.chatService.editMessage(messageId, content).subscribe(() => {
      this.cancelEdit();
    });
  }

  // --- Delete ---

  deleteMessage(messageId: string): void {
    this.chatService.deleteMessage(messageId).subscribe();
  }

  // --- Reactions ---

  toggleReactionPicker(messageId: string): void {
    this.showReactionPicker = this.showReactionPicker === messageId ? null : messageId;
  }

  addReaction(messageId: string, emoji: string): void {
    this.chatService.toggleReaction(messageId, emoji).subscribe();
    this.showReactionPicker = null;
  }

  hasUserReacted(msg: ChatMessage, emoji: string): boolean {
    return msg.reactions?.[emoji]?.includes(this.currentUserId) ?? false;
  }

  getReactionEntries(msg: ChatMessage): { emoji: string; count: number }[] {
    if (!msg.reactions) return [];
    return Object.entries(msg.reactions)
      .filter(([, users]) => users.length > 0)
      .map(([emoji, users]) => ({ emoji, count: users.length }));
  }

  // --- Search ---

  toggleSearch(): void {
    this.showSearch = !this.showSearch;
    if (!this.showSearch) {
      this.closeSearch();
    }
  }

  closeSearch(): void {
    this.showSearch = false;
    this.searchQuery = '';
    this.searchResults = [];
    this.chatService.clearSearchResults();
  }

  onSearch(): void {
    if (!this.searchQuery.trim()) return;
    this.chatService.searchMessages(this.room.id, this.searchQuery).subscribe(res => {
      this.searchResults = res.data.content || res.data;
    });
  }

  // --- Export ---

  exportChat(): void {
    this.chatService.exportChat(this.room.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `chat-${this.room.name || this.room.id}.txt`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const el = this.messagesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }
}
