# 1. Overview & Context
The system should be able to parse ToC from the following ebook formats:
- epub
- azw3
- mobi
The ToC heading locations should be computed using the user calibration file.
The user should be able to output only the ToC itself using a optional template.
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
#### FR02_01-HP_01

| ID:               | FR02_01-HP_01                                                                                                                                                |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Scenario**      | File converted from .mobi or .azw3 to .epub                                                                                                                  |
| **Precondition**  |                                                                                                                                                              |
| **Trigger**       | Supplied ebook file is .mobi or .azw3                                                                                                                        |
| **System action** | 1. Calls Calibre's ebook-convert tool on the supplied file<br>2. Calibre converts the book into .epub and saves it in the same location as was supplied from |
| **UI reaction**   | Calibre output is outputted<br>The user is notified of the successful conversion<br>The path of the converted .epub file is outputted                        |
#### FR02_01-HP_02

| ID:               | FR02_01-HP_02                                                                                                          |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Provided .mobi or .azw3 file but .epub file exists at location, no conversion needed                                   |
| **Precondition**  |                                                                                                                        |
| **Trigger**       | Supplied ebook file is .mobi or .azw3                                                                                  |
| **System action** | 1. System checks same name files at location for .epub variant<br>2. If .epub variant exists, uses the .epub file      |
| **UI reaction**   | The user is notified a .epub file was already present and will be used<br>The path of the used .epub file is outputted |
#### FR02_01-HP_03

| ID:               | FR02_01-HP_03                                                                                          |
| ----------------- | ------------------------------------------------------------------------------------------------------ |
| **Scenario**      | Provided .epub file, no conversion needed                                                              |
| **Precondition**  |                                                                                                        |
| **Trigger**       | Supplied ebook file is .epub                                                                           |
| **System action** | 1. Does nothing, uses the provided .epub file<br>2. Extension matching is case-insensitive (.EPUB works) |
| **UI reaction**   | The path of the used .epub file is outputted                                                           |
### 2.2. Edge cases
#### **ID**: FR02_01-EC_01
**Scenario**: Non supported ebook file format
	**Given** the user provided ebook file format is other than .azw3/.mobi/.epub
	**When** the system checks the file format
	**Then** the system should output a error message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_02
**Scenario**: No ebook file provided
	**Given** the user doesn't provide a ebook file
	**When** the system checks passed arguments
	**Then** the system should output a error message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_03
**Scenario**: Calibre not installed or not in PATH
	**Given** the system calls the Calibre `ebook-convert` subprocess
	**When** the call returns an error
	**Then** the system should output a error message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_04
**Scenario**: Calibre conversion fails
	**Given** the system calls the Calibre `ebook-convert` subprocess
	**When** the call returns an error
	**Then** the system should output a error message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_05
**Scenario**: Input ebook file doesn't exist
	**Given** the system tries loading the provided ebook file
	**When** the file doesn't exist at the provided path
	**Then** the system should output a error message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_06
**Scenario**: Lack of permission for writing .epub to directory
	**Given** the ebook file is converted to .epub
	**When** the system tries saving the .epub file to the path
	**And** the saving fails because of lack of permission
	**Then** the system should output a error message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_07
**Scenario**: Calibre conversion interrupted by user
	**Given** the calibre subprocess is called and running
	**When** the subprocess is interrupted (Ctrl-C)
	**Then** the system should forcibly terminate the Calibre process
	**And** the system should output a message to the user
	**And** the system should exit
#### **ID**: FR02_01-EC_08
**Scenario**: File has no extension
	**Given** the user provides a file with no extension
	**When** the system checks the file format
	**Then** the system treats it as an unsupported format (see EC_01)
	**And** the system should output an error message to the user
	**And** the system should exit
### 2.3. Entities involved

### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FR02_02 - Computing ToC heading locations

| ID            | FR02_02                                                                                                                                                                                                                                                                                      |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | The system should compute ToC heading locations based on a user calibration file                                                                                                                                                                                                             |
| Explanation   | The user will provide a calibration file with locations of some headings. The system will compute ToC heading locations and use the calibration file to calibrate the computed locations. The parsed ToC headings with their calibrated locations will be stored as objects for further use. |
| Priority      |                                                                                                                                                                                                                                                                                              |
| FR dependency |                                                                                                                                                                                                                                                                                              |
### 2.1. Happy path
#### FR02_02-HP_01

| ID:               | FR02_02-HP_01                                                                                                                   |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Computing of ToC heading locations with perfect calibration file                                                                |
| **Precondition**  | Successfully loaded .epub file<br>Parsed ToC isn't empty<br>Calibration file exists<br>Calibration file has >=2 valid locations |
| **Trigger**       | Parsing of ToC into headings                                                                                                    |
| **System action** | 1. ToC is parsed<br>2. Calibration file is parsed<br>3. ToC locations are fitted using the calibration locations                |
| **UI reaction**   | User is notified of the number of ToC headings resolved and the RMSE value                                                      |

### 2.2. Edge cases
#### **ID**: FR02_02-EC_01
**Scenario**: Parsed ToC is empty
	**Given** the .epub was successfully loaded
	**When** the ToC is parsed
	**And** the parsed ToC is empty because no valid headings were found, or no TOC file (toc.ncx / nav.xhtml) is referenced in the EPUB manifest
	**Then** the system should output an error
	**And** the system should exit
