package com.universityofengineers.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudentManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementSystemApplication.class, args);
    }
}

// docker-compose down -v
// docker-compose up --build

//using docker CLI to view table
// docker exec -it ue_sms_db psql -U ue_sms -d ue_sms

/*
-- List all tables
\dt

-- Describe a specific table structure
\d courses
\d students
\d teachers
\d departments

-- View all data from a table
SELECT * FROM courses;
SELECT * FROM students;
SELECT * FROM teachers;
SELECT * FROM departments;

-- Count records in a table
SELECT COUNT(*) FROM students;

-- View table with limit
SELECT * FROM courses LIMIT 10;

-- Exit psql
\q


ue_sms=# \dt
         List of relations
 Schema |    Name     | Type  | Owner
--------+-------------+-------+--------
 public | courses     | table | ue_sms
 public | departments | table | ue_sms
 public | students    | table | ue_sms
 public | teachers    | table | ue_sms

ue_sms=# SELECT * FROM courses;
 id | name | credit | department_id
----+------+--------+---------------
(0 rows)

ue_sms=# \q
*/