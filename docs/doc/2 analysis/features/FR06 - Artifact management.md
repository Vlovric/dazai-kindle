# 1. Overview & Context
The user should be able to upload artifacts needed to run the tool, and should be able to see stored past artifacts

> **Implementation note (25 - Full run):** artifacts are never stored as a separate
> flat file pool. Book/calibration/template/headingsTemplate uploads land inside a
> **draft run folder** (identified by an opaque `draftId` minted on first upload and
> reused for later uploads in the same session); clippings is the one exception —
> a single fixed file, always overwritten, not scoped to any run. "Stored past
> artifacts" (FR06_02) means reusing an artifact from an existing **completed** run
> in the library (see FR07), not a separately browsable upload history. A draft that
> never reaches a successful run is deleted by a scheduled cleanup sweep once it's
> past a TTL — there's no explicit "delete this upload" action, since nothing is
> ever left outside a run folder to begin with.
- - -
# 2. Features
- - -
## FR06_01 - Uploading artifacts

| ID            | FR06_01                                                                                                    |
| ------------- | ---------------------------------------------------------------------------------------------------------- |
| Requirement   | Uploading artifacts through the UI                                                                         |
| Explanation   | The user can upload all artifacts needed to run the tool, aka parameters and flags that the tool supports. |
| Priority      |                                                                                                            |
| FR dependency |                                                                                                            |
### 2.1. Happy path
#### FR06_01-HP_01

| ID:               | FR06_01-HP_01                                                                                                         |
| ----------------- | --------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful uploading of artifact                                                                                      |
| **Precondition**  |                                                                                                                       |
| **Trigger**       | User presses the upload button                                                                                        |
| **System action** | 1. User gets to select the file from the filesystem<br>2. After choosing the file, the file is uploaded to the system |
| **UI reaction**   | Uploaded file is visible to the user                                                                                  |
### 2.2. Edge cases
#### **ID**: FR06_01-EC_01
**Scenario**: Invalid artifact type
	**Given** the user chose a file to upload
	**When** the file is not a supported file type
	**Then** the system should reject the upload
	**And** an error message should be shown
	- Supported types: book = `.epub` / `.azw3` / `.mobi`, calibration = `.txt`, clippings = `.txt`, template = `.ftl`
#### **ID**: FR06_01-EC_02
**Scenario**: File with same name already exists
	**Given** the user chose a file to upload
	**When** a file with the same name already exists in the storage location
	**Then** the existing file is silently overwritten

> **Implementation note:** overwriting is keyed by artifact *type* within the
> current draft/target, not by matching filename — e.g. uploading a second book
> file into the same draft replaces whatever book artifact was already there
> (regardless of original filename), and uploading clippings always replaces the
> one fixed clippings file.
### 2.3. Entities involved
- [[2_1 Data Dictionary#Uploaded artifacts]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[artifact_upload.png]]
- - -
## FR06_02 - Storing artifacts

| ID            | FR06_02                                                                                                                                                      |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Requirement   | Storing artifacts for future use                                                                                                                             |
| Explanation   | The user can browse artifacts from previous **completed runs** in the UI and reuse them (book, calibration, template, headings template) in a new run without uploading again. There is no separate view/delete for individual uploads — deleting a run (FR07) removes its artifacts. |
| Priority      |                                                                                                                                                              |
| FR dependency |                                                                                                                                                              |
### 2.1. Happy path
#### FR06_02-HP_01

| ID:               | FR06_02-HP_01                                                     |
| ----------------- | ----------------------------------------------------------------- |
| **Scenario**      | Successful storing of uploaded artifact                           |
| **Precondition**  |                                                                   |
| **Trigger**       | User successfully uploaded an artifact                            |
| **System action** | 1. The system saves the artifact in the filesystem for future use |
| **UI reaction**   |                                                                   |
### 2.2. Edge cases
#### **ID**: FR06_02-EC_01
**Scenario**: Lack of permission to save artifact
	**Given** the user successfully uploaded an artifact
	**When** the system saves the artifacts and gets an error
	**Then** an error message should be shown to the user
#### **ID**: FR06_02-EC_02
**Scenario**: Configured storage path does not exist
	**Given** the user successfully uploaded an artifact
	**When** the configured storage path for that artifact type does not exist on the filesystem
	**Then** an error message should be shown to the user
### 2.3. Entities involved
- [[2_1 Data Dictionary#Stored artifacts]]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[artifact_storage.png]]
