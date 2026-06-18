# Project name: KindleParser
# 1. Purpose
The purpose of this system is to provide a way to locally parse Kindle reading highlights and notes along with the Table of Content structure and positioning for each highlight and note. The system also provides a templating mechanism to template the output of the system to suit various needs.
# 2. Scope
## 2.1. Feature list (High-Level)

| ID                                  | Name               | Short description                                                                                 | Priority | Status (implemented, in progress, empty) | Dependent on |
| ----------------------------------- | ------------------ | ------------------------------------------------------------------------------------------------- | -------- | ---------------------------------------- | ------------ |
| [[FR01 - Parsing of entries\|FR01]] | Parsing of entries | The system should enable parsing of .epub/.azw3/.mobi files for highlights and notes (entries)    | 2        |                                          | FR04, FR02   |
| [[FR02 - Parsing of ToC\|FR02]]     | Parsing of ToC     | The system should enable parsing of the book ToC                                                  | 2        |                                          |              |
| [[FR03 - Output templating\|FR03]]  | Output templating  | The system should enable templating for the parsed entries + ToC content                          | 3        |                                          |              |
| [[FR04 - Parse calibration\|FR04]]  | Parse calibration  | The system should provide the user with the ability to calibrate the ToC positions via a template | 1        |                                          |              |
| [[FR05 - Debug mode\|FR05]]         | Debug mode         | The system should provide a comprehensive debug mode to catch edge cases                          | 4        |                                          |              |

## 2.2. Out of Scope
- GUI
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
- **args4j** for CLI parsing
- **jackson** for JSON Deserialization
- **jsoup** for EPUB HTML Parsing
# 4. Definitions, acronims
- entry -> Kindle highlight/note/clipping/bookmark
- ToC -> table of contents