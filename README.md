# Nhajc Server

The backend of Nhajc, a music streaming app. It runs in Docker: the server, PostgreSQL and MinIO (file storage).

## Docs

- `docs/api.md`: every endpoint
- `docs/use-cases.md`: which screen calls which endpoint
- `docs/postman/`: Postman collection and sample data

## Build and Run

### Requirements

- Docker with Docker Compose

### 1. Setup

Copy the example settings file:

```sh
cp .env.example .env
# Fill the empty values in `.env`
```

### 2. Run

```sh
docker compose up --detach --build
```

| Port | Service |
|---|---|
| 8080 | API (http://<address>:8080/api/v1/) |
| 9000 | MinIO (images and audio) |
| 9001 | MinIO web console |
| 5432 | PostgreSQL |


### Other commands

* Shows the containers: `docker compose ps`
* Shows the server log: `docker compose logs --follow server`
* Stops everything: `docker compose down`
* Stops everything and deletes all data: `docker compose down --volumes`
