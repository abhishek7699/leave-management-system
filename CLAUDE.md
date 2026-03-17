# Leave Management System — CLAUDE.md

## Project Overview
A production-grade Employee Leave Management System built with two independent Spring Boot
microservices. Employees can apply for leaves, managers can approve or reject them, and all
inter-service communication happens asynchronously via Kafka. The system is secured with JWT,
cached with Redis, rate-limited at the API Gateway, and fully orchestrated with Docker Compose.

---

## Architecture

```
Client
  ↓
API Gateway :8080  (Spring Cloud Gateway)
  ├── JWT Validation
  ├── Rate Limiting (Redis)
  ├── Routes /api/employees/** → employee-service:8081
  └── Routes /api/leaves/**    → leave-service:8082
        │                              │
        ▼                              ▼
employee-service :8081        leave-service :8082
├── Employee CRUD             ├── Apply leave
├── Auth (Login/Register)     ├── Approve/Reject leave
├── JWT generation            ├── Cancel leave
├── Role management           └── Leave history
└── Leave balance (cache)
        │                              │
        ▼                              ▼
  employee_db :5432             leave_db :5433
  (PostgreSQL)                  (PostgreSQL)
        │                              │
        └──────────┬───────────────────┘
                   ▼
             Kafka :9092
             Topics:
             ├── leave.applied
             ├── leave.approved
             ├── leave.rejected
             └── leave.cancelled
                   │
                   ▼
             Redis :6379
             ├── Rate limiting (Gateway)
             ├── JWT blacklist (logout)
             └── Leave balance cache
```

---

## Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Language           | Java 17                             |
| Framework          | Spring Boot 3.x                     |
| Build Tool         | Maven                               |
| API Gateway        | Spring Cloud Gateway                |
| Messaging          | Apache Kafka + Zookeeper            |
| Database           | PostgreSQL (separate DB per service)|
| Cache              | Redis                               |
| Security           | Spring Security 6.x + JWT           |
| ORM                | Spring Data JPA / Hibernate         |
| Mapping            | MapStruct                           |
| Testing            | JUnit 5 + Mockito + Testcontainers  |
| Containerization   | Docker + Docker Compose             |

---

## Modules

```
leave-management-system/
├── api-gateway/
├── employee-service/
├── leave-service/
├── docker-compose.yml
└── CLAUDE.md
```

---

## Architecture Patterns

### Vertical Slice Architecture
Organize by feature, not by layer. Each feature folder is self-contained.

```
features/
├── auth/
│   ├── login/
│   │   ├── LoginCommand.java
│   │   ├── LoginCommandHandler.java
│   │   ├── LoginController.java
│   │   └── LoginResponse.java
│   └── register/
│       ├── RegisterCommand.java
│       ├── RegisterCommandHandler.java
│       ├── RegisterController.java
│       └── RegisterRequest.java
├── employee/
│   ├── create/
│   ├── update/
│   ├── delete/
│   ├── getById/
│   └── list/
└── leavebalance/
    ├── getBalance/
    └── updateBalance/
```

### CQRS
- Commands → write operations (create, update, delete, apply, approve, reject, cancel)
- Queries → read operations (get, list, history, balance)
- Each command/query has its own dedicated handler class

### Mediator Pattern (Custom — no library)
```java
public interface ICommandHandler<TCommand, TResult> {
    TResult handle(TCommand command);
}

public interface IQueryHandler<TQuery, TResult> {
    TResult handle(TQuery query);
}

@Component
public class Mediator {
    // Spring auto-wires all handlers
    // Controllers call mediator.send(command) or mediator.query(query)
}
```

### Result Pattern
Never throw raw exceptions from handlers. Return Result<T> instead.
```java
public class Result<T> {
    private final T value;
    private final String error;
    private final boolean isSuccess;

    public static <T> Result<T> success(T value) { ... }
    public static <T> Result<T> failure(String error) { ... }
    public boolean isSuccess() { ... }
    public boolean isFailure() { ... }
    public T getValue() { ... }
    public String getError() { ... }
}
```

