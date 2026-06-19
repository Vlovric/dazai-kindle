# 1. Overview & Context
The system should support a optional debug flag where important artifacts will be produced to support debugging
Artifacts are written in a timestamped directory
Artifacts are the following:

- entire unzipped EPUB file structure
- epub metadata
- spine of epub

- ToC entries
- Original href

- Final list of Heading objects after calibration
- List of spine file paths and their offsets

- Fyodor output directory
- absolute path where the template was installed
- full stdout + stderr of Fyodor
- Listing of all outputted Fyodor files
- information of what book was selected
- Fyodor json file parse error information

- Stats about the parsed clippings such as expected book title, count, number of entries with null location etc...
- sample of parsed clipping objects

- summary of grouping results
- all before first ToC entries (location 0 or similar entries)
- - -
# 2. Features
- - -
## FR05_01 - Debug mode

| ID            | FR05_01                                                                                     |
| ------------- | ------------------------------------------------------------------------------------------- |
| Requirement   | Debugging mode creates artifacts                                                            |
| Explanation   | The debug flag should enable the creation of artifacts in each step of the runtime process. |
| Priority      |                                                                                             |
| FR dependency |                                                                                             |
### 2.1. Happy path

| ID:               | FR05_01-HP_01          |
| ----------------- | ---------------------- |
| **Scenario**      | Debug mode             |
| **Precondition**  |                        |
| **Trigger**       | Debug flag is provided |
| **System action** |                        |
| **UI reaction**   |                        |
### 2.2. Edge cases

### 2.3. Entities involved
- [[2_1 Data Dictionary#Entitet naziv|Entity]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -