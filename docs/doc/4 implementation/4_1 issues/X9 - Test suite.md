# Integration Test Suite

## Context

The project has 82 unit tests that mock all external dependencies (Fyodor subprocess, EpubLoader, etc.), but nothing tests the pipeline end-to-end with real data. The goal is a regression harness that detects if location mapping regresses, output changes, or calibration math breaks — using real (obfuscated) fixtures committed to the repo.

---

## Fixtures (user provides, committed to repo)

**Location:** `src/test/resources/integration/`

| File | Description |
|------|-------------|
| `book.epub` | Obfuscated real EPUB (stripped/changed content, structure intact) |
| `calibration.txt` | Known Kindle heading→location pairs for the test EPUB (≥3 entries) |
| `clippings.jsonl` | Pre-baked Fyodor JSON lines — one JSON object per line, obfuscated content. Locations must fall within the EPUB's location range. |
| `template.ftl` | Simple FreeMarker template used across all rendering tests |
| `expected-output.md` | Golden file — generated on first run, then committed. Updated intentionally when output is expected to change. |

**`template.ftl` suggested format** (simple, stable, hashable):
```
${title}
<#list groups as g><#if g.clippings?has_content>
== ${g.heading.title} [${g.heading.location}] ==
<#list g.clippings as c>${c.type}: ${c.content}
</#list></#if></#list>
```

---

## New Test Class

**`src/test/java/io/github/vlovric/kindleparser/integration/PipelineIntegrationTest.java`**  
Package: `io.github.vlovric.kindleparser.integration`

Uses JUnit 5 + Mockito (already in pom.xml). No new dependencies needed.

---

## Test Cases

### 1. `fullPipeline_rendersGoldenOutput()`
Verifies nothing silently broke in rendering.

- Build `AppArgs` pointing at `book.epub`, `calibration.txt`, `template.ftl` from test resources; output to `@TempDir`
- Mock `FyodorClippingsParser` via `mockConstruction` to return a `FyodorParseResult` loaded from `clippings.jsonl`
- Call `new Pipeline(appArgs).run()`
- Read actual output; normalize line endings
- Compare normalized content against `expected-output.md` — assert equal
- **Updating the golden file:** delete `expected-output.md` and re-run once; the test writes it and passes; commit the new file

### 2. `calibrationAccuracy_allPointsWithinOneLoc()`
Verifies the byte-offset → location math stays accurate after changes.

- Manually run the pipeline steps up to and including `ResolveHeadingsStep` (no Fyodor needed):
  ```java
  PipelineContext ctx = new PipelineContext(null);
  new PreprocessBookStep(args).execute(ctx);
  new LoadEpubStep().execute(ctx);
  new ParseTocStep().execute(ctx);
  new FitCalibrationStep(args).execute(ctx);
  new ResolveHeadingsStep().execute(ctx);
  ```
- Parse `calibration.txt` for the known `heading → expected location` pairs
- For each calibration point, find the matching resolved heading and assert `|resolved - expected| <= 1`
- Also assert `rmse <= 2.0` (adjust threshold once real fixtures are in place)

### 3. `headingsOnly_containsAllHeadings()`
Verifies `--headings-only` mode produces correct output.

- Build `AppArgs` with `headingsOnly=true`, output path in `@TempDir`
- Run the partial pipeline (steps 1–6, same as test 2) plus `HeadingsOnlyStep`
- Read output file; assert every heading title from `ctx.tocEntries` appears in the output

### 4. `printCalibrationTemplate_containsAllTocEntries()`
Verifies the calibration template generator works end-to-end.

- Build `AppArgs` with `printCalibrationTemplate` pointing to `@TempDir/calib-template.txt`
- Run pipeline (exits early at `PrintCalibrationTemplateStep`)
- Assert file exists and each TOC entry title appears in it

---

## FyodorClippingsParser Mock Pattern

```java
// Load synthetic clippings from fixture
List<Clipping> syntheticClippings = loadClippingsFromJsonl(
    getClass().getResourceAsStream("/integration/clippings.jsonl"));

FyodorParseResult fakeResult = new FyodorParseResult(
    syntheticClippings, null, null, "Test Book Title");

try (var mocked = mockConstruction(FyodorClippingsParser.class,
        (mock, ctx) -> when(mock.parse(any(), any(), any(), any()))
            .thenReturn(fakeResult))) {
    new Pipeline(appArgs).run();
}
```

A private helper `loadClippingsFromJsonl(InputStream)` deserializes using Jackson's `ObjectMapper` (already a project dependency).

---

## Golden File Update Workflow

When a deliberate change affects output (e.g. template logic, grouping changes):
1. Delete `src/test/resources/integration/expected-output.md`
2. Run `mvn test -Dtest=PipelineIntegrationTest#fullPipeline_rendersGoldenOutput`
3. The test writes the actual output as the new golden file and passes
4. Inspect the diff, commit if correct

This is implemented by having the test check `if (!goldenPath.toFile().exists()) { Files.copy(actual, golden); return; }`.

---

## Temporary Artifact Handling

- All outputs (`--output`, `--headings-template` output) go to `@TempDir` — JUnit deletes them automatically
- `--debug` is always `false` in integration tests — no `debug-runs/` directories created
- Fyodor is mocked — no `~/.config/fyodor/` writes, no temp dirs created by Fyodor
- `EpubLoader` unzips to a temp dir internally; `PipelineContext` implements `AutoCloseable` and cleans it up — handled by `Pipeline.run()` via try-with-resources

---

## Files to Create

| File | Action |
|------|--------|
| `src/test/java/io/github/vlovric/kindleparser/integration/PipelineIntegrationTest.java` | Create |
| `src/test/resources/integration/template.ftl` | Create |
| `src/test/resources/integration/book.epub` | You provide (obfuscated real EPUB) |
| `src/test/resources/integration/calibration.txt` | You provide |
| `src/test/resources/integration/clippings.jsonl` | You provide |
| `src/test/resources/integration/expected-output.md` | Auto-generated on first run |

---

## Running

```bash
# Integration tests only
mvn test -Dtest=PipelineIntegrationTest

# Full suite
mvn test
```

All tests should pass with 0 failures. The golden file test writes `expected-output.md` on first run; subsequent runs compare against it.
# Note
- watch out for copyright when uploading epubs to resource