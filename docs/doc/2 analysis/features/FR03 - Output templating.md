# 1. Overview & Context
The user should be able to template the output of:
- the clipping entries with ToC headings
- only ToC headings
- - -
# 2. Features
- - -
## FR03_01 - Clipping entry templating

| ID            | FR03_01                                                                                                                                                                                                                                               |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Templating of clipping and ToC objects                                                                                                                                                                                                                |
| Explanation   | The user should be able to template the output of the system which exposes objects for parsed clipping entries from Fyodor and the parsed ToC headings. The template should be in FreeMarker. A default template is provided if no template is given. |
| Priority      |                                                                                                                                                                                                                                                       |
| FR dependency |                                                                                                                                                                                                                                                       |
### 2.1. Happy path
#### FR03_01-HP_01

| ID:               | FR03_01-HP_01                     |
| ----------------- | --------------------------------- |
| **Scenario**      | Valid template                    |
| **Precondition**  |                                   |
| **Trigger**       |                                   |
| **System action** | Template is rendered successfully |
| **UI reaction**   | File output is logged to the user |
#### FR03_01-HP_02

| ID:               | FR03_01-HP_02                        |
| ----------------- | ------------------------------------ |
| **Scenario**      | Template uses loops and conditionals |
| **Precondition**  |                                      |
| **Trigger**       |                                      |
| **System action** | Template is rendered successfully    |
| **UI reaction**   | File output is logged to the user    |
### 2.2. Edge cases
#### **ID**: FR03_01-EC_01
**Scenario**: Template file does not exist
	**Given** the user provided the template file
	**When** the template file loading throws an error because it doesn't exist
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_02
**Scenario**: User lacks read permission for template file
	**Given** the user provided the template file
	**When** the template file loading throws an error because of lack of priviledge
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_03
**Scenario**: Output path is invalid
	**Given** the user provided the output path
	**When** the output is saved
	**And** the output saving throws an error
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_04
**Scenario**: User lacks write permissions for saving output to path
	**Given** the user provided the output path
	**When** the output is saved
	**And** the output saving throws an error because of lack of permission
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_05
**Scenario**: Template has syntax errors
	**Given** the provided template exists
	**When** the template has syntax errors
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_06
**Scenario**: Template references non existent variables
	**Given** the provided template exists
	**When** the template references non existent variables
	**Then** the system should log the error
	**And** the system should exit
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FR03_02 - ToC heading only templating

| ID            | FR03_02                                                                                                                                                                                                                                                                                   |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Templating of ToC headings                                                                                                                                                                                                                                                                |
| Explanation   | The user should be able to template the ToC heading output if the user provided the needed flag for heading only output. A default template is provided if no template is given. Exposed objects are the headings, their level and their location. Calibration is optional for this flag. |
| Priority      |                                                                                                                                                                                                                                                                                           |
| FR dependency |                                                                                                                                                                                                                                                                                           |
### 2.1. Happy path
#### FR03_02-HP_01

| ID:               | FR03_02-HP_01                                                                    |
| ----------------- | -------------------------------------------------------------------------------- |
| **Scenario**      | Valid provided template                                                          |
| **Precondition**  | Book is loaded<br>Output template is provided                                    |
| **Trigger**       | Flag for only heading output is provided                                         |
| **System action** | 1. System parses book ToC<br>2. Outputs only the ToC using the provided template |
| **UI reaction**   | System logs where the output is saved                                            |
#### FR03_02-HP_02
| ID:               | FR03_02-HP_02                                                                   |
| ----------------- | ------------------------------------------------------------------------------- |
| **Scenario**      | Default template used                                                           |
| **Precondition**  | Book is loaded<br>Output template is not provided                               |
| **Trigger**       | Flag for only heading output is provided                                        |
| **System action** | 1. System parses book ToC<br>2. Outputs only the ToC using the default template |
| **UI reaction**   | System logs where the output is saved                                           |
### 2.2. Edge cases
#### **ID**: FR03_02-EC_01
**Scenario**: No headings found
	**Given** the book ToC is parsed
	**When** the parsed list of headings is empty
	**Then** the system logs out an error
	**And** the system exits
#### **ID**: FR03_02-EC_02
**Scenario**: Provided template doesn't exist
	**Given** the system is launched with the only ToC heading output flag
	**When** the output template can't be loaded
	**Then** the system logs out an error
	**And** the system exits
#### **ID**: FR03_02-EC_03
**Scenario**: User lacks read permission for template file
	**Given** the user provided the template file
	**When** the template file loading throws an error because of lack of priviledge
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_03-EC_04
**Scenario**: Output path is invalid
	**Given** the user provided the output path
	**When** the output is saved
	**And** the output saving throws an error
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_02-EC_05
**Scenario**: User lacks write permissions for saving output to path
	**Given** the user provided the output path
	**When** the output is saved
	**And** the output saving throws an error because of lack of permission
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_06
**Scenario**: Template has syntax errors
	**Given** the provided template exists
	**When** the template has syntax errors
	**Then** the system should log the error
	**And** the system should exit
#### **ID**: FR03_01-EC_07
**Scenario**: Template references non existent variables
	**Given** the provided template exists
	**When** the template references non existent variables
	**Then** the system should log the error
	**And** the system should exit
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe