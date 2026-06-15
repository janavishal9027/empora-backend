# Empora Backend — Spring Boot REST API

> Employee Management System backend built with Java 17, Spring Boot 3.2, Spring Security (JWT), and Spring Data JPA.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 (production) / H2 (development) |
| Build Tool | Maven |
| Utilities | Lombok, MapStruct |

---

## Project Structure

```
src/main/java/com/quickems/
├── config/
│   ├── DataSeeder.java          # Auto-creates admin & HR accounts on startup
│   ├── SecurityConfig.java      # JWT filter chain, CORS, BCrypt
│   └── SetupController.java     # /api/setup/reset-users utility endpoint
│
├── controller/
│   ├── AuthController.java      # POST /api/auth/login, /api/auth/set-password
│   ├── DashboardController.java # GET  /api/dashboard/stats
│   ├── DepartmentController.java
│   ├── EmployeeController.java  # CRUD + /generate-temp-password
│   └── LeaveController.java
│
├── dto/
│   ├── auth/
│   │   ├── AuthResponse.java    # Includes requiresPasswordChange flag
│   │   ├── LoginRequest.java
│   │   └── SetPasswordRequest.java
│   ├── DashboardStats.java
│   ├── DepartmentDto / DepartmentRequest
│   ├── EmployeeDto.java         # Includes temporaryPassword on creation response
│   ├── EmployeeRequest.java
│   ├── LeaveRequestDto / LeaveRequestCreate
│
├── entity/
│   ├── Department.java
│   ├── Employee.java
│   ├── LeaveRequest.java
│   └── User.java                # passwordSet, temporaryPassword, tempPasswordExpiry
│
├── enums/
│   ├── EmploymentStatus.java    # ACTIVE, INACTIVE, ON_LEAVE, TERMINATED, PROBATION
│   ├── Gender.java
│   ├── LeaveStatus.java         # PENDING, APPROVED, REJECTED, CANCELLED
│   ├── LeaveType.java
│   └── Role.java                # ROLE_ADMIN, ROLE_HR, ROLE_EMPLOYEE
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
│
├── repository/
│   ├── DepartmentRepository.java
│   ├── EmployeeRepository.java  # Custom search + stats queries
│   ├── LeaveRequestRepository.java
│   └── UserRepository.java
│
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtils.java
│   └── UserDetailsServiceImpl.java
│
└── service/
    ├── AuthService.java         # Login (temp + permanent), set-password
    ├── DashboardService.java
    ├── DepartmentService.java
    ├── EmployeeService.java     # Auto-generates temp password on create
    └── LeaveService.java
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+ (or use H2 for dev/testing)

---

## Setup & Run

### 1. Database Setup (MySQL)

```sql
CREATE DATABASE ems;
```

Run the schema migration to add new columns if upgrading:

```sql
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS temporary_password VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS temp_password_expiry DATETIME NULL,
  ADD COLUMN IF NOT EXISTS password_set TINYINT(1) NOT NULL DEFAULT 0;
```

### 2. Configure `application.properties`

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ems
spring.datasource.username=root
spring.datasource.password=yourpassword

# JPA
spring.jpa.hibernate.ddl-auto=update

# JWT
app.jwt.secret=empora_super_secret_key_that_is_at_least_256_bits_long
app.jwt.expiration=86400000

# CORS
app.cors.allowed-origins=http://localhost:3000
```

### 3. Run

```bash
mvn spring-boot:run
```

Server starts on **http://localhost:8080**

---

## Default Accounts

On every startup, `DataSeeder` ensures these accounts exist:

| Role | Email | Password |
|---|---|---|
| Admin | admin@empora.com | Empora@Admin1 |
| HR | hr@empora.com | Empora@Hr2024 |

> If login fails, reset passwords via:
> `GET http://localhost:8080/api/setup/reset-users?adminPwd=Empora@Admin1&hrPwd=Empora@Hr2024`

---

## API Reference

### Auth

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login (temp or permanent password) |
| POST | `/api/auth/set-password` | JWT required | Employee sets permanent password |

### Employees

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/employees` | All roles | List employees (search, filter, paginate) |
| GET | `/api/employees/{id}` | All roles | Get employee by ID |
| POST | `/api/employees` | ADMIN, HR | Create employee (auto-generates temp password) |
| PUT | `/api/employees/{id}` | ADMIN, HR | Update employee |
| DELETE | `/api/employees/{id}` | ADMIN only | Delete employee |
| PATCH | `/api/employees/{id}/status` | ADMIN, HR | Update employment status |
| POST | `/api/employees/{id}/generate-temp-password` | ADMIN only | Generate new temp password |

### Departments

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/departments` | All roles | List all departments |
| POST | `/api/departments` | ADMIN, HR | Create department |
| PUT | `/api/departments/{id}` | ADMIN, HR | Update department |
| DELETE | `/api/departments/{id}` | ADMIN only | Delete department |

### Leave Requests

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/leave-requests` | All roles | List (filter by status) |
| POST | `/api/leave-requests` | All roles | Submit leave request |
| PATCH | `/api/leave-requests/{id}/review` | ADMIN, HR | Approve or reject |
| PATCH | `/api/leave-requests/{id}/cancel` | All roles | Cancel request |

### Dashboard

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/dashboard/stats` | All roles | Aggregate stats for dashboard |

---

## Employee Temporary Password Flow

```
Admin creates employee
        ↓
Backend auto-generates secure 12-char password
        ↓
User account created with passwordSet = false, expiry = now + 1 hour
        ↓
Admin copies password from response and shares with employee
        ↓
Employee logs in with temp password → JWT returned with requiresPasswordChange: true
        ↓
Employee calls POST /api/auth/set-password with temp + new password
        ↓
passwordSet = true, temporaryPassword cleared, fresh JWT returned
        ↓
Employee uses permanent password for all future logins
```

If the temp password expires before the employee logs in, the admin uses
`POST /api/employees/{id}/generate-temp-password` to issue a new one.

---

## Security Notes

- Passwords are BCrypt-hashed (cost factor 10)
- JWT tokens signed with HS256, configurable expiry (default 24h)
- Role-based method security via `@PreAuthorize`
- CORS restricted to configured origins
- Temp passwords stored as plain text only during the 1-hour window, then cleared
