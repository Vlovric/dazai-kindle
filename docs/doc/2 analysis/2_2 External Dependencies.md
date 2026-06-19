# 1. Calibre (ebook-convert)

## 1.1. When is it triggered

- **Trigger 1:** Conversion of a non‑EPUB ebook file – when the user provides a book in `.azw3` or `.mobi` format and no sibling `.epub` already exists.

## 1.2. Data

- **Outbound (request):** The command‑line invocation of `ebook-convert` with the absolute paths of the source file and the target `.epub` file.
- **Inbound (response):** The process exit code, merged stdout/stderr stream, and the newly written `.epub` file on disk.
- **State change:** The original file remains untouched; a new `.epub` file is created in the same directory.

## 1.3. Trigger 1 – Conversion of .azw3 / .mobi to .epub

### 1.3.1. Outbound

- **Executable:** `ebook-convert`
- **Arguments:**
  1. `inputPath` – absolute path to the source `.azw3` or `.mobi` file.
  2. `outputPath` – absolute path to the destination `.epub` file (same basename, sibling directory).
- **Streams:** stdout and stderr are merged and forwarded to the console.

### 1.3.2. Inbound

- **Exit code:** `0` indicates success; any non‑zero value causes the pipeline to fail with `IOException`.
- **stdout/stderr:** Captured line‑by‑line and printed with `[Calibre]` prefix. Used only for logging/debugging – not parsed programmatically.
- **File output:** The `.epub` file is expected to exist at the target path and be readable by the subsequent `EpubLoader`.
- **Error cases:** If `ebook-convert` is not installed, the `ProcessBuilder.start()` call throws `IOException`; if the process returns non‑zero, the conversion is considered failed.

---

# 2. Fyodor (fyodor binary)

## 2.1. When is it triggered

- **Trigger 1:** Main parsing pipeline – when the user provides `--clippings`, the system is not in `--headings-only` mode, and calibration has succeeded.

## 2.2. Data

- **Outbound (request):** The path to `My Clippings.txt`, the path to an output directory (either a debug‑run subfolder or a temporary directory), and the implicit use of the user’s Fyodor configuration (`~/.config/fyodor/fyodor.toml`).
- **Inbound (response):** A set of JSON Lines (`.jsonl`) files (one per book found in the clippings), the process exit code, and the merged stdout/stderr stream.
- **State change:** No permanent state is modified outside the output directory.

## 2.3. Trigger 1 – Parsing My Clippings.txt

### 2.3.1. Outbound

- **Executable:** `fyodor` – resolved via:
  1. The `FYODOR_BIN` environment variable (if set and non‑empty) – used as the absolute path.
  2. Otherwise, looked up via the system `PATH`.
- **Arguments:**
  1. `clippingsPath` – absolute path to the user’s `My Clippings.txt` file.
  2. `outputDir` – absolute path to a directory where Fyodor should write its output files.
- **Configuration dependency:** Fyodor uses `~/.config/fyodor/template.erb` and `~/.config/fyodor/fyodor.toml`. The Java code **ensures** `template.erb` exists (by copying the bundled version) but only **checks** for `fyodor.toml` (it does not create it – it prints a warning if missing).
- **Streams:** stdout and stderr are merged and forwarded to the console.

### 2.3.2. Inbound

- **Exit code:** `0` indicates success; any non‑zero value causes the pipeline to fail with `IOException`.
- **stdout/stderr:** Captured and printed with `[Fyodor]` prefix. Used only for logging – not parsed programmatically.
- **File output:** One or more `.jsonl` files are written to the specified output directory. Each line is a JSON object matching the `Clipping` record structure (`book_title`, `author`, `type`, `loc`, `page`, `date`, `text`). The Java code then selects the most relevant file based on the EPUB title and/or user‑supplied `--title` filter.
- **Error cases:** If `fyodor` is not installed, `ProcessBuilder.start()` throws `IOException`. If the process writes no output files, the pipeline fails with `IOException("No Fyodor output files found")`. If a JSON line is malformed, the parsing loop fails (this is a **critical gap** – it should ideally skip the line instead of failing the whole book).