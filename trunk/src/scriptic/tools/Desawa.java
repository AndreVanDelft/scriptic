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

public class Desawa extends ClassesEnvironment {

   int noOfErrors;
   boolean doPrintStackTraceOnError;
   boolean doDissassemble;

   public void parserError (int severity, String message, Scanner scanner, LanguageConstruct languageConstruct) {
       parserError (severity, message, new ScannerPosition (scanner, languageConstruct.sourceStartPosition,
                                                                     languageConstruct.  sourceEndPosition));
   }
   public void parserError (int severity, String message, ScannerPosition position) {
      noOfErrors++;
      if (position!=null) {
        int column = position.tokenColumn();

        output.println (position.scanner.sourceName
                            + "(" 
                            + position.lineNumber
                            + ","
                            + column
                            + "): "
                            + message);
        output.println (position.getSourceLine());

        if (column < 1) column = 1;
        char spaces [] = new char [position.tokenColumn() - 1];
        for (int i = 0; i < spaces.length; i++) spaces[i] = ' ';
        output.println (new String (spaces) + '^');
        if (doPrintStackTraceOnError) new Exception(message).printStackTrace();
      }
      else output.println (message);
      output.flush();
   }

   public void parserError (int severity, String message) {parserError(severity, message,(ScannerPosition) null);}
   public int  parserErrorCount () {return noOfErrors;}

   public void usage () {
      System.out.println ("Usage: java Desawa [-c] [-f fileName] packageName.className ...");
   }

   public static void main (String args []) {Desawa d = new Desawa(); d.doMain(args);}
   public void doMain (String args[ ]) {
    if (args.length==0) {
        usage();
        return;
    }
    parserErrorHandler = this; // dangerous...
    initialize();

      for (int i=0; i<args.length; i++) {
          if (args[i].equals("-c")) {doDissassemble=true; continue;}
          if (args[i].equals("-f")) {
             i++;
             if (i>=args.length) {
               usage();
               return;
             }
             String fileName = args[i];
             try {
                output = new PrintWriter (new FileOutputStream(new File(fileName)));
             } catch (IOException e) {
                 System.out.println("Could not open file "+fileName);
                 System.out.println(e.toString());
                 usage();
                 System.exit(1);
             }
             continue;
          }
          String className = args[i];
          ClassType c = resolveClassNameWithDots (className, true);
          if (c==null) {
              output.println ("Could not find class :"+className);
              continue;
          }
          output.println (c.getMemberDescription (this, doDissassemble));
       }
       output.flush();
       output.close();
   }
}

/*
          c.    getShortDescription()
          c.getAttributeDescription()
          c.getVariablesDescription()
          c.  getMethodsDescription()
          c.getConstantsDescription()
*/

