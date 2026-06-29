# Spring Boot GUI Server — Project Plan

## Recommendation: Maven multi-module monorepo

Keep everything in this repository. Convert it to a Maven multi-module project so the CLI and the new Spring Boot server share the same core parsing library. A single GitHub release tag will attach both JARs.

**Why not a separate repo?** The server needs the same parsing logic. Sharing it as a Maven module is cleaner than shelling out to the CLI as a subprocess (no temp-file juggling, no process management, fully testable).

**Why not a branch?** Branches are for parallel development of the same product, not for separate deliverables.

---

## Target Structure

```
KindleParser/                   ← repo root, parent POM only
├── pom.xml                     ← parent POM (packaging=pom, manages versions/deps)
├── core/                       ← shared parsing library
│   ├── pom.xml
│   └── src/                    ← current src/ minus Main.java and CLI args wiring
├── cli/                        ← thin CLI wrapper
│   ├── pom.xml                 ← depends on core
│   └── src/                    ← Main.java + args4j wiring only
└── server/                     ← Spring Boot app
    ├── pom.xml                 ← depends on core + spring-boot-starter-web
    └── src/
        └── main/java/...       ← controllers, services, DTOs
```

---

## Migration Steps

### 1. Promote root `pom.xml` to parent POM
- Change `<packaging>` to `pom`
- Add `<modules>` listing `core`, `cli`, `server`
- Move shared dependency versions into `<dependencyManagement>`
- Remove the shade plugin from parent (it belongs in `cli` and `server`)

### 2. Create `core/` module
- Move all of `src/main/java/**` (except `Main.java`) → `core/src/main/java/`
- Move `src/test/java/**` → `core/src/test/java/`
- Move `src/main/resources/` → `core/src/main/resources/`
- Keep jsoup, freemarker, jackson, junit, mockito here
- Remove args4j (CLI-only concern)
- Packaging: `jar` (plain library, no shade)

### 3. Create `cli/` module
- Depends on `core`
- Contains only `Main.java` + args4j CLI wiring
- Uses `maven-shade-plugin` → produces `dazai-kindle-cli-<version>.jar`

### 4. Create `server/` module
- Depends on `core`
- Uses `spring-boot-maven-plugin` → produces `dazai-kindle-server-<version>.jar`
- Service layer calls core pipeline classes directly — no subprocess

### 5. Update GitHub Actions release workflow
- Root `mvn package -DskipTests` builds all modules
- Attach both artifacts to the release:
  - `cli/target/dazai-kindle-cli-*.jar`
  - `server/target/dazai-kindle-server-*.jar`

---

## Server API Sketch

```
POST /api/parse          upload clippings file + epub, run pipeline, return result
GET  /api/templates      list available FreeMarker templates
POST /api/calibrate      run calibration step
```

Controllers delegate to the same pipeline/service classes from `core` that the CLI uses today.

---

## Verification

- `mvn package -DskipTests` at root produces both fat JARs
- CLI: `java -jar cli/target/dazai-kindle-cli-*.jar --help` works as before
- Server: `java -jar server/target/dazai-kindle-server-*.jar` starts on port 8080
- All existing tests pass: `mvn test`
- Release workflow: push a tag and confirm both JARs appear as release assets
