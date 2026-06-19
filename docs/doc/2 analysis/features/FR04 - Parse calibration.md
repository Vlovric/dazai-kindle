# 1. Overview & Context
The user should be able to provide a calibration file where multiple heading locations are specified as a reference point for the ToC heading location calculation.
- - -
# 2. Features
- - -
## FR04_01 - Generation of calibration file

| ID            | FR04_01                                                                                                                                                                                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Generation of calibration file                                                                                                                                                                                                                                            |
| Explanation   | When provided with a flag, the system should provide the user a templated calibration file with the ToC headings of the book. The system parses the ebook ToC and outputs the headings. On the next user run the user will provide this file with values of some headings |
| Priority      |                                                                                                                                                                                                                                                                           |
| FR dependency |                                                                                                                                                                                                                                                                           |
### 2.1. Happy path
#### FR04_01-HP_01
| ID:               | FR04_01-HP_01                                                                                                                 |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Book ToC is outputted using template                                                                                          |
| **Precondition**  | book provided                                                                                                                 |
| **Trigger**       | Calibration file template flag provided                                                                                       |
| **System action** | 1. The system parses the book ToC<br>2. The system uses the default template to output the ToC structure for user calibration |
| **UI reaction**   | The path to the file is outputted                                                                                             |

### 2.2. Edge cases
#### **ID**: FR04_01-EC_01
**Scenario**: ToC is empty
	**Given** the loaded book ToC is parsed
	**When** the parsed ToC is empty
	**Then** the outputted calibration file will be empty
#### **ID**: FR04_01-EC_02
**Scenario**: Output path is invalid
	**Given** the ToC is parsed
	**When** the calibration file is constructed
	**And** the saving fails because the output path is invalid
	**Then** the system should output the error
	**And** the system should exit
#### **ID**: FR04_01-EC_03
**Scenario**: Lack of priviledge for writing to outputh path
	**Given** the ToC is parsed
	**When** the calibration file is constructed
	**And** the saving fails because of lack of write priviledge
	**Then** the system should output the error
	**And** the system should exit
### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -