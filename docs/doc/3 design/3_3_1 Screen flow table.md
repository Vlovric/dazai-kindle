# Dashboard

| Source screen | User Action (Element)              | Event/Condition | Target screen                   |
| ------------- | ---------------------------------- | --------------- | ------------------------------- |
| Dashboard     | Clicks "Full Run"                  | —               | Configure New Full Run          |
| Dashboard     | Clicks "Generate Calibration File" | —               | Generate Calibration File       |
| Dashboard     | Clicks "Headings Only"             | —               | Configure New Headings Only Run |
# Sidebar (Any screen)

| Source screen | User Action (Element)         | Event/Condition | Target screen |
| ------------- | ----------------------------- | --------------- | ------------- |
| Any screen    | Clicks "Library" in sidebar   | —               | Library       |
| Any screen    | Clicks "New run" in sidebar   | —               | Dashboard     |
| Any screen    | Clicks "Templates" in sidebar | —               | Templates     |
| Any screen    | Clicks "Paths" in sidebar     | —               | Paths         |
# Library

| Source screen | User Action (Element)                     | Event/Condition | Target screen                            |
| ------------- | ----------------------------------------- | --------------- | ---------------------------------------- |
| Library       | Clicks "View clippings file"              | —               | Clippings file                           |
| Library       | Clicks a book entry                       | —               | Book detail                              |
| Library       | Clicks "Export" (hover element on select) | —               | OS file picker (save copy)               |
| Library       | Clicks "Delete" (hover element on select) | Files selected  | Confirmation popup → Library (refreshed) |
# Configure New Full Run

| Source screen          | User Action (Element)                                  | Event/Condition            | Target screen                                |
| ---------------------- | ------------------------------------------------------ | -------------------------- | -------------------------------------------- |
| Configure New Full Run | Clicks "Upload New File" (Select Book)                 | —                          | OS file picker                               |
| Configure New Full Run | Clicks "Choose from Library" (Select Book)             | —                          | Select Book from Library (modal)             |
| Configure New Full Run | Clicks "Upload New File" (Select Calibration File)     | —                          | OS file picker                               |
| Configure New Full Run | Clicks "Choose from Library" (Select Calibration File) | —                          | Select Calibration File from Library (modal) |
| Configure New Full Run | Clicks "Upload New File" (Select Clippings File)       | —                          | OS file picker                               |
| Configure New Full Run | Clicks "Choose Current" (Select Clippings File)        | —                          | Pre-fills with stored clippings file         |
| Configure New Full Run | Clicks "Upload New File" (Select Output Template)      | —                          | OS file picker                               |
| Configure New Full Run | Clicks "Choose from Library" (Select Output Template)  | —                          | Select File from Library (modal)             |
| Configure New Full Run | Clicks "Extract"                                       | All required fields filled | Library (book detail of new run)             |
# Select Book or Calibration file Modal

| Source screen                                | User Action (Element)                                          | Event/Condition        | Target screen                                     |
| -------------------------------------------- | -------------------------------------------------------------- | ---------------------- | ------------------------------------------------- |
| Select Book from Library (modal)             | Clicks "Confirm Selection"                                     | File selected          | Configure New Full Run                            |
| Select Book from Library (modal)             | Clicks "Cancel"                                                | —                      | Configure New Full Run                            |
| Select Calibration File from Library (modal) | Clicks "Confirm Selection"                                     | File selected          | Configure New Full Run                            |
| Select Calibration File from Library (modal) | Clicks "Cancel"                                                | —                      | Configure New Full Run                            |
# Generate Calibration File

| Source screen                   | User Action (Element)                                          | Event/Condition        | Target screen                                     |
| ------------------------------- | -------------------------------------------------------------- | ---------------------- | ------------------------------------------------- |
| Generate Calibration File       | Clicks "Upload New File" (Select Book)                         | —                      | OS file picker                                    |
| Generate Calibration File       | Clicks "Choose from Library" (Select Book)                     | —                      | Select Book from Library (modal)                  |
| Generate Calibration File       | Clicks "Generate"                                              | Required fields filled | Library (book detail of new run)                  |
# Configure New Headings Only Run

| Source screen                   | User Action (Element)                                          | Event/Condition        | Target screen                                |
| ------------------------------- | -------------------------------------------------------------- | ---------------------- | -------------------------------------------- |
| Configure New Headings Only Run | Clicks "Upload New File" (Select Book)                         | —                      | OS file picker                               |
| Configure New Headings Only Run | Clicks "Choose from Library" (Select Book)                     | —                      | Select Book from Library (modal)             |
| Configure New Headings Only Run | Clicks "Upload New File" (Select Calibration File)             | —                      | OS file picker                               |
| Configure New Headings Only Run | Clicks "Choose from Library" (Select Calibration File)         | —                      | Select Calibration File from Library (modal) |
| Configure New Headings Only Run | Clicks "Upload New File" (Select Headings Output Template)     | —                      | OS file picker                               |
| Configure New Headings Only Run | Clicks "Choose from Library" (Select Headings Output Template) | —                      | Select File from Library (modal)             |
| Configure New Headings Only Run | Clicks "Extract"                                               | Required fields filled | Library (book detail of new run)             |
# Book detail

| Source screen | User Action (Element)                     | Event/Condition | Target screen                                |
| ------------- | ----------------------------------------- | --------------- | -------------------------------------------- |
| Book detail   | Clicks file format entry                  | —               | Opens file in filesystem (external)          |
| Book detail   | Clicks "Export" (hover element on select) | —               | OS file picker (save copy)                   |
| Book detail   | Clicks "Delete" (hover element on select) | Files selected  | Confirmation popup → Book detail (refreshed) |
# Clippings file

| Source screen  | User Action (Element)        | Event/Condition | Target screen                                     |
| -------------- | ---------------------------- | --------------- | ------------------------------------------------- |
| Clippings file | Clicks "Open in file system" | —               | Opens file in filesystem (external)               |
| Clippings file | Clicks "Delete"              | —               | Confirmation popup → Clippings file (empty state) |
| Clippings file | Clicks "Export"              | —               | OS file picker (save copy)                        |
# Paths

| Source screen | User Action (Element)           | Event/Condition | Target screen                         |
| ------------- | ------------------------------- | --------------- | ------------------------------------- |
| Paths         | Clicks "Change"                 | —               | OS file picker (folder)               |
| Paths         | Clicks location element in list |                 | Folder opens in filesystem (external) |


| Source screen      | User Action (Element)            | Event/Condition | Target screen                                   |
| ------------------ | -------------------------------- | --------------- | ----------------------------------------------- |
| Templates          | Clicks "+ New Template"          | —               | OS file picker                                  |
| Templates          | Clicks a template entry          | —               | Template renderer (pre-filled)                  |
| Templates          | Clicks "Export" on hover element | Files selected  | Confirmation popup → OS file picker (save copy) |
| Templates          | Clicks "Delete" on hover element | Files selected  | Confirmation popup → Templates (refreshed)      |
| Template editor    | Clicks "Export"                  | —               | Confirmation popup → OS file picker (save copy) |
| Template editor    | Clicks "Delete"                  | —               | Confirmation popup → Templates                  |
| Confirmation popup | Confirms                         | —               | OS file picker or refreshed source screen       |
| Confirmation popup | Cancels                          | —               | Source screen                                   |
