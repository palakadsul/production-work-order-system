# Production Work Order System (PWOS)

A web application for tracking stock items and production work orders, built as a 15-week DevOps course project. Every push to `main` is built, tested with Selenium, packaged as a Docker image, released to a registry, backed up, deployed and provisioned with Ansible — automatically, through Jenkins.

**Status:** Complete. Final release: build #20 (`pwos:20`).

## Features

- **Item catalogue:** add, edit, search (name or SKU), delete; low-stock alert
- **Work orders:** create, edit, change status (Pending → In Progress → Done), cancel, delete; search by item plus status filter; overdue alert
- **Stock rule:** marking an order *Done* adds its quantity to the item's stock; reopening takes it back
- **Dashboard** with live counts and alerts
- **Login with four roles** (Spring Security, BCrypt-hashed passwords)
- **History page:** audit log of who changed what and when

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

## Pipeline (Jenkinsfile, 10 stages)

1. Checkout
2. Build: `mvn clean compile`
3. Package: jar archived
4. Docker Build: `pwos:<build number>` and `latest`
5. Deploy to Test: container `pwos-test` on 8090
6. Selenium Tests: 5 user journeys, the **quality gate**
7. Push Image: to `localhost:5001`
8. Backup DB: `pg_dump` to `~/pwos-backups`
9. Deploy: container `pwos-live` on 8081
10. Provision Node (Ansible): updates `pwos-node` on 8082

If any Selenium journey fails, nothing is pushed or deployed.

## Ports

| Service | URL |
|---|---|
| Live app | http://localhost:8081 |
| Ansible node | http://localhost:8082 |
| Jenkins | http://localhost:9090 |
| Registry | http://localhost:5001/v2/pwos/tags/list |

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

## Known limitations

- Runs on a single machine; Jenkins polls GitHub only while it is awake
- Plain HTTP; a few seconds of downtime per deployment
- Rollback works to release 20 or later (older images use the old database password)
