# Leave Management System

A production-grade, event-driven microservices platform for managing employee leave requests — built with Spring Boot 3, Apache Kafka, Redis, and Docker.

---

## Architecture

```
                          ┌─────────────────────────────────────┐
                          │           Client (HTTP)              │
                          └──────────────────┬──────────────────┘
                                             │
                          ┌──────────────────▼──────────────────┐
                          │           API Gateway :8080          │
                          │  • JWT validation & forwarding       │
                          │  • Rate limiting (Redis)             │
                          │  • Route /api/employees/**           │
                          │  • Route /api/leaves/**              │
                          └────────┬──────────────────┬─────────┘
                                   │                  │
              ┌────────────────────▼──┐        ┌──────▼────────────────────┐
              │   employee-service    │        │      leave-service         │
              │        :8081          │        │          :8082             │
              │                       │        │                            │
              │  • Register / Login   │        │  • Apply leave             │
              │  • JWT generation     │        │  • Approve / Reject        │
              │  • Employee CRUD      │        │  • Cancel leave            │
              │  • Leave balances     │        │  • Leave history           │
              └──────────┬────────────┘        └──────────┬────────────────┘
                         │                                │
              ┌──────────▼────────────┐        ┌──────────▼────────────────┐
              │    employee_db :5432  │        │      leave_db :5433        │
              │      (PostgreSQL)     │        │       (PostgreSQL)         │
              └──────────┬────────────┘        └──────────┬────────────────┘
                         │                                │
                         └──────────────┬─────────────────┘
                                        │
                          ┌─────────────▼───────────────┐
                          │        Kafka :9092           │
                          │                              │
                          │  Topics:                     │
                          │  ├─ leave.applied            │
                          │  ├─ leave.approved  ◄────────┼─── leave-service publishes
                          │  ├─ leave.rejected           │
                          │  └─ leave.cancelled          │
                          │             │                │
                          │             ▼                │
                          │  employee-service consumes   │
                          │  → deducts leave balance     │
                          └─────────────────────────────┘
                                        │
                          ┌─────────────▼───────────────┐
                          │         Redis :6379          │
                          │                              │
                          │  • Rate limiting (Gateway)   │
                          │  • JWT blacklist (logout)    │
                          │  • Leave balance cache       │
                          └─────────────────────────────┘
```

---

## Tech Stack

| Layer            | Technology                                      |
|------------------|-------------------------------------------------|
| Language         | Java 17                                         |
| Framework        | Spring Boot 3.2.3                               |
| Build Tool       | Maven (multi-module)                            |
| API Gateway      | Spring Cloud Gateway 2023.0.0                   |
| Messaging        | Apache Kafka + Zookeeper (Confluent 7.4.0)      |
| Database         | PostgreSQL 15 (separate DB per service)         |
| Cache            | Redis 7                                         |
| Security         | Spring Security 6 + JWT (jjwt)                  |
| ORM              | Spring Data JPA / Hibernate 6                   |
| Containerization | Docker + Docker Compose                         |
| Testing          | JUnit 5 + Mockito + Testcontainers              |

---

## Design Patterns

This project is intentionally over-engineered for learning purposes and demonstrates several enterprise patterns in a microservices context:

| Pattern                    | Where Used                                                     |
|----------------------------|----------------------------------------------------------------|
| **Vertical Slice Architecture** | Features organized by capability, not layer (`features/apply/`, `features/approve/`, ...) |
| **CQRS**                   | Commands (write) and Queries (read) are separate handler classes |
| **Mediator**               | Controllers dispatch to handlers via a custom `Mediator` — no direct coupling |
| **Result Pattern**         | Handlers return `Result<T>` instead of throwing exceptions for control flow |
| **Event-Driven**           | Leave state changes publish Kafka events; employee-service reacts asynchronously |
| **Cache-Aside**            | Leave balances cached in Redis; invalidated on `leave.approved` event |

---

## How It Works — Full Leave Approval Flow

