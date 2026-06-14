# 💬 Real-Time Chat Application

A production-grade real-time chat backend built with **Spring Boot 3.5**, featuring JWT authentication, WebSocket/STOMP messaging, Redis pub/sub, MySQL persistence, presence tracking, and in-app notifications.

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Redis](https://img.shields.io/badge/Redis-7.x-red?logo=redis)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client Layer                          │
│         (Swagger UI / Postman / WebSocket Client)       │
└───────────────┬──────────────────┬──────────────────────┘
                │ REST (HTTP)      │ WebSocket (STOMP)
                ▼                  ▼
┌─────────────────────────────────────────────────────────┐
│                Spring Boot Application                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │   Auth   │ │   Chat   │ │ Presence │ │Notification│ │
│  │ (JWT)    │ │(Messages)│ │ (Redis)  │ │  (Push)    │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────┬──────┘ │
│       │             │            │              │        │
│  ┌────▼─────────────▼────────────▼──────────────▼──────┐ │
│  │              Service Layer                           │ │
│  └─────────┬──────────────┬────────────────────────────┘ │
└────────────┼──────────────┼──────────────────────────────┘
             │              │
     ┌───────▼──────┐  ┌────▼─────┐
     │   MySQL 8.0  │  │ Redis 7  │
     │  (Messages,  │  │ (Pub/Sub,│
     │   Users,     │  │ Presence,│
     │   Tokens)    │  │ Caching) │
     └──────────────┘  └──────────┘
```

---

## ✨ Features

- **JWT Authentication** — Register, login, token refresh, logout with BCrypt hashing
- **Real-Time Messaging** — WebSocket/STOMP for instant message delivery
- **Redis Pub/Sub** — Horizontally scalable message broadcasting
- **1-on-1 & Group Chats** — Private conversations and group management
- **Presence Tracking** — Online/offline status via Redis TTL
- **Notifications** — In-app push notifications via WebSocket
- **Unread Counts** — Accurate per-conversation unread message tracking
- **Message Management** — Edit, soft-delete, read receipts
- **Swagger UI** — Full API documentation with JWT auth support
- **Docker Ready** — One-command setup with Docker Compose

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Java 17+ (for local development)

### Run with Docker Compose (Recommended)
```bash
# Build and start all services
docker-compose up --build

# Application starts at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

### Run Locally (Development)
```bash
# 1. Start MySQL and Redis
docker-compose up mysql redis -d

# 2. Build and run the app
./mvnw spring-boot:run        # Linux/Mac
.\mvnw.cmd spring-boot:run    # Windows
```

### Run Tests
```bash
./mvnw test        # Linux/Mac
.\mvnw.cmd test    # Windows
```

---

## 📡 API Endpoints

### Authentication — `/api/auth`
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/register` | Register new user | ❌ |
| POST | `/login` | Login, returns JWT | ❌ |
| POST | `/refresh` | Refresh access token | ❌ |
| POST | `/logout` | Invalidate tokens | ✅ |

### Users — `/api/users`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/me` | Current user profile |
| PUT | `/me` | Update profile |
| GET | `/{username}` | User public profile |
| GET | `/search?q={query}` | Search users |

### Conversations — `/api/conversations`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create conversation |
| GET | `/` | List my conversations |
| GET | `/{id}` | Conversation details |
| POST | `/{id}/participants` | Add to group (admin) |
| DELETE | `/{id}/participants/{userId}` | Remove from group |
| DELETE | `/{id}` | Leave conversation |

### Messages — `/api/conversations/{id}/messages`
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Send message |
| GET | `/` | Paginated history |
| PUT | `/{msgId}` | Edit message |
| DELETE | `/{msgId}` | Soft-delete message |
| POST | `/{msgId}/read` | Mark as read |
| GET | `/unread-count` | Unread count |

### Notifications — `/api/notifications`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | All notifications (paginated) |
| POST | `/{id}/read` | Mark as read |
| POST | `/read-all` | Mark all as read |
| GET | `/unread-count` | Unread count |

### Presence — `/api/presence`
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{userId}` | Check online status |

---

## ⚡ WebSocket (STOMP)

### Connection
```
ws://localhost:8080/ws?token=<JWT>
```

### Channels
| Direction | Channel | Purpose |
|-----------|---------|---------|
| Client → Server | `/app/chat.send` | Send message |
| Client → Server | `/app/chat.read` | Mark as read |
| Client → Server | `/app/presence.ping` | Stay online |
| Server → Client | `/topic/conversation.{id}` | Group messages |
| Server → Client | `/user/queue/messages` | Private messages |
| Server → Client | `/user/queue/notifications` | Notifications |
| Server → Client | `/topic/presence.{userId}` | Status updates |

---

## 🗄️ Database Schema

| Table | Purpose |
|-------|---------|
| `users` | User accounts (BCrypt passwords) |
| `conversations` | Chat rooms (PRIVATE/GROUP) |
| `conversation_participants` | Membership + read tracking |
| `messages` | Chat messages with soft-delete |
| `notifications` | In-app notifications |
| `refresh_tokens` | JWT refresh token storage |

---

## 🧪 Testing

**23 unit tests** covering all core services:

| Test Suite | Tests | Coverage |
|------------|-------|----------|
| AuthServiceTest | 6 | Register, login, duplicates, wrong password |
| ChatServiceTest | 4 | Create conversations, participant management |
| MessageServiceTest | 5 | Send, edit, delete, authorization |
| PresenceServiceTest | 4 | Online/offline tracking |
| NotificationServiceTest | 3 | Create, read, count |

---

## 🔧 Design Decisions

1. **Stateless JWT Auth** — No server-side sessions; scales horizontally
2. **Redis Pub/Sub** — Enables multi-instance message delivery
3. **Redis TTL for Presence** — Simple, reliable online status tracking
4. **Soft Deletes** — Messages show "This message was deleted" instead of disappearing
5. **Refresh Token Rotation** — Old tokens are revoked on refresh for security
6. **Pagination** — All list endpoints use Spring `Pageable` for efficiency
7. **DTO Pattern** — Entities never exposed directly; passwords never in responses
8. **Global Exception Handler** — Consistent error responses with proper HTTP codes

---

## 📁 Project Structure

```
src/main/java/com/kaif/chatapp/
├── ChatAppApplication.java
├── config/          # Security, WebSocket, Redis, Swagger
├── security/        # JWT utils, auth filter, WS interceptor
├── auth/            # Register, login, token management
├── user/            # User profiles and search
├── chat/            # Conversations, messages, Redis pub/sub
├── presence/        # Online/offline tracking
├── notification/    # In-app notifications
└── exception/       # Global error handling
```

---

## 📄 License

MIT License — feel free to use this project for learning and portfolio purposes.
