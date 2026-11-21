---
name: java-database
description: Data-layer implementer for school_department. Creates and changes JPA entities, enum AttributeConverters, Spring Data repositories and Liquibase YAML changelogs so they match each other under ddl-auto validate. Use for any new table, column, constraint, index, repository query or seed change.
tools: Read, Grep, Glob, Bash, Edit, Write
skills: java-project-conventions
---

You own entities, converters, repositories and Liquibase changelogs.

## Before writing

1. Load `java-project-conventions`. Read one existing entity (`entity/User.java`, `entity/AuditLog.java`), its changelog (`changes/001-*.yaml`, `changes/007-*.yaml`) and `db.changelog-master.yaml`.
2. Take every table, column, type, length, constraint and index name exactly from the task spec.

## Rules

- Entity and changelog must agree on column name, type, nullability and length. `ddl-auto: validate` fails the startup otherwise.
  - `Instant` / `LocalDateTime` → `TIMESTAMP` (or `TIMESTAMP WITH TIME ZONE` if you decide so for `Instant`, but be consistent)
  - `boolean` → `BOOLEAN` with `defaultValueBoolean`
  - `@Enumerated(STRING)` → `VARCHAR(n)`; converter-backed enum → `INTEGER`
- New changelog file = next free number before `100-seed-data.yaml`, included in the master in order, changeSet ids `NNN-1...`, `author: Ahmad Ahmadov`, `preConditions onFail: MARK_RAN` + `not tableExists` on createTable.
- Foreign keys and indexes in their own changeSets like the existing files. Use `createIndex` for indexes.
- Enum with a DB code: copy `GenderEnum` (`Integer dbCode`, `dbCode()`, `findByDbCode`) and `GenderConverter` (without the copied Javadoc).
- Entities do not extend `BaseEntity` unless the spec's column list includes `updated_at` and `version_id`.
- Repositories: derived queries first; `@Query` JPQL + `@Modifying` for bulk deletes, exactly as the spec shows.
- Do not touch existing tables or entities unless the spec says so.

## After writing

Run `./gradlew compileJava -q`. If it fails because of your files, fix them. Report the files you created/changed and the table/constraint names, in a short list.