```
Employee                  API Gateway            leave-service          employee-service         Kafka
    │                          │                      │                       │                    │
    │── POST /api/leaves ──────►                      │                       │                    │
    │                          │── validate JWT ─────►│                       │                    │
    │                          │   forward headers    │                       │                    │
    │                          │                      │── GET /balance ───────►                    │
    │                          │                      │   (check ANNUAL days) │                    │
    │                          │                      │◄──────────────────────│                    │
    │                          │                      │── check overlaps ─────►(leave_db)          │
    │                          │                      │── save PENDING ───────►(leave_db)          │
    │                          │                      │── publish ────────────────────────────────►│
    │◄── 201 PENDING ──────────│◄─────────────────────│   leave.applied                            │
    │                          │                      │                       │                    │
Manager                        │                      │                       │                    │
    │── PUT /api/leaves/1      │                      │                       │                    │
    │      /approve ───────────►                      │                       │                    │
    │                          │── validate JWT ─────►│                       │                    │
    │                          │   forward headers    │── verify managerId ───►(leave_db)          │
    │                          │                      │── update APPROVED ────►(leave_db)          │
    │                          │                      │── publish ────────────────────────────────►│
    │◄── 200 APPROVED ─────────│◄─────────────────────│   leave.approved       │                  │
    │                          │                      │                       │◄── consume ────────│
    │                          │                      │                       │   deduct balance   │
    │                          │                      │                       │── update used_days ►(employee_db)
    │                          │                      │                       │── evict Redis ─────►(Redis)
```

**Step-by-step:**

1. **Employee registers** → `POST /api/employees/auth/register` — account created, leave balances seeded (ANNUAL: 20, SICK: 10, UNPAID: unlimited)
2. **Employee logs in** → `POST /api/employees/auth/login` — JWT issued with `employeeId`, `role`, `department`, `managerId` as claims
3. **Employee applies for leave** → `POST /api/leaves` — gateway validates JWT, leave-service checks remaining balance (via REST to employee-service) and overlapping dates, saves `PENDING`, publishes `leave.applied`
4. **Manager approves** → `PUT /api/leaves/{id}/approve` — leave-service verifies `manager_id` matches the approver's `employeeId`, updates to `APPROVED`, publishes `leave.approved`
5. **Balance deducted asynchronously** → employee-service consumes `leave.approved`, increments `used_days`, evicts the Redis balance cache
6. **Employee logs out** → `POST /api/employees/auth/logout` — JWT added to Redis blacklist for remaining TTL; all subsequent requests with that token are rejected at the gateway

---

## API Reference

All requests go through the gateway at `http://localhost:8080`. Protected endpoints require:
```
Authorization: Bearer <jwt>
```

### Auth & Employees

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/employees/auth/register` | Public | Register a new employee or manager |
| `POST` | `/api/employees/auth/login` | Public | Login and receive a JWT |
| `POST` | `/api/employees/auth/logout` | Any | Blacklist the current JWT |
| `GET` | `/api/employees/{id}` | Employee (own), Manager | Get employee by ID |
| `GET` | `/api/employees` | Manager only | List all employees |
| `PUT` | `/api/employees/{id}` | Employee (own only) | Update employee details |
| `DELETE` | `/api/employees/{id}` | Manager only | Delete an employee |
| `GET` | `/api/employees/{id}/balance` | Employee (own), Manager | Get all leave balances |
| `GET` | `/api/employees/{id}/balance/{leaveType}` | Employee (own), Manager | Get balance for a specific leave type |

**Register request body:**
```json
{
  "name": "Jane Smith",
  "email": "jane@company.com",
  "password": "secret",
  "role": "EMPLOYEE",
  "department": "Engineering",
  "managerId": 5
}
```

**Login response:**
```json
{ "token": "eyJhbGci..." }
```

---

### Leave Requests

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/leaves` | Employee | Apply for leave |
| `GET` | `/api/leaves/{id}` | Employee (own), Manager (team) | Get leave by ID |
| `GET` | `/api/leaves/my` | Employee | Own leave history |
| `GET` | `/api/leaves/pending` | Manager only | Pending leaves for your team |
| `PUT` | `/api/leaves/{id}/approve` | Manager only | Approve a pending leave |
| `PUT` | `/api/leaves/{id}/reject` | Manager only | Reject a pending leave |
| `PUT` | `/api/leaves/{id}/cancel` | Employee only | Cancel a pending leave |

**Apply request body:**
```json
{
  "leaveType": "ANNUAL",
  "startDate": "2026-04-01",
  "endDate": "2026-04-03",
  "reason": "Family vacation"
}
```

**Leave types:** `ANNUAL` · `SICK` · `UNPAID`

**Leave status flow:**
```
PENDING ──► APPROVED   (manager approves)
        └─► REJECTED   (manager rejects, rejectionReason required)
        └─► CANCELLED  (employee cancels before decision)
```

---

## Running Locally

**Prerequisites:** Docker Desktop (or Docker Engine + Compose v2)

```bash
git clone https://github.com/your-username/leave-management-system.git
cd leave-management-system
docker compose up --build
```

That's it. Docker Compose handles startup ordering — Zookeeper and Kafka come up first, then the databases, then the two services, then the gateway.

