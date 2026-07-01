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
- get stats
	- highlight number
	- entry number
	- last run time
**Paths**
- get path
	- type
	- path
- change path
	- change path for type
**Templates**
- get templates
	- name
	- last modified
- filter templates by type
	- output / heading
- sort templates
	- by name
	- by last modified
- pagination for templates
- upload new template
	- saves to filesystem
- delete one or more templates
- export one or more templates
**Chosen template**
- get the template
	- name
	- content
- render template content
- delete template
- export template
**Library**
- get book collections (folders)
	- name
	- author
	- no. of highlights
	- date modified
- search books
- paginating of books
- sort books
	- by name
	- by author
	- by no. of highlights
	- by date modified
- delete one or more collections
- export one or more collections
**Chosen book**
- get book information
	- name
	- author
	- no. of highlights
	- date modified
- get all artifacts in book collection
	- book name and format and location(?)
	- calibration file name and format and location(?)
	- output file name and format and location(?)
	- headings only output name and format and location(?)
	- debug run name and format and location(?)
- delete one or more artifacts
- export one or more artifacts
**Clippings file**
- get information
	- date of upload
	- path
**Configure new full run**
- upload new book file
- upload new calibration file
- upload new clippings file
- upload new output template file
- extract book
	- paths to all uploaded files(?)
	- title of book
	- debug mode boolean
	- overwrite fyodor template boolean
**Select book file modal**
- get books
	- title
	- probably path
- search books by name
- pagination with "Load more"
**Select calibration file modal**
- get calibration files
	- title
	- probably path
- search files by name
- pagination with "Load more"
**Generate calibration file**
- upload new book file
- generate file
	- book path
	- debug mode boolean
**Configure New Headings Only Run**
- upload new book file
- upload new calibration file
- upload new headings output template file
- extract
	- paths of all uploaded/chosen files (?)
	- debug mode boolean
- - -
# Endpoint
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
