- `entities of the system`
- `high level business logic`
# Uploaded artifacts
| Artifact                | Purpose / Usage                                        | Rules / Validations |
| :---------------------- | :----------------------------------------------------- | :------------------ |
| Output template         | For defining the output file structure of a normal run | .ftl                |
| Heading output template | For defining the output heading only file structure    | .ftl                |
| Clippings file          | Kindle clippings                                       | .txt                |
| Ebook                   | Ebook in a supported file format                       | .azw3/.mobi/.epub   |
| Calibration file        | Filled previously generated calibration file for book  | .txt                |
# Stored artifacts
| Artifact                     | Purpose / Usage                                        | Rules / Validations |
| :--------------------------- | :----------------------------------------------------- | :------------------ |
| Output template              | For defining the output file structure of a normal run | .ftl                |
| Heading output template      | For defining the output heading only file structure    | .ftl                |
| Clippings file               | Last uploaded Kindle clippings file                    | .txt                |
| Ebook                        | Ebook in a supported file format                       | .azw3/.mobi/.epub   |
| Calibration file             | Last uploaded calibration file for book                | .txt                |
| Last output for book         | Last output result for ebook                           |                     |
| Last heading output for book | Last heading output result for ebook                   |                     |
| Last debug run for book      | Last debug output result for ebook                     |                     |
# Templates
| Artifact                | Purpose / Usage                                        | Rules / Validations |
| :---------------------- | :----------------------------------------------------- | :------------------ |
| Output template         | For defining the output file structure of a normal run | .ftl                |
| Heading output template | For defining the output heading only file structure    | .ftl                |

# Past run collection
- for every book

| Artifact         | Purpose / Usage                                          | Rules / Validations |
| :--------------- | :------------------------------------------------------- | :------------------ |
| Ebook            | The used ebook                                           | .azw3/.mobi/.epub   |
| Calibration file | The filled calibration file used for the run             | .txt                |
| Output           | The output of a full run                                 |                     |
| Heading output   | The output of a headings only run                        |                     |
| Debug folder     | The debug folder produced from a run with the debug flag |                     |
# Clippings file
| Atribute    | Purpose / Usage | Rules / Validations |
| :---------- | :-------------- | :------------------ |
| Name        |                 |                     |
| Upload date |                 |                     |
# Calibration template generation run
| Atribute         | Purpose / Usage              | Rules / Validations |
| :--------------- | :--------------------------- | :------------------ |
| Book             | --book                       |                     |
| Run mode boolean | --print-calibration-template |                     |
| Debug boolean    | --debug                      | Optional            |

# Headings-only run
| Atribute          | Purpose / Usage     | Rules / Validations |
| :---------------- | :------------------ | :------------------ |
| Book              | --book              |                     |
| Calibration file  | --calibrate         |                     |
| Run mode boolean  | --headings-only     |                     |
| Headings template | --headings-template |                     |
| Debug boolean     | --debug             | Optional            |
# Full run
| Atribute                          | Purpose / Usage             | Rules / Validations |
| :-------------------------------- | :-------------------------- | :------------------ |
| Book                              | --book                      |                     |
| Calibration file                  | --calibrate                 |                     |
| Clippings file                    | --clippings                 |                     |
| Output template                   | --template                  |                     |
| Title of the book                 | --title                     | Optional            |
| Debug boolean                     | --debug                     | Optional            |
| Fyodor overwrite template boolean | --overwrite-fyodor-template | Optional            |