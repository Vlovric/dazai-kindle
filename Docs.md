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
### Todo

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

**Relevant files (to implement)**
- `pom.xml` — maven build and dependencies (args4j, jsoup, jmustache, jackson)
- `src/main/resources/kindle_headings.erb` — exact copy of existing ERB logic for fyodor
- Java source files (e.g. `Main.java`, `EpubLoader.java`, `LocationResolver.java`, `FyodorClippingsParser.java`, `TemplateRenderer.java`)

**Verification**
1. Run `mvn clean package` to ensure dependencies resolve and compiles.
2. Execute JAR passing existing Python arguments: `-book mybook.epub -clippings MyClippings.txt`
3. Verify output matches Python pipeline JSON equivalent (when fed the same NDJSON from Fyodor subprocess)
4. Ensure custom `.mustache` file is respected on run (`-template my_output.mustache`)

**Decisions**
- Dropped the "Pure Java" clippings parser entirely. Execution guarantees `Fyodor` must be installed.
- **args4j** used for CLI input mapping
- **JMustache** used for output templating because it maintains extreme simplicity, avoids logic in views, and allows GPLv3 compliance.