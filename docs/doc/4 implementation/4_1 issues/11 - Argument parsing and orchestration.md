# KindleParser — Current Flow & New Pipeline Design

---

## Part 1: Current Flow (Pseudocode)

```
main(args)
  parse args4j onto Main fields
  if parse fails → print usage, exit(1)

  isPrintingCalibrationTemplate = (printCalibrationTemplate != null)

  // Cross-arg validation (scattered, imperative)
  if !isPrintingCalibrationTemplate && calibrate == null → exit(1)
  if !isPrintingCalibrationTemplate && calibrate != null && !calibrate.exists() → exit(1)
  if !isPrintingCalibrationTemplate && !headingsOnly
    if clippings == null → exit(1)
    if template == null  → exit(1)

  try
    // --- DEBUG SETUP ---
    if debug
      create ./debug-runs/run-<timestamp>/
      dbg = new DebugArtifacts(runDir)

    // --- STEP 1: Preprocess book ---
    bookPath = BookPreprocessor.preprocess(book.toPath())
      // if .azw3 or .mobi:
      //   if sibling .epub already exists → return it
      //   else run `ebook-convert` via Calibre subprocess → return new .epub path
      // else → return original path as-is

    // --- STEP 2: Load EPUB (try-with-resources) ---
    open EpubLoader(bookPath)
      extract zip to temp dir
      parse META-INF/container.xml → find OPF path
      parse OPF manifest + spine
      find TOC href (nav.xhtml for EPUB3, toc.ncx for EPUB2)
      parse book title + author from OPF metadata

    epubTitle  = loader.getBookTitle()
    epubAuthor = loader.getBookAuthor()
    if debug → write 01_epub_metadata.json, 01_spine.json

    // --- STEP 3: Parse TOC ---
    tocEntries = TocParserResolver.parse()
      read TOC file bytes from loader
      dispatch to XhtmlTocParser (nav.xhtml) or NcxTocParser (toc.ncx)
      resolve all relative hrefs against OPF root
      return List<TocEntry>

    if debug → write 02_toc_entries.json, 02_toc_source.txt
    if tocEntries.isEmpty() → exit(1)

    // --- EARLY EXIT A: --print-calibration-template ---
    if isPrintingCalibrationTemplate
      build calibration template string from tocEntries
      write to printCalibrationTemplate path
      print success → RETURN (done)

    // --- STEP 4: Fit calibration ---
    CalibrationFit fit = fitCalibrationFromFile(calibrate, loader, tocEntries)
      parse calibration file → Map<headingTitle, kindleLocation>
      create uncalibrated LocationResolver (default 128 bytes/loc)
      for each calibration target:
        fuzzy-match title against tocEntries (exact → prefix → contains)
        resolve byte offset of matched TocEntry via uncalibrated resolver
        collect (byteOffset, kindleLocation) points
      least-squares linear fit → bytesPerLocation, locationBias
      compute RMSE on calibration points
      return CalibrationFit

    // --- STEP 5: Resolve headings ---
    resolver = new LocationResolver(loader, fit.bytesPerLocation(), fit.locationBias())
    resolvedHeadings = resolver.resolve(tocEntries)
      build per-file byte offsets by walking spine in order
      for each TocEntry:
        look up file offset in spine map
        if anchor present → find anchor byte offset within that file (regex on raw HTML)
        convert global byte offset → Kindle location via calibrated formula
      sort headings by location

    if debug → write 03_resolved_headings.json, 03_file_offsets.json
    EpubLoader closed here (end of try-with-resources)

    // --- EARLY EXIT B: --headings-only ---
    if headingsOnly
      print headings table to stdout → RETURN (done)

    // --- STEP 6: Parse clippings (Fyodor subprocess) ---
    FyodorClippingsParser.parse(titleFilter, epubTitle, fyodorOutDir, dbg)
      install/overwrite ~/.config/fyodor/template.erb from bundled resources
      check ~/.config/fyodor/fyodor.toml exists (warn if missing)
      run `fyodor <clippingsPath> <outputDir>` subprocess
      collect all .json/.jsonl output files from outputDir
      score each file against epubTitle + titleFilter → pick best match
      parse selected file line-by-line → List<Clipping>
      sort clippings by location
      return FyodorParseResult

    if debug → write 04_fyodor_stdout.txt, 04_fyodor_out_listing.json,
                       04_selected_book.json, 04_parse_error_line.jsonl (on error),
                       05_clippings_stats.json, 05_clippings_sample.json

    if parsedClippings.isEmpty() → exit(1)

    // --- STEP 7: Group clippings under headings ---
    groups = Grouper.group(parsedClippings, resolvedHeadings)
      build LinkedHashMap: index -1 → "Before first heading" sentinel
                           index 0..N → one HeadingGroup per heading
      for each clipping:
        binary search headings by location → find last heading with loc <= clipping.loc
        add clipping to that group
      return all groups (including empty ones)

    if debug → write 06_grouping_summary.json, 06_before_first_bucket.json

    // --- STEP 8: Render output ---
    resolve output file path (use --output or derive from book title)
    TemplateRenderer.render(groups, title, fileWriter)
      configure FreeMarker (sandboxed, no class resolution)
      map HeadingGroup list → TemplateGroup list (view model)
      process template → write to writer

    print success → DONE

  catch Exception → print stack trace, exit(1)
```

---

## Step inventory

