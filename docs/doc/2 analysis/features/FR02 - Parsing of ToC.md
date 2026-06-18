# 1. Overview & Context
The system should be able to parse ToC from the following ebook formats:
- epub
- azw3
- mobi
The ToC heading locations should be computed using the user calibration file.
The user should be able to output only the ToC itself using a template.
- - -
# 2. Features
- - -
## FR02_01 - Ebook format conversion

| ID            | FR02_01                                                                                                                                                                                                                                          |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Requirement   | Conversion of ebook formats                                                                                                                                                                                                                      |
| Explanation   | The user can provide a .mobi/.azw3/.epub file to extract the ToC from. If the format is .mobi or .azw3 it needs to first be converted to .epub for the system to parse it. The conversion should be done with Calibre's `ebook-convert` CLI tool |
| Priority      |                                                                                                                                                                                                                                                  |
| FR dependency |                                                                                                                                                                                                                                                  |
### 2.1. Happy paths

| ID:               | FR02_01-HP_01                                                                                                                                                |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Scenario**      | File converted from .mobi or .azw3 to .epub                                                                                                                  |
| **Precondition**  |                                                                                                                                                              |
| **Trigger**       | Supplied ebook file is .mobi or .azw3                                                                                                                        |
| **System action** | 1. Calls Calibre's ebook-convert tool on the supplied file<br>2. Calibre converts the book into .epub and saves it in the same location as was supplied from |
| **UI reaction**   | Calibre output is outputted<br>The user is notified of the successful conversion<br>The path of the converted .epub file is outputted                        |

| ID:               | FR02_01-HP_02                                                                                                          |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Provided .mobi or .azw3 file but .epub file exists at location, no conversion needed                                   |
| **Precondition**  |                                                                                                                        |
| **Trigger**       | Supplied ebook file is .mobi or .azw3                                                                                  |
| **System action** | 1. System checks same name files at location for .epub variant<br>2. If .epub variant exists, uses the .epub file      |
| **UI reaction**   | The user is notified a .epub file was already present and will be used<br>The path of the used .epub file is outputted |

| ID:               | FR02_01-HP_03                                 |
| ----------------- | --------------------------------------------- |
| **Scenario**      | Provided .epub file, no conversion needed     |
| **Precondition**  |                                               |
| **Trigger**       | Supplied ebook file is .epub                  |
| **System action** | 1. Does nothing, uses the provided .epub file |
| **UI reaction**   | The path of the used .epub file is outputted  |
### 2.2. Edge cases

**ID**: FR02_01-EC_01
**Scenario**: Non supported ebook file format
	**Given** the user provided ebook file format is other than .azw3/.mobi/.epub
	**When** the system checks the file format
	**Then** the system should output a error message to the user
	**And** the system should exit

**ID**: FR02_01-EC_02
**Scenario**: No ebook file provided
	**Given** the user doesn't provide a ebook file
	**When** the system checks passed arguments
	**Then** the system should output a error message to the user
	**And** the system should exit

**ID**: FR02_01-EC_03
**Scenario**: Calibre not installed or not in PATH
	**Given** the system calls the Calibre `ebook-convert` subprocess
	**When** the call returns an error
	**Then** the system should output a error message to the user
	**And** the system should exit

**ID**: FR02_01-EC_04
**Scenario**: Calibre conversion fails
	**Given** the system calls the Calibre `ebook-convert` subprocess
	**When** the call returns an error
	**Then** the system should output a error message to the user
	**And** the system should exit

**ID**: FR02_01-EC_05
**Scenario**: Input ebook file doesn't exist
	**Given** the system tries loading the provided ebook file
	**When** the file doesn't exist at the provided path
	**Then** the system should output a error message to the user
	**And** the system should exit

**ID**: FR02_01-EC_06
**Scenario**: Lack of permission for writing .epub to directory
	**Given** the ebook file is converted to .epub
	**When** the system tries saving the .epub file to the path
	**And** the saving fails because of lack of permission
	**Then** the system should output a error message to the user
	**And** the system should exit

**ID**: FR02_01-EC_07
**Scenario**: Calibre conversion interrupted by user
	**Given** the calibre subprocess is called and running
	**When** the subprocess is interrupted (Ctrl-C)
	**Then** the system should output a message to the user
	**And** the system should exit
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FRXX_XX - Name

| ID            | FRXX_XX                                   |
| ------------- | ----------------------------------------- |
| Requirement   | verb then noun in infinitive              |
| Explanation   | Short description of purpose and use case |
| Priority      |                                           |
| FR dependency |                                           |
### 2.1. Happy path

| ID:               | FRXX_XX-HP_XX      |
| ----------------- | ------------------ |
| **Scenario**      | Name of happy path |
| **Precondition**  |                    |
| **Trigger**       |                    |
| **System action** |                    |
| **UI reaction**   |                    |
### 2.2. Edge cases
**ID**: FRXX_XX-EC_XX
**Scenario**: Name of edge case
	**Given**
	**When**
	**And**
	**Then**
	**But**
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe