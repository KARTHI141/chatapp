# ChatApp — Production-Grade Real-Time Chat Application

A WhatsApp-like real-time chat system built with modern industry practices.

## Tech Stack

| Layer     | Technology                                      |
|-----------|------------------------------------------------|
| Backend   | Java 17, Spring Boot 3.2, Spring Security, JWT |
| WebSocket | STOMP over SockJS                              |
| Frontend  | Angular 17, RxJS, STOMP.js                     |
| Database  | MySQL (users), MongoDB (messages)              |
| Cache     | Redis (presence, pub/sub, message cache)       |
| DevOps    | Docker, Docker Compose, Nginx                  |

## Architecture

```
backend/
├── src/main/java/com/chatapp/
│   ├── auth/           # JWT authentication, User entity, Security
│   ├── chat/           # Chat rooms, messages, WebSocket controllers
│   ├── user/           # User profile, presence service
│   ├── notification/   # Push notifications via WebSocket
│   ├── common/         # Global exception handling, DTOs
│   └── config/         # Security, WebSocket, Redis, MongoDB config
frontend/
├── src/app/
│   ├── core/           # Services, guards, interceptors, models
│   ├── features/       # Auth, Chat, User feature modules
│   └── shared/         # Pipes, shared components
```

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+
- Docker & Docker Compose

### Run with Docker (recommended)
```bash
docker-compose up --build
```
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- WebSocket: ws://localhost:8080/ws

### Run locally (development)

**Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm start
```

## API Endpoints

### Auth
| Method | Endpoint             | Description        |
|--------|---------------------|--------------------|
| POST   | /api/auth/register  | Register new user  |
| POST   | /api/auth/login     | Login              |

### Chat
| Method | Endpoint                              | Description              |
|--------|--------------------------------------|--------------------------|
| POST   | /api/chat/rooms                      | Create chat room         |
| GET    | /api/chat/rooms                      | Get user's rooms         |
| GET    | /api/chat/rooms/{id}/messages        | Get chat history         |
| PUT    | /api/chat/messages/{id}              | Edit a message           |
| DELETE | /api/chat/messages/{id}              | Delete a message         |
| POST   | /api/chat/messages/{id}/reactions    | Toggle emoji reaction    |
| GET    | /api/chat/rooms/{id}/messages/search | Search messages          |
| GET    | /api/chat/rooms/{id}/export          | Export chat as text file |

### WebSocket
| Destination                   | Direction | Description           |
|-------------------------------|-----------|----------------------|
| /app/chat.sendMessage         | Client→   | Send message         |
| /app/chat.typing              | Client→   | Typing indicator     |
| /app/chat.markRead            | Client→   | Mark messages read   |
| /app/chat.editMessage         | Client→   | Edit message         |
| /app/chat.deleteMessage       | Client→   | Delete message       |
| /app/chat.toggleReaction      | Client→   | Toggle reaction      |
| /user/{id}/queue/messages     | →Client   | Receive messages     |
| /user/{id}/queue/message.*    | →Client   | Edit/delete/reaction |
| /topic/status                 | →Client   | User online/offline  |

## Features
- ✅ JWT Authentication with OTP email verification
- ✅ One-to-One & Group Chat
- ✅ Real-Time Messaging (WebSocket/STOMP)
- ✅ Message Edit & Delete (own messages only)
- ✅ Emoji Reactions (👍 ❤️ 😂 😮 😢 🙏)
- ✅ Message Search (regex-powered, per room)
- ✅ Chat History Export (download as .txt)
- ✅ Typing Indicators
- ✅ Read Receipts (✓ sent, ✓✓ delivered/read)
- ✅ Online/Offline Presence (Redis)
- ✅ File/Image Upload (10MB limit, whitelist)
- ✅ Push Notifications (WebSocket)
- ✅ Chat History pagination (MongoDB)
- ✅ Message Caching (Redis, 1h TTL)
- ✅ Redis Pub/Sub for horizontal scaling
- ✅ Role-based Access Control (USER/ADMIN)
- ✅ Token Refresh & Blacklisting
- ✅ Dockerized deployment (5 services)