| Service           | URL                          |
|-------------------|------------------------------|
| API Gateway       | http://localhost:8080        |
| employee-service  | http://localhost:8081        |
| leave-service     | http://localhost:8082        |
| PostgreSQL (emp)  | localhost:5432               |
| PostgreSQL (leave)| localhost:5433               |
| Redis             | localhost:6379               |
| Kafka             | localhost:9092               |

**Quick smoke test:**
```bash
# Register
curl -X POST http://localhost:8080/api/employees/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@co.com","password":"pass","role":"EMPLOYEE","department":"Engineering","managerId":1}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/employees/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@co.com","password":"pass"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Apply leave
curl -X POST http://localhost:8080/api/leaves \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"leaveType":"ANNUAL","startDate":"2026-05-01","endDate":"2026-05-03","reason":"Holiday"}'
```

---

## Environment Variables

All variables have safe defaults for local development. **Override `JWT_SECRET` before deploying to any shared environment.**

| Variable             | Service(s)                        | Default                                     | Description                          |
|----------------------|-----------------------------------|---------------------------------------------|--------------------------------------|
| `JWT_SECRET`         | employee-service, leave-service, api-gateway | `bXlzdXBlcnNlY3JldGtl...` | Base64-encoded HMAC-SHA256 signing key |
| `JWT_EXPIRY_MS`      | employee-service                  | `86400000` (24 h)                           | Token TTL in milliseconds            |
| `DB_URL`             | employee-service, leave-service   | `jdbc:postgresql://...`                     | JDBC connection URL                  |
| `DB_USERNAME`        | employee-service, leave-service   | `postgres`                                  | Database username                    |
| `DB_PASSWORD`        | employee-service, leave-service   | `postgres`                                  | Database password                    |
| `REDIS_HOST`         | employee-service, api-gateway     | `localhost`                                 | Redis hostname                       |
| `KAFKA_BOOTSTRAP`    | employee-service, leave-service   | `localhost:9092`                            | Kafka bootstrap servers              |
| `EMPLOYEE_SERVICE_URL` | leave-service, api-gateway      | `http://localhost:8081`                     | Internal URL for employee-service    |
| `LEAVE_SERVICE_URL`  | api-gateway                       | `http://localhost:8082`                     | Internal URL for leave-service       |

---

## Project Structure

```
leave-management-system/
├── api-gateway/                        # Spring Cloud Gateway
│   └── src/main/java/com/lms/gateway/
│       ├── filter/AuthFilter.java      # JWT validation + header injection
│       └── config/RateLimiterConfig.java
│
├── employee-service/                   # Auth, employee CRUD, leave balances
│   └── src/main/java/com/lms/employee/
│       ├── features/
│       │   ├── auth/login/             # LoginCommand + Handler + Controller
│       │   ├── auth/register/          # RegisterCommand + Handler + Controller
│       │   ├── employee/               # CRUD slices
│       │   └── leavebalance/           # Balance query + Kafka-driven update
│       ├── common/
│       │   ├── mediator/               # Custom Mediator, ICommandHandler, IQueryHandler
│       │   ├── result/                 # Result<T> pattern
│       │   └── security/              # JwtFilter, JwtService, SecurityConfig
│       └── infrastructure/
│           ├── kafka/                  # LeaveEventConsumer
│           └── redis/                  # JwtBlacklistService, LeaveBalanceCache
│
├── leave-service/                      # Leave lifecycle + Kafka events
│   └── src/main/java/com/lms/leave/
│       ├── features/
│       │   ├── apply/                  # ApplyLeaveCommand + Handler
│       │   ├── approve/                # ApproveLeaveCommand + Handler
│       │   ├── reject/                 # RejectLeaveCommand + Handler
│       │   ├── cancel/                 # CancelLeaveCommand + Handler
│       │   └── list/                   # Query handlers
│       ├── common/                     # Mediator, Result<T>, security
│       └── infrastructure/
│           ├── kafka/                  # LeaveEventProducer
│           └── client/                 # EmployeeServiceClient (REST)
│
└── docker-compose.yml
```

---

## Business Rules

- Employees cannot apply if their leave balance is insufficient (checked via service-to-service REST call)
- Overlapping leave requests are rejected (checked against existing `PENDING`/`APPROVED` leaves)
- Managers can only approve/reject leaves for employees reporting to them (`managerId` must match)
- Employees can only cancel their own `PENDING` requests — not approved or rejected ones
- `totalDays` is always computed server-side (`endDate - startDate + 1`); client values are ignored
- On logout, the JWT is blacklisted in Redis for its remaining TTL — all services reject it immediately
- `UNPAID` leave type has no balance limit (`Integer.MAX_VALUE`)

---

## License

MIT
