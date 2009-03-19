/* This file is part of Sawa, the Scriptic-Java compiler
 * Copyright (C) 2009 Andre van Delft
 *
 * Sawa is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.tools;

import java.io.*;
import java.util.*;

import scriptic.tools.lowlevel.*;

public class Sawa extends CompilerEnvironment
                implements scriptic.tokens.JavaTokens, ParserErrorHandler {
   
   public ArrayList<File> sourceFiles     = new ArrayList<File> ();
   public HashMap<String, File> sourceFilesByName = new HashMap<String, File>();

   private File    currentFile;
   public  String  programName;
   public  String  currentFileArguments;
   public  String  currentSwitchArguments;
   public  boolean dontCompile       = false;
   public  boolean doVerbose         = false;
   public  boolean doPrintStackTraceOnError  = false;

   public  int     noOfErrors;
   CompilationUnit compilationUnit;
   ScripticScanner scanner;
   PrintWriter out = new PrintWriter(System.out);

   public ParserErrorHandler errorHandler;

   /*-------------------------- Constructors -------------------------*/

   public Sawa (String programName) {
      super (null); // risky...
      this.programName  = programName;
      this.errorHandler = this;
      // cannot call this(progamName, this)...would give compiliation error:
      //   Can't reference this before the superclass constructor has been called
   }


   public Sawa (String programName, ParserErrorHandler errorHandler) {
      super (null);
      this.programName  = programName;
      this.errorHandler = errorHandler;
   }

   /*-----------------------------------------------------------------*/

   public void addSourceFile (File sourceFile) {

      /* Should check that fileName does not end in a path separator...? */
      if (sourceFile.isDirectory ()) {
          System.out.println (sourceFile.getPath() + ": is a directory");
          System.out.flush();
          return;
      }
      if (!sourceFile.canRead ()) {
          System.out.println (sourceFile.getPath() + ": cannot read");
          System.out.flush();
          return;
      }
      // prevent files being named twice; also prevent different extensions
      // for one name: ".sawa" and ".s.java" have preference over ".java"
      String sourcePath = sourceFile.getPath();
      String name = null;
      String ext  = null;
      String[] extensions = {".s.java", ".sawa", ".java"};
      for (int i=0; i<extensions.length; i++) {
        if (sourcePath.endsWith(extensions[i])) {
          name = sourcePath.substring(0, sourcePath.length()-extensions[i].length());
          ext = extensions[i];
          break;
        }
      }
      if (ext == null) {
        // internal error ... 
      }
      File other = (File) sourceFilesByName.get(name);
      if (other==null) {
        sourceFiles.add (sourceFile);
        sourceFilesByName.put(name, sourceFile);
      }
      else if (other.getPath().endsWith(".java")
           && !other.getPath().endsWith(".s.java")) {
        sourceFiles.remove (other);
        sourceFiles.add (sourceFile);
        sourceFilesByName.put(name, sourceFile);
      }
   }

   /*-----------------------------------------------------------------*/

   public void processArguments (String arguments []) {
      ArrayList<String> fileNames  = new ArrayList<String> ();
      StringBuffer fileArguments   = new StringBuffer ();
      StringBuffer switchArguments = new StringBuffer ();
      boolean      argError        = false;
      
      sourceFiles     = new ArrayList<File> ();

      int index = 0;
      while (index < arguments.length) {
         String arg = arguments[index++];
         if (arg.startsWith ("-")) {
         
            /* Test for switches with parameters. */
            /* NOTE: This error checking is quite PARTIAL.
                     E.g. the command line arguments "-d -classpath"
                     would not cause an error here! */

            if (arg.equals ("-d") 
            ||  arg.equals ("-o") 
            ||  arg.equals ("-f") 
            ||  arg.equals ("-classpath")) {
               if (index >= arguments.length) {
                  System.out.println ( "Command line error - Option \"" +  arg
                                     + "\" must have a parameter");
                  argError = true;
               }
               else {
                       if (arg.equals ("-f"      )) {String fileName = arguments[index++]; 
                                                     try {
                                                        out = new PrintWriter (new FileOutputStream(fileName));
                                                     } catch (IOException e) {
                                                        System.out.println("Could not open file "+fileName);
                                                        System.out.println(e.toString());
                                                        usage();
                                                        System.exit(1);
                                                    }}
                  else if (arg.equals("-o")){outputDirectoryName = arguments[index++]; 
	                      File outputDirectory = new File(outputDirectoryName);
	                      if (!outputDirectory.exists() || !outputDirectory.isDirectory())
	                      {
		                      System.out.println("Specified output directory not found: "+outputDirectory);
		                      usage();
		                      System.exit(1);
	                      }
	              }
                  else if (arg.equals("-classpath")){classPath = arguments[index++];}
               }
            } else {
                       if (arg.equals ("-c"      )) {dontCompile              = true;}
                  else if (arg.equals ("-verbose")) {doVerbose                = true;}
                  else if (arg.equals ("-pc"     )) {doPrintClassinfo         = true;}
                  else if (arg.equals ("-ps"     )) {doPrintStackTraceOnError = true;}
                  else                              {switchArguments.append (arg);
                                                     switchArguments.append (' ');}
            }
         } else {
            /* Not a switch argument */
            if (arg.endsWith (".sawa")   && arg.length() > 5
            ||  arg.endsWith (".s.java") && arg.length() > 7
            ||  arg.endsWith (".java")   && arg.length() > 5) {

               fileNames.add (arg);
               fileArguments.append (arg);
               fileArguments.append (' ');
            } else {
               System.out.println ( "Command line error - File argument \"" +  arg
                                  + "\" must end in \".sawa\" or \".s.java\" or \".java\"" );
               argError = true;
            }
         }
      }

      if (argError || fileArguments.length() == 0) {
         currentFileArguments   = null;
         currentSwitchArguments = null;
         usage ();
         System.exit (1);
      } else {
         currentFileArguments   = fileArguments.toString();
         currentSwitchArguments = switchArguments.toString();
         
         for (String fileName: fileNames) {
            File   file     = new File (fileName);
            
            int wildcardIndex = fileName.lastIndexOf ('*');
            if (   wildcardIndex >= 0
                && wildcardIndex > fileName.lastIndexOf(File.separatorChar)) {
            
               /* Extract directory and pattern */
               FilePattern pat    = new FilePattern (file.getName());
               String      parent = file.getParent();
               File        dir    = new File (parent == null
                                                   ? System.getProperty("user.dir")
                                                   : parent);

               /* Get matching file(s) from directory */
               String [ ] fileNameList = null;
               try {
                  fileNameList = dir.list (pat);
               } catch (Exception excpt) {
                  /* A bug in java.io.File's list(fileFilter) method
                     causes an exception if the directory doesn't exist */
               }
               if (fileNameList != null) {
                 for (int n = 0; n < fileNameList.length; n++) {
                    if (parent == null)
                       addSourceFile (new File (fileNameList [n]));
                    else
                       /* Preserve directory path information */
                       addSourceFile (new File (dir, fileNameList [n]));
                       
                    /* NOTE: This will also add directories
                             whose name matches the pattern! */
                 }
               }
            } else {
               File f = new File (fileName);
               addSourceFile (f);
            }
         }
         if (sourceFiles.size() == 0) {
            System.out.println ("No files found to process");
            System.out.flush();
            usage ();
            System.exit (1);
         }
      }
   }

   /*-----------------------------------------------------------------*/

   File currentSourceFile() {
       if (compilationUnit != null) return compilationUnit.sourceFile;
       return currentFile;
   }
   Scanner currentScanner() {
       if (compilationUnit != null) return compilationUnit.scanner;
       return scanner;
   }

   public void parserError (int severity, String message, ScannerPosition position) {
      if (position!=null) {
        int column = position.tokenColumn();

        out.println (position.scanner.sourceName
                            + "(" 
                            + position.lineNumber
                            + ","
                            + column
                            + "): "
                            + message);
        out.println (position.getSourceLine());

        if (column < 1) column = 1;
        char spaces [] = new char [position.tokenColumn() - 1];
        for (int i = 0; i < spaces.length; i++) spaces[i] = ' ';
        out.println (new String (spaces) + '^');
      }
      else out.println (message);
      if (severity>=2) noOfErrors++;
      if (severity>=2) if (doPrintStackTraceOnError) new Exception(message).printStackTrace();
      out.flush();
   }

   protected void parserError (int severity, String message, CompilationUnit compilationUnit,
                                int startPosition, int endPosition) {
      parserError (severity, message, compilationUnit==null? null
                                             : new ScannerPosition (compilationUnit.scanner,
                                                                    startPosition, endPosition));
   }

   public void parserError (int severity, String message, Scanner scanner, LanguageConstruct languageConstruct) {
       parserError (severity, message, new ScannerPosition (scanner, languageConstruct.sourceStartPosition,
                                                                     languageConstruct.  sourceEndPosition));
   }
   public void parserError (int severity, String message) {parserError(severity, message,(ScannerPosition)null);}

   public int  parserErrorCount () {return noOfErrors;}

   /*-----------------------------------------------------------------*/

   public void usage () {
      System.out.println ("");
      System.out.println ("Usage:");
      System.out.println ("");
      System.out.println ("    " + programName
                                 + " [-c]"
                                 + " [-f logFileName]"
                                 + " [-o outputDirectory]"
                                 + " [-pc]"
                                 + " [-ps]"
                                 + " filespec...");
      System.out.println ("");
      System.out.println ("where");
      System.out.println ("");
      System.out.println ("-c      Only translate Scriptic to Java.");
      System.out.println ("-pc     Print detailed class information");
      System.out.println ("-ps     Print stack dump on error");
      System.out.println ("-o      Specify output directory for generated class files");
      System.out.println ("");
      System.out.println ("Scriptic source file names must end in \".sawa\" or \".s.java\",");

      /* and on Win32 platforms: */
      System.out.println ("and may contain at most one \"*\" in the filename (not in a directory name).");

      /* On ALL platforms: */
      System.out.println ("File names are case sensitive!");
      System.out.flush();
  }
   

   /*-----------------------------------------------------------------*/

   public void doMain (String args[ ]) {

      out.println ("Scriptic/Java compiler; open source software - GNU General Public License");
      processArguments (args);
      out.println ();
      out.flush();

      noOfErrors = 0;
      parserErrorHandler = this;
      doGenerateJavaCode = dontCompile;
      try {
        compile (sourceFiles);

        if (noOfErrors > 0) {
           out.println (noOfErrors + (noOfErrors == 1 ? " error" : " errors"));
           System.exit (1);
        }
      } catch (Exception e) {e.printStackTrace(out);
      } finally {out.close();
      }
   }

   /*-----------------------------------------------------------------*/

   public static void main (String args[ ]) {
      Sawa sawa = new Sawa ("java sawa");
      sawa.doMain(args);
   }
}

