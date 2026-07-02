# 1. Overview & Context
The user should be able to change the location where artifacts are stored by pointing to a directory. Existing content in old locations doesn't get moved, the user should manually move existing content if the user changes the storage path for an artifact. This should probably be a config file that gets updated through the UI so paths are remembered
- - -
# 2. Features
- - -
## FR10_01 - View configured paths

| ID            | FR10_01                                                                                                    |
| ------------- | ---------------------------------------------------------------------------------------------------------- |
| Requirement   | Viewing the configured filesystem paths                                                                    |
| Explanation   | The user can view the currently configured storage paths for the Library and Templates directories         |
| Priority      |                                                                                                            |
| FR dependency |                                                                                                            |
### 2.1. Happy path
#### FR10_01-HP_01

| ID:               | FR10_01-HP_01                                          |
| ----------------- | ------------------------------------------------------ |
| **Scenario**      | Viewing configured paths                               |
| **Precondition**  |                                                        |
| **Trigger**       | User navigates to the Paths screen                     |
| **System action** | 1. System reads the current paths from the config file |
| **UI reaction**   | The current Library path and Templates path are shown  |

### 2.2. Edge cases
### 2.3. Entities involved
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[paths.png]]
- - -
## FR10_02 - Change a storage path

| ID            | FR10_02                                                                                                                                                                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Requirement   | Changing the filesystem path for a storage location                                                                                                                                                                                        |
| Explanation   | The user can change the Library or Templates storage path by selecting a folder via an OS folder picker. The new path is saved to the config file and takes effect immediately. Existing content at the old path is not moved automatically. |
| Priority      |                                                                                                                                                                                                                                            |
| FR dependency | FR10_01                                                                                                                                                                                                                                    |
### 2.1. Happy path
#### FR10_02-HP_01

| ID:               | FR10_02-HP_01                                                                                              |
| ----------------- | ---------------------------------------------------------------------------------------------------------- |
| **Scenario**      | Successful path change                                                                                     |
| **Precondition**  |                                                                                                            |
| **Trigger**       | User clicks Browse next to a path field                                                                    |
| **System action** | 1. OS folder picker opens<br>2. User selects a folder<br>3. System saves the new path to the config file  |
| **UI reaction**   | The updated path is shown in the UI                                                                        |

### 2.2. Edge cases
#### **ID**: FR10_02-EC_01
**Scenario**: Selected path does not exist or is inaccessible
	**Given** the user selected a folder via the OS picker
	**When** the system validates the selected path
	**And** the path does not exist or cannot be accessed
	**Then** an error message is shown to the user
	**And** the path remains unchanged
#### **ID**: FR10_02-EC_02
**Scenario**: Config file cannot be written
	**Given** the user selected a valid folder
	**When** the system tries to save the new path to the config file
	**And** the write fails due to lack of permission or other error
	**Then** an error message is shown to the user
	**And** the path remains unchanged
### 2.3. Entities involved
- ... 
### 2.4. Activity diagram
Link to diagram
### 2.5. Wireframe
[[paths.png]]
