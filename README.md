# University of Engineers — Student Management System (Spring Boot)

This project is a **full-stack** Student Management System for a fictional **University of Engineers**, built with:

- Spring Boot (REST API)
- Spring Security **Role-Based Access Control** (2 roles: **STUDENT**, **TEACHER**)
- PostgreSQL
- **Liquibase** migrations
- Docker + docker-compose
- Minimal HTML/JS UI (`/`, `/student.html`, `/teacher.html`)
- Swagger/OpenAPI docs (`/swagger-ui.html`)

## 1) Entity model & practical cardinalities

**Entities:** `Department`, `Student`, `Teacher`, `Course`, `Enrollment`, plus an auth table `UserAccount`.

**Cardinality rules implemented (practical university setup):**
- `Department 1..M Student` (one department has many students)
- `Department 1..M Teacher` (one department has many teachers)
- `Department 1..M Course` (one department offers many courses)
- `Teacher 1..M Course` (one teacher can teach many courses)
- `Student M..M Course` implemented through `Enrollment` join entity
  - `Student 1..M Enrollment`
  - `Course 1..M Enrollment`

`Enrollment` also stores real-world fields like `status` and `grade`.

## 2) Roles, RBAC, and “mature” authorization

### Roles
- **STUDENT**
- **TEACHER**

### Key real-life rules enforced
- ✅ **A student cannot delete/disable their own account.**  
  The **only** delete/disable endpoint for students is teacher-only: `DELETE /api/students/{id}`.
- ✅ **Students cannot “sign up as teacher”.**  
  The public registration endpoint is **only** `/api/auth/register`, and it creates a **STUDENT** account.  
  There is **no** public teacher sign-up endpoint.
- ✅ **Teachers have privileges students do not**, e.g.:
  - Department CRUD
  - Course CRUD
  - View all students
  - Disable student accounts
  - Reset student passwords
  - Create teacher accounts

### Security implementation
- **JWT-based authentication** (Bearer token)
- **Role checks** via `@PreAuthorize("hasRole('TEACHER')")` / `hasRole('STUDENT')`

## 3) Run the system

### Option A — Docker (recommended)
```bash
docker compose up --build
```

Then open:
- UI: http://localhost:8080/
- Swagger: http://localhost:8080/swagger-ui.html

### Option B — Local run (Maven)
1) Start PostgreSQL and create DB/user, or use Docker for DB only.
2) Set environment variables:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=ue_sms
export DB_USER=ue_sms
export DB_PASSWORD=ue_sms_password
export JWT_SECRET="please_change_me_please_change_me_please_change_me_1234"
```
3) Run:
```bash
mvn spring-boot:run
```

## 4) Bootstrap teacher (first login)

On first run, the system auto-creates a bootstrap teacher if there are no teachers yet:

- Email: `admin.teacher@ue.edu`
- Password: `ChangeMe123!`

Change these via environment variables in `docker-compose.yml`:
- `APP_BOOTSTRAP_TEACHER_EMAIL`
- `APP_BOOTSTRAP_TEACHER_PASSWORD`

## 5) API quick examples

### Student register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"student1@ue.edu",
    "password":"Passw0rd!",
    "fullName":"Student One",
    "departmentId":1
  }'
```

### Login (student or teacher)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin.teacher@ue.edu","password":"ChangeMe123!"}'
```
