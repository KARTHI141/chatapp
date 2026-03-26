export interface ChatMessage {
  id: string;
  chatRoomId: string;
  senderId: number;
  senderUsername: string;
  content: string;
  type: MessageType;
  status: MessageStatus;
  fileUrl?: string;
  fileName?: string;
  timestamp: string;
  editedAt?: string;
  deleted?: boolean;
  reactions?: { [emoji: string]: number[] };
}

export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  SYSTEM = 'SYSTEM'
}

export enum MessageStatus {
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  READ = 'READ'
}

export interface SendMessageRequest {
  chatRoomId: string;
  content: string;
  type?: string;
  fileUrl?: string;
  fileName?: string;
}

export interface TypingIndicator {
  chatRoomId: string;
  userId: number;
  username: string;
  typing: boolean;
}

export interface ReadReceipt {
  chatRoomId: string;
  messageIds: string[];
  readByUserId: number;
  status: MessageStatus;
}
