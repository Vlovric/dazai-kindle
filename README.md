[![CI](https://github.com/Vlovric/dazai-kindle/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Vlovric/dazai-kindle/actions/workflows/ci.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL_3.0-yellow.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

# dazai-kindle

Export your Kindle highlights and notes into any format you want, with your headings resolved to the correct chapters.

## What it does

Kindle's `My Clippings.txt` gives you highlights and notes but no chapter information — entries are just raw text with a location number. dazai-kindle bridges that gap: it parses your eBook's table of contents, uses a small calibration step to map Kindle locations to chapters, and groups your clippings under the correct headings. The result is rendered through a [FreeMarker](https://freemarker.apache.org/) template you write, so the output format is entirely up to you.

**This tool is for you if you:**
- Want to export Kindle clippings locally, without cloud services or subscriptions
- Want full control over the output format (Markdown, HTML, plain text, anything)
- Are comfortable with a command-line tool and a one-time calibration step

## Prerequisites

- **Java 21+** on PATH
- **[Fyodor](https://github.com/rc2dev/fyodor)** — parses `My Clippings.txt` into structured JSON

  ```bash
  gem install fyodor
  ```

  Then add to `~/.config/fyodor/fyodor.toml`:

  ```toml
  [output]
  filename = "%{author} - %{title}.json"
  ```

- **[Calibre](https://calibre-ebook.com/download)** (`ebook-convert` on PATH) — required only if your book is `.azw3` or `.mobi`

## Installation

Download the latest `dazai-kindle-*.jar` from [Releases](https://github.com/Vlovric/dazai-kindle/releases) and run it with `java -jar`.

## How it works

dazai-kindle has three modes that you run in order for a new book:

| Step | Mode | Purpose |
|------|------|---------|
| 1 | Generate calibration template | Extract all TOC headings into a file you fill in |
| 2 | Headings only | Verify the resolved chapter locations look correct |
| 3 | Normal run | Produce your final output file |

### Step 1 — Generate calibration template

```bash
java -jar dazai-kindle.jar \
  --book book.epub \
  --print-calibration-template calib.txt
```

Opens the book, extracts the table of contents, and writes `calib.txt` with all chapter titles and blank location fields. Open the file and fill in the Kindle location numbers for at least 2 headings you can verify.

### Step 2 — Check heading locations

```bash
java -jar dazai-kindle.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only
```

Fits a calibration model from your filled-in values and resolves locations for every TOC heading. Output goes to `<BookTitle>_headings.md` so you can verify the chapter locations make sense before running the full export.

### Step 3 — Export clippings

```bash
java -jar dazai-kindle.jar \
  --book book.epub \
  --calibrate calib.txt \
  --clippings "My Clippings.txt" \
  --template template.ftl
```

Parses your clippings via Fyodor, groups them under the resolved headings, and renders the result through your FreeMarker template. Output goes to `<BookTitle>.md` by default.

## Output template

Write a `.ftl` file that receives `title` (String) and `groups` (List), where each group has a `heading` and a list of `clippings`. Minimal example:

```ftl
# ${title}
<#list groups as g><#if g.clippings?has_content>
## ${g.heading.title} (loc ${g.heading.location})
<#list g.clippings as c>
- ${c.content}
</#list>
</#if></#list>
```

See the [Usage Guide](docs/doc/6%20maintenance/6_1%20Usage%20Documentation.md) for the full template API, all flags, calibration file format, and debug mode.

## License

Licensed under the [GPL-3.0 license](LICENSE).

Uses [Fyodor](https://github.com/rc2dev/fyodor) and [Calibre](https://calibre-ebook.com/) as external tools (invoked as subprocesses, not linked).
