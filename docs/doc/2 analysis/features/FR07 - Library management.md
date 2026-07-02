# 1. Overview & Context
The user should be able to view and delete all stored artifacts of uploads and of previous runs. This serves as sort of a abstraction UI layer for the filesystem.
- - -
# 2. Features
- - -
## FR07_01 - Template library

| ID            | FR07_01                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing and manipulating templates                                                                                                                                                                                                                                                                                                                                                                                              |
| Explanation   | The user should be able to view, save and delete previously stored templates. Templates are filtered into 2 categories:<br>- output templates<br>- heading templates<br>The user can filter by type and sort by modification date<br>The user can also delete templates which deletes them from the filesystem<br>The user can also choose to save a copy of a template to the filesystem in a location where the user chooses. |
| Priority      |                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| FR dependency |                                                                                                                                                                                                                                                                                                                                                                                                                                 |
### 2.1. Happy path
#### FR07_01-HP_01

| ID:               | FR07_01-HP_01                                                                                                                                      |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Viewing all templates                                                                                                                              |
| **Precondition**  | At least one template exists in the configured Templates path                                                                                      |
| **Trigger**       | User navigates to the Templates screen                                                                                                             |
| **System action** | 1. System reads all `.ftl` files from the Templates path<br>2. Files ending in `_h` are typed as heading templates, all others as output templates |
| **UI reaction**   | Templates are listed with name, type, and last modified date; filterable by type, searchable by name, sortable, and paginated                       |

#### FR07_01-HP_02

| ID:               | FR07_01-HP_02                                                                                                   |
| ----------------- | --------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Viewing a single template with rendered preview                                                                 |
| **Precondition**  | Template exists                                                                                                 |
| **Trigger**       | User clicks a template in the list                                                                              |
| **System action** | 1. System reads the template file content<br>2. System renders the template using hardcoded example entries     |
| **UI reaction**   | Template content and rendered preview are shown in readonly mode                                                |

#### FR07_01-HP_03

| ID:               | FR07_01-HP_03                                                                                 |
| ----------------- | --------------------------------------------------------------------------------------------- |
| **Scenario**      | Deleting one or more templates                                                                |
| **Precondition**  | At least one template is selected                                                             |
| **Trigger**       | User clicks Delete                                                                            |
| **System action** | 1. Confirmation popup shown<br>2. User confirms<br>3. Selected templates deleted from filesystem |
| **UI reaction**   | Deleted templates removed from the list                                                       |

#### FR07_01-HP_04

| ID:               | FR07_01-HP_04                                                                                              |
| ----------------- | ---------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Exporting one or more templates                                                                            |
| **Precondition**  | At least one template is selected                                                                          |
| **Trigger**       | User clicks Export                                                                                         |
| **System action** | 1. Server packages the selected templates (single file if one, `.zip` if multiple)<br>2. File sent as download |
| **UI reaction**   | Browser native save dialog opens for the user to choose download location                                  |

### 2.2. Edge cases
#### **ID**: FR07_01-EC_01
**Scenario**: No templates exist
	**Given** the Templates path is empty or contains no `.ftl` files
	**When** the user navigates to the Templates screen
	**Then** an empty state is shown
#### **ID**: FR07_01-EC_02
**Scenario**: Delete fails due to filesystem permission error
	**Given** the user confirmed deletion of one or more templates
	**When** the system tries to delete the files
	**And** the deletion fails due to lack of permission
	**Then** an error message is shown
	**And** the template list remains unchanged
#### **ID**: FR07_01-EC_03
**Scenario**: Template render fails
	**Given** the user clicked a template to view it
	**When** the system renders the template using example entries
	**And** the template has a syntax error or references undefined variables
	**Then** the template content is still shown
	**And** an error message is shown in place of the rendered preview
