# KindleParser – Development Plan & Next Steps

## Completed (as of current codebase)

- [x] Java rewrite with Maven + args4j
- [x] EPUB extraction and spine offset calculation using raw bytes
- [x] NCX and XHTML TOC parsing
- [x] Linear calibration from a text file (mandatory)
- [x] Fyodor integration with auto‑installed ERB template
- [x] Grouping clippings under headings (binary search)
- [x] FreeMarker templating
- [x] Debug artifacts (JSON + extracted EPUB)
- [x] `--headings-only` and `--print-calibration-template` modes
- [x] Mandatory calibration enforced

## Moji komentari
- neke knjige imaju page u clippingu, fyodor mi moze to dat, mogu li nekako dobiti to iz epub-a? Ako mogu onda bi to mogao koristit za automatsko poravnavanje offseta ili grupiranje


## Known Issues / Limitations

- **Anchor byte offset detection** uses regex on the entire HTML string. May fail for anchors inside large files (performance) or with unusual formatting. Consider a streaming XML parser that reports byte positions.
- **No fallback for missing TOC** – if no TOC found, the tool exits. Could generate a flat list from spine files as a last resort.
- **Calibration file matching** uses contains as a last resort, which can produce false positives. Add a warning when contains match is used and allow user to disambiguate with more precise titles.
- **Fyodor output selection** depends on filename pattern; if the user misconfigures `fyodor.toml`, the parser may see no JSON files. Add explicit error message and check for expected pattern.
- **Grouping includes the “before first heading” group** even when it has clippings – that’s fine, but the group is not filtered out. Consider adding a flag to suppress groups with zero clippings (currently they are kept but harmless).
- **Calibration points with identical byte offsets** cause division by zero. Currently fails with an exception. Handle by detecting and asking user to remove duplicate or add another point.

## Short‑Term Improvements (Next 1‑2 iterations)

1. **Better anchor location** – Use a lightweight SAX‑like parser (e.g. `javax.xml.stream.XMLStreamReader`) on the raw byte stream to record exact byte offset of the element start. This would be more robust than regex.
2. **Calibration quality checks** – Emit a warning if RMSE > 5 locations; suggest adding more calibration points.
3. **Support for page numbers** – Kindle sometimes includes page numbers (real page numbers). The `Clipping` model already has `page`; we could allow grouping by page number as an alternative to location (via a new flag).
4. **Option to keep empty groups** – Currently all groups are emitted; add `--drop-empty-groups` to suppress headings with zero clippings (useful for very long TOCs).
5. **Improve `--print-calibration-template`** – Allow limiting to top N headings (e.g. `--print-calibration-template 20`) to avoid huge files.

## Medium‑Term Ideas

- **Automatic calibration from clippings** – If `My Clippings.txt` contains highlights with known locations that fall near TOC headings, the tool could suggest calibration points. Would require heuristics to avoid noise.
- **Piecewise calibration** – For books where the mapping changes non‑linearly (e.g. heavily illustrated chapters), support multiple linear segments defined by the same calibration file (using comments like `# segment: 0-5000 bytes`).
- **Parallel extraction** – Processing large EPUBs could be sped up by reading spine files concurrently (but careful with offset ordering).
- **Standalone TOC viewer** – A simple UI (or TUI) to inspect headings and their resolved locations without needing a template or clippings.

## Architectural Decisions to Revisit

- **EpubLoader keeps extracted files open** – Currently we hold a reference to the temp directory but do not track individual file handles. Memory is fine. However, when debugging, the extracted copy is never deleted. Add `--cleanup-debug` to optionally remove after run.
- **Fyodor template installation** – Overwrites `~/.config/fyodor/template.erb` every run. This is acceptable because the template is idempotent. But if the user has a custom template, they will lose it. Check if file exists and is identical before overwriting.
- **LocationResolver defaults** – The fallback `bytesPerLocation=128` is still used if calibration fails? Actually calibration is mandatory, so the default constructor is only used during calibration fitting (where we need the uncalibrated byte offsets). That is fine.

## Testing & Validation

- Create a test suite with known EPUBs and matching `My Clippings.txt` samples (obfuscated). Verify that location mapping stays within ±1 location for calibration points.
- Benchmark on large books (e.g. 1500+ page EPUB) to ensure memory and speed are acceptable.

## Documentation TODOs

- Add a step‑by‑step “Getting Started” guide in `Docs_usage.md`.
- Provide a sample `fyodor.toml` and explain how to install `fyodor` (Ruby gem).
- Include a note about the required Calibre installation for AZW3/MOBI conversion.

## Long‑Term Vision

- Possibly rewrite the Fyodor dependency in pure Java (using a ANTLR grammar for My Clippings.txt) to remove the Ruby subprocess. This would simplify distribution but is a large undertaking.
- Add support for Kindle “Notebook” export (CSV/HTML) as an alternative input.