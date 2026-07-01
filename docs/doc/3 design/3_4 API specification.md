- Use Swagger with the following annotations:
	- @Tag(name, description) - for controllers
	- @Operation(summary, description) - for endpoints
	- @ApiResponse(responseCode, description) - for endpoints

- `descriptions should be high level, the code is the documentation`
# API constraints
- Authorization rules: **all endpoints public**
- Error format:
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

### Success response

| **Code:** | 201                            |
| --------- | ------------------------------ |
| **Data:** | `{ name, type, lastModified }` |

### Error response

| **Scenario:** | Invalid file type                      |
| ------------- | -------------------------------------- |
| **Code:**     | 400                                    |
| **Data:**     | —                                      |

| **Scenario:** | Template with same name already exists |
| ------------- | -------------------------------------- |
| **Code:**     | 409                                    |
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
## POST /files/book

| **Purpose:**          | Uploading a book file to the library for use in runs |
| --------------------- | ---------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                             |
| **Request payload:**  | `multipart/form-data` — book file                    |
| **Response payload:** | Uploaded file reference                              |

### Success response

| **Code:** | 201                      |
| --------- | ------------------------ |
| **Data:** | `{ name, lastModified }` |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## POST /files/calibration

| **Purpose:**          | Uploading a calibration file to the library for use in runs |
| --------------------- | ----------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                    |
| **Request payload:**  | `multipart/form-data` — calibration file                    |
| **Response payload:** | Uploaded file reference                                     |

### Success response

| **Code:** | 201                      |
| --------- | ------------------------ |
| **Data:** | `{ name, lastModified }` |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## POST /files/template

| **Purpose:**          | Uploading an output template file to the library for use in runs |
| --------------------- | ---------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                         |
| **Request payload:**  | `multipart/form-data` — template file                            |
| **Response payload:** | Uploaded file reference                                          |

### Success response

| **Code:** | 201                      |
| --------- | ------------------------ |
| **Data:** | `{ name, lastModified }` |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## POST /files/headingsTemplate

| **Purpose:**          | Uploading a headings template file to the library for use in runs |
| --------------------- | ----------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                          |
| **Request payload:**  | `multipart/form-data` — headings template file                    |
| **Response payload:** | Uploaded file reference                                           |

### Success response

| **Code:** | 201                      |
| --------- | ------------------------ |
| **Data:** | `{ name, lastModified }` |

### Error response

| **Scenario:** | Invalid file type |
| ------------- | ----------------- |
| **Code:**     | 400               |
| **Data:**     | —                 |

- - -
## GET /files

| **Purpose:**          | Fetching a paginated, searchable list of uploaded files of a given type |
| --------------------- | ----------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                                |
| **Request payload:**  | `?type=book\|calibration&search=&page=`                                 |
| **Response payload:** | Paginated list of files                                                 |

### Success response

| **Code:** | 200                                                            |
| --------- | -------------------------------------------------------------- |
| **Data:** | `{ files: [{ name, lastModified }], totalPages, currentPage }` |

### Error response

| **Scenario:** | Invalid or missing type |
| ------------- | ----------------------- |
| **Code:**     | 400                     |
| **Data:**     | —                       |

- - -
# execute
## POST /execute/full

| **Purpose:**          | Starting a full parsing run                                                                                    |
| --------------------- | -------------------------------------------------------------------------------------------------------------- |
| **Authentication:**   | `PUBLIC`                                                                                                       |
| **Request payload:**  | `{ bookRef, calibrationRef, clippingsRef, templateRef, title?, debugMode, overwriteFyodorTemplate }`          |
| **Response payload:** | Run ID for log streaming                                                                                       |

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

| **Purpose:**          | Starting a calibration file generation run |
| --------------------- | ------------------------------------------ |
| **Authentication:**   | `PUBLIC`                                   |
| **Request payload:**  | `{ bookRef, debugMode }`                   |
| **Response payload:** | Run ID for log streaming                   |

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
## POST /execute/headings

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
