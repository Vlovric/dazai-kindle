- Use Swagger with the following annotations:
	- @Tag(name, description) - for controllers
	- @Operation(summary, description) - for endpoints
	- @ApiResponse(responseCode, description) - for endpoints

- `descriptions should be high level, the code is the documentation`
# API constraints
- Authorization rules: **all endpoints public**
- Error format: `{ message: string }` - every error response below carries this body, even where a scenario's **Data** column says `—`
- - -
# stats
## GET /stats

| **Purpose:**          | Fetching all statistics for dashboard             |
| --------------------- | ------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                          |
| **Request payload:**  | —                                                 |
| **Response payload:** | highlight number<br>entry number<br>last run time |

### Success response

| **Code:** | 200                                                                             |
| --------- | ------------------------------------------------------------------------------- |
| **Data:** | `{ highlightCount: number, entryCount: number, lastRunTime: timestamp \| null}` |

- - -
# paths
## GET /paths

| **Purpose:**          | Fetching all configured filesystem paths   |
| --------------------- | ------------------------------------------ |
| **Authentication:**   | `PUBLIC`                                   |
| **Request payload:**  | —                                          |
| **Response payload:** | list of folders, for each:<br>- name<br>- path |

### Success response

| **Code:** | 200                      |
| --------- | ------------------------ |
| **Data:** | `[{ name, path }]`       |

- - -
## PUT /paths/{name}

| **Purpose:**          | Updating the filesystem path for a given folder |
| --------------------- | ----------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                        |
| **Request payload:**  | `{ path: string }`                              |
| **Response payload:** | Updated path configuration                      |

### Success response

| **Code:** | 200                |
| --------- | ------------------ |
| **Data:** | `{ name, path }`   |

### Error response

| **Scenario:** | Path name not found |
| ------------- | ------------------- |
| **Code:**     | 404                 |
| **Data:**     | —                   |

- - -
# templates
## GET /templates

| **Purpose:**          | Fetching a paginated, filtered and sorted list of templates           |
| --------------------- | --------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                              |
| **Request payload:**  | `?type=output\|heading&sort=name\|lastModified&order=asc\|desc&page=` |
| **Response payload:** | paginated list of templates                                           |

### Success response

| **Code:** | 200                                                                              |
| --------- | -------------------------------------------------------------------------------- |
| **Data:** | `{ templates: [{ name, type, lastModified }], totalPages, currentPage }`         |

### Error response

| **Scenario:** | Invalid type or sort value |
| ------------- | -------------------------- |
| **Code:**     | 400                        |
| **Data:**     | —                          |

- - -
## POST /templates

| **Purpose:**          | Uploading a new template file to the filesystem |
| --------------------- | ----------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                        |
| **Request payload:**  | `multipart/form-data` — template file           |
| **Response payload:** | Created template metadata                       |

> Per FR09_01-EC_02: a template with the same filename is silently overwritten,
> not rejected — matches `POST /files/template` / `POST /files/headingsTemplate`.

### Success response

| **Code:** | 201                            |
| --------- | ------------------------------ |
| **Data:** | `{ name, type, lastModified }` |

### Error response

| **Scenario:** | Invalid file type                      |
| ------------- | -------------------------------------- |
| **Code:**     | 400                                    |
| **Data:**     | —                                      |

- - -
## DELETE /templates

| **Purpose:**          | Deleting one or more templates from the filesystem |
| --------------------- | -------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                           |
| **Request payload:**  | `?names=foo,bar`                                   |
| **Response payload:** | —                                                  |

### Success response

| **Code:** | 204 |
| --------- | --- |
| **Data:** | —   |

### Error response

| **Scenario:** | One or more template names not found |
| ------------- | ------------------------------------ |
| **Code:**     | 404                                  |
| **Data:**     | —                                    |

- - -
## GET /templates/export

| **Purpose:**          | Downloading one or more templates; returns .zip if multiple |
| --------------------- | ----------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                    |
| **Request payload:**  | `?names=foo,bar`                                            |
| **Response payload:** | File or .zip download                                       |

### Success response

| **Code:** | 200                                               |
| --------- | ------------------------------------------------- |
| **Data:** | File download (`Content-Disposition: attachment`) |

### Error response

| **Scenario:** | One or more template names not found |
| ------------- | ------------------------------------ |
| **Code:**     | 404                                  |
| **Data:**     | —                                    |

- - -
## GET /templates/{name}

| **Purpose:**          | Fetching the raw content of a single template |
| --------------------- | --------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                      |
| **Request payload:**  | —                                             |
| **Response payload:** | Template name and content                     |

### Success response

| **Code:** | 200                 |
| --------- | ------------------- |
| **Data:** | `{ name, content }` |

### Error response

| **Scenario:** | Template not found |
| ------------- | ------------------ |
| **Code:**     | 404                |
| **Data:**     | —                  |

- - -
## POST /templates/preview

| **Purpose:**          | Rendering template content using example entries |
| --------------------- | ------------------------------------------------ |
| **Authentication:**   | `PUBLIC`                                         |
| **Request payload:**  | `{ content: string }`                            |
| **Response payload:** | Rendered output or error                         |

### Success response

| **Code:** | 200                    |
| --------- | ---------------------- |
| **Data:** | `{ rendered: string }` |

### Error response

| **Scenario:** | Template content could not be rendered |
| ------------- | -------------------------------------- |
| **Code:**     | 422                                    |
| **Data:**     | Error message                          |

- - -
# runs
## GET /runs

| **Purpose:**          | Fetching a paginated, sorted and searchable list of past runs                        |
| --------------------- | ------------------------------------------------------------------------------------ |
| **Authentication:**   | `PUBLIC`                                                                             |
| **Request payload:**  | `?search=&sort=name\|author\|highlights\|lastModified&order=asc\|desc&page=`         |
| **Response payload:** | Paginated list of runs                                                               |

### Success response

| **Code:** | 200                                                                                   |
| --------- | ------------------------------------------------------------------------------------- |
| **Data:** | `{ runs: [{ name, author, highlightCount, lastModified }], totalPages, currentPage }` |

### Error response

| **Scenario:** | Invalid sort value |
| ------------- | ------------------ |
| **Code:**     | 400                |
| **Data:**     | —                  |

- - -
## DELETE /runs

| **Purpose:**          | Deleting one or more runs and all their artifacts from the filesystem |
| --------------------- | --------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                              |
| **Request payload:**  | `?names=Dazai,Kafka`                                                  |
| **Response payload:** | —                                                                     |

### Success response

| **Code:** | 204 |
| --------- | --- |
| **Data:** | —   |

### Error response

| **Scenario:** | One or more run names not found |
| ------------- | ------------------------------- |
| **Code:**     | 404                             |
| **Data:**     | —                               |

- - -
## GET /runs/export

| **Purpose:**          | Downloading one or more runs as a .zip            |
| --------------------- | ------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                          |
| **Request payload:**  | `?names=Dazai,Kafka`                              |
| **Response payload:** | .zip file download                                |

### Success response

| **Code:** | 200                                               |
| --------- | ------------------------------------------------- |
| **Data:** | .zip download (`Content-Disposition: attachment`) |

### Error response

| **Scenario:** | One or more run names not found |
| ------------- | ------------------------------- |
| **Code:**     | 404                             |
| **Data:**     | —                               |

- - -
## GET /runs/{name}

| **Purpose:**          | Fetching metadata and artifact list for a single run |
| --------------------- | ---------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                             |
| **Request payload:**  | —                                                    |
| **Response payload:** | Run detail with all artifacts                        |

### Success response

| **Code:** | 200 |
| --------- | --- |
| **Data:** | `{ name, author, highlightCount, lastModified, artifacts: { book, calibration, output, headingsOutput, debugRun } }` each artifact: `{ name, format, location }` |

### Error response

| **Scenario:** | Run not found |
| ------------- | ------------- |
| **Code:**     | 404           |
| **Data:**     | —             |

- - -
## DELETE /runs/{name}/artifacts

| **Purpose:**          | Deleting one or more artifacts from a run |
| --------------------- | ----------------------------------------- |
| **Authentication:**   | `PUBLIC`                                  |
| **Request payload:**  | `?names=book,calibration`                 |
| **Response payload:** | —                                         |

### Success response

| **Code:** | 204 |
| --------- | --- |
| **Data:** | —   |

### Error response

| **Scenario:** | Run not found |
| ------------- | ------------- |
| **Code:**     | 404           |
| **Data:**     | —             |

| **Scenario:** | One or more artifact names not found |
| ------------- | ------------------------------------ |
| **Code:**     | 404                                  |
| **Data:**     | —                                    |

- - -
## GET /runs/{name}/export

| **Purpose:**          | Downloading one or more artifacts from a run; single file or .zip |
| --------------------- | ----------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                          |
| **Request payload:**  | `?artifacts=book,calibration`                                     |
| **Response payload:** | File or .zip download                                             |

### Success response

| **Code:** | 200                                               |
| --------- | ------------------------------------------------- |
| **Data:** | File download (`Content-Disposition: attachment`) |

### Error response

| **Scenario:** | Run not found |
| ------------- | ------------- |
| **Code:**     | 404           |
| **Data:**     | —             |

| **Scenario:** | One or more artifact names not found |
| ------------- | ------------------------------------ |
| **Code:**     | 404                                  |
| **Data:**     | —                                    |

- - -
# clippings
## GET /clippings

| **Purpose:**          | Fetching metadata of the current clippings file |
| --------------------- | ----------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                        |
| **Request payload:**  | —                                               |
| **Response payload:** | Clippings file metadata                         |

### Success response

| **Code:** | 200                          |
| --------- | ---------------------------- |
| **Data:** | `{ name, uploadedAt, path }` |

### Error response

| **Scenario:** | No clippings file uploaded yet |
| ------------- | ------------------------------ |
| **Code:**     | 404                            |
| **Data:**     | —                              |


- - -
# files

> **Implementation note (25 - Full run):** book/calibration uploads are no longer a
> flat file pool. The first upload of a new-run session creates a **draft run
> folder**, named by an opaque, server-minted `draftId`, under the library path.
> Every upload response for these two types now includes that `draftId`; the client
> must pass it back (`draftId` multipart field) on later uploads in the same session
> so they land in the same folder. If a run is never executed, the draft folder is
> deleted by a scheduled cleanup sweep once it's past a TTL (currently 24h) — there
> is no separate "delete an orphaned upload" affordance, since nothing is ever
> orphaned outside a run folder. `POST /files/clippings` is unaffected by any of
> this — see its note below.
>
> `POST /files/template` and `POST /files/headingsTemplate` are **not** part of this
> draft-run scheme: templates live flatly in the Templates storage path (see FR09),
> named by their own filename, independent of any run's lifecycle. They take no
> `draftId` and their response's `draftId` is always `null` - the ref a client passes
> back for `templateRef` is simply the template's filename.

## POST /files/book

| **Purpose:**          | Uploading a book file into a draft run, for use once that run executes |
| --------------------- | ------------------------------------------------------------------------ |
| **Authentication:**   | `PUBLIC`                                                                |
| **Request payload:**  | `multipart/form-data` — book file, plus optional `draftId` field to target an existing draft (omit to start a new one) |
| **Response payload:** | Uploaded file reference, including the draft's id                       |

### Success response

| **Code:** | 201                                  |
| --------- | ------------------------------------- |
| **Data:** | `{ name, lastModified, draftId }`    |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## POST /files/clippings

| **Purpose:**          | Uploading a clippings file, replacing the single current clippings file |
| --------------------- | ------------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                                  |
| **Request payload:**  | `multipart/form-data` — clippings file                                    |
| **Response payload:** | Uploaded file reference                                                   |

> Clippings are not run-scoped: there is one fixed clippings file on disk, and this
> endpoint always overwrites it. It takes no `draftId` and its response's `draftId`
> is always `null`.

### Success response

| **Code:** | 201                                |
| --------- | ------------------------------------ |
| **Data:** | `{ name, lastModified, draftId: null }` |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## POST /files/calibration

| **Purpose:**          | Uploading a calibration file into a draft run, for use once that run executes |
| --------------------- | --------------------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                                          |
| **Request payload:**  | `multipart/form-data` — calibration file, plus optional `draftId` field           |
| **Response payload:** | Uploaded file reference, including the draft's id                                 |

### Success response

| **Code:** | 201                                  |
| --------- | ------------------------------------- |
| **Data:** | `{ name, lastModified, draftId }`    |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## POST /files/template

| **Purpose:**          | Uploading an output template file to the Templates storage path |
| --------------------- | ----------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                          |
| **Request payload:**  | `multipart/form-data` — template file                             |
| **Response payload:** | Uploaded file reference                                           |

> Not run-scoped - always saved directly under the Templates storage path by its
> own filename, silently overwriting a same-named template. Takes no `draftId`;
> its response's `draftId` is always `null`. Since `GET /files` tells output and
> heading templates apart purely by whether the filename (before its extension)
> ends in `_h` (see FR09), this endpoint rejects filenames that end in `_h` - use
> `POST /files/headingsTemplate` for those instead.

### Success response

| **Code:** | 201                                      |
| --------- | ------------------------------------------ |
| **Data:** | `{ name, lastModified, draftId: null }`    |

### Error response

| **Scenario:** | Invalid file type, or filename ends in `_h` |
| ------------- | -------------------------------------------- |
| **Code:**     | 400                                           |
| **Data:**     | —                                             |

- - -
## POST /files/headingsTemplate

| **Purpose:**          | Uploading a headings template file to the Templates storage path |
| --------------------- | -------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                              |
| **Request payload:**  | `multipart/form-data` — headings template file                       |
| **Response payload:** | Uploaded file reference                                              |

> Not run-scoped - always saved directly under the Templates storage path by its
> own filename, silently overwriting a same-named template. Takes no `draftId`;
> its response's `draftId` is always `null`. Since `GET /files` tells output and
> heading templates apart purely by whether the filename (before its extension)
> ends in `_h` (see FR09), this endpoint rejects filenames that don't end in `_h` -
> use `POST /files/template` for those instead.

### Success response

| **Code:** | 201                                      |
| --------- | ------------------------------------------ |
| **Data:** | `{ name, lastModified, draftId: null }`    |

### Error response

| **Scenario:** | Invalid file type, or filename doesn't end in `_h` |
| ------------- | ----------------------------------------------------- |
| **Code:**     | 400                                                     |
| **Data:**     | —                                                       |

- - -
## GET /files

| **Purpose:**          | Fetching a paginated, searchable list of files of the given type, for reuse in a new run |
| --------------------- | ----------------------------------------------------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                                                                          |
| **Request payload:**  | `?type=book\|calibration\|template\|headingTemplate&search=&page=`                                                |
| **Response payload:** | Paginated list of matching files                                                                                  |

> For `type=book\|calibration`, "Choose from Library" reuses an artifact from an
> existing **completed** run (draft/in-progress runs are excluded), not a separate
> flat upload pool — the library is a collection of runs, one per book (see FR07 /
> issue 23). Each entry's `name` is that run's (book's) title, and `lastModified`
> reflects that specific artifact file, not the whole run folder.
>
> For `type=template\|headingTemplate`, entries are files listed directly from the
> Templates storage path (not runs) — `name` is the template's own filename, and
> `headingTemplate` vs `template` is decided by whether the filename (before its
> extension) ends in `_h`, per FR09. `draftId` in every entry is always `null`.

### Success response

| **Code:** | 200                                                                          |
| --------- | ----------------------------------------------------------------------------- |
| **Data:** | `{ files: [{ name, lastModified, draftId: null }], totalPages, currentPage }` |

### Error response

| **Scenario:** | Invalid or missing type |
| ------------- | ----------------------- |
| **Code:**     | 400                     |
| **Data:**     | —                       |

- - -
# execute

> **Implementation note (25 - Full run, 26 - Calibration run, 27 - Headings run):**
> `POST /execute/full`, `POST /execute/generate`, and `POST /execute/headings` are
> all implemented, and all three run **synchronously** — the request blocks until
> the pipeline finishes, then returns its result directly (202 + `{ runId }` for
> full/headings, 200 + file download for generate). `GET /execute/{runId}/logs`
> (SSE log streaming) is not implemented yet; the `runId` in the full/headings
> response is already forward-compatible with it though (see below), so wiring up
> async execution + SSE later shouldn't need another contract change.

## POST /execute/full

| **Purpose:**          | Starting a full parsing run                                                                                    |
| --------------------- | -------------------------------------------------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                                                                       |
| **Request payload:**  | `{ bookRef, calibrationRef, clippingsRef, templateRef, title?, debugMode, overwriteFyodorTemplate }`          |
| **Response payload:** | Run ID for log streaming                                                                                       |

> `bookRef` / `calibrationRef` are each resolved as *either* an existing run's title
> (reusing that artifact from a completed run in the library — what "Choose from
> Library" passes) *or* a `draftId` returned from a prior `POST /files/*` upload (a
> freshly uploaded artifact still sitting in its draft folder). `templateRef` is
> resolved differently: it's simply the template's filename in the Templates
> storage path (see the `POST /files/template` note above) - never a run title or a
> `draftId`, since templates aren't run/draft-scoped. `clippingsRef` is only checked
> for presence — the actual file used is always the single fixed clippings file (see
> `POST /files/clippings` above), not resolved from the ref's value.
>
> On success, the draft folder backing the run (whichever ref resolved to one, or
> a newly minted one if every ref pointed at existing library runs) is finalized:
> `run.json` is written and the folder is renamed to the book's title, overwriting
> any prior run for that same book. `runId` in the response is that draft's
> **opaque id**, not the final title-based folder name — it stays stable across the
> whole flow (the client already has it from the upload responses) and won't need
> to change meaning once the run needs to be addressable *before* its title is known
> (i.e. once async execution + SSE streaming are added).

### Success response

| **Code:** | 202         |
| --------- | ----------- |
| **Data:** | `{ runId }` |

### Error response

| **Scenario:** | Missing required fields  |
| ------------- | ------------------------ |
| **Code:**     | 400                      |
| **Data:**     | —                        |

| **Scenario:** | File reference not found |
| ------------- | ------------------------ |
| **Code:**     | 404                      |
| **Data:**     | —                        |

- - -
## POST /execute/generate

| **Purpose:**          | Starting a calibration file generation run                |
| --------------------- | ----------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                    |
| **Request payload:**  | `{ bookRef, debugMode }`                                    |
| **Response payload:** | The generated calibration file itself (download)            |

> `bookRef` resolves the same way as in `POST /execute/full` (either an existing
> run's title or a draftId from a prior `POST /files/book` upload). Unlike a full
> run, this endpoint never creates or finalizes a library entry: the freshly
> generated `calibration.txt` (a template with blank Kindle location fields the user
> fills in by hand) is streamed straight back as the response body
> (`Content-Disposition: attachment`) and is never written anywhere under the
> DazaiKindle folder. If `bookRef` pointed at a fresh upload rather than an existing
> completed run, that upload's scratch draft folder is deleted once the response is
> built, so nothing is left behind either way. It's on the user to save the
> downloaded file wherever they like and re-upload it (filled in) as the
> `calibrationRef` of a later `POST /execute/full` call.

### Success response

| **Code:** | 200                                                |
| --------- | --------------------------------------------------- |
| **Data:** | File download (`Content-Disposition: attachment`)   |

### Error response

| **Scenario:** | Missing required fields  |
| ------------- | ------------------------ |
| **Code:**     | 400                      |
| **Data:**     | —                        |

| **Scenario:** | File reference not found |
| ------------- | ------------------------ |
| **Code:**     | 404                      |
| **Data:**     | —                        |

- - -
## POST /execute/headings

> **Implementation note (27 - Headings run):** runs synchronously like
> `/execute/full`, and finalizes into a library run folder the same way (no
> download semantics, unlike `/execute/generate`). `bookRef`/`calibrationRef`
> resolve exactly as in `POST /execute/full`; `headingsTemplateRef` resolves
> like `templateRef` does there (a filename in the Templates storage path).
> `PipelineResult.highlightCount()` is always 0, since `HeadingsOnlyStep` exits
> the pipeline before clippings are ever parsed - `run.json` is still written
> so the run appears in the library like any other.
>
> `RunRepository.finalize()` (formerly `DraftRunService.finalize()`, since folded
> into a server-wide repository layer replacing all direct filesystem access in
> Service classes) was extended (for this issue, also benefiting `/execute/full`)
> to merge any artifact from an existing same-titled run
> folder that the new draft doesn't already have of its own - e.g. running a
> full run on a book previously used for a headings-only run keeps that run's
> `*_headings.md` (and vice versa), while an artifact the new draft already
> produced/copied (a freshly uploaded book, this run's own output) is never
> overwritten by the old one.
>
> If the book's EPUB has no usable title (no `matchedBookTitle` from clippings
> matching - always the case here - and no `epubTitle` metadata either), the run
> folder falls back to `Untitled_<runId>` rather than the bare `runId`: the
> latter is the draft folder's own current name, so resolving it as the target
> title would make `finalize()` try to move the draft onto itself.
> `finalize()` itself now also guards against target-equals-draft as a
> second line of defense, returning the draft unchanged instead of corrupting it.

| **Purpose:**          | Starting a headings-only parsing run                                  |
| --------------------- | --------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                              |
| **Request payload:**  | `{ bookRef, calibrationRef, headingsTemplateRef, debugMode }`         |
| **Response payload:** | Run ID for log streaming                                              |

### Success response

| **Code:** | 202         |
| --------- | ----------- |
| **Data:** | `{ runId }` |

### Error response

| **Scenario:** | Missing required fields  |
| ------------- | ------------------------ |
| **Code:**     | 400                      |
| **Data:**     | —                        |

| **Scenario:** | File reference not found |
| ------------- | ------------------------ |
| **Code:**     | 404                      |
| **Data:**     | —                        |

- - -
## GET /execute/{runId}/logs

> **Not yet implemented.** `/execute/full` runs synchronously for now (see note
> above); this endpoint will matter once execution becomes asynchronous.

| **Purpose:**          | SSE stream of log output for an active run         |
| --------------------- | -------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                           |
| **Request payload:**  | —                                                  |
| **Response payload:** | `text/event-stream` — log lines until run completes |

### Success response

| **Code:** | 200                                         |
| --------- | ------------------------------------------- |
| **Data:** | SSE stream of log lines until run completes |

### Error response

| **Scenario:** | Run ID not found |
| ------------- | ---------------- |
| **Code:**     | 404              |
| **Data:**     | —                |