### 2.3. Entities involved
- [[2_1 Data Dictionary#Templates]]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[template_library.png]]
- - -
## FR07_02 - Book library

| ID            | FR07_02                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing and manipulating past runs                                                                                                                                                                                                                                                                                                                                                                                      |
| Explanation   | The user should be able to view all the books parsed from past runs, and all their produced artifacts. New runs overwrite past runs. The user can delete past runs, or specific artifacts from past runs. The user can also save artifacts from past runs as copies onto the filesystem.<br>The user can open the files in the filesystem via the UI.<br>If the user deletes a file, it is deleted from the filesystem. |
| Priority      |                                                                                                                                                                                                                                                                                                                                                                                                                         |
| FR dependency |                                                                                                                                                                                                                                                                                                                                                                                                                         |
### 2.1. Happy path
#### FR07_02-HP_01

| ID:               | FR07_02-HP_01                                                                                                                            |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Viewing all past runs                                                                                                                    |
| **Precondition**  | At least one past run exists in the configured Library path                                                                              |
| **Trigger**       | User navigates to the Library screen                                                                                                     |
| **System action** | 1. System reads all run folders from the Library path and collects metadata from each                                                    |
| **UI reaction**   | Runs listed with book name, author, highlight count, and date modified; searchable, sortable, and paginated                              |

#### FR07_02-HP_02

| ID:               | FR07_02-HP_02                                                                                                                        |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **Scenario**      | Viewing artifacts of a past run                                                                                                      |
| **Precondition**  | Past run exists                                                                                                                      |
| **Trigger**       | User clicks a run in the list                                                                                                        |
| **System action** | 1. System reads the run folder contents                                                                                              |
| **UI reaction**   | All artifacts shown (book file, calibration file, output, headings-only output, debug run) each with name, format, and filesystem path |

#### FR07_02-HP_03

| ID:               | FR07_02-HP_03                                             |
| ----------------- | --------------------------------------------------------- |
| **Scenario**      | Opening an artifact in the filesystem                     |
| **Precondition**  | Artifact exists and its path is known                     |
| **Trigger**       | User clicks an artifact entry                             |
| **System action** | 1. System resolves the artifact's filesystem path         |
| **UI reaction**   | The file is opened in the OS filesystem browser           |

#### FR07_02-HP_04

| ID:               | FR07_02-HP_04                                                                                          |
| ----------------- | ------------------------------------------------------------------------------------------------------ |
| **Scenario**      | Deleting one or more runs                                                                              |
| **Precondition**  | At least one run is selected                                                                           |
| **Trigger**       | User clicks Delete                                                                                     |
| **System action** | 1. Confirmation popup shown<br>2. User confirms<br>3. Selected run folders deleted from filesystem     |
| **UI reaction**   | Deleted runs removed from the list                                                                     |

#### FR07_02-HP_05

| ID:               | FR07_02-HP_05                                                                                                   |
| ----------------- | --------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Exporting one or more runs                                                                                      |
| **Precondition**  | At least one run is selected                                                                                    |
| **Trigger**       | User clicks Export                                                                                              |
| **System action** | 1. Server packages the selected run folders as `.zip`<br>2. File sent as download                               |
| **UI reaction**   | Browser native save dialog opens                                                                                |

#### FR07_02-HP_06

| ID:               | FR07_02-HP_06                                                                                               |
| ----------------- | ----------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Deleting one or more artifacts from a run                                                                   |
| **Precondition**  | At least one artifact is selected within a run                                                              |
| **Trigger**       | User clicks Delete                                                                                          |
| **System action** | 1. Confirmation popup shown<br>2. User confirms<br>3. Selected artifacts deleted from filesystem            |
| **UI reaction**   | Deleted artifacts removed from the run view                                                                 |

#### FR07_02-HP_07

| ID:               | FR07_02-HP_07                                                                                                      |
| ----------------- | ------------------------------------------------------------------------------------------------------------------ |
| **Scenario**      | Exporting one or more artifacts from a run                                                                         |
| **Precondition**  | At least one artifact is selected within a run                                                                     |
| **Trigger**       | User clicks Export                                                                                                 |
| **System action** | 1. Server packages the selected artifacts (single file if one, `.zip` if multiple or if debug run)<br>2. File sent as download |
| **UI reaction**   | Browser native save dialog opens                                                                                   |

### 2.2. Edge cases
#### **ID**: FR07_02-EC_01
**Scenario**: No past runs exist
	**Given** the Library path is empty or contains no run folders
	**When** the user navigates to the Library screen
	**Then** an empty state is shown
#### **ID**: FR07_02-EC_02
**Scenario**: Delete fails due to filesystem permission error
	**Given** the user confirmed deletion of one or more runs or artifacts
	**When** the system tries to delete the files
	**And** the deletion fails due to lack of permission
	**Then** an error message is shown
	**And** the library list remains unchanged
### 2.3. Entities involved
- [[2_1 Data Dictionary#Past run collection]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[book_library.png]]
- - -
## FR07_03 - Clippings library

| ID            | FR07_03                                                                                                                                                                            |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing the last uploaded clippings file                                                                                                                                           |
| Explanation   | The user should be able to view the name, uploaded date, and filesystem path of the last uploaded clippings file. This clippings file is taken as the default clippings file for runs, unless a new one is uploaded. |
| Priority      |                                                                                                                                                                                    |
| FR dependency |                                                                                                                                                                                    |
### 2.1. Happy path
#### FR07_03-HP_01

| ID:               | FR07_03-HP_01                                                    |
| ----------------- | ---------------------------------------------------------------- |
| **Scenario**      | Viewing the current clippings file info                          |
| **Precondition**  | A clippings file has been previously uploaded                    |
| **Trigger**       | User navigates to the Clippings file screen via the Library      |
| **System action** | 1. System reads the stored clippings file metadata               |
| **UI reaction**   | Filename, upload date, and filesystem path are shown             |

### 2.2. Edge cases
#### **ID**: FR07_03-EC_01
**Scenario**: No clippings file has been uploaded yet
	**Given** no clippings file has been uploaded
	**When** the user navigates to the Clippings file screen
	**Then** an empty state is shown
### 2.3. Entities involved
- [[2_1 Data Dictionary#Clippings file]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[clippings_library.png]]
