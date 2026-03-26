export interface ChatRoom {
  id: string;
  name: string;
  type: ChatRoomType;
  memberIds: number[];
  createdBy: number;
  createdAt: string;
  lastMessage?: string;
  lastMessageAt?: string;
  unreadCount: number;
}

export enum ChatRoomType {
  PRIVATE = 'PRIVATE',
  GROUP = 'GROUP'
}

export interface CreateRoomRequest {
  name?: string;
  memberIds: number[];
  type?: string;
}