### SOLID Principles
- S: Each handler class has ONE responsibility
- O: New features = new slices, never modify existing handlers
- L: ICommandHandler/IQueryHandler interfaces are substitutable
- I: ICommandHandler and IQueryHandler are separate interfaces
- D: Controllers depend on Mediator interface, not concrete handlers

---

## employee-service

### Package Structure
```
src/main/java/com/lms/employee/
├── features/
│   ├── auth/
│   │   ├── login/
│   │   └── register/
│   ├── employee/
│   │   ├── create/
│   │   ├── update/
│   │   ├── delete/
│   │   ├── getById/
│   │   └── list/
│   └── leavebalance/
│       ├── getBalance/
│       └── updateBalance/
├── common/
│   ├── result/            ← Result<T>
│   ├── mediator/          ← Mediator, ICommandHandler, IQueryHandler
│   ├── exceptions/        ← Custom exceptions + GlobalExceptionHandler
│   └── security/          ← JwtFilter, SecurityConfig, JwtService
├── infrastructure/
│   ├── kafka/             ← LeaveEventConsumer
│   ├── redis/             ← RedisConfig, JwtBlacklistService, LeaveBalanceCache
│   └── persistence/       ← JPA entities, Spring Data repositories
└── config/                ← AppConfig, beans
```

### Domain Entities

**Employee**
```
id               Long (auto-generated)
name             String (not null)
email            String (not null, unique)
password         String (not null, bcrypt hashed)
role             Enum: EMPLOYEE | MANAGER
department       String (not null)
managerId        Long (nullable — null if the employee IS a manager)
createdAt        LocalDateTime (not null, not updatable)
```

**LeaveBalance**
```
id               Long (auto-generated)
employeeId       Long (not null, unique per leaveType)
leaveType        Enum: ANNUAL | SICK | UNPAID
totalDays        Integer (not null)
usedDays         Integer (not null, default 0)
remainingDays    Integer (computed: totalDays - usedDays)
```

Default leave allocations on employee creation:
- ANNUAL: 20 days
- SICK: 10 days
- UNPAID: unlimited (remainingDays = Integer.MAX_VALUE)

### Commands and Queries

Commands (Write):
- RegisterEmployeeCommand
- UpdateEmployeeCommand
- DeleteEmployeeCommand
- LoginCommand → returns JWT token
- UpdateLeaveBalanceCommand (called by Kafka consumer)

Queries (Read):
- GetEmployeeByIdQuery
- ListEmployeesQuery (admin only)
- GetLeaveBalanceQuery (by employeeId + leaveType)

### API Endpoints

```
POST   /api/employees/auth/register    → public
POST   /api/employees/auth/login       → public
POST   /api/employees/auth/logout      → authenticated (blacklists JWT in Redis)

GET    /api/employees/{id}             → EMPLOYEE (own), MANAGER (any in dept)
GET    /api/employees                  → MANAGER only
PUT    /api/employees/{id}             → EMPLOYEE (own only)
DELETE /api/employees/{id}             → MANAGER only

GET    /api/employees/{id}/balance             → EMPLOYEE (own), MANAGER
GET    /api/employees/{id}/balance/{leaveType} → EMPLOYEE (own), MANAGER
```

### JWT
- Secret key: configured in application.yml (move to env var)
- Expiry: 24 hours
- Claims: employeeId, email, role, department, managerId
- Gateway validates token and forwards headers:
  - X-Employee-Id
  - X-Employee-Role
  - X-Employee-Department
  - X-Manager-Id

### Redis Usage in employee-service
- JWT Blacklist: key = `blacklist:{token}`, TTL = remaining token lifetime
- Leave Balance Cache: key = `balance:{employeeId}:{leaveType}`, TTL = 1 hour
- Invalidate balance cache on Kafka consume (leave.approved)

### Kafka Consumer
Topic: `leave.approved`
Action: deduct usedDays from LeaveBalance, invalidate Redis cache

Topic: `leave.rejected`
Action: no balance change, log event only

