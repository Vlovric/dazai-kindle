## Plan: Java Migration of KindleParser

Java rewrite of the Kindle clippings pipeline, delegating parsing to the Fyodor Ruby subprocess while supporting custom Mustache output templates and using Maven + args4j.

### Done
**Steps**
1. **Bootstrap Project**
   - Create a standard Maven project structure
   - Add dependencies in `pom.xml`: `args4j` (CLI), `jackson-databind` (JSON), `jsoup` (EPUB HTML parsing), `jmustache` (Templating)
2. **Define Domain Models**
   - Create Java Records (or POJOs) for `Clipping`, `TocEntry`, `Heading`, and `HeadingGroup` mapping to Python's `models.py`.
3. **Book Pre-processing & EPUB Pipeline**
   - Check input file extension: if it is `.azw3`, invoke Calibre (`ebook-convert`) via `ProcessBuilder` to convert it to `.epub`.
   - Implement `EpubLoader` to handle zip extraction of the EPUB file to a temp directory
   - Implement `TocParser` returning `List<TocEntry>` (parsing `nav.xhtml` or `toc.ncx` via DOM/Jsoup)
   - Implement `LocationResolver` to read EPUB files and map TOC entries to Kindle locations (`List<Heading>`)
4. **Fyodor Subprocess Orchestrator**
   - Implement `FyodorClippingsParser` to act as a bridge
   - Ensure the internal `kindle_headings.erb` is embedded in resources and extracted to a temp dir at runtime
   - Execute `ProcessBuilder` with `fyodor` pointing to the ERB template to get NDJSON back on standard output (exactly how Python does it)
   - Deserialize the NDJSON output into `Clipping` models via Jackson
5. **Grouping Module**
   - Implement `Grouper` mapping `Clipping`s to `Heading`s based on `location` boundaries.
6. **Templated Output**
   - Build `TemplateRenderer` utilizing JMustache, taking the user's custom Mustache file as input instead of hardcoded strings
   - Render the `List<HeadingGroup>` into the requested output
7. **CLI Shell**
   - Implement `Main.java` orchestrating the pipeline 
   - Wire all CLI parameters via `@Option` annotations from `args4j`
### Todo


Plan: Make Pipeline Step-by-Step Debuggable
Add an explicit “debug/workdir” mode to the CLI so every pipeline stage can persist its intermediate inputs/outputs (extracted EPUB, parsed TOC, resolved headings, Fyodor stdout, Fyodor JSON outputs, selected book file, parsed clippings, grouping report). This turns the current opaque temp-dir + in-memory flow into a reproducible, inspectable run folder you can diff across attempts.

Steps

Add debug/workdir CLI options in Main.java (blocks all later steps)

Add options:
--debug-dir (directory where a single run folder is created, default ./debug-runs)
--keep-workdir (do not delete intermediate directories)
--workdir (optional explicit work directory; when provided, reuse it)
--dump-stage (optional, e.g. toc, headings, fyodor, grouping, all; or keep it simple as just --debug)
In run(), compute a per-run folder like debug-runs/run-YYYYMMDD-HHMMSS/ and print it once at startup.
Create a small debug artifact writer utility (depends on 1)

Add a class like DebugArtifacts with helpers:
write JSON (pretty), text, and directory copies
Standardize file names with stage numbers so you can inspect in order.
Make EPUB extraction inspectable (depends on 1)

Problem: EpubLoader.java always extracts to a temp folder and deletes it in close().
Approach:
Add a constructor overload that extracts into {runDir}/epub-extracted/, or
Add “keep temp dir” behavior and record its path into the run folder.
Keep artifacts:
01_epub_extracted_path.txt and/or the extracted folder
01_spine.json, opfRoot, tocHref
Dump TOC parsing output (depends on 1; parallel with step 5)

After TocParser.parse() (in Main), write:
02_toc_entries.json (the List<TocEntry>)
02_toc_source.txt (tocHref + resolved toc path)
Dump location resolution output (depends on 1; parallel with step 4)

After LocationResolver.resolve() (in Main), write:
03_resolved_headings.json (title, level, file, anchor, charOffset, kindleLocation)
Optional but very useful:
export fileOffsets to 03_file_offsets.json
record missing anchors to 03_missing_anchors.txt
Make Fyodor step deterministic + inspectable (depends on 1)

Stop using a temp output dir when debugging; instead:
use {runDir}/fyodor-out/ as the Fyodor output directory
Capture Fyodor output:
04_fyodor_stdout.txt (verbatim), while still prefix-printing [Fyodor] to console
Validate user config early:
check ~/.config/fyodor/fyodor.toml; if missing/misconfigured, warn and write 04_fyodor_config_check.txt with the required snippet:
[output] filename = "%{author_fill} - %{title}.json"
Template management stays as you want:
copy the bundled template to ~/.config/fyodor/template.erb (overwrite) and write 04_template_installed_path.txt
Select the correct book JSON produced by Fyodor (depends on 6)

Problem: Fyodor can output multiple book files; parsing all mixes books and makes grouping nonsense.
Add an explicit selection step:
Extend EpubLoader.java to parse OPF metadata (dc:title, dc:creator) and dump 01_epub_metadata.json
List {runDir}/fyodor-out/* and write 04_fyodor_out_listing.json (filename, size, mtime)
Pick target file by a simple, debuggable heuristic:
prefer the file whose NDJSON book_title best matches EPUB title (case-insensitive, normalized)
tie-break by “most entries”
fallback: user-provided --title
if still ambiguous: fail with candidates and write 04_selection_failure.json
Record selection: 04_selected_book.json
Improve parse error diagnostics (depends on 6)

When JSON parsing fails, include file + line number in the exception and write the offending line to 04_parse_error_line.jsonl.
Dump sanity stats for the selected book:
05_clippings_stats.json (counts by type, min/max location, null-loc count)
05_clippings_sample.json (first N entries)
Dump grouping results (depends on 5 and 7)

After grouping, write:
06_grouping_summary.json (heading title/location → count)
06_before_first_bucket.json (the BEFORE_FIRST bucket)
Define a repeatable debugging workflow (depends on 1–9)

Run with: --debug-dir ./debug-runs --keep-workdir
Inspect in order:
01_epub_metadata.json + extracted EPUB folder
02_toc_entries.json
03_resolved_headings.json
04_fyodor_stdout.txt + fyodor-out/*.json
04_selected_book.json
05_clippings_stats.json
06_grouping_summary.json
Relevant files

Main.java — flags + per-stage dumps
EpubLoader.java — keep extraction dir + OPF metadata parsing
LocationResolver.java — optional debug exports
FyodorClippingsParser.java — capture stdout, list outputs, select correct JSON, keep dirs
Grouper.java — grouping summary dump (or do it in Main)
template.erb — keep deterministic NDJSON (including the require 'json' fix)