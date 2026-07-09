# Project name: DazaiKindle
# 1. Purpose
The purpose of this system is to provide a way to locally parse Kindle reading highlights and notes along with the Table of Content structure and positioning for each highlight and note. The system also provides a templating mechanism to template the output of the system to suit various needs.
There are two ways to interact with the system: either through the **CLI tool** or the **Java Spring Boot server.**
# 2. Scope
## 2.1. Feature list (High-Level)

| ID                                  | Name               | Short description                                                                                 | Priority | Status (implemented, in progress, empty) | Dependent on |
| ----------------------------------- | ------------------ | ------------------------------------------------------------------------------------------------- | -------- | ---------------------------------------- | ------------ |
| [[FR01 - Parsing of entries\|FR01]] | Parsing of entries | The system should enable parsing of .epub/.azw3/.mobi files for highlights and notes (entries)    | 2        | Implemented                              | FR04, FR02   |
| [[FR02 - Parsing of ToC\|FR02]]     | Parsing of ToC     | The system should enable parsing of the book ToC                                                  | 2        | Implemented                              |              |
| [[FR03 - Output templating\|FR03]]  | Output templating  | The system should enable templating for the parsed entries + ToC content                          | 3        | Implemented                              | FR01, FR02   |
| [[FR04 - Parse calibration\|FR04]]  | Parse calibration  | The system should provide the user with the ability to calibrate the ToC positions via a template | 1        | Implemented                              | FR02         |
| [[FR05 - Debug mode\|FR05]]         | Debug mode         | The system should provide a comprehensive debug mode to catch edge cases                          | 4        | Implemented                              |              |
| [[FR06 - Artifact management\|FR06]] | Artifact management | The system should enable uploading and storing artifacts (books, calibration files, templates, clippings) through the UI | 5        | Implemented                              |              |
| [[FR07 - Library management\|FR07]] | Library management | The system should enable viewing, exporting and deleting stored artifacts and past runs through the UI | 5        | Implemented                              |              |
| [[FR08 - Pipeline execution screens\|FR08]] | Pipeline execution screens | The system should enable executing all pipeline run types (calibration generation, headings-only, full run) through the UI | 5        | Implemented                              | FR01, FR02, FR03, FR04, FR06 |
| [[FR09 - Template creation\|FR09]] | Template creation | The system should enable uploading new output and heading templates through the UI | 5        | Implemented                              | FR06, FR07   |
| [[FR10 - Storage location management\|FR10]] | Storage location management | The system should enable viewing and changing configured storage paths for artifacts | 5        | Implemented                              |              |

## 2.2. Out of Scope
- Self written parsing of entries
- Formats other than .azw3/.mobi/.epub
## 2.3. Constraints
- dependency on [Fyodor](https://github.com/rc2dev/fyodor) for highlight parsing from the `My Clippings.txt` file
- dependency on [Calibre](https://calibre-ebook.com/)'s `ebook-convert` for converting .azw3 or .mobi to .epub
- ToC positions can only be estimated through calculations, dependent on user calibration for accuracy
- Highlight information is constrained to what the `My Clippings.txt` provides: Entry *page*, *location*, *date* and *time*
# 3. Technology overview
- vanilla **Java** for the CLI app
- **FreeMarker** as the templating system
- **Java Spring Boot** for the server
- **React** for the web UI
## 3.1. Dependencies
- **args4j** (2.33) - CLI argument parsing
- **freemarker** (2.3.33) - templating
- **jsoup** (1.17.2) - EPUB HTML parsing
- **jackson-databind** (2.17.1) - JSON deserialization

- **spring-boot-dependencies** (4.0.7) - Spring Boot BOM
- **spring-boot-starter-webmvc** - Spring Boot web server
- **springdoc-openapi-starter-webmvc-ui** (3.0.2) - Swagger/OpenAPI docs

- **junit-jupiter** (5.10.2) - testing
- **mockito-core** / **mockito-junit-jupiter** (5.11.0) - test mocking

- **maven-shade-plugin** (3.5.3) - CLI fat jar packaging
- **spring-boot-maven-plugin** - server fat jar repackaging
- **frontend-maven-plugin** (1.15.1) - builds the React frontend and bundles it into the server's fat jar
# 4. Definitions, acronims
- entry -> Kindle highlight/note/clipping/bookmark
- ToC -> table of contents