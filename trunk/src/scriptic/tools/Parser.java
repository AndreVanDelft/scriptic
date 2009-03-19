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



public class Parser implements ParserErrorHandler {
   protected Scanner scanner;
   public    ParserErrorHandler errorHandler;

   /* Constructors */
   public Parser (Scanner scanner) {
      this.scanner      = scanner;
      this.errorHandler = scanner.errorHandler;
   }

   public Parser (Scanner scanner, ParserErrorHandler errorHandler) {
      this.scanner      = scanner;
      this.errorHandler = errorHandler;
   }

   protected boolean expectToken (int token) {
      if (scanner.token == token) return true;
      parserError (2, (new StringBuffer())
                           .append (scanner.quotedTokenRepresentation (token))
                           .append (" expected")
                           .toString ());
      return false;
   }


   protected boolean expectTokens (int tokens []) {
      for (int i = 0; i < tokens.length; i++) {
         if (scanner.token == tokens[i]) return true;
      }

      StringBuffer message   = new StringBuffer ();
      boolean      firstTime = true;

      for (int i = 0; i < tokens.length; i++) {
         if (firstTime)
            firstTime = false;
         else if (tokens.length > 2)
            message.append (", ");

         if (tokens.length > 1 && i == tokens.length - 1)
            message.append (" or ");

         message.append (scanner.quotedTokenRepresentation (tokens[i]));
      }
      message.append (" expected");
      parserError (2, message.toString ());
      return false;
   }


   protected boolean skipToken (int token) {
      if (!expectToken (token)) return false;
      scanner.next();
      return true;
   }


   protected boolean nextToken (int token) {
      scanner.next();
      return expectToken (token);
   }


   public void parserError (int severity, String message) {
      parserError (severity, message, scanner.getPosition());
   }


   public void parserError (int severity, String message, int position) {
      parserError (severity, message, 
                   new ScannerPosition (scanner, position));
   }


   public void parserError (int severity, String message, Scanner scanner, LanguageConstruct languageConstruct) {
      parserError (severity, message, 
                   new ScannerPosition (scanner, languageConstruct.sourceStartPosition,
                                                 languageConstruct.  sourceEndPosition));
   }
   public void parserError (int severity, String message, int startPosition, int endPosition) {
      parserError (severity, message, 
                   new ScannerPosition (scanner, startPosition, endPosition));
   }


   public void parserError (int severity, String message, ScannerPosition scannerPosition) {
      if (errorHandler != null)
         errorHandler.parserError (severity, message, scannerPosition);
   }

   public int parserErrorCount () {
      if (errorHandler != null) return errorHandler.parserErrorCount ();
      return 0;
   }
}


