---
name: run-and-verify-api
description: How to build, start and exercise the school_department API locally (Postgres via docker compose, env vars, gradle bootRun, curl with the /test-demo context path, psql checks, log grep). Load when you need to compile the project, run the app, reproduce a bug, or walk a spec's verification checklist.
---

# Run and verify the API

## 1. Build only (fast check)

```bash
cd /home/impr/projects/abb_teach_java/school_department
./gradlew compileJava -q          # compile errors only
./gradlew build -x test -q        # full build without tests
```

Read the first error, not the last. MapStruct and Lombok errors show up as "unmapped target property" or "cannot find symbol get...".

## 2. Database

```bash
docker compose up -d              # container: school-postgres, db school_db, postgres/postgres
docker exec school-postgres pg_isready -U postgres
PGPASSWORD=postgres psql -h localhost -U postgres -d school_db -c "select ..."
```

Never run `docker compose down -v` without asking: it deletes the data volume.

## 3. Start the app in the background

Secrets come from env vars. For local runs generate throwaway values, do not write them into tracked files:

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export ADMIN_PASSWORD='Admin1234'
./gradlew bootRun > /tmp/school-app.out 2>&1 &   # or use the Bash tool's run_in_background
# wait until started
until grep -qE "Started SchoolDepartmentApplication|APPLICATION FAILED" /tmp/school-app.out; do sleep 2; done
```

If startup fails, read the "APPLICATION FAILED TO START" block and the first `Caused by:`. Typical causes: Liquibase changelog error, `ddl-auto: validate` mismatch (entity column vs table), missing env var, bean cycle.

Stop it afterwards (`pkill -f SchoolDepartmentApplication` or stop the background task).

## 4. Calling endpoints

Base URL: `http://localhost:8080/test-demo`

```bash
B=http://localhost:8080/test-demo
curl -s -i "$B/api/students/?lang=rus"
curl -s -X POST "$B/api/auth/login" -H 'Content-Type: application/json' -d '{"login":"admin","password":"Admin1234"}'
curl -s "$B/api/students/" -H "Authorization: Bearer $ACCESS"
```

Note the trailing slash on some controllers (`@PostMapping("/")`, `@GetMapping("/")`). Check the controller mapping before calling.

Use `jq` to pull fields: `ACCESS=$(... | jq -r .accessToken)`.

## 5. Checking results

- Response: status line, `code`, `message` language, `traceId` present
- DB: `psql` select on the touched tables
- Logs: `logs/app.log` (project dir). Log pattern includes `[traceId]` and, after security, `[username]`
- Secrets never in logs: `grep -E 'Admin1234|eyJ' logs/app.log` must print nothing

## 6. Report format

For each checklist item: number, command (short), expected, actual, PASS/FAIL. For a FAIL include the response body and the relevant log lines, nothing more.
