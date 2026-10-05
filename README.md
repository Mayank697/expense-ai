# Personal Expense AI

Local-first personal expense tracker and AI assistant built with Spring Boot, Spring AI, PostgreSQL/pgvector, and Ollama.

## Stack

- Java 21
- Spring Boot 4.0.8
- Spring AI 2.0.1
- PostgreSQL + pgvector
- Ollama
- Liquibase
- Maven

## Start infrastructure

```bash
docker compose up -d
```

If Ollama is running in Docker, pull the models:

```bash
docker exec -it expense-ai-ollama ollama pull llama3.2
docker exec -it expense-ai-ollama ollama pull nomic-embed-text
```

Then start the application:

```bash
mvn spring-boot:run
```

Health check:

```text
http://localhost:8080/actuator/health
```

## Planned implementation

1. Expense CRUD
2. CSV/bank-statement import
3. Expense analytics
4. AI expense categorization
5. Spring AI tool calling
6. Personal financial notes/knowledge base
7. pgvector RAG
8. Hybrid SQL + RAG assistant
9. React dashboard

## Liquibase XML

The master changelog is `src/main/resources/db/changelog/db.changelog-master.xml`.
Future migrations should be added as new XML changeSets/files under `db/changelog/changes/` and included from the master changelog. Hibernate uses `ddl-auto: validate` so Liquibase owns schema changes.
