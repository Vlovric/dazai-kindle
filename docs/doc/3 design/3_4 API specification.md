- Use Swagger with the following annotations:
	- @Tag(name, description) - for controllers
	- @Operation(summary, description) - for endpoints
	- @ApiResponse(responseCode, description) - for endpoints

- `descriptions should be high level, the code is the documentation`
# API constraints
- Authorization rules: **all endpoints public**
- Error format:
- - -
**Dashboard**
~~- get stats
	- highlight number
	- entry number
	- last run time~~
**Paths**
~~- get path
	- type
	- path~~
~~- change path
	- change path for type~~
**Templates**
~~- get templates
	- name
	- last modified~~
~~- filter templates by type
	- output / heading~~
~~- sort templates
	- by name
	- by last modified~~
- ~~pagination for templates~~
- ~~upload new template~~
	- ~~saves to filesystem~~
- ~~delete one or more templates~~
- ~~export one or more templates~~
**Chosen template**
- ~~get the template~~
	- ~~name~~
	- ~~content~~
- ~~render template content~~
- ~~delete template~~
- ~~export template~~
**Library**
- ~~get book collections (folders)~~
	- name
	- author
	- no. of highlights
	- date modified
- ~~search books
- ~~paginating of books
- ~~sort books
	- by name
	- by author
	- by no. of highlights
	- by date modified
- ~~delete one or more collections
- ~~export one or more collections
**Chosen book**
- ~~get book information
	- name
	- author
	- no. of highlights
	- date modified
- ~~get all artifacts in book collection
	- book name and format and location(?)
	- calibration file name and format and location(?)
	- output file name and format and location(?)
	- headings only output name and format and location(?)
	- debug run name and format and location(?)
- ~~delete one or more artifacts
- ~~export one or more artifacts
**Clippings file**
- ~~get information
	- date of upload
	- path
**Configure new full run**
- ~~upload new book file
- ~~upload new calibration file
- ~~upload new clippings file
- ~~upload new output template file
- ~~extract book
	- paths to all uploaded files(?)
	- title of book
	- debug mode boolean
	- overwrite fyodor template boolean
**Select book file modal**
- ~~get books
	- title
	- probably path
- ~~search books by name
- ~~pagination with "Load more"~~
**Select calibration file modal**
- ~~get calibration files
	- title
	- probably path
- ~~search files by name
- ~~pagination with "Load more"
**Generate calibration file**
- ~~upload new book file
- ~~generate file
	- book path
	- debug mode boolean
**Configure New Headings Only Run**
- ~~upload new book file
- ~~upload new calibration file
- ~~upload new headings output template file
- ~~extract
	- paths of all uploaded/chosen files (?)
	- debug mode boolean
- - -
**Resources**

**stats**
GET /stats
- returns 
	- highlight number
	- entry number
	- last run time

**paths**
GET /paths
- returns
	- list of folders, for each
		- path
		- name
PUT /paths/{name}
- receives
	- name of folder
	- new path (from OS picker)
- returns
	- new path

**templates**
GET /templates?type=&sort=&page=
- receives
	- type for filter (output/heading)
	- sort criteria (desc asc support also) (by name/by last modified)
	- page number
- returns
	- list of templates, for each
		- name
		- last modified
		- type
POST /templates
- receives
	- chosen file from picker
- saves file to filesystem
- returns
	- new template object
		- template name
		- template last modofied
		- template type
DELETE /templates?names=
- receives
	- list of template names
- deletes from filesystem
GET /templates/export?names=
- receives
	- list of template names
- provides file for download (one template or .zip)
GET /templates/{name}
- receives
	- template name
- returns
	- template name
	- template content
POST /templates/preview
- receives
	- template content
- returns
	- render

**runs**
GET /runs?search=&page=&sort=
- receives
	- search text
	- page
	- sort criteria (asc desc support) (name/author/highlight no./date modified)
- returns
	- previous runs (folders), for each run
		- name
		- author
		- no. of highlights
		- date modified
DELETE /runs?names=
- receives
	- list of run names
- deletes from filesystem
GET /runs/export?names=
- receives
	- list of run names
- provides file for download (since it's a folder maybe it's a zip by default? Also if multiple)
GET /runs/{name}
- returns
	- book name
	- author
	- no. of highlights
	- date modified
	- list of artifacts
		- book
			- name
			- format
			- location
		- calibration file
			- name
			- format
			- location
		- output template
			- name
			- format
			- location
		- headings only output template
			- name
			- format
			- location
		- debug run
			- name
			- format
			- location
DELETE /runs/{name}/artifacts?names=
- receives
	- run name
	- list of artifact types
- deletes from filesystem
GET /runs/{name}/export?artifacts=
- receives
	- run name
	- list of artifact types
- provides file for download (.zip if multiple files, debug folder is .zip by default?)

**clippings**
GET /clippings
- returns
	- file name
	- date of upload
	- path

**files**
POST /files/book
- returns
	- reference/path
	- name
POST /files/calibration
- returns
	- reference/path
	- name
POST /files/clippings
- returns
	- reference/path
	- name
POST /files/template
- returns
	- reference/path
	- name
POST /files/headingsTemplate
- returns
	- reference/path
	- name
GET /files?type=&search=&page=
- receives
	- file type (book/calibration file)
	- search text
	- page number
- returns
	- file title

**execute**
POST /execute/full
- receives body with
	- book path
	- calibration file path
	- clippings file path
	- output template path
	- book title (optional)
	- debug mode boolean
	- overwrite fyodor template boolean
- returns
	- run ID
POST /execute/generate
- receives body with
	- book path
	- debug mode boolean
- returns
	- run ID
POST /execute/headings
- receives body with
	- book path
	- calibration file path
	- headings output template path
	- debug mode boolean
- returns
	- run ID
GET /execute/{runID}/logs
- receives
	- run ID
- returns
	- SSE stream
- - -
# stats
## GET /stats

| **Purpose:**          | Fetching all statistics for dashboard             |
| --------------------- | ------------------------------------------------- |
| **Authentication:**   |                                                   |
| **Request payload:**  |                                                   |
| **Response payload:** | highlight number<br>entry number<br>last run time |
### Success response

| **Code:** | 200 |
| --------- | --- |
| **Data:** |     |
# paths
## GET /paths

| **Purpose:**          | Fetching all folders with their paths and information |
| --------------------- | ----------------------------------------------------- |
| **Authentication:**   |                                                       |
| **Request payload:**  |                                                       |
| **Response payload:** | list of folders, for each:<br>- name<br>- path        |
### Success response

| **Code:** | 200 |
| --------- | --- |
| **Data:** |     |
### Error response

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |

- - -
# Endpoints
## path of endpoint

| **Purpose:**          |                    |
| --------------------- | ------------------ |
| **Authentication:**   | `PUBLIC/PROTECTED` |
| **Request payload:**  |                    |
| **Response payload:** |                    |
### Success response

| **Code:**             |     |
| --------------------- | --- |
| **Data:**             |     |
### Error response

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |
## path of endpoint

| **Purpose:**          |                    |
| --------------------- | ------------------ |
| **Authentication:**   | `PUBLIC/PROTECTED` |
| **Request payload:**  |                    |
| **Response payload:** |                    |
### Success response

| **Code:**             |     |
| --------------------- | --- |
| **Data:**             |     |
### Error response

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |
