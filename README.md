# AI Chat API

Spring Boot REST API for the AI Chat Platform.

Part of [ai-chat-platform](https://github.com/felipemacedo1/ai-chat-platform).

## Tech Stack

- Java 8
- Spring Boot 2.7
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- jqwik (Property-Based Testing)

## Getting Started

```bash
./mvnw spring-boot:run
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout

### Conversations
- `GET /api/conversations` - List conversations (paginated)
- `POST /api/conversations` - Create conversation
- `GET /api/conversations/{id}` - Get conversation
- `PATCH /api/conversations/{id}` - Update conversation
- `DELETE /api/conversations/{id}` - Delete conversation

### Messages
- `GET /api/conversations/{id}/messages` - Get messages
- `POST /api/conversations/{id}/messages` - Send message

## Running Tests

```bash
./mvnw test
```
