[![CI](https://github.com/Vlovric/dazai-kindle/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Vlovric/dazai-kindle/actions/workflows/ci.yml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL_3.0-yellow.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

# dazai-kindle

Export your Kindle highlights and notes into any format you want, with your headings resolved to the correct chapters. CLI and GUI versions.

## What it does

Kindle's `My Clippings.txt` gives you highlights and notes but no chapter information: entries are just raw text with a location number. dazai-kindle bridges that gap: it parses your eBook's table of contents, uses a small calibration step to map Kindle locations to chapters, and groups your clippings under the correct headings. The result is rendered through a [FreeMarker](https://freemarker.apache.org/) template you write, so the output format is entirely up to you.

**This tool is for you if you:**
- Want to export Kindle clippings locally, without cloud services or subscriptions
- Want full control over the output format (Markdown, HTML, plain text, anything)
- Are comfortable with a command-line tool and a one-time calibration step
- Want a user friendly GUI experience

## Prerequisites

- **Java 21+** on PATH
- **[Fyodor](https://github.com/rc2dev/fyodor)**, parses `My Clippings.txt` into structured JSON

  ```bash
  gem install fyodor
  ```

  Then add to `~/.config/fyodor/fyodor.toml`:

  ```toml
  [output]
  filename = "%{author} - %{title}.json"
  ```

- **[Calibre](https://calibre-ebook.com/download)** (`ebook-convert` on PATH), required only if your book is `.azw3` or `.mobi`

## Installation

Download the latest `dazai-kindle-*.jar` (cli for CLI, server for GUI) from [Releases](https://github.com/Vlovric/dazai-kindle/releases) and run it with `java -jar`.

## CLI

dazai-kindle has three modes that you run in order for a new book:

| Step | Mode | Purpose |
|------|------|---------|
| 1 | Generate calibration template | Extract all TOC headings into a file you fill in |
| 2 | Headings only | Verify the resolved chapter locations look correct |
| 3 | Normal run | Produce your final output file |

### Step 1 - Generate calibration template

```bash
java -jar dazai-kindle.jar \
  --book book.epub \
  --print-calibration-template calib.txt
```

Opens the book, extracts the table of contents, and writes `calib.txt` with all chapter titles and blank location fields. Open the file and fill in the Kindle location numbers for at least 2 headings you can verify.

### Step 2 - Check heading locations

```bash
java -jar dazai-kindle.jar \
  --book book.epub \
  --calibrate calib.txt \
  --headings-only
```

Fits a calibration model from your filled-in values and resolves locations for every TOC heading. Output goes to `<BookTitle>_headings.md` so you can verify the chapter locations make sense before running the full export.

### Step 3 - Export clippings

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

## Web UI

Everything above also runs through a browser. The `dazai-kindle-server` module wraps the same core pipeline in a Spring Boot server with a bundled React frontend: no flags, no manual file bookkeeping, just point-and-click through the same three steps (generate calibration, verify headings, export).

Download the latest `dazai-kindle-server-*.jar` from [Releases](https://github.com/Vlovric/dazai-kindle/releases), run it with `java -jar`, and open `http://localhost:8080` (Fyodor and Calibre prerequisites still apply).

### Dashboard

![Dashboard](docs/doc/6%20maintenance/screenshots/dashboard.png)

The landing screen. Shows highlight/entry counts and last run time at a glance, and is the launch point for all three run types: Full Run, Generate Calibration File, and Headings Only.

### Generate Calibration File

![Generate calibration](docs/doc/6%20maintenance/screenshots/generate_calibration.png)

Step 1 of the CLI flow: pick a book, optionally enable debug mode, and click Generate. Returns `calib.txt` as a download, nothing is kept on the server, ready for you to fill in and re-upload for the next steps.

### Configure New Headings Only Run

![Headings only run](docs/doc/6%20maintenance/screenshots/headings_run.png)

Step 2 of the CLI flow: pick a book, calibration file, and a headings output template, then Extract. Lets you sanity-check the resolved chapter locations before committing to a full export. Unlike calibration generation, this produces a Library entry.

### Configure New Full Run

![Full run](docs/doc/6%20maintenance/screenshots/full_run.png)

Step 3 of the CLI flow, as a form: pick a book and calibration file (upload fresh or reuse one from the Library), the clippings file defaults to the last one uploaded, pick an output template, optionally set a title, toggle debug mode, and hit Extract. Produces a new (or updated) entry in the Library.

### Library

![Library](docs/doc/6%20maintenance/screenshots/library.png)

Every past run, one row per book (author, highlight count, last modified), searchable, sortable, and paginated. Select one or more runs to export as a `.zip` or delete outright.

### Book detail

![Book detail](docs/doc/6%20maintenance/screenshots/book_detail.png)

Going into a run shows every artifact it produced (book file, calibration file, output, headings-only output, debug run), each openable directly in your OS file browser, exportable, or deletable individually.

### Clippings file

![Clippings](docs/doc/6%20maintenance/screenshots/clippings.png)

Shows the name, upload date, and filesystem path of the one clippings file the server keeps around as the default for new runs, with a shortcut to open it in the OS file browser.

### Template Management

![Templates](docs/doc/6%20maintenance/screenshots/templates.png)

Every template on disk, split into Output and Headings tabs, filterable and sortable. Upload a new `.ftl` file with `+ New Template` (files named `*_h.ftl` are filed as headings templates automatically), or select existing ones to export or delete.

### Template detail

![Template detail](docs/doc/6%20maintenance/screenshots/template_detail.png)

Read-only view of a template's raw content next to a live render against hardcoded example entries, so you can check a template renders correctly without running it against a real book.

### Paths

![Paths](docs/doc/6%20maintenance/screenshots/paths.png)

Shows the configured filesystem locations for the Library and Templates directories, and lets you repoint either one. Existing files at the old path aren't moved automatically.

## License

Licensed under the [GPL-3.0 license](LICENSE).

Uses [Fyodor](https://github.com/rc2dev/fyodor) and [Calibre](https://calibre-ebook.com/) as external tools (invoked as subprocesses, not linked).
