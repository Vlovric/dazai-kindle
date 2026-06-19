# KindleParser – Agent Insights (Gotchas & Lessons)

**Always keep these in mind when modifying the code.**

## 1. Byte offsets ≠ character offsets

- **Mistake:** Used `text.length()` (characters) from a DOM‑stripped version of XHTML. Kindle locations are based on **raw UTF‑8 bytes** including all markup, scripts, and CSS.
- **Consequence:** Headings appeared hundreds of locations too early, especially in front matter (cover, TOC page) which has heavy markup but little visible text.
- **Solution:** `LocationResolver` now sums `Files.readAllBytes().length` for each spine file in reading order. For anchors, it searches the raw HTML string for `id`/`name`/`xml:id` and counts bytes to that position.

## 2. The “128 bytes per location” rule is not a constant

- **Observed:** For `80,000 Hours`, the implied bytes/location ranged from ~122 to ~134 across the book. For `Thinking in Systems`, it was ~143. Drift can exceed 100 locations.
- **Lesson:** Never trust hardcoded 128. **Calibration is mandatory** for accurate output. The old JVM system properties are removed.

## 3. Fyodor’s `entry.loc` can be a range (e.g. `"1847-1852"`)

- **Current handling:** `Clipping.location()` splits on `-` and takes the first number. This is the start location, which is correct for grouping (the highlight begins at that location).
- **Note:** The raw range is preserved in `rawLocation` field in case templates want the end location.

## 4. Highlight + Note pairing in templates

- **Template logic:** The example `template.ftl` assumes a note immediately follows a highlight in the same group and uses `skipNext` to pair them. This relies on the **order** of clippings as returned by Fyodor (which preserves reading order). Do not reorder clippings arbitrarily before grouping.

## 5. Empty groups are not dropped

## 6. Calibration title matching is fuzzy

- **Algorithm:** Normalise (lowercase, replace non‑alphanumeric with spaces, collapse spaces). Try exact match, then prefix, then contains.
- **Risk:** Over‑matching – e.g. “Introduction” might match “Introduction to …” unintentionally. Solution: print matched titles in the log; user can refine calibration file.

## 7. Fyodor output file selection

- **Heuristic:** Score each JSON file by: exact expected title (1000 pts), contains expected (500), contains `--title` filter (200). Tie‑breaker: larger file (more clippings). This works because Fyodor writes one file per book in the clippings file. Ensure `fyodor.toml` uses a unique filename pattern (e.g. include author).

## 8. Debug mode keeps extracted EPUB and intermediate JSON

- **Important:** When `--debug` is used, the unzipped EPUB is **not deleted** (it is saved in the run directory). This allows manual inspection but can consume disk space. The run directory also includes a copy of the original EPUB if conversion happened.

## 9. Calibration must be applied per run – not persisted

- **Reason:** Different devices, different conversion pipelines, even different firmware versions change the mapping. Persisting calibration would be misleading. The `--calibrate` file is re‑fitted every run.

## 10. Anchor detection in raw HTML is naive

- **Method:** Regex `\b(?:id|name|xml:id)\s*=\s*(["']?)anchor\1`. This works for well‑formed EPUBs but may fail if the attribute is split across lines or uses unusual quoting. No fallback to DOM parsing (because we need byte offsets). Future improvements could use a streaming parser to locate the exact byte position of the element start.