/* This file is part of the Scriptic Virtual Machine
 * Copyright (C) 2009 Andre van Delft
 *
 * The Scriptic Virtual Machine is free software: 
 * you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.vm;

import java.io.*;

public class Scanner extends BasicScanner {

   public Scanner (File file)
               throws FileNotFoundException, IOException {

      super (file);
   }
   
   public Scanner (String input) {
      super (input);
   }

}


class BasicScanner {
   protected char charBuffer [];
   protected int  charPosition;
   protected int  totalCharactersCounter;
   

   public BasicScanner (File file) 
               throws FileNotFoundException, IOException {

      charBuffer      = new char [0];
      charPosition    = 0;
      totalCharactersCounter = 0;
      
      /* Read the whole file all at once. This is the fastest way to read files. */
      FileInputStream inputStream = new FileInputStream (file);
      byte  byteBuffer [] = null;
      int   bytePosition = 0;
      
      try {
         int noOfBytes  = inputStream.available();
         byteBuffer     = new byte [noOfBytes];
         inputStream.read (byteBuffer);
      } catch (Exception e) {
         return;
      } finally {
         inputStream.close();
      }

      /* Convert bytes to Unicode chars; process Unicode escapes. 
         The "+ 10" in the charBuffer allocation reduces the need to test
         for end-of-buffer when scanning multi-character patterns. The extra
         space should be larger than the longest non-keyword token. Note
         that the charBuffer might be way too long anyway, due to the
         presence of Unicode escapes. */

      charBuffer      = new char [byteBuffer.length + 10];
      charPosition    = 0;
      totalCharactersCounter = 0;

      byte b;
      char ch;
      while (bytePosition < byteBuffer.length) {
         b  = byteBuffer [bytePosition];
         ch = (char) (b & 0xff);             
         if (b == (byte) '\\') {

            /* Test if "\" is followed by "uXXXX" */
            if (   bytePosition + 5 < byteBuffer.length
                && byteBuffer [bytePosition + 1] == (byte) 'u') {

               /* Skip extraneous "u"s */
               int startPosition = bytePosition;
               bytePosition += 2;
               while (   bytePosition + 4 < byteBuffer.length
                      && byteBuffer [bytePosition] == (byte) 'u') {
                  bytePosition++;
               }
               
               /* Read four hex digits. 
                  Note that "bytePosition + 3 < byteBuffer.length" is guaranteed. */
               int i1, i2, i3, i4;
               if (   ((i1 = Character.digit ((char) (byteBuffer [bytePosition++] & 0xff), 16)) >= 0)
                   && ((i2 = Character.digit ((char) (byteBuffer [bytePosition++] & 0xff), 16)) >= 0)
                   && ((i3 = Character.digit ((char) (byteBuffer [bytePosition++] & 0xff), 16)) >= 0)
                   && ((i4 = Character.digit ((char) (byteBuffer [bytePosition  ] & 0xff), 16)) >= 0)) {
                  ch = (char) (i1 * 4096 + i2 * 256 + i3 * 16 + i4);
               } else {
                  /* Java Spec (1.1) says it's an error for the "\" and "u" 
                     not to be followed by four hex digits, even inside of a comment
                     or string literal. However, for the time being, we let 
                     this pass without error.
                     Unless it occurs inside of a String literal or comment,
                     the scanner will stumble upon it later. */
                  bytePosition = startPosition;
               }
            }
         }
         
         charBuffer [charPosition] = ch;
         charPosition++;
         bytePosition++;
      }
      
      /* Unicode escape conversion finished. */
      totalCharactersCounter = charPosition;
      charPosition    = 0;
   }

   public BasicScanner (String input) {
      totalCharactersCounter = input.length();
	   charBuffer      = new char [totalCharactersCounter + 10];
   	input.getChars(0, totalCharactersCounter, charBuffer, 0);
      charPosition    = 0;
   }

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
      }
      
      return line;
   }
   
   public boolean atEnd () {
      return charPosition >= charBuffer.length;
   }
   
   public void reset () {
      charPosition = 0;
   }

   public int totalCharacters () {
      return totalCharactersCounter;
   }
   
   protected void skipWhitespace () {
      while (   charPosition < charBuffer.length 
             && (Scanner.isSpace (charBuffer [charPosition])))
         charPosition++;      
   }
   
/* ------------------------------------------------------------------- */
/*                              Predicates                             */
/* ------------------------------------------------------------------- */

   public static boolean isSpace(char ch) {
      return Character.isSpaceChar (ch);
   }

   public static boolean isDigit(char ch) {
      return Character.isDigit (ch);
   }
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

