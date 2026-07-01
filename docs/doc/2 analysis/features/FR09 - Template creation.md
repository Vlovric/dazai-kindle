# 1. Overview & Context
The user should be able to upload template files through the UI. Uploaded templates are saved to the Templates storage path and become visible in the Template Library ([[FR07 - Library management#FR07_01 - Template library]]). The template type is determined by the filename: files ending in `_h.ftl` are heading templates, all other `.ftl` files are output templates.
- - -
# 2. Features
- - -
## FR09_01 - Creation of output templates

| ID            | FR09_01                                                                                                                                                          |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Uploading an output template through the UI                                                                                                                      |
| Explanation   | The user can upload a FreeMarker (`.ftl`) template file that does not end in `_h`. The file is saved to the Templates storage path and listed as an output template in the Template Library. |
| Priority      |                                                                                                                                                                  |
| FR dependency | FR06, FR07_01                                                                                                                                                    |
### 2.1. Happy path
#### FR09_01-HP_01

| ID:               | FR09_01-HP_01                                                                                                                                     |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful upload of an output template                                                                                                           |
| **Precondition**  |                                                                                                                                                   |
| **Trigger**       | User clicks `+ New Template`                                                                                                                      |
| **System action** | 1. OS file picker opens<br>2. User selects a `.ftl` file whose name does not end in `_h`<br>3. File is saved to the Templates storage path        |
| **UI reaction**   | Template appears in the Templates list as output type                                                                                             |

### 2.2. Edge cases
#### **ID**: FR09_01-EC_01
**Scenario**: Selected file is not a `.ftl` file
	**Given** the user selected a file via the OS picker
	**When** the file does not have a `.ftl` extension
	**Then** the upload is rejected
	**And** an error message is shown
#### **ID**: FR09_01-EC_02
**Scenario**: Template with same name already exists
	**Given** the user selected a `.ftl` file
	**When** a template with the same filename already exists in the Templates storage path
	**Then** the existing file is silently overwritten
### 2.3. Entities involved
- [[2_1 Data Dictionary#Templates]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FR09_02 - Creation of headings templates

| ID            | FR09_02                                                                                                                                                              |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Uploading a headings template through the UI                                                                                                                         |
| Explanation   | The user can upload a FreeMarker (`.ftl`) template file whose name ends in `_h`. The file is saved to the Templates storage path and listed as a headings template in the Template Library. |
| Priority      |                                                                                                                                                                      |
| FR dependency | FR06, FR07_01                                                                                                                                                        |
### 2.1. Happy path
#### FR09_02-HP_01

| ID:               | FR09_02-HP_01                                                                                                                                       |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful upload of a headings template                                                                                                            |
| **Precondition**  |                                                                                                                                                     |
| **Trigger**       | User clicks `+ New Template`                                                                                                                        |
| **System action** | 1. OS file picker opens<br>2. User selects a `.ftl` file whose name ends in `_h`<br>3. File is saved to the Templates storage path                  |
| **UI reaction**   | Template appears in the Templates list as headings type                                                                                             |

### 2.2. Edge cases
#### **ID**: FR09_02-EC_01
**Scenario**: Selected file is not a `.ftl` file
	**Given** the user selected a file via the OS picker
	**When** the file does not have a `.ftl` extension
	**Then** the upload is rejected
	**And** an error message is shown
### 2.3. Entities involved
- [[2_1 Data Dictionary#Templates]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