Topic: `leave.cancelled`
Action: no balance change, log event only

---

## leave-service

### Package Structure
```
src/main/java/com/lms/leave/
├── features/
│   ├── apply/
│   ├── approve/
│   ├── reject/
│   ├── cancel/
│   ├── getById/
│   └── list/
├── common/
│   ├── result/
│   ├── mediator/
│   ├── exceptions/
│   └── security/          ← JWT validation only (no generation)
├── infrastructure/
│   ├── kafka/             ← LeaveEventProducer
│   └── persistence/       ← JPA entities, Spring Data repositories
└── config/
```

### Domain Entity

**LeaveRequest**
```
id               Long (auto-generated)
employeeId       Long (not null) ← from JWT header, NO FK to employee table
managerId        Long (not null) ← from JWT header
leaveType        Enum: ANNUAL | SICK | UNPAID
startDate        LocalDate (not null)
endDate          LocalDate (not null)
totalDays        Integer (computed on apply: endDate - startDate + 1)
reason           String (not null)
status           Enum: PENDING | APPROVED | REJECTED | CANCELLED
rejectionReason  String (nullable — populated on rejection)
appliedAt        LocalDateTime (not null, not updatable)
updatedAt        LocalDateTime (auto-updated)
```

### Status Flow
```
PENDING → APPROVED   (manager approves)
PENDING → REJECTED   (manager rejects, rejectionReason required)
PENDING → CANCELLED  (employee cancels before decision)
APPROVED → (terminal, cannot be changed)
REJECTED → (terminal, cannot be changed)
CANCELLED → (terminal, cannot be changed)
```

### Business Rules
- Employee cannot apply for leave if insufficient balance (check via REST call to employee-service)
- Employee cannot apply for overlapping leave dates (check existing PENDING/APPROVED leaves)
- Employee can only cancel their OWN leave requests
- Employee can only cancel PENDING requests (not APPROVED or REJECTED)
- Manager can only approve/reject leaves from their own department (managerId match)
- totalDays is calculated server-side, never trusted from client

### Commands and Queries

Commands (Write):
- ApplyLeaveCommand
- ApproveLeaveCommand
- RejectLeaveCommand
- CancelLeaveCommand

Queries (Read):
- GetLeaveByIdQuery
- ListLeavesByEmployeeQuery
- ListPendingLeavesByManagerQuery

### API Endpoints

```
POST   /api/leaves                     → EMPLOYEE only (apply)
GET    /api/leaves/{id}                → EMPLOYEE (own), MANAGER (team)
GET    /api/leaves/my                  → EMPLOYEE (own leave history)
GET    /api/leaves/pending             → MANAGER only (pending team leaves)
PUT    /api/leaves/{id}/approve        → MANAGER only
PUT    /api/leaves/{id}/reject         → MANAGER only (body: rejectionReason)
PUT    /api/leaves/{id}/cancel         → EMPLOYEE only (own, PENDING only)
```

### Kafka Producer
Publish to these topics after each state change:
- `leave.applied`   → payload: { leaveRequestId, employeeId, leaveType, totalDays }
- `leave.approved`  → payload: { leaveRequestId, employeeId, leaveType, totalDays }
- `leave.rejected`  → payload: { leaveRequestId, employeeId, leaveType, rejectionReason }
- `leave.cancelled` → payload: { leaveRequestId, employeeId, leaveType }

---

## api-gateway

### Dependencies
- spring-cloud-starter-gateway
- spring-boot-starter-data-redis-reactive
- jjwt (JWT validation)

### Routing Config
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: employee-service
          uri: http://employee-service:8081
          predicates:
            - Path=/api/employees/**
          filters:
            - AuthFilter
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20

        - id: leave-service
          uri: http://leave-service:8082
          predicates:
            - Path=/api/leaves/**
          filters:
            - AuthFilter
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

### Public Routes (skip JWT validation)
- POST /api/employees/auth/register
- POST /api/employees/auth/login

### AuthFilter Behavior
1. Extract JWT from Authorization header
2. Validate signature and expiry
3. Check Redis blacklist (if blacklisted → 401)
4. Extract claims and forward as headers:
   - X-Employee-Id
   - X-Employee-Role
   - X-Employee-Department
   - X-Manager-Id
5. If invalid → 401 Unauthorized

---

## Exception Handling

### Custom Exceptions (per service)

employee-service:
- EmployeeNotFoundException (404)
- EmailAlreadyExistsException (409)
- InvalidCredentialsException (401)
- TokenBlacklistedException (401)
- InsufficientLeaveBalanceException (400)

leave-service:
- LeaveRequestNotFoundException (404)
- LeaveAlreadyCancelledException (400)
- LeaveNotPendingException (400)
- OverlappingLeaveException (409)
- UnauthorizedLeaveActionException (403)

### GlobalExceptionHandler (@RestControllerAdvice)
Maps all exceptions to standard error response:
```json
{
  "timestamp": "2026-03-16T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id: 5",
  "path": "/api/employees/5"
}
```

---

## Testing Strategy

### Unit Tests (JUnit 5 + Mockito)
Test each CommandHandler and QueryHandler in isolation.
Mock all dependencies (repositories, kafka, redis).

Files to test per feature slice:
- *CommandHandler or *QueryHandler → one test class per handler
- Cover: happy path, not found, validation failure, unauthorized

### Integration Tests (Spring Boot Test + Testcontainers)
Spin up real PostgreSQL and Kafka containers.
Test full request → handler → DB → Kafka flow.

```java
@SpringBootTest
@Testcontainers
class ApplyLeaveIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
}
```

Test naming convention: `methodName_stateUnderTest_expectedBehavior`
Example: `handle_whenLeaveIsPending_shouldCancelSuccessfully`

---

## Docker Compose

```yaml
services:
  api-gateway:
    build: ./api-gateway
    ports: ["8080:8080"]
    depends_on: [employee-service, leave-service, redis]

  employee-service:
    build: ./employee-service
    ports: ["8081:8081"]
    depends_on: [employee-db, kafka, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://employee-db:5432/employee_db

  leave-service:
    build: ./leave-service
    ports: ["8082:8082"]
    depends_on: [leave-db, kafka]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://leave-db:5432/leave_db

  employee-db:
    image: postgres:15
    environment:
      POSTGRES_DB: employee_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports: ["5432:5432"]

  leave-db:
    image: postgres:15
    environment:
      POSTGRES_DB: leave_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports: ["5433:5432"]

  redis:
    image: redis:7
    ports: ["6379:6379"]

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

---

## Environment Variables

| Variable              | Service           | Description                  |
|-----------------------|-------------------|------------------------------|
| JWT_SECRET            | employee-service  | JWT signing key              |
| JWT_EXPIRY_MS         | employee-service  | Token TTL in ms (default 86400000) |
| REDIS_HOST            | all               | Redis hostname               |
| KAFKA_BOOTSTRAP       | all               | Kafka bootstrap servers      |
| DB_URL                | per service       | PostgreSQL JDBC URL          |
| DB_USERNAME           | per service       | DB username                  |
| DB_PASSWORD           | per service       | DB password                  |

---

## Build Order

Scaffold in this order to avoid dependency issues:

1. Common module (Result<T>, Mediator, ICommandHandler, IQueryHandler)
2. employee-service infrastructure (entities, repos, security, redis, kafka consumer)
3. employee-service features (auth → employee → leavebalance)
4. leave-service infrastructure (entities, repos, kafka producer)
5. leave-service features (apply → approve → reject → cancel → queries)
6. api-gateway (routing, AuthFilter, rate limiting)
7. Docker Compose wiring
8. Unit tests per handler
9. Integration tests with Testcontainers

---

## Future Scope (Do Not Implement Now)
- React TypeScript frontend (Vite, React Router, Axios, deployed to Cloud Run)
- GCP Cloud SQL migration (replace Docker Postgres)
- Secret Manager for credentials
- Distributed tracing (Micrometer + Zipkin)
- Notification service (email on approval/rejection)