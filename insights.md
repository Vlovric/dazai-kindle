# Session insights — heading location drift & calibration

This project maps TOC headings to Kindle “Location” numbers so clippings from `My Clippings.txt` can be grouped under the right sections.

This session focused on a bug where TOC heading locations were systematically wrong.

## What was wrong originally

### 1) We were counting *text characters*, but converting as if they were *bytes*

The original resolver logic:

- parsed each spine XHTML into a DOM (Jsoup)
- extracted only text nodes (and inserted extra spaces)
- used `text.length()` as the “offset”
- converted to Kindle location using: `location = (offset / 128) + 1`

Problem:

- Kindle’s location rule-of-thumb is based on **bytes**, not “visible text characters”.
- Stripping markup (tags/attributes/whitespace), scripts/styles, and any non-text content causes the computed offset to be **much smaller** than the real byte stream Kindle uses.

This is why headings appeared far too early.

### 2) The mismatch is especially severe in front matter

Front matter commonly contains:

- a cover/title page with heavy markup
- big “contents” pages with tons of links
- images/fonts/CSS references that don’t show up as visible text

So a “text-only” count underestimates the real size early, shifting *everything*.

## What we changed (and why it helped)

### Change: Use raw UTF‑8 byte offsets across the spine

We changed the location computation to:

- build `fileOffset[file]` by summing `rawBytes.length` for each spine XHTML file in reading order
- resolve anchors by finding `id`/`name`/`xml:id` in the **raw XHTML string** and counting UTF‑8 bytes up to the element start

This aligns the model with the real thing we’re trying to approximate: a byte-based position in the book.

Observed improvement on the `80,000 Hours` debug extraction:

- Heading locations moved from ~100–300 range up into ~300–700+ where the real Kindle clippings started (~380+), i.e. *the right order of magnitude*.

## Why the remaining offset still happens (drift)

Even after switching to raw XHTML bytes, we still saw drift:

- `80,000 Hours`: close around early chapters, then by Chapter 11 the generated location was ~146 locations too high.
- `Thinking in Systems`: drift grew even more (hundreds by Appendix).

This is expected because **“128 bytes per location” is only an approximation** and is not constant across:

- formats (`.epub` vs `.azw3`/KF8)
- Kindle firmware / device
- how the file was converted (Calibre, KindleGen-like pipelines)
- the internal byte stream Kindle uses (it may include additional metadata/control bytes not present in extracted XHTML)
- compression/segmentation differences

In other words:

- We can compute a consistent “byte offset” in the extracted package.
- But Kindle’s displayed `Location` is based on a different internal representation.
- The relationship between “our bytes” and “Kindle locations” is roughly linear, but with a **different slope** and sometimes a **bias**.

### Evidence we saw

For `80,000 Hours`, the implied bytes-per-location from chapter start points increased through the book (roughly ~122 → ~134 bytes/location when using raw XHTML bytes). That’s the “drift” you see.

For `Thinking in Systems`, the best fit slope was different again (~143 bytes/location).

## What we added to make this practical

We added an **optional linear calibration**:

\[\
location = \lfloor (byteOffset / bytesPerLocation) + bias \rfloor + 1
\]

Defaults remain:

- `bytesPerLocation = 128`
- `bias = 0`

But you can override them per run using JVM system properties:

- `-Dkindleparser.bytesPerLocation=...`
- `-Dkindleparser.locationBias=...`

This lets you fit a per-book (or per-device) mapping without changing the core pipeline.

### Implemented: per-run calibration from a small text file (no persistence)

We implemented calibration automation for a single run via a CLI option:

- `--calibrate <path>`
  - the file contains lines like `Heading - 123` or `Heading: 123`
  - blank lines and `#` comments are ignored
  - titles are matched to TOC entries with tolerant matching (normalized, then exact → prefix → contains)

From those points, the program:

- computes `byteOffset` for each matched TOC entry using the **uncalibrated** resolver (raw spine offsets + anchor offsets)
- fits least-squares `y ≈ m*x + c` where:
  - `x = byteOffset`
  - `y = kindleLocation - 1`
- converts to:
  - `bytesPerLocation = 1/m`
  - `bias = c`
- re-runs heading resolution using the fitted `bytesPerLocation` + `bias`

Important behavior:

- If `--calibrate` is provided, it takes precedence over `-Dkindleparser.bytesPerLocation` / `-Dkindleparser.locationBias`.
- This stays minimal and computed per run: no DB / no persistent storage.

## Plan: make calibration mandatory

Goal: ensure we never emit “uncalibrated” Kindle locations, because uncalibrated drift is expected and can be large.

### Step 1 — Define what “mandatory” means (choose one)

Option A (strict):

- Any run that resolves headings must provide calibration (`--calibrate`), otherwise exit with an error.

Option B (compat-friendly, recommended transition):

- Add `--require-calibration` which errors out if calibration is missing.
- After a couple of runs / once you’re confident, flip the default so calibration is required unless `--allow-uncalibrated` is passed.

### Step 2 — Make it easy to create the calibration file

To reduce friction, add a helper mode that prints a suggested stub:

- `--print-calibration-template <N>`
  - prints the first N *top-level* TOC entries as `Title - ` (blank location)
  - user fills in the Kindle locations and re-runs with `--calibrate`

This keeps the workflow minimal and still “computed per run”.

### Step 3 — Decide the fallback if user insists on no file

If you want calibration mandatory *without* any extra file, there are only two realistic sources:

- existing `My Clippings.txt` (if it contains location numbers for highlights/notes)
- manual JVM props (`-Dkindleparser.bytesPerLocation=... -Dkindleparser.locationBias=...`)

Plan for this decision:

- If clippings-based calibration is acceptable:
  - implement `--calibrate-from-clippings` that extracts a handful of location points and maps them to nearest headings (heuristic; needs careful validation).
- If you want to stay deterministic and simple:
  - keep `--calibrate <file>` as the only mandatory path.

### Step 4 — Tighten validation + messaging

- Require at least 2 matched points; print which ones matched and which didn’t.
- Error out on non-positive slope / degenerate fit.
- Print fitted params + point count so the run is reproducible.

### Step 5 — Enforce precedence rules

- If calibration is mandatory, document and enforce a single source of truth:
  - `--calibrate` wins over `-D...` (current behavior)
  - or alternatively, disallow mixing (error if both are provided)

### Step 6 — (Optional) Better accuracy without persistence

If single-line calibration still shows systematic error in front matter:

- support piecewise linear calibration (front matter vs main content) driven purely by the provided calibration points in the same `--calibrate` file.

---

## Bottom line

- The original bug was a *unit mismatch*: text characters ≠ bytes.
- Fixing to raw bytes made locations sane.
- Remaining mismatch is because `128 bytes/location` is not a universal constant in real Kindle rendering, so per-book calibration is the pragmatic solution.
