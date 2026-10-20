# Production Work Order System (PWOS)

A web application for tracking stock items and production work orders, built as a 15-week DevOps course project. Every push to `main` is built, tested with Selenium, packaged as a Docker image, released to a registry, backed up, deployed and provisioned with Ansible — automatically, through Jenkins.

**Status:** Complete. Latest release: build #36 (`pwos:36`).

## Features

- **Item catalogue:** add, edit, search (name or SKU), delete; low-stock alert
- **Work orders:** create, edit, change status (Pending → In Progress → Done), cancel, delete; search by item plus status filter; overdue alert
- **Stock rule:** marking an order *Done* adds its quantity to the item's stock; reopening takes it back
- **Dashboard** with live counts and alerts
- **Login with four roles** (Spring Security, BCrypt-hashed passwords)
- **History page:** audit log of who changed what and when
- **Admin user management:** create users, change roles, reset passwords, enable/disable logins
- **Change my password** for every user
- **Date-range filter** on work orders (due from / to)
- **Dashboard charts:** orders by status, stock vs reorder threshold
- **CSV export** of items, orders and history (formula-injection safe)
- **Pagination:** 20 rows per page

| Role | Can do |
|---|---|
| ADMIN | Everything, including delete and History |
| SUPERVISOR | Create, edit, cancel orders; change status |
| CLERK | Add and edit items |
| VIEWER | View everything (including History); change nothing |

## Tech stack

| Layer | Tools |
|---|---|
| Application | Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security, Thymeleaf, Maven |
| Database | PostgreSQL 15 |
| Version control | Git, GitHub (feature branches, pull requests, tags) |
| CI/CD | Jenkins LTS (declarative Jenkinsfile, Poll SCM) |
| Testing | Selenium WebDriver 4, JUnit 5, headless Chrome |
| Containers | Docker, private registry (`registry:2`) |
| Configuration management | Ansible |
| Database migrations | Flyway (versioned SQL in `src/main/resources/db/migration`) |
| Security scanning | Trivy |
| Monitoring | Spring Boot Actuator, Prometheus, Grafana (Docker Compose) |
| Unit testing | JUnit 5, Mockito, JaCoCo |

## Pipeline (Jenkinsfile, 12 stages)

1. Checkout
2. Build: `mvn clean compile`
3. **Unit Tests:** 39 JUnit/Mockito tests + JaCoCo coverage report
4. Package: jar archived
5. Docker Build: `pwos:<build number>` and `latest`
6. **Security Scan (Trivy):** fails on fixable CRITICAL vulnerabilities; HIGH report archived
7. Deploy to Test: container `pwos-test` on 8090
8. Selenium Tests: 5 user journeys, the **quality gate**
9. Push Image: to `localhost:5001`
10. Backup DB: `pg_dump` to `~/pwos-backups`
11. Deploy: container `pwos-live` on 8081
12. Provision Node (Ansible): updates `pwos-node` on 8082

If unit tests, the security scan or any Selenium journey fails, nothing is pushed or deployed.

## Ports

| Service | URL |
|---|---|
| Live app | http://localhost:8081 |
| Ansible node | http://localhost:8082 |
| Jenkins | http://localhost:9090 |
| Registry | http://localhost:5001/v2/pwos/tags/list |
| Grafana (monitoring dashboard) | http://localhost:3000 |
| Prometheus (metrics) | http://localhost:9091 |
| App health / metrics | http://localhost:8081/actuator/health, `/actuator/prometheus` |

## Secrets

No passwords are stored in this repository. `application.properties` reads them from environment variables:

```
DB_PASSWORD
PWOS_ADMIN_PASSWORD
PWOS_SUPERVISOR_PASSWORD
PWOS_CLERK_PASSWORD
PWOS_VIEWER_PASSWORD
```

Keep them in `~/.pwos/pwos.env` (mode 600, never committed). Jenkins reads the same file as the Secret file credential `pwos-env`; Ansible writes it to the node with `no_log`.

## Running locally

1. Create the database (once):
```sql
   CREATE DATABASE pwos_db;
   CREATE USER pwos_user WITH PASSWORD '<DB_PASSWORD>';
   GRANT ALL PRIVILEGES ON DATABASE pwos_db TO pwos_user;
   -- inside pwos_db:
   GRANT ALL ON SCHEMA public TO pwos_user;
```
2. Create `~/.pwos/pwos.env` with the five variables above.
3. Run:
```bash
   . scripts/load-env.sh && mvn spring-boot:run
```
4. Open http://localhost:8080 and sign in (`admin`, `supervisor`, `clerk` or `viewer`).

## Running the full system

After a restart, one script starts PostgreSQL, Docker and its containers, Jenkins and the node service:

```bash
bash ~/start-pwos.sh
```

## Useful commands

```bash
# Selenium journeys against the live app
. scripts/load-env.sh && mvn test -Dtest=PwosJourneysIT -Dbase.url=http://localhost:8081 -Dheadless=true

# Backup / restore
bash scripts/backup-db.sh
bash scripts/restore-db.sh ~/pwos-backups/<file>.sql.gz

# Roll the node to a release (20 or later)
cd ansible && ansible-playbook playbook.yml -e pwos_version=20
```

## Project structure

```
Jenkinsfile                 pipeline as code
Dockerfile                  runtime image (Java 21 JRE, non-root user)
scripts/                    deploy, cleanup, env loading, backup, restore
ansible/                    playbook, inventory, run logs
src/main/java/com/pwos/     item/, workorder/, security/, audit/
src/test/java/.../selenium/ SeleniumBase, ScreenshotOnFailure, PwosJourneysIT
```

## Monitoring

```bash
docker compose -f monitoring/docker-compose.yml up -d
```

Grafana opens the **PWOS Monitoring** dashboard: servers up, 5xx errors, requests per second, response time, JVM heap and DB connections for both servers.

## Known limitations

- Runs on a single machine; Jenkins polls GitHub only while it is awake
- Plain HTTP; a few seconds of downtime per deployment
- Rollback works to release 20 or later (older images use the old database password); schema changes are now versioned with Flyway
