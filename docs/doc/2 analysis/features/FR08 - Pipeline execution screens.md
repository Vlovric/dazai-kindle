# 1. Overview & Context
The user should be able to execute all possible pipelines through the UI. The user should be able to upload files in these screens, check flags and see outputted logs and the outputted artifacts. When a run is finished, the user can see the resulting artifacts in the Library ([[FR07 - Library management]]).
The user can choose the run type through the dashboard UI
- - -
# 2. Features
## Mandatory prerequisites for all cases
- All pipeline execution screens must stream server log messages to the UI and show errors to the user distinctly
- Currently `/execute/full` (FR08_03), `/execute/generate` (FR08_01), and `/execute/headings` (FR08_02) all run **synchronously** server-side — the HTTP request blocks until the run finishes. Live SSE log streaming per the requirement above is the target design; it isn't built yet (`GET /execute/{runId}/logs` is a stub)
- Of the three, only FR08_01 (calibration generation) produces a downloadable file instead of a library run entry — see its Explanation below
- - -
## FR08_01 - Execute calibration template generation run

| ID            | FR08_01                                                                                                                                             |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Executing a calibration template generation run through the UI                                                                                      |
| Explanation   | The user selects a book file and optionally enables debug mode, then starts a calibration generation run. The result is a downloadable calibration file, not a library entry — nothing is left behind on the server afterward, not even a scratch draft for a freshly uploaded book. The user picks where to save it (a native OS save-location picker via the File System Access API where supported, e.g. Chromium; a plain browser download elsewhere) and is responsible for re-uploading it (filled in) when running a later full run. Internal run behavior is documented in [[FR04 - Parse calibration]] |
| Priority      |                                                                                                                                                     |
| FR dependency | FR04, FR06                                                                                                                                          |
### 2.1. Happy path
#### FR08_01-HP_01

| ID:               | FR08_01-HP_01                                                                                                                                                |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Scenario**      | Successful calibration generation run                                                                                                                        |
| **Precondition**  |                                                                                                                                                              |
| **Trigger**       | User selects a book file (upload or from library) and clicks Generate                                                                                        |
| **System action** | 1. Server starts the calibration generation process<br>2. Log output is streamed to the UI via SSE<br>3. Run completes successfully<br>4. Generated calibration file is returned to the browser as a download; nothing is stored server-side, and any scratch draft created for a freshly uploaded book is deleted too |
| **UI reaction**   | Log lines appear in real time; on completion, the user is prompted to choose where on the filesystem to save the calibration file (native OS save picker via the File System Access API where supported, e.g. Chromium; a plain browser download otherwise) |

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
| Explanation   | The user selects a book file, calibration file, and headings output template, then starts a headings-only run. Logs are streamed live and the outcome is shown on completion. Unlike calibration generation (FR08_01), the result is a new (or updated) library run entry — the book, calibration file, and generated headings output become part of a run folder in the Library, not a downloadable file. If the same book was already used in a different run type (a full run, or an earlier headings-only run), see FR08_03-EC_04 for what's preserved when the two are merged. Internal run behavior is documented in [[FR02 - Parsing of ToC]] and [[FR03 - Output templating]] |
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
| Explanation   | The user selects a book file, calibration file, output template, and optionally a clippings file (defaults to last uploaded), then starts a full run. Book and calibration each resolve to either a freshly uploaded file (still sitting in its not-yet-executed draft run) or an artifact reused from an existing **completed** run in the Library — not a separate flat pool of individually browsable uploads. The output template resolves differently: it's never run/draft-scoped, and is picked directly by filename from the flat pool of templates (see FR09), never from a run's draft or another completed run. Logs are streamed live and the outcome is shown on completion. Internal run behavior is documented in [[FR01 - Parsing of entries]], [[FR02 - Parsing of ToC]], and [[FR03 - Output templating]] |
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
| **System action** | 1. Uploaded clippings file replaces the single fixed clippings file kept on disk (there's only ever one, so this automatically becomes the default for future runs) |
| **UI reaction**   | Clippings file field shows the uploaded filename; run proceeds as in HP_01                          |

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
#### **ID**: FR08_03-EC_04
**Scenario**: Prior run artifacts from a different run type on the same book
	**Given** the same book was previously used in a run of a different type (e.g. a headings-only run, or an earlier full run)
	**When** a full run (or headings-only run) is executed again for that same book, overwriting the prior run's library entry
	**Then** artifacts the prior run produced that this run doesn't produce itself (e.g. a previous headings output file, debug files) are preserved in the resulting library folder instead of being deleted
	**And** any artifact this run does produce or replace itself (a freshly uploaded book, this run's own output) overwrites the old one as usual
### 2.3. Entities involved
- [[2_1 Data Dictionary#Full run]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[full_run.png]]
[[dashboard.png]]
