# 1. Overview & Context
Parsed entries are received from the Fyodor subprocess
The parsed entries should be:
- highlights
- notes
- clippings
- bookmarks
The parsed entries should be stored as objects for further use and manipulation.
- - -
# 2. Features
- - -
## Mandatory prerequisites for all cases
- user calibration file
- output template file
- book file
- clippings file

## FR01_01 - Entry parsing

| ID            | FR01_01                                                                                                                                                                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Requirement   | Parsing of kindle entries into objects                                                                                                                                                                                                     |
| Explanation   | The user provides the kindle clippings file. Fyodor is used to parse the clippings file into entries for each ebook in the clippings file. The targeted book is selected and the entries of the book are loaded into the system as objects |
| Priority      |                                                                                                                                                                                                                                            |
| FR dependency |                                                                                                                                                                                                                                            |
### 2.1. Happy path
#### FR01_01-HP_01

| ID:               | FR01_01-HP_01                                                                                                                                               |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | All entries parsed into objects                                                                                                                             |
| **Precondition**  | Fyodor installed<br>All files provided                                                                                                                      |
| **Trigger**       | The user starts the tool                                                                                                                                    |
| **System action** | 1. Arguments validated<br>2. Fyodor subprocess creates .json files<br>3. Right clippings json file is loaded<br>4. All entries parsed into Clipping objects |
| **UI reaction**   | User notified of success                                                                                                                                    |
#### FRXX_XX-HP_02

| ID:               | FR01_01-HP_02                                                                                                                                               |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | All entries parsed into objects, FYODOR_BIN environment variable set                                                                                        |
| **Precondition**  | Fyodor installed<br>All files provided                                                                                                                      |
| **Trigger**       | The user starts the tool                                                                                                                                    |
| **System action** | 1. Arguments validated<br>2. Fyodor subprocess creates .json files<br>3. Right clippings json file is loaded<br>4. All entries parsed into Clipping objects |
| **UI reaction**   | User notified of success                                                                                                                                    |
#### FR01_01-HP_03

| ID:               | FR01_01-HP_03                                                                                                                                                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | No title filter provided                                                                                                                                                              |
| **Precondition**  | Fyodor installed<br>All files provided                                                                                                                                                |
| **Trigger**       | The user starts the tool                                                                                                                                                              |
| **System action** | 1. Arguments validated<br>2. Fyodor subprocess creates .json files<br>3. Right clippings json file is loaded using the EPUB book title<br>4. All entries parsed into Clipping objects |
| **UI reaction**   | User notified of success                                                                                                                                                              |
#### FR01_01-HP_04

| ID:               | FR01_01-HP_04                                                                                                                                                                                               |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Multiple books in clippings file                                                                                                                                                                            |
| **Precondition**  | Fyodor outputs separate .json files                                                                                                                                                                         |
| **Trigger**       | Multiple .json files in output location                                                                                                                                                                     |
| **System action** | 1. Fyodor uses the title from the .toml file for the .json title structure <br>2. System matches .json title using provided title filter or epub book title<br>3. json file loaded<br>4. All entries parsed |
| **UI reaction**   | Outputted which book is selected<br>User notified of success                                                                                                                                                |
#### FR01_01-HP_05

| ID:               | FR01_01-HP_05                  |
| ----------------- | ------------------------------ |
| **Scenario**      | Fyodor toml file doesn't exist |
| **Precondition**  |                                |
| **Trigger**       | Fyodor toml file doesn't exist |
| **System action** | The program continues normally |
| **UI reaction**   |                                |
### 2.2. Edge cases
#### **ID**: FR01_01-EC_01
**Scenario**: Existing different fyodor template
	**Given** a fyodor template exists
	**And** a flag for overwriting wasn't specified
	**When** the fyodor template has different content than the needed default template
	**Then** the user should be warned a different template exists
	**And** the system should exit
#### **ID**: FR01_01-EC_02
**Scenario**: Fyodor template doesn't exist
	**Given** a fyodor template does not exist
	**When** the program runs
	**Then** a fyodor template should be saved to the config file
	**And** the system should output the template has been saved at path
#### **ID**: FR01_01-EC_03
**Scenario**: Fyodor template saving failed
	**Given** a fyodor template does not exist
	**When** the fyodor template is being saved to the config file
	**And** an error is returned because of lack of permission or other
	**Then** the error should be outputted to the user
	**And** the system should exit
#### **ID**: FR01_01-EC_04
**Scenario**: Fyodor not installed
	**Given** program ran
	**When** fyodor is not installed
	**Then** the system should output the error
	**And** the system should exit
#### **ID**: FR01_01-EC_05
**Scenario**: No books in clippings file
	**Given** the clippings file exists
	**When** the clippings file is empty
	**And** fyodor produces no output files
	**Then** the system should output the error
	**And** the system should exit
#### **ID**: FR01_01-EC_06
**Scenario**: Clippings file does not exist
	**Given** all arguments are provided
	**When** the clippings file does not exist
	**Then** the system should output the error
	**And** the system should exit
#### **ID**: FR01_01-EC_07
**Scenario**: Fyodor fails with error code
	**Given** fyodor is called successfully
	**When** foyodor returns an error code
	**Then** the system should output the error
	**And** the system should exit
#### **ID**: FR01_01-EC_08
**Scenario**: Fyodor doesn't fail but writes out to stderr
	**Given** fyodor is called successfully
	**When** fyodor completes the process
	**And** prints out to stderr
	**Then** the system should output the log
	**And** the system should continue
#### **ID**: FR01_01-EC_09
**Scenario**: Stale .json fyodor files
	**Given** the fyodor has previously been successfully called
	**When** a previous .json file exists for the book
	**Then** it should be overwritten
	**And** the system should load the newly written .json file
#### **ID**: FR01_01-EC_10
**Scenario**: Book title from EPUB missing and no provided title
	**Given** the book title from the EPUB is empty
	**And** the user didn't provide a title
	**When** the program runs
	**Then** the system should output an error
	**And** the system should exit
#### **ID**: FR01_01-EC_11
**Scenario**: Two files with equal title match
	**Given** fyodor .json files have been generated
	**When** two files have an equally matched title
	**Then** the system should pick the newer file
	**And** the system should output which file it picked and why
#### **ID**: FR01_01-EC_12
**Scenario**: Malformed JSON line in fyodor output
	**Given** fyodor clippings file is loaded
	**When** a JSON file is malformed
	**Then** the system should output an error with the line
	**And** the system should exit
#### **ID**: FR01_01-EC_13
**Scenario**: Location in JSON line is null
	**Given** fyodor clippings file is loaded
	**When** a entry location is null
	**Then** the system should treat it as location 0
	**And** the system should output which line has a location null
#### **ID**: FR01_01-EC_14
**Scenario**: User provided title does not match any book
	**Given** the user provides a title through an argument
	**When** no such book is found
	**Then** the system should output an error
	**And** the system should exit
#### **ID**: FR01_01-EC_XX
**Scenario**: Name of edge case
	**Given**
	**When**
	**And**
	**Then** the system should output the error
	**And** the system should exit
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe