# Razmena sličica

Radni skelet. Ktor servira stranicu, Flyway pokrene migraciju na Postgres, Exposed se poveže na istu bazu.

## Preduslovi

- JDK 21
- Docker, sa Compose dodatkom

## Pokretanje

Baza mora da radi pre aplikacije. Ako Postgres nije podignut, `./gradlew run` staje i ispisuje da prvo pokreneš Compose.

```bash
docker compose up -d
```

Sačekaj da kontejner bude zdrav (`docker compose ps`), pa:

```bash
./gradlew run
```

Sajt: http://127.0.0.1:43123

Provera migracije: http://127.0.0.1:43123/health

Compose diže Postgres 16 na portu 5432, bazu `razmena`, korisnika `razmena` i lozinku `razmena`. Aplikacija sluša na `0.0.0.0:43123` i te podrazumevane podatke koristi sama. Drugačiju bazu možeš da zadaš preko `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` i `DB_PASSWORD`.