| Step class | Reads from ctx | Writes to ctx | Exits if |
|---|---|---|---|
| `PreprocessBookStep` | — | `bookPath` | — |
| `LoadEpubStep` | `bookPath` | `epubTitle`, `epubAuthor` | — |
| `ParseTocStep` | `bookPath` | `tocEntries` | `tocEntries` empty |
| `PrintCalibrationTemplateStep` | `tocEntries` | — | `--print-calibration-template` set |
| `FitCalibrationStep` | `tocEntries`, `bookPath` | `bytesPerLocation`, `locationBias` | — |
| `ResolveHeadingsStep` | `tocEntries`, `bytesPerLocation`, `locationBias` | `resolvedHeadings` | — |
| `HeadingsOnlyStep` | `resolvedHeadings` | — | `--headings-only` set |
| `ParseClippingsStep` | `epubTitle` | `clippings`, `matchedBookTitle` | `clippings` empty |
| `GroupClippingsStep` | `clippings`, `resolvedHeadings` | `groups` | — |
| `RenderOutputStep` | `groups`, `matchedBookTitle`, `epubTitle` | — (writes file) | — |

> Note: `EpubLoader` is a `Closeable` resource. Since it needs to stay open across both `LoadEpubStep`
> and `FitCalibrationStep`/`ResolveHeadingsStep`, the cleanest approach is to store it in
> `PipelineContext` as well (`public EpubLoader epubLoader`) and close it explicitly at the end of
> `ResolveHeadingsStep` (the last step that needs it), or wrap those three steps in a single
> resource-managing composite step.
# Example of a Step class
```java
public class PrintCalibrationTemplateStep implements PipelineStep {

    private final AppConfig config;

    public PrintCalibrationTemplateStep(AppConfig config) {
        this.config = config;
    }

    @Override
    public StepResult execute(PipelineContext ctx) throws Exception {
        if (config.printCalibrationTemplate() == null) {
            return StepResult.CONTINUE;
        }

        Path outputPath = config.printCalibrationTemplate();

        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String content = buildCalibrationTemplate(ctx.tocEntries);

        if (ctx.debug != null) {
            ctx.debug.writeText("03_calibration_template_preview.txt", content);
        }

        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        System.out.println("[KindleParser] ✅ Calibration template written to " + outputPath.toAbsolutePath());

        return StepResult.DONE;
    }

    private String buildCalibrationTemplate(List<TocEntry> tocEntries) {
        StringBuilder sb = new StringBuilder();
        sb.append("# KindleParser calibration template\n");
        sb.append("# Fill in at least 2 locations, keep the rest blank.\n");
        sb.append("# Format: Title - 123\n\n");

        int prevLevel = -1;
        boolean first = true;
        for (TocEntry e : tocEntries) {
            int level = e.level();
            if (!first) {
                if (level == 1 || (prevLevel != -1 && level != prevLevel)) {
                    sb.append("\n");
                }
            }
            String indent = "  ".repeat(Math.max(0, level - 1));
            sb.append(indent).append(e.title()).append(" - \n");
            prevLevel = level;
            first = false;
        }

        return sb.toString();
    }
}
```
# Package structure
Keep existing structure
```
kindleparser/
  calibre/        ← BookPreprocessor (external process wrapper)
  fyodor/         ← FyodorClippingsParser, FyodorParseResult (external process wrapper)
  models/         ← Clipping, Heading, HeadingGroup, TocEntry (pure data)
  template/       ← TemplateClipping, TemplateGroup, TemplateHeading (view model)
  toc/            ← LocationResolver, TocParser*, NcxTocParser, XhtmlTocParser (TOC domain)
```
New packages for steps:
```
kindleparserv2/
  Main.java
  ArgsParser.java
  AppConfig.java

  pipeline/
    Pipeline.java
    PipelineContext.java
    PipelineStep.java
    StepResult.java

    steps/
      PreprocessBookStep.java
      LoadEpubStep.java
      ParseTocStep.java
      PrintCalibrationTemplateStep.java
      FitCalibrationStep.java
      ResolveHeadingsStep.java
      HeadingsOnlyStep.java
      ParseClippingsStep.java
      GroupClippingsStep.java
      RenderOutputStep.java
```
# Test folder structure
```
src/test/java/io/github/vlovric/kindleparserv2/

  unit/
    pipeline/
      steps/
        PreprocessBookStepTest.java
        ParseTocStepTest.java
        FitCalibrationStepTest.java
        ResolveHeadingsStepTest.java
        PrintCalibrationTemplateStepTest.java
        HeadingsOnlyStepTest.java
        ParseClippingsStepTest.java
        GroupClippingsStepTest.java
        RenderOutputStepTest.java
    toc/
      NcxTocParserTest.java
      XhtmlTocParserTest.java
      LocationResolverTest.java
    fyodor/
      FyodorClippingsParserTest.java
    calibre/
      BookPreprocessorTest.java
    GrouperTest.java

  integration/
    FullPipelineIT.java
    HeadingsOnlyPipelineIT.java
    PrintCalibrationTemplateIT.java

src/test/resources/
  books/
    minimal.epub               ← smallest valid epub you can make
    epub2-ncx.epub             ← EPUB2 with NCX toc
    epub3-nav.epub             ← EPUB3 with nav.xhtml
  clippings/
    single-book.txt
    multi-book.txt
    empty.txt
  calibration/
    valid-two-points.txt
    valid-many-points.txt
    missing-headings.txt       ← titles that don't match any TOC entry
    empty.txt
  templates/
    basic.ftl
    headings-only.ftl
  expected/                    ← golden output files for integration assertions
    minimal-output.md
    headings-only-output.txt
```
