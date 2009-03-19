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


public class ScannerPosition {
   public Scanner scanner;

   /* Scanner status */
   public int     token;
   public Object  tokenValue;
   public int     tokenStartPosition, tokenEndPosition;

   /* BasicScanner status */
   public int     charPosition;
   public int     lineStartPosition;
   public int     lineNumber;


   /*---------------------------- Constructors --------------------------*/

   public ScannerPosition (Scanner scanner) {
      this.scanner = scanner;
   }

   public ScannerPosition (Scanner scanner, int position) {
      this.scanner = scanner;
      convertPosition (position, position);
   }

   public ScannerPosition (Scanner scanner, int startPosition, int endPosition) {
      this.scanner = scanner;
      convertPosition (startPosition, endPosition);
   }

   protected void convertPosition (int startPosition, int endPosition) {
      tokenStartPosition = startPosition;
      tokenEndPosition   = endPosition;
      charPosition       = startPosition;

      /***** OLD  Lookup char position
      int index;
      for (index = 1; index <= scanner.highestLineNumber; index++) {
         if (scanner.lineStartPositionArray [index] > startPosition)
            break;
      }
      lineNumber        = index - 1;
      *********/
      lineNumber        = scanner.lineAtPosition (startPosition);
      lineStartPosition = scanner.lineStartPositionArray [lineNumber];
   }

   /*----------------------------- Accessing ----------------------------*/

   public int tokenColumn () {
      return tokenStartPosition - lineStartPosition + 1;
   }
   
   public String getSourceLine () {
      return scanner.getSourceLine (lineStartPosition);
   }
}
