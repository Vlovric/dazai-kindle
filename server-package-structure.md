# Server package structure

Package by resource — matches the convention the `core` module already uses (feature packages like `pipeline`, `toc`, `calibration`, not technical layers). Each resource package holds more than just a controller though.

```
io.github.vlovric.dazaikindle
├── ServerApplication.java
├── stats/
│   ├── StatsController.java
│   ├── StatsService.java
│   └── dto/
│       └── StatsResponse.java
├── paths/
│   ├── PathsController.java
│   ├── PathsService.java
│   └── dto/
│       ├── PathResponse.java
│       └── UpdatePathRequest.java
├── templates/
│   ├── TemplatesController.java
│   ├── TemplatesService.java
│   └── dto/
│       ├── TemplateResponse.java
│       ├── TemplateListResponse.java
│       └── ...
├── runs/
├── clippings/
├── files/
├── execute/
│   ├── ExecuteController.java
│   ├── ExecuteService.java
│   ├── RunRegistry.java        (tracks active runId → SseEmitter/log state)
│   └── dto/
└── common/
    ├── exception/
    │   ├── NotFoundException.java
    │   ├── InvalidRequestException.java
    │   └── GlobalExceptionHandler.java   (@RestControllerAdvice)
    └── storage/                          (shared filesystem access helpers)
```

## Why not "controller only" per resource

- **Controller** — HTTP concerns only: map request → DTO, call service, map result → response, set status codes. No filesystem/business logic in here.
- **Service** — the actual work: reading/writing files, validation, building response data. This is where "template with same name already exists" (409) or "run not found" (404) checks live, since your API spec has no database (`3_1 schema.sql.md` is empty) — everything is filesystem-backed, so services will mostly wrap `Path`/`Files` operations rather than JPA repositories.
- **dto package** — request/response records, kept separate from your `core` module's domain models (`Clipping`, `TocEntry`, etc.) so the API contract doesn't leak into or get constrained by core parsing logic.
- **common** — cross-cutting stuff every resource needs: a global exception handler mapping your custom exceptions to the 400/404/409/422 codes in the spec, and (if file-path logic ends up shared, e.g. resolving a run's artifact directory) a shared storage helper.

## Exception: `execute`

Unlike the other resources, `execute` isn't just CRUD — it kicks off async pipeline runs and streams logs via SSE. That package needs something to track in-flight runs (a `runId → SseEmitter` map, likely in a `RunRegistry` or similar), which the other resource packages don't need.

## Skip a repository layer

There's no database, so a repository abstraction over the filesystem would be unnecessary indirection; the service can talk to `Path`/`Files` directly.
