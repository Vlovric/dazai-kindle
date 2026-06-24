# KindleParser – Usage Guide

KindleParser extracts highlights, notes, and bookmarks from a Kindle clippings file, matches them to the correct sections of an eBook, and renders a report using a custom FreeMarker template.

---

## Prerequisites

- **Java 21+** on PATH
- [**Calibre**](https://calibre-ebook.com/download) (`ebook-convert`) on PATH — required only when the book is `.azw3` or `.mobi`
- [**Fyodor**](https://github.com/rc2dev/fyodor) Ruby gem (`fyodor`) on PATH — required for parsing Kindle clippings
- A configured `~/.config/fyodor/fyodor.toml` with an `[output]` section

```toml
[output]
filename = "%{author} - %{title}.json"
```

---

## Three Operating Modes

The tool has three modes. Which mode is active depends on the flags you provide.

| Mode | Required flags | When to use |
|------|---------------|-------------|
| **Generate calibration template** | `--book`, `--print-calibration-template` | First run on a new book to produce the calibration file you fill in |
| **Headings only** | `--book`, `--calibrate`, `--headings-only` | Inspect all TOC headings with their computed Kindle locations |
| **Normal run** | `--book`, `--calibrate`, `--clippings`, `--template` | Full extraction: clippings grouped under headings and rendered to a file |

`--calibrate` is **mandatory for all modes except** `--print-calibration-template`.

---

## Typical First-Time Workflow

```
1. Run --print-calibration-template  →  get calib.txt with all TOC headings
2. Open calib.txt, fill in Kindle locations for 2+ headings you can verify
3. Run --headings-only               →  verify all heading locations look correct
4. Run normal mode                   →  produce the final output file
```

---

## Mode 1 — Generate Calibration Template

Parses the book TOC and writes a pre-filled template with blank location fields. You fill in at least 2 known Kindle locations and save the file as your calibration file.

### Minimal
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --print-calibration-template calib.txt
```
Writes `calib.txt` with all TOC headings, then exits.

### With debug artifacts
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --print-calibration-template calib.txt \
  --debug
```
Also writes `./debug-runs/run-<timestamp>/01_epub_metadata.json`, `02_toc_entries.json`, and `03_calibration_template_preview.txt`.

### With AZW3/MOBI input (Calibre required)
```bash
java -jar kindle-parser.jar \
  --book "80000 Hours - Benjamin Todd.azw3" \
  --print-calibration-template calib.txt
```
Calibre converts the book to `.epub` in the same directory before TOC parsing. If a sibling `.epub` already exists, conversion is skipped.

---

## Mode 2 — Headings Only

Runs the full calibration pipeline and writes all resolved TOC headings with their computed Kindle locations to a file. Does **not** require a clippings file or output template.

### Minimal (default template, derived output path)
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only
```
Output file: `<BookTitle>_headings.md` (sanitised title, written to current directory). Uses the bundled default template: `## Title  *(Location: N)*`.

### With explicit output path
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only \
  --output headings.md
```

### With custom headings template
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only \
  --headings-template my-headings.ftl
```
Renders headings using your FreeMarker template. Exposed variables: `title` (String), `headings` (List of `TemplateHeading`). See [Headings Template](#headings-template-freemarker).

### With custom headings template and explicit output
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only \
  --headings-template my-headings.ftl \
  --output headings.txt
```

### With debug artifacts
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only \
  --debug
```
Saves `01_epub_metadata.json`, `02_toc_entries.json`, `03_resolved_headings.json` to `./debug-runs/run-<timestamp>/`.

### Full headings-only (all options)
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only \
  --headings-template my-headings.ftl \
  --output headings.txt \
  --debug
```

---

## Mode 3 — Normal Run

Parses clippings via Fyodor, matches them to TOC headings using the calibration fit, groups them, and renders the result through your FreeMarker template.

### Minimal (derived output path)
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl
```
Output file: `<BookTitle>.md` (sanitised title, written to current directory).

### With explicit output path
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl \
  --output "80000 Hours.md"
```

### With title filter (multiple books in clippings file)
`My Clippings.txt` often contains highlights from many books. Use `--title` to select the one you want if the default title matching mechanism fails. The match is case-insensitive substring.

```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl \
  --title "80,000 Hours"
```
Without `--title`, Fyodor is invoked with the EPUB's embedded title as the filter. In other words, the title is derived from the provided EPUB itself.

### With title filter and explicit output
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl \
  --title "80,000 Hours" \
  --output notes.md
```

### With debug artifacts
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl \
  --debug
```
Writes all intermediate artifacts to `./debug-runs/run-<timestamp>/`. See [Debug Artifacts](#debug-artifacts).

### With Fyodor template overwrite
The tool installs its required ERB template to `~/.config/fyodor/template.erb` on every run. If that file already has different content, the tool exits with an error unless you pass this flag.

```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl \
  --overwrite-fyodor-template
```

### Full normal run (all options)
```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl \
  --title "80,000 Hours" \
  --output notes.md \
  --debug \
  --overwrite-fyodor-template
```

---

## Flag Reference

| Flag | Required | Mode(s) | Description |
|------|----------|---------|-------------|
| `--book <path>` | **Always** | All | Path to `.epub`, `.azw3`, or `.mobi`. AZW3/MOBI are auto-converted to EPUB via Calibre's `ebook-convert`. If a sibling `.epub` already exists, conversion is skipped. |
| `--calibrate <path>` | **Yes** (except mode 1) | 2, 3 | Path to a filled-in calibration file. Must exist. At least 2 valid heading–location pairs required. |
| `--clippings <path>` | **Yes** (mode 3 only) | 3 | Path to `My Clippings.txt` (or any Kindle clippings file). Passed to Fyodor for parsing. |
| `--template <path>` | **Yes** (mode 3 only) | 3 | Path to a FreeMarker `.ftl` template for the clippings output. |
| `--print-calibration-template <path>` | — | 1 | **Activates mode 1.** Path where the calibration template is written. Skips `--calibrate`, `--clippings`, and `--template`. |
| `--headings-only` | — | 2 | **Activates mode 2.** Skips `--clippings` and `--template`. |
| `--output <path>` | No | 2, 3 | Output file path. Default for mode 2: `<BookTitle>_headings.md`. Default for mode 3: `<BookTitle>.md`. Characters illegal in filenames (`\ / : * ? " < > \|`) are replaced with `_`. |
| `--title <string>` | No | 3 | Case-insensitive substring filter passed to Fyodor for book selection. If omitted, the EPUB's embedded title is used. |
| `--headings-template <path>` | No | 2 | FreeMarker template for headings-only output. If omitted, the bundled default is used (`## Title  *(Location: N)*`). |
| `--debug` | No | 1, 2, 3 | Write intermediate artifacts to `./debug-runs/run-<timestamp>/`. |
| `--overwrite-fyodor-template` | No | 3 | Overwrite `~/.config/fyodor/template.erb` if it already differs from the required template. Without this flag the tool exits with an error on mismatch. |

---

## Calibration File Format

Plain text, UTF-8. Each mapping line: `Heading title - 123` or `Heading title: 123`.  
Blank lines and lines starting with `#` are ignored. Lines without a location value (e.g. `Chapter 1 -`) are ignored and can be left blank.

Delimiters supported: ` - `, ` – ` (en-dash), ` — ` (em-dash), `: `.

Title matching is normalised (lowercase, non-alphanumeric → spaces) and tries exact → prefix → contains.

```
# KindleParser calibration template
# Fill in at least 2 locations, keep the rest blank.
# Format: Title - 123

About the authors - 24

Introduction -

  How to use this guide -

Chapter 1 What makes for a dream job? - 356
Chapter 2 Can one person make a difference? - 631
```

---

## Output Template (FreeMarker)

Used by mode 3 (`--template`).

| Variable | Type | Description |
|----------|------|-------------|
| `title` | `String` | Book title (matched from Fyodor output, or falls back to `--title` filter or EPUB title) |
| `groups` | `List<TemplateGroup>` | All heading groups in reading order. Groups with no clippings are included. |

**`TemplateGroup`**

| Field | Type | Description |
|-------|------|-------------|
| `heading` | `TemplateHeading` | The TOC heading this group belongs to |
| `clippings` | `List<TemplateClipping>` | All clippings under this heading, ordered by location |

**`TemplateHeading`**

| Field | Type | Description |
|-------|------|-------------|
| `title` | `String` | Heading text from TOC |
| `level` | `int` | Hierarchy depth (1 = top level) |
| `location` | `int` | Computed Kindle location |
| `charOffset` | `int` | Byte offset within the EPUB spine file |
| `file` | `String` | EPUB spine file containing this heading |
| `anchor` | `String` | HTML anchor ID (may be null) |

**`TemplateClipping`**

| Field | Type | Description |
|-------|------|-------------|
| `content` | `String` | The highlighted or noted text |
| `type` | `String` | `"highlight"`, `"note"`, `"bookmark"`, or `"clip"` |
| `location` | `int` | Parsed Kindle location (0 if missing) |
| `rawLocation` | `String` | Raw location string from Fyodor (e.g. `"356-358"`) |
| `page` | `String` | Page number if present (may be null) |
| `date` | `String` | Date string from the clipping |
| `bookTitle` | `String` | Book title as stored in the clipping |
| `author` | `String` | Author as stored in the clipping |

### Minimal example template

```ftl
# ${title}
<#list groups as g><#if g.clippings?has_content>
## ${g.heading.title} (loc ${g.heading.location})
<#list g.clippings as c>
- ${c.content}
</#list>
</#if></#list>
```

---

## Headings Template (FreeMarker)

Used by mode 2 (`--headings-only --headings-template`). If `--headings-template` is omitted, the bundled default renders:

```markdown
## Heading Title  *(Location: 123)*
### Sub-heading   *(Location: 456)*
```

| Variable | Type | Description |
|----------|------|-------------|
| `title` | `String` | Book title from the EPUB metadata |
| `headings` | `List<TemplateHeading>` | All resolved headings in reading order |

`TemplateHeading` fields are the same as in the output template above.

### Minimal example headings template

```ftl
${title}
<#list headings as h>${"  "?repeat(h.level - 1)}${h.title} - ${h.location}
</#list>
```

---

## Debug Artifacts

When `--debug` is set, a timestamped directory `./debug-runs/run-<timestamp>/` is created containing:

| File | Written by | Contents |
|------|-----------|----------|
| `01_epub_metadata.json` | LoadEpubStep | Title, author, and basic EPUB info |
| `02_toc_entries.json` | ParseTocStep | All raw TOC entries (title, file, anchor, level) |
| `03_calibration_template_preview.txt` | PrintCalibrationTemplateStep *(mode 1 only)* | Preview of the calibration template |
| `03_resolved_headings.json` | ResolveHeadingsStep | TOC entries with computed locations and byte offsets |
| `04_fyodor_stdout.txt` | ParseClippingsStep | Raw stdout from the Fyodor subprocess |
| `04_selected_book.json` | ParseClippingsStep | The matched clippings JSON file content |
| `05_clippings_stats.json` | ParseClippingsStep | Count, type breakdown, and location range of parsed clippings |
| `06_grouping_summary.json` | GroupClippingsStep | How many clippings fell under each heading |

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FYODOR_BIN` | `fyodor` | Path to the `fyodor` executable. Set this if `fyodor` is not on your system PATH or you want to use a specific version. |
