import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '@env/environment';
import { ApiResponse } from '../models/api-response.model';
import { ChatMessage, SendMessageRequest, TypingIndicator, ReadReceipt } from '../models/message.model';
import { ChatRoom, CreateRoomRequest } from '../models/chat-room.model';
import { WebSocketService } from './websocket.service';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly API = `${environment.apiUrl}/chat`;

  private roomsSubject = new BehaviorSubject<ChatRoom[]>([]);
  rooms$ = this.roomsSubject.asObservable();

  private activeRoomSubject = new BehaviorSubject<ChatRoom | null>(null);
  activeRoom$ = this.activeRoomSubject.asObservable();

  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  messages$ = this.messagesSubject.asObservable();

  private searchResultsSubject = new BehaviorSubject<ChatMessage[]>([]);
  searchResults$ = this.searchResultsSubject.asObservable();

  constructor(
    private http: HttpClient,
    private wsService: WebSocketService
  ) {
    this.listenForIncomingMessages();
    this.listenForMessageUpdates();
  }

  // --- REST API Calls ---

  loadRooms(): Observable<ApiResponse<ChatRoom[]>> {
    return this.http.get<ApiResponse<ChatRoom[]>>(`${this.API}/rooms`)
      .pipe(tap((res: ApiResponse<ChatRoom[]>) => this.roomsSubject.next(res.data)));
  }

  createRoom(request: CreateRoomRequest): Observable<ApiResponse<ChatRoom>> {
    return this.http.post<ApiResponse<ChatRoom>>(`${this.API}/rooms`, request)
      .pipe(tap(() => this.loadRooms().subscribe()));
  }

  loadMessages(roomId: string, page = 0, size = 50): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<any>>(`${this.API}/rooms/${roomId}/messages`, { params })
      .pipe(tap((res: ApiResponse<any>) => {
        const messages = res.data.content || res.data;
        this.messagesSubject.next(messages.reverse());
      }));
  }

  setActiveRoom(room: ChatRoom): void {
    this.activeRoomSubject.next(room);
    this.loadMessages(room.id).subscribe();
    this.subscribeToRoomTopics(room.id);
  }

  // --- Edit & Delete ---

  editMessage(messageId: string, content: string): Observable<ApiResponse<ChatMessage>> {
    return this.http.put<ApiResponse<ChatMessage>>(`${this.API}/messages/${messageId}`, { content })
      .pipe(tap(res => this.updateMessageInList(res.data)));
  }

  deleteMessage(messageId: string): Observable<ApiResponse<ChatMessage>> {
    return this.http.delete<ApiResponse<ChatMessage>>(`${this.API}/messages/${messageId}`)
      .pipe(tap(res => this.updateMessageInList(res.data)));
  }

  // --- Reactions ---

  toggleReaction(messageId: string, emoji: string): Observable<ApiResponse<ChatMessage>> {
    return this.http.post<ApiResponse<ChatMessage>>(
      `${this.API}/messages/${messageId}/reactions`, { emoji }
    ).pipe(tap(res => this.updateMessageInList(res.data)));
  }

  // --- Search ---

  searchMessages(roomId: string, query: string, page = 0): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', '20');
    return this.http.get<ApiResponse<any>>(`${this.API}/rooms/${roomId}/messages/search`, { params })
      .pipe(tap((res: ApiResponse<any>) => {
        const results = res.data.content || res.data;
        this.searchResultsSubject.next(results);
      }));
  }

  clearSearchResults(): void {
    this.searchResultsSubject.next([]);
  }

  // --- Export ---

  exportChat(roomId: string): Observable<Blob> {
    return this.http.get(`${this.API}/rooms/${roomId}/export`, {
      responseType: 'blob'
    });
  }

  // --- WebSocket Operations ---

  sendMessage(request: SendMessageRequest): void {
    this.wsService.send('/app/chat.sendMessage', request);
  }

  sendTypingIndicator(chatRoomId: string, typing: boolean): void {
    this.wsService.send('/app/chat.typing', { chatRoomId, typing });
  }

  markAsRead(chatRoomId: string): void {
    this.wsService.send('/app/chat.markRead', { chatRoomId, messageIds: [] });
  }

  private subscribeToRoomTopics(roomId: string): void {
    this.wsService.subscribe(`/topic/room.${roomId}.typing`);
    this.wsService.subscribe(`/topic/room.${roomId}.read`);
  }

  private listenForIncomingMessages(): void {
    this.wsService.subscribe('/user/queue/messages').subscribe((event: { destination: string; body: any }) => {
      const message: ChatMessage = event.body;
      const currentMessages = this.messagesSubject.value;
      const activeRoom = this.activeRoomSubject.value;

      if (activeRoom && message.chatRoomId === activeRoom.id) {
        this.messagesSubject.next([...currentMessages, message]);
      }

      this.loadRooms().subscribe();
    });
  }

  private listenForMessageUpdates(): void {
    // Listen for edit/delete/reaction updates
    const destinations = ['/user/queue/message.edited', '/user/queue/message.deleted', '/user/queue/message.updated'];
    destinations.forEach(dest => {
      this.wsService.subscribe(dest).subscribe((event: { destination: string; body: any }) => {
        const updatedMessage: ChatMessage = event.body;
        this.updateMessageInList(updatedMessage);
      });
    });
  }

  private updateMessageInList(updatedMsg: ChatMessage): void {
    const current = this.messagesSubject.value;
    const index = current.findIndex(m => m.id === updatedMsg.id);
    if (index !== -1) {
      const updated = [...current];
      updated[index] = updatedMsg;
      this.messagesSubject.next(updated);
    }
  }
}