#### **ID**: FR02_02-EC_02
**Scenario**: A title has a typo
	**Given** the .epub was successfully loaded
	**And** the ToC is not empty
	**And** the calibration file exists
	**When** a calibration title has a typo
	**Then** the system should match the heading to a ToC heading if possible (starts with or contains)
	**And** include the title in the calibration if it matches a ToC heading
#### **ID**: FR02_02-EC_03
**Scenario**: Calibration file uses unsupported delimiters
	**Given** the .epub was successfully loaded
	**And** the ToC is not empty
	**And** the calibration file exists
	**When** a calibration heading uses anything other than "-", ":", "—" as a delimiter
	**Then** the heading is skipped
#### **ID**: FR02_02-EC_04
**Scenario**: Calibration file uses "-", ":", "—"  delimiters
	**Given** the .epub was successfully loaded
	**And** the ToC is not empty
	**And** the calibration file exists
	**When** a calibration heading uses any of "-", ":", "—" as a delimiter
	**Then** the heading is correctly parsed
#### **ID**: FR02_02-EC_05
**Scenario**: Calibration file headings have whitespaces
	**Given** the .epub was successfully loaded
	**And** the ToC is not empty
	**And** the calibration file exists
	**When** a calibration heading has whitespace before or after location
	**Then** the heading is correctly parsed
#### **ID**: FR02_02-EC_06
**Scenario**: Calibration file empty
	**Given** the .epub was successfully loaded
	**And** the ToC is not empty
	**When** the calibration file is empty
	**Then** the system should output a error
	**And** the system should exit
#### **ID**: FR02_02-EC_07
**Scenario**: Calibration file doesn't exist or lack of priviledge
	**Given** the .epub was successfully loaded
	**And** the ToC is not empty
	**When** the calibration file is doesn't exist or lack of priviledge for reading
	**Then** the system should output a error
	**And** the system should exit
#### **ID**: FR02_02-EC_08
**Scenario**: Calibration file heading location is not a positive integer
	**Given** the calibration file exists
	**When** a calibration file heading location is not a positive integer
	**Then** the system should output a error
	**And** the system should exit
#### **ID**: FR02_02-EC_09
**Scenario**: Calibration file has unknown headings
	**Given** the calibration file exists
	**When** a calibration file has an unknown heading
	**Then** the heading should be logged
	**And** the heading should be skipped
	**And** the system should continue
#### **ID**: FR02_02-EC_10
**Scenario**: Parsed calibration file has less than 2 locations
	**Given** the calibration file was successfully parsed
	**When** the number of user given locations is less than 2
	**Then** the system should output a error
	**And** the system should exit
#### **ID**: FR02_02-EC_11
**Scenario**: Zero variance in byte offset
	**Given** the fitting has zero variance in byte offset
	**Then** the system should output a error
	**And** the system should exit
#### **ID**: FR02_02-EC_12
**Scenario**: Slope <= 0
	**Given** when fitting the slope is <=0
	**Then** the system should output a error
	**And** the system should exit
#### **ID**: FR02_02-EC_13
**Scenario**: RMSE > 5
	**Given** after fitting the RMSE is > 5
	**Then** the system should warn the user
	**And** the system should continue
#### **ID**: FR02_02-EC_14
**Scenario**: EPUB file is structurally invalid
	**Given** the provided file has a .epub extension
	**When** the system tries to open and parse the EPUB structure (META-INF/container.xml, OPF)
	**And** the file is not a valid ZIP, or is missing required structural files
	**Then** the system should output an error
	**And** the system should exit
#### **ID**: FR02_02-EC_15
**Scenario**: EPUB metadata (title/author) is missing or unparseable
	**Given** the EPUB is successfully loaded
	**When** the OPF metadata section is absent or malformed
	**Then** the system continues with null title/author
	**And** downstream steps use fallback values (e.g. derived from filename)
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FR02_03 - Output of only ToC headings

| ID            | FR02_03                                                                                                                                                                                                 |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | The system should be able to only output the book ToC structure without entries                                                                                                                         |
| Explanation   | When provided with a flag, the system should only output the book ToC structure structure without entries. The objects should be exposed for templating. A default markdown template should be provided |
| Priority      |                                                                                                                                                                                                         |
| FR dependency |                                                                                                                                                                                                         |
### 2.1. Happy path
#### FR02_03-HP_01

| ID:               | FR02_03-HP_01                                                                                                                |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | ToC heading output without template                                                                                          |
| **Precondition**  | Book path is provided<br>A optional heading template is not provided<br>A optional output path is not provided               |
| **Trigger**       | A flag for headings only output is provided.                                                                                 |
| **System action** | 1. ToC is parsed from the book<br>2. The default markdown template is used<br>3. The ToC output is saved to default location |
| **UI reaction**   | The user is notified of the path for the output                                                                              |
#### FR02_03-HP_02
| ID:               | FR02_03-HP_02                                                                                                                                   |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | ToC heading output with template                                                                                                                |
| **Precondition**  | Book path is provided<br>A optional heading template is provided<br>A optional output path is provided<br>The needed Calibre conversion is done |
| **Trigger**       | A flag for headings only output is provided.                                                                                                    |
| **System action** | 1. ToC is parsed from the book<br>2. The provided heading template is used<br>3. The ToC output is saved to the provided location               |
| **UI reaction**   | The user is notified of the path for the output                                                                                                 |
### 2.2. Edge cases
#### **ID**: FR02_03-EC_01
**Scenario**: Invalid book provided
	**Given** a ebook path is provided
	**When** the ebook path is not a valid ebook file
	**Then** the system should output an error
	**And** the system should exit
#### **ID**: FR02_03-EC_XX
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