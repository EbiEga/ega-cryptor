# ega-cryptor
Enables submitters to produce EGA compliant files. Multiple files will be processed in parallel.
Application will generate 3 files .md5, .gpg and gpg.md5 for each source file respectively.

E.g. After encryption of **source_file.bam** these files will be generated **source_file.bam.md5**, **source_file.bam.gpg**, **source_file.bam.gpg.md5** 

# How to run
Make sure Java 1.8 or above version is installed on your system. type **java -version** and press enter to
check what java version is installed. Download the **ega-cryptor-2.0.0.jar** and navigate to folder in which jar has been downloaded.
- ##### Get help options
  It displays all available options.
  ```
  java -jar ega-cryptor-2.0.0.jar --h
  ```
- ##### Single File To Encrypt
  It will generate output files at same location as that of source file.
  ```
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file.bam"
  ```
  It will generate output files in the target directory specified in outputFolderPath.
  ```
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file.bam" --o="/path/to/output/folder"
  ```
- ##### Multiple Files or/and Folders To Encrypt
  Specify multiple files comma separated. It will generate output files at same location as that of source files.
  ```
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file1.bam, /path/to/file/tobe/encrypted/file2.bam, /path/to/folder/tobe/encrypted/test1, /path/to/folder/tobe/encrypted/test2"
  ```
  It will generate output files in the target directory specified in outputFolderPath.
  ```
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file1.bam, /path/to/folder/tobe/encrypted/test1" --o="/path/to/output/folder"
  ```
- ##### Process multiple files in parallel - You can specify one of the the 3 options to allow application to   determine suitable no. of threads to be created.
  Allow application to use full resource capacity
  ```  
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file1.bam, /path/to/folder/tobe/encrypted/test1" --o="/path/to/output/folder" -f
  ```
  Allow application to use moderate (75%) resource capacity
  ```  
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file1.bam, /path/to/folder/tobe/encrypted/test1" --o="/path/to/output/folder" -m
  ```
  Allow application to use low/half (50%) resource capacity
  ```  
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file1.bam, /path/to/folder/tobe/encrypted/test1" --o="/path/to/output/folder" -l
  ```
  Allow application to use user specified resources/threads
  ```  
  java -jar ega-cryptor-2.0.0.jar --i="/path/to/file/tobe/encrypted/file1.bam, /path/to/folder/tobe/encrypted/test1" --o="/path/to/output/folder" -t=3
  ```
  *Note if no option will be provided, application will process files sequenctially using single thread.