# Compiling and running Scriptic programs #

## Compiling Scriptic sources ##

The class scriptic.tools.Sawa is the entry point for the compiler. It may be invoked in the following way (DOS command file):

`java -cp .;..\..\bin\sawa.jar;C:\\Java\jre6\lib\rt.jar scriptic.tools.Sawa %1`

When run without parameters, a usage message appears:
```
Usage:

    java sawa [-c] [-f logFileName] [-o outputDirectory] [-pc] [-ps] filespec...

where

-c      Only translate Scriptic to Java.
-pc     Print detailed class information
-ps     Print stack dump on error
-o      Specify output directory for generated class files

Scriptic source file names must end in ".sawa" or ".s.java",
and may contain at most one "*" in the filename (not in a directory name).
File names are case sensitive!
```

Unfortunately it is necessary to add the Java runtime archive in the class path explicitly.

## Executing Scriptic programs ##

To execute a Scriptic program, include the Scriptic virtual machine archive in the class path, e.g.,

`java -cp ..\..\bin\scripticvm.jar;..\bin LifeFrame`