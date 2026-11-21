---
name: implement-feature-spec
description: Orchestration playbook for implementing a task spec from docs/feature/**.md with the project's Java agent team (java-explorer, java-database, java-auth, java-feature, java-debug, java-test, java-code-reviewer). Use when the user says "implement this md", "bu tapsirigi implement et", or points at a docs/feature spec.
---

# Implement a feature spec with the agent team

The main session is the orchestrator. It reads the spec, splits it, hands each part to the right agent, and checks the result. It does not write the feature code itself.

## Team

| Agent | Use for | Writes code |
| --- | --- | --- |
| `java-explorer` | map existing code a part will touch, find conventions and call sites | no |
| `java-database` | entity, Liquibase changelog, repository, enum converter | yes |
| `java-auth` | Spring Security, JWT, filters, auth services/controllers, 401/403 handling | yes |
| `java-feature` | regular controllers, services, DTOs, mappers, validation, ErrorCode + i18n, Swagger, logging tweaks | yes |
| `java-debug` | compile errors, startup failures, wrong responses: find root cause, apply the minimal fix | yes (fix only) |
| `java-test` | build, start app, walk the spec checklist with curl/psql/log grep | no (no test files) |
| `java-code-reviewer` | review the diff against spec + conventions | no |

## Steps

1. **Read the spec fully.** List its numbered parts, required files, exact names (tables, constraints, error codes, bundle keys), the recommended order, and its checklist. Note what is bonus or process-only (branches, PRs, screenshots): skip those unless the user asks, and say so at the end.
2. **Explore once.** One `java-explorer` run with the list of files/areas the spec touches. Keep its summary; pass relevant parts to later agents instead of re-exploring.
3. **Plan the split** in the spec's recommended order. Each agent prompt gets: the spec path and the exact section numbers it owns, the files it may create or edit, the names it must use, what other agents already did (file list), and "load the java-project-conventions skill" (plus `spring-security-jwt` for security work). Run independent parts in parallel only when they touch different files.
4. **Build after each write step.** If `./gradlew compileJava` fails, send the error to `java-debug`.
5. **Verify.** `java-test` starts the app and walks the spec's checklist. FAILs go to `java-debug` with the failing item.
6. **Review.** `java-code-reviewer` on the final diff. Fix only findings that break the spec or the conventions, through the agent that owns the file.
7. **Report** to the user: what was implemented per spec part, checklist PASS/FAIL, what was skipped (bonus, branches/PRs) and why, any manual step left (env vars, screenshots).

## Rules for every agent prompt

- Stay inside the spec. No extra endpoints, options, abstractions or refactors.
- Hard-coded value sets become enums; repeated names become constants.
- Junior-level code. Comments only where the spec asks or the why is not obvious, in the owner's ASCII Azerbaijani style.
- No unit tests. No commits unless the user asks.
