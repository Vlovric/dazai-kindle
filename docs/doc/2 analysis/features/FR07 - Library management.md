# 1. Overview & Context
The user should be able to view and delete all stored artifacts of uploads and of previous runs. This serves as sort of a abstraction UI layer for the filesystem.
- - -
# 2. Features
- - -
## FR07_01 - Template library

| ID            | FR07_01                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing and manipulating templates                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Explanation   | The user should be able to view, save and delete previously stored templates. Templates are filtered into 2 categories:<br>- output templates<br>- heading templates<br>The user can filter by type, search by file name and sort by modification date<br>The user can also delete templates which deletes them from the filesystem<br>The user can also choose to save a copy of a template to the filesystem in a location where the user chooses. |
| Priority      |                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| FR dependency |                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
### 2.1. Happy path
#### FR07_01-HP_01

| ID:               | FR07_01-HP_01         |
| ----------------- | --------------------- |
| **Scenario**      | Viewing all templates |
| **Precondition**  |                       |
| **Trigger**       |                       |
| **System action** |                       |
| **UI reaction**   |                       |
### 2.2. Edge cases
#### **ID**: FRXX_XX-EC_XX
**Scenario**: Name of edge case
	**Given**
	**When**
	**And**
	**Then**
	**But**
### 2.3. Entities involved
- [[2_1 Data Dictionary#Templates]]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FR07_02 - Book library

| ID            | FR07_02                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing and manipulating past runs                                                                                                                                                                                                                                                                                                                                                                                      |
| Explanation   | The user should be able to view all the books parsed from past runs, and all their produced artifacts. New runs overwrite past runs. The user can delete past runs, or specific artifacts from past runs. The user can also save artifacts from past runs as copies onto the filesystem.<br>The user can open the files in the filesystem via the UI.<br>If the user deletes a file, it is deleted from the filesystem. |
| Priority      |                                                                                                                                                                                                                                                                                                                                                                                                                         |
| FR dependency |                                                                                                                                                                                                                                                                                                                                                                                                                         |
### 2.1. Happy path
#### FRXX_XX-HP_XX

| ID:               | FRXX_XX-HP_XX      |
| ----------------- | ------------------ |
| **Scenario**      | Name of happy path |
| **Precondition**  |                    |
| **Trigger**       |                    |
| **System action** |                    |
| **UI reaction**   |                    |
### 2.2. Edge cases
#### **ID**: FRXX_XX-EC_XX
**Scenario**: Name of edge case
	**Given**
	**When**
	**And**
	**Then**
	**But**
### 2.3. Entities involved
- [[2_1 Data Dictionary#Past run collection]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe
- - -
## FR07_03 - Clippings library

| ID            | FR07_03                                                                                                                                                                                                                                                                                                           |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing the last uploaded clippings file                                                                                                                                                                                                                                                                          |
| Explanation   | The user should be able to view the name and uploaded date of the last uploaded clippings file. The user should be able to open the clipping file in a readonly mode in the browser to inspect the entries.<br>This clippings file is taken as the default clippings file for runs, unless a new one is uploaded. |
| Priority      |                                                                                                                                                                                                                                                                                                                   |
| FR dependency |                                                                                                                                                                                                                                                                                                                   |
### 2.1. Happy path
#### FRXX_XX-HP_XX

| ID:               | FRXX_XX-HP_XX      |
| ----------------- | ------------------ |
| **Scenario**      | Name of happy path |
| **Precondition**  |                    |
| **Trigger**       |                    |
| **System action** |                    |
| **UI reaction**   |                    |
### 2.2. Edge cases
#### **ID**: FRXX_XX-EC_XX
**Scenario**: Name of edge case
	**Given**
	**When**
	**And**
	**Then**
	**But**
### 2.3. Entities involved
- [[2_1 Data Dictionary#Clippings file]]
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
Link to wireframe