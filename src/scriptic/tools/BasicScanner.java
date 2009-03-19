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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class BasicScanner {

             char [ ] charBuffer;
             int      charPosition;
             int      totalCharacters;

   public int         lineStartPosition;
   public int [ ]     lineStartPositionArray;
   public int         lineNumber;
   public int         highestLineNumber;
   public String      sourceName;
   private File       sourceFile;

   /*---------------------------- Constructors --------------------------*/

   public BasicScanner () {initialize ();}

   /*--------------------------- Initialization -------------------------*/

   /* Set the scanner to its initial (empty) state. */

   protected void initialize () {

      charBuffer             = new char [0];
      charPosition           = 0;
      totalCharacters        = 0;

      lineStartPosition      = 0;
      lineNumber             = 1;
      highestLineNumber      = 1;
      lineStartPositionArray = new int [2];
   }

   // in case of a sourceFile, the charBuffer may be dropped and reloaded later...
   void dropCharBuffer() {
      if (sourceFile != null) charBuffer = null;
   }
   char[] getCharBuffer() {
      if (sourceFile != null
      &&  charBuffer == null) {
        try {
          readFromSourceFile();
        }
        catch (Exception e) {
           e.printStackTrace();
        }
        processUnicodeEscapes();
      }
      return charBuffer;
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   /* Set the source text to be scanned. Does not process Unicode escapes. */

   public void setString (String input, String sourceName) {

      this.sourceName   = sourceName;
      totalCharacters   = input.length();
      charPosition      = 0;
	   charBuffer   = new char [totalCharacters + 10];
   	input.getChars (0, totalCharacters, charBuffer, 0);
      
      lineStartPosition      = 0;
      lineNumber             = 1;
      highestLineNumber      = 1;
      lineStartPositionArray = new int [256];
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   /* Read the source file to be scanned into the char buffer. 
      Does not process Unicode escapes. */

   public void setFile (File file) 
                           throws FileNotFoundException, IOException {
      initialize ();
      this.sourceFile   = file;
      this.sourceName   = file.getPath();
      readFromSourceFile();
      highestLineNumber      = 1;
      lineStartPositionArray = new int [256];
   }

   private void readFromSourceFile() throws FileNotFoundException, IOException {

      /* Read the whole file all at once. This is the fastest way to read files. */
      FileInputStream inputStream = new FileInputStream (sourceFile);
      byte [ ]  byteBuffer;
      try {
         int noOfBytes  = inputStream.available ();
         byteBuffer     = new byte [noOfBytes];
         inputStream.read (byteBuffer);
      } finally {
         inputStream.close();
      }

      /* Copy bytes to chars, one byte at a time.
         There is a much faster function for doing this in the Java library...
         but it's not accessible from Java *@#*%#$#@ !  */
      totalCharacters   = byteBuffer.length;
      charPosition      = 0;
	   charBuffer        = new char [totalCharacters + 10];

      for (int index = 0; index < totalCharacters; index++ )
         charBuffer [index] = (char) (byteBuffer [index] & 0xff);
     
      lineStartPosition      = 0;
      lineNumber             = 1;
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   /* Process Unicode escapes into char values. Optimized for the cases that
      the source contains no Unicode escapes or only a few. */

   public void processUnicodeEscapes () {

      boolean  seenAnyUnicodes      = false;
      char [ ] newCharBuffer        = null;
      int      sourceChunkStartPos  = 0;
      int      sourceChunkEndPos    = 0;
      int      targetEndPos         = 0;
      int      currentPos           = 0;
      do {

         /* Locate a backslash */
         while (   currentPos < totalCharacters
                && (charBuffer [currentPos]) != '\\')
            currentPos++;

         if (currentPos >= totalCharacters)
            break;

         sourceChunkEndPos = currentPos - 1;

         /* Test if "\" is followed by "uXXXX" */
         if (   currentPos + 5 < totalCharacters
             && charBuffer [currentPos + 1] == 'u') {
   
            /* Skip extraneous "u"s */
            int slashPos = currentPos;
            currentPos += 2;
            while (   currentPos + 4 < totalCharacters
                   && charBuffer [currentPos] == 'u') {
               currentPos++;
            }
                  
            /* Read four hex digits. 
               Note that, at this point, "currentPos + 3 < totalCharacters" is guaranteed. */
            int i1, i2, i3, i4;
            if (   ((i1 = Character.digit ((char) (charBuffer [currentPos++] & 0xff), 16)) >= 0)
                && ((i2 = Character.digit ((char) (charBuffer [currentPos++] & 0xff), 16)) >= 0)
                && ((i3 = Character.digit ((char) (charBuffer [currentPos++] & 0xff), 16)) >= 0)
                && ((i4 = Character.digit ((char) (charBuffer [currentPos  ] & 0xff), 16)) >= 0)) {

               char newCh = (char) (i1 * 4096 + i2 * 256 + i3 * 16 + i4);

               if (!seenAnyUnicodes) {
                  newCharBuffer   = new char [charBuffer.length];
                  seenAnyUnicodes = true;
               }

               int chunkLength = sourceChunkEndPos - sourceChunkStartPos + 1;
               if (chunkLength > 0) {
                  System.arraycopy (charBuffer, sourceChunkStartPos,
                                    newCharBuffer, targetEndPos, 
                                    chunkLength);
                  targetEndPos += chunkLength;
               }

               newCharBuffer [targetEndPos++] = newCh;
               sourceChunkStartPos = currentPos + 1;

            } else {
               /* Java Spec (1.1) says it's an error for the "\" and "u" 
                  not to be followed by four hex digits, even inside of a comment
                  or string literal. However, for the time being, we let 
                  this pass without error.
                  Unless it occurs inside of a String literal or comment,
                  the scanner will stumble upon it later. */
               currentPos = slashPos;
            }
         }
         currentPos++;
      } while (true);

      if (!seenAnyUnicodes)
         return;

      /* Copy remainder of buffer */
      int chunkLength = totalCharacters - sourceChunkStartPos;
      if (chunkLength > 0)
         System.arraycopy (charBuffer, sourceChunkStartPos,
                           newCharBuffer, targetEndPos, 
                           chunkLength);

      /* Switch to new buffer */
      charBuffer      = newCharBuffer;
      totalCharacters = targetEndPos;
      charPosition    = 0;

      lineStartPosition      = 0;
      lineNumber             = 1;
      highestLineNumber      = 1;
      lineStartPositionArray = new int [256];
   }


   /*------------------------------ Scanning ----------------------------*/

   protected void maintainLineNumberPositionArray (int nextLineStartPosition) {

      lineNumber++;
      if (lineNumber > highestLineNumber)
         highestLineNumber = lineNumber;

      if (lineStartPositionArray.length <= highestLineNumber) {
         int newLength = lineStartPositionArray.length * 2;
         while (newLength <= highestLineNumber)
            newLength += newLength;

         int [ ] newLineStartPositionArray = new int [ newLength ];
         System.arraycopy (lineStartPositionArray, 0,
                           newLineStartPositionArray, 0, 
                           lineStartPositionArray.length);
         lineStartPositionArray = newLineStartPositionArray;
      }

      lineStartPosition                   = nextLineStartPosition;
      lineStartPositionArray [lineNumber] = nextLineStartPosition;
   }

   /** return the 1-based line at the given position
    */
   public int lineAtPosition (int pos) {
     int low = 0;
     int hi  = highestLineNumber+1;
     int med = 0;

     while (hi > low) {
         med = (hi+low) / 2;
         int posAtMed = lineStartPositionArray [med];
         // System.out.println(pos+" "+posAtMed+" "+med+" "+low+" "+hi);
         if (posAtMed == pos) break;
         if (posAtMed >  pos)
         {
             hi = med;
         }
         else if (low==med) {
             break;
         }
         else {
             low = med;
         }
     }
     return med;
   }

   /** return the 1-based column position at the given line and global position
    */
   public int positionOnLine (int line, int position) {
     return position - lineStartPositionArray[line]+1;
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   public String nextLine () {
      int   startPosition = charPosition;
      char  ch = 0;
      
      while (   charPosition < charBuffer.length 
             && (ch = charBuffer [charPosition]) != '\r'
             && ch != '\n')
         charPosition++;
         
      String line = new String (charBuffer,
                                startPosition, charPosition - startPosition);

      if (charPosition < charBuffer.length) {
         /* Skip the end-of-line marker */
         charPosition++;
         
         /* Check for '\r\n' */
         if (   ch == '\r'
             && charPosition < charBuffer.length
             && charBuffer [charPosition] == '\n')
            charPosition++;

         maintainLineNumberPositionArray (charPosition);
      }

      return line;
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   public String getSourceLine (int startPosition) {
      int   position = startPosition;
      char  ch = 0;
      getCharBuffer(); // make sure it is there...

      while (   position < charBuffer.length 
             && (ch = charBuffer [position]) != '\r'
             && ch != '\n')
         position++;
         
      String line = new String (charBuffer,
                                startPosition, position - startPosition);
      return line;
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   public String getSource (int startPosition, int endPosition) {
      getCharBuffer(); // make sure it is there...
      return new String (charBuffer,
                         startPosition, endPosition - startPosition);
   }
   
   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   public boolean atEnd () {return charPosition >= charBuffer.length;}
   
   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   public void reset () {
      charPosition      = 0;
      lineStartPosition = 0;
      lineNumber        = 1;
   }

   /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -*/

   public int getTotalCharacters () {return totalCharacters;}
   
   
/* ------------------------------------------------------------------- */
/*                              Predicates                             */
/* ------------------------------------------------------------------- */

   public static boolean isSpace(char ch) {return Character.isWhitespace (ch);}
   public static boolean isDigit(char ch) {return Character.isDigit (ch);}
   public static boolean isIdentifierStartChar(char ch) {
      return Scanner.isUnicodeLetter (ch);
   }
   public static boolean isIdentifierChar(char ch) {
      return    Scanner.isUnicodeLetter (ch)
             || Scanner.isUnicodeDigit  (ch);
   }

   public static boolean isUnicodeLetter(char ch) {
      return (   (ch >= 'A' && ch <= 'Z')
              || (ch >= 'a' && ch <= 'z')
              || (ch == '$')
              || (ch == '_')
              || (ch >  (char) 128));
      
      /* 
      Official implementation: (from Java Spec 1.6)
      
      A Unicode character is a letter if it falls in 
      one of the following ranges and is not a digit:
      
      \u0024 $ dollar sign (for historical reasons) 
      \u0041-\u005a A-Z Latin capital letters 
      \u005f _ underscore (for historical reasons) 
      \u0061-\u007a a-z Latin small letters 
      \u00c0-\u00d6 various Latin letters with diacritics 
      \u00d8-\u00f6 various Latin letters with diacritics 
      \u00f8-\u00ff various Latin letters with diacritics 
      \u0100-\u1fff other non-CJK alphabets and symbols 
      \u3040-\u318f Hiragana, Katakana, Bopomofo, and Hangul 
      \u3300-\u337f CJK squared words 
      \u3400-\u3d2d Korean Hangul Symbols 
      \u4e00-\u9fff Han (Chinese, Japanese, Korean) 
      \uf900-\ufaff Han compatibility
      */
   }
   
   public static boolean isUnicodeDigit(char ch) {
      return Character.isDigit (ch);
      
      /* 
      Official implementation: (from Java Spec 1.6)
      
      \u0030-\u0039 ISO-LATIN-1 digits
      \u0660-\u0669 Arabic-Indic digits 
      \u06f0-\u06f9 Eastern Arabic-Indic digits 
      \u0966-\u096f Devanagari digits 
      \u09e6-\u09ef Bengali digits 
      \u0a66-\u0a6f Gurmukhi digits 
      \u0ae6-\u0aef Gujarati digits 
      \u0b66-\u0b6f Oriya digits 
      \u0be7-\u0bef Tamil digits 
      \u0c66-\u0c6f Telugu digits 
      \u0ce6-\u0cef Kannada digits 
      \u0d66-\u0d6f Malayalam digits 
      \u0e50-\u0e59 Thai digits 
      \u0ed0-\u0ed9 Lao digits 
      \u1040-\u1049 Tibetan digits
      */
   }

   public static boolean [] IsoLatinLetterTable;
   
   static {
      IsoLatinLetterTable = new boolean [256];
      for (int i = 0; i < IsoLatinLetterTable.length; i++)
         IsoLatinLetterTable [i] = false;
         
      IsoLatinLetterTable [ 0x0024 ] = true;   /* $ dollar sign (for historical reasons) */
      IsoLatinLetterTable [ 0x005f ] = true;   /* _ underscore (for historical reasons)  */

      for (int i = 0x0041; i <= 0x005a; i++)   /* A-Z Latin capital letters */
         IsoLatinLetterTable [ i ] = true;
      for (int i = 0x0061; i <= 0x007a; i++)   /* a-z Latin small letters */
         IsoLatinLetterTable [ i ] = true;
         
      for (int i = 0x00c0; i <= 0x00ff; i++)   /* various Latin letters with diacritics */
         IsoLatinLetterTable [ i ] = true;
      IsoLatinLetterTable [ 0x00d7 ] = false;
      IsoLatinLetterTable [ 0x00f7 ] = false;
      
      /*
      \u00c0-\u00d6 various Latin letters with diacritics 
      \u00d8-\u00f6 various Latin letters with diacritics 
      \u00f8-\u00ff various Latin letters with diacritics 
      */
   }
}


