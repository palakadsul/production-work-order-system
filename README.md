# Production Work Order System

A CI/CD-driven production work order management system built as part of a DevOps course project — covering the full pipeline from Git to Jenkins, Selenium, Docker, and Ansible/Puppet.

## Tech Stack
- Java 21, Spring Boot 4.1
- PostgreSQL
- Thymeleaf (server-rendered UI)
- Maven

## MVP Scope
- Item catalogue (CRUD)
- Create/update work order transactions
- Stock/order status tracking
- Search/filter
- Exception alerts (low stock / overdue orders)

## Local Setup
1. Ensure Java 21 and PostgreSQL are installed
2. Create the database:
```sql
   CREATE DATABASE pwos_db;
   CREATE USER pwos_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE pwos_db TO pwos_user;
```
3. Update `src/main/resources/application.properties` with your DB credentials
4. Run:
```bash
   mvn spring-boot:run
```
5. Visit `http://localhost:8080`

## Project Status
   In development — Week 4 of 15 (Git/CI-CD setup phase)
