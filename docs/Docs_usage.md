# KindleParser – Usage Guide

KindleParser extracts highlights, notes, and bookmarks from a Kindle `My Clippings.txt` file, matches them to the correct sections of an eBook (EPUB or converted AZW3/MOBI), and renders a Markdown report using a custom FreeMarker template.

## Command‑Line Arguments

| Option | Required | Description |
|--------|----------|-------------|
| `--book <path>` | Yes | Path to `.epub`, `.azw3` or `.mobi` file. AZW3/MOBI are auto‑converted to EPUB using Calibre’s `ebook-convert`. |
| `--clippings <path>` | Yes* | Path to `My Clippings.txt` from Kindle. (*Not required for `--headings-only` or `--print-calibration-template`.) |
| `--template <path>` | Yes* | Path to a FreeMarker template (`.ftl`). (*Not required for `--headings-only` or `--print-calibration-template`.) |
| `--output <path>` | No | Output file path. Default: `<Book Title>.md` (sanitised). |
| `--title <string>` | No | Filter clippings by book title (case‑insensitive substring). Useful when `My Clippings.txt` contains multiple books. |
| `--headings-only` | No | Print extracted headings with their computed Kindle locations and exit. Does not require clippings or template. |
| `--debug` | No | Write intermediate artifacts to `./debug-runs/run-<timestamp>/`. Includes extracted EPUB, TOC JSON, resolved headings, Fyodor output, grouping stats. |
| `--calibrate <file>` | **Mandatory** | Path to a calibration file (see format below). The file provides 2+ known heading‑location pairs to fit a linear mapping for this run. |
| `--print-calibration-template <path>` | No | Generate a calibration template containing all TOC headings (indented, blank locations) and exit. User fills in Kindle locations and re‑runs with `--calibrate`. |

> **Calibration is mandatory** (unless `--headings-only` or `--print-calibration-template`).  

## Calibration File Format

Plain text, UTF‑8. Each mapping line: `Heading title - 123` or `Heading title: 123`.  
Blank lines and lines starting with `#` are ignored. Lines without a location (e.g. `Chapter 1 -`) are ignored.

Example:

```
# KindleParser calibration template
# Fill in at least 2 locations, keep the rest blank.
# Format: Title - 123

About the authors - 24

Introduction - 

  How to use this guide - 

Chapter 1 What makes for a dream job? - 
```

The parser normalises titles (lowercase, non‑alphanumeric → spaces) and matches by exact, then prefix, then contains. At least two valid points are required.

## Usage Examples

### 1. Generate a calibration template
```bash
java -jar kindle-parser.jar --book book.epub --print-calibration-template calib.txt
```

### 2. Normal run

```bash
java -jar kindle-parser.jar \
  --book book.epub \
  --clippings My\ Clippings.txt \
  --template template.ftl \
  --calibrate calib.txt \
```

### 3. Inspect headings only

```bash
java -jar kindle-parser.jar --book book.epub --headings-only --calibrate calib.txt
```

### 4. Debug mode (saves all intermediate data)

```bash
java -jar kindle-parser.jar --book book.epub --clippings clippings.txt --template template.ftl --calibrate calib.txt --debug
```

### 5. Filter clippings by book title

```bash
java -jar kindle-parser.jar ... --title "80,000 Hours"
```

## Environment Variables

- FYODOR_BIN – Path to the `fyodor` executable (default: `fyodor`).
- The Fyodor ERB template is automatically installed to `~/.config/fyodor/template.erb`.
- Ensure `~/.config/fyodor/fyodor.toml` exists with an `[output]` section, e.g.:

```toml
[output]
filename = "%{author} - %{title}.json"
```

## Output Template (FreeMarker)

| Variable | Type | Description |
| --- | --- | --- |
| `title` | String | Book title |
| `groups` | List of `TemplateGroup` | Each group corresponds to one heading and its clippings |

`TemplateGroup` has:
- `heading` – `TemplateHeading` (fields: `title`, `level`, `location`, `charOffset`, `file`, `anchor`)
- `clippings` – List of `TemplateClipping` (fields: `type`, `content`, `location`, `rawLocation`, `page`, `date`, `bookTitle`, `author`)

See the example `template.ftl` template.
The output can be any file type.