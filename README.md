SNAC-Saxon-Extensions
=====================

Saxon HE Extensions for use with SNAC

Installation
------------

Using the Java extensions requires Saxon 9.5+ HE, which can be downloaded from the Saxon website [here](http://sourceforge.net/projects/saxon/files/ "Saxon Download Page")

The Java date parser extension provided requires Apache Commons, which is made freely available [here](http://commons.apache.org/proper/commons-lang/download_lang.cgi "Apache Commons").

To install and run, 

1.  Download the zip file of this repository and unzip
2.  Download and unzip the Saxon 9.5+ HE JAR files
3.  Download and unzip the Apache Commons JAR files
4.  Edit the saxon.sh script in the root of this directory, as directed in the file.  It requires a full path to the saxon9he.jar file, the commons-lang3-x.x.jar file, and the java/bin directory from this repository.  If you need to recompile, the source files are found in the java/src directory.
5.  Execute using: ./saxon.sh /path/to/XMLfile /path/to/XSLTfile
