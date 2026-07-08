# 1. Overview & Context
The user should be able to execute all possible pipelines through the UI. The user should be able to upload files in these screens, check flags and see outputted logs and the outputted artifacts. When a run is finished, the user can see the resulting artifacts in the Library ([[FR07 - Library management]]).
The user can choose the run type through the dashboard UI
- - -
# 2. Features
## Mandatory prerequisites for all cases
- All pipeline execution screens must stream server log messages to the UI and show errors to the user distinctly

> **Implementation note (25 - Full run, 26 - Calibration run, 27 - Headings run):** the
> backend for `/execute/full` (FR08_03), `/execute/generate` (FR08_01), and
> `/execute/headings` (FR08_02) is implemented so far, and all three run
> **synchronously** — the HTTP request blocks until the run finishes, and there is
> no SSE log streaming yet (`GET /execute/{runId}/logs` is an unimplemented stub).
> Live log streaming remains the target design; it just hasn't been built yet.
>
> Like FR08_03, FR08_02 produces a library run folder (via the same finalize step),
> not a download - it's the calibration generation run (FR08_01) that's the
> exception. `RunRepository.finalize()` (formerly `DraftRunService.finalize()`,
> since folded into a server-wide repository layer replacing all direct
> filesystem access in Service classes) now also merges over any artifact from an
> existing same-titled run folder that the new draft doesn't already have of its own
> (e.g. a full run on a book previously used for a headings-only run keeps that run's
> headings output, and vice versa), so switching between run types on the same book
> doesn't lose prior artifacts - only an artifact the new draft actually replaces
> (a freshly uploaded book, this run's own output) is skipped during the merge.
>
> Unlike FR08_03, FR08_01 doesn't produce a library entry at all: `/execute/generate`
> responds with the generated `calibration.txt` itself (`Content-Disposition:
> attachment`), and nothing is left behind in the DazaiKindle folder afterwards - not
> even a scratch draft for a freshly uploaded book. The client saves the file wherever
> the user chooses (an OS save-location picker via the File System Access API where
> supported, e.g. Chromium; a plain browser download elsewhere) and it's entirely on
> the user to hold onto it and upload it again (filled in) as the calibration file of
> a later full run.
- - -
## FR08_01 - Execute calibration template generation run

| ID            | FR08_01                                                                                                                                             |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Executing a calibration template generation run through the UI                                                                                      |
| Explanation   | The user selects a book file and optionally enables debug mode, then starts a calibration generation run. The result is a downloadable calibration file, not a library entry — the user picks where to save it and is responsible for re-uploading it (filled in) when running a later full run. Internal run behavior is documented in [[FR04 - Parse calibration]] |
| Priority      |                                                                                                                                                     |
| FR dependency | FR04, FR06                                                                                                                                          |
### 2.1. Happy path
#### FR08_01-HP_01

| ID:               | FR08_01-HP_01                                                                                                                                                |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Scenario**      | Successful calibration generation run                                                                                                                        |
| **Precondition**  |                                                                                                                                                              |
| **Trigger**       | User selects a book file (upload or from library) and clicks Generate                                                                                        |
| **System action** | 1. Server starts the calibration generation process<br>2. Log output is streamed to the UI via SSE<br>3. Run completes successfully<br>4. Generated calibration file is returned to the browser as a download; nothing is stored server-side |
| **UI reaction**   | Log lines appear in real time; on completion, the user is prompted to choose where on the filesystem to save the calibration file                            |

### 2.2. Edge cases
#### **ID**: FR08_01-EC_01
**Scenario**: Book file not selected
	**Given** the user is on the Generate Calibration File screen
	**When** no book file has been selected
	**Then** the Generate button is disabled
#### **ID**: FR08_01-EC_02
**Scenario**: Run fails
	**Given** the user started a calibration generation run
	**When** the run fails for any reason (see [[FR01 - Parsing of entries]], [[FR02 - Parsing of ToC]], [[FR04 - Parse calibration]] for specific failure causes)
	**Then** a failure notification is shown
	**And** the full log output remains visible for inspection
### 2.3. Entities involved
- [[2_1 Data Dictionary#Calibration template generation run]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[calibration_run.png]]
[[dashboard.png]]
- - -
## FR08_02 - Execute headings-only run

| ID            | FR08_02                                                                                                                                                                |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Executing a headings-only run through the UI                                                                                                                           |
| Explanation   | The user selects a book file, calibration file, and headings output template, then starts a headings-only run. Logs are streamed live and the outcome is shown on completion. Internal run behavior is documented in [[FR02 - Parsing of ToC]] and [[FR03 - Output templating]] |
| Priority      |                                                                                                                                                                        |
| FR dependency | FR02, FR03, FR06                                                                                                                                                       |
### 2.1. Happy path
#### FR08_02-HP_01

| ID:               | FR08_02-HP_01                                                                                                                                                               |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful headings-only run                                                                                                                                                |
| **Precondition**  |                                                                                                                                                                             |
| **Trigger**       | User selects book, calibration file, and headings output template (upload or from library), optionally enables debug mode, and clicks Extract                               |
| **System action** | 1. Server starts the headings-only run<br>2. Log output is streamed to the UI via SSE<br>3. Run completes successfully                                                      |
| **UI reaction**   | Log lines appear in real time; success notification shown when run completes                                                                                                |

### 2.2. Edge cases
#### **ID**: FR08_02-EC_01
**Scenario**: Required field not selected
	**Given** the user is on the Configure New Headings Only Run screen
	**When** any required field (book, calibration file, or headings output template) has not been selected
	**Then** the Extract button is disabled
#### **ID**: FR08_02-EC_02
**Scenario**: Run fails
	**Given** the user started a headings-only run
	**When** the run fails for any reason (see [[FR02 - Parsing of ToC]], [[FR03 - Output templating]] for specific failure causes)
	**Then** a failure notification is shown
	**And** the full log output remains visible for inspection
### 2.3. Entities involved
- [[2_1 Data Dictionary#Headings-only run]]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[headings_run.png]]
[[dashboard.png]]
- - -
## FR08_03 - Execute full run

| ID            | FR08_03                                                                                                                                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Executing a full parsing run through the UI                                                                                                                                                                                     |
| Explanation   | The user selects a book file, calibration file, output template, and optionally a clippings file (defaults to last uploaded), then starts a full run. Logs are streamed live and the outcome is shown on completion. Internal run behavior is documented in [[FR01 - Parsing of entries]], [[FR02 - Parsing of ToC]], and [[FR03 - Output templating]] |
| Priority      |                                                                                                                                                                                                                                 |
| FR dependency | FR01, FR02, FR03, FR06                                                                                                                                                                                                          |
### 2.1. Happy path
#### FR08_03-HP_01

| ID:               | FR08_03-HP_01                                                                                                                                                                                                                    |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful full run with default clippings file                                                                                                                                                                                  |
| **Precondition**  | A clippings file has been previously uploaded                                                                                                                                                                                    |
| **Trigger**       | User selects book, calibration file, and output template (upload or from library); clippings file pre-filled with last uploaded; user optionally sets title, debug mode, overwrite Fyodor template; clicks Extract              |
| **System action** | 1. Server starts the full run<br>2. Log output is streamed to the UI via SSE<br>3. Run completes successfully                                                                                                                    |
| **UI reaction**   | Log lines appear in real time; success notification shown when run completes                                                                                                                                                     |

#### FR08_03-HP_02

| ID:               | FR08_03-HP_02                                                                                       |
| ----------------- | --------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful full run with newly uploaded clippings file                                              |
| **Precondition**  | No clippings file previously uploaded, or user chooses to upload a different one                    |
| **Trigger**       | User uploads a new clippings file in the run configuration screen                                   |
| **System action** | 1. Uploaded clippings file is stored and used for this run<br>2. Becomes the new default clippings file |
| **UI reaction**   | Clippings file field shows the uploaded filename; run proceeds as in HP_01                          |

> **Implementation note:** there's only ever one clippings file on disk — uploading
> one always overwrites it, so "becomes the new default" is automatic rather than a
> separate step. The full-run request's clippings reference is only checked for
> presence; the file actually used is always that one fixed clippings file.
> Similarly, "book/calibration (upload or from library)" resolves to either a
> freshly uploaded artifact (still sitting in its not-yet-executed draft run) or an
> artifact reused from an existing **completed** run in the library — not a
> separate flat pool of individually browsable uploads.
>
> The output template (and, for FR08_02, the headings template) is different: it's
> never run/draft-scoped. It resolves directly by filename from the Templates
> storage path (see FR09 / FR10), which *is* a flat pool of individually browsable
> uploads — "upload or from library" for a template means either uploading a new
> file there or picking an existing one from that same pool, never from a run's
> draft or from another completed run.

### 2.2. Edge cases
#### **ID**: FR08_03-EC_01
**Scenario**: Required field not selected
	**Given** the user is on the Configure New Full Run screen
	**When** any required field (book, calibration file, or output template) has not been selected
	**Then** the Extract button is disabled
#### **ID**: FR08_03-EC_02
**Scenario**: No clippings file available and none uploaded
	**Given** no clippings file has ever been uploaded
	**When** the user has not uploaded or selected a clippings file
	**Then** the Extract button is disabled until a clippings file is provided
#### **ID**: FR08_03-EC_03
**Scenario**: Run fails
	**Given** the user started a full run
	**When** the run fails for any reason (see [[FR01 - Parsing of entries]], [[FR02 - Parsing of ToC]], [[FR03 - Output templating]] for specific failure causes)
	**Then** a failure notification is shown
	**And** the full log output remains visible for inspection
### 2.3. Entities involved
- [[2_1 Data Dictionary#Full run]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[full_run.png]]
[[dashboard.png]]
