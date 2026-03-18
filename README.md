
# University of Engineers — Student Management System (Spring Boot)

This project is a **full-stack** Student Management System for a fictional **University of Engineers**, built with:

- Spring Boot (REST API)
- Spring Security **Role-Based Access Control** (2 roles: **STUDENT**, **TEACHER**)
- PostgreSQL
- **Liquibase** migrations
- Docker + docker-compose
- Minimal HTML/JS UI (`/`, `/student.html`, `/teacher.html`)
- Swagger/OpenAPI docs (`/swagger-ui.html`)

---

## 1) Entity model & practical cardinalities

**Entities:**  
`Department`, `Student`, `Teacher`, `Course`, `Enrollment`, plus an auth table `UserAccount`.

### Cardinality rules implemented (practical university setup):

- `Department 1..M Student` (one department has many students)
- `Department 1..M Teacher` (one department has many teachers)
- `Department 1..M Course` (one department offers many courses)
- `Teacher 1..M Course` (one teacher can teach many courses)
- `Student M..M Course` implemented through `Enrollment` join entity:
  - `Student 1..M Enrollment`
  - `Course 1..M Enrollment`

`Enrollment` also stores real-world fields like `status` and `grade`.

---

## 2) Roles, RBAC, and authorization rules

### Roles
- **STUDENT**
- **TEACHER**

### Real-life rules enforced

- A student cannot delete or disable their own account.
- The only delete/disable endpoint for students is teacher-only:  
  `DELETE /api/students/{id}`
- Students cannot sign up as teacher.
- The public registration endpoint is only `/api/auth/register`, and it creates a **STUDENT** account.
- There is no public teacher sign-up endpoint.

### Teacher-only privileges

- Department CRUD
- Course CRUD
- View all students
- Disable student accounts
- Reset student passwords
- Create teacher accounts

### Security implementation

- JWT-based authentication (Bearer token)
- Role checks via:
  ```java
  @PreAuthorize("hasRole('TEACHER')")
  @PreAuthorize("hasRole('STUDENT')")

---

## 3) Run the system

### Option A — Docker (recommended)

```bash
docker compose up --build
```

Then open:

* UI: [http://localhost:8080/](http://localhost:8080/)
* Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Option B — Local run (Maven)

Start PostgreSQL and create DB/user, or use Docker for DB only.

#### Set environment variables:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=ue_sms
export DB_USER=ue_sms
export DB_PASSWORD=ue_sms_password
export JWT_SECRET="please_change_me_please_change_me_please_change_me_1234"
```

#### Run:

```bash
mvn spring-boot:run
```

---

## 4) Run the project in IntelliJ IDEA

### Prerequisites

* IntelliJ IDEA installed
* JDK 17+ installed
* Maven (or IntelliJ bundled Maven)
* PostgreSQL running OR Docker available

---

### Open the project

1. Open IntelliJ IDEA
2. Click **Open**
3. Select the `Student_Management1` project folder
4. Let IntelliJ import dependencies

---

### Configure JDK

* Go to `File > Project Structure > Project`
* Set **Project SDK** to your JDK
* Adjust language level if needed

---

### Configure environment variables

Go to:

`Run > Edit Configurations`

Add:

```
DB_HOST=localhost;
DB_PORT=5432;
DB_NAME=ue_sms;
DB_USER=ue_sms;
DB_PASSWORD=ue_sms_password;
JWT_SECRET=please_change_me_please_change_me_please_change_me_1234
```

---

### Run from IntelliJ

* Open main Spring Boot class → click **Run**
* OR use `Run > Run...`

---

### Run PostgreSQL with Docker only

Start DB container first:

```bash
docker compose up db
```

Then run Spring Boot from IntelliJ.

---

### Access after startup

* UI: [http://localhost:8080/](http://localhost:8080/)
* Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 5) Bootstrap teacher (first login)

On first run, the system auto-creates a bootstrap teacher if none exists:

* Email: `admin.teacher@ue.edu`
* Password: `ChangeMe123!`

### Customize via environment variables:

* `APP_BOOTSTRAP_TEACHER_EMAIL`
* `APP_BOOTSTRAP_TEACHER_PASSWORD`

---

## 6) API quick examples

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

---

### Login (student or teacher)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin.teacher@ue.edu",
    "password":"ChangeMe123!"
  }'


