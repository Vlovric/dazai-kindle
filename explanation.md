~/DazaiKindle/
- pathsConfig.json(?)   (fixed location)
- clippings.txt         (fixed location, overwritten on upload)
- templates             (path user configurable)
    - template.ftl
    - template_h.ftl
    - template2.ftl
    - ...
- library                (path user configurable, library aka runs)
    - book1
        - book1.epub
        - book1_calibration.txt
        - book1_output.txt
        ...
    - book2
        - ...

Lets assume all directories are empty, the user just started the app
        
execute/generate

User uploads book "MyBook" from filesystem and executes
The book and generated calibration file are saved to runs/MyBook

execute/headings

User then chooses the previously uploaded book, chooses the existing calibration file that he filled out, uploads a headings template and executes
The uploaded headings template and generated output are also saved to runs/MyBook

execute/full

The user chooses the book, calibration file, uploads clippings, uploads template and executes
The uploaded clippings file gets stored in the DazaiKindle root, the template in templates/ and the output of the execution into runs/MyBook

From now on the user can select the previously uploaded clippings file in future runs, and also any other previously uploaded artifact