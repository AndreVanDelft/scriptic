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



import java.io.PrintWriter;
import java.io.OutputStream;
import java.util.Stack;

public class PreprocessorOutputStream extends PrintWriter {

   public int line     = 0;
   public int column   = 0;
   public int position = 0;
   public int indentLevel;
   Stack<Integer>  indentStack = new Stack<Integer> ();

   public boolean generatingMultilineStringLiteral = false;

   public static final int      literalStringDelimiter        
      = (int) '\"';
   public static final char [ ] literalStringLineContinuation 
      = { (char)'+', (char)' ', (char)literalStringDelimiter };

   public static final int indentAmount = 2;


   public PreprocessorOutputStream (OutputStream out) {
      super (out);
      handleNewline ();
   }

   public PreprocessorOutputStream (OutputStream out, 
                                    boolean autoflush) {
      super (out, autoflush);
      handleNewline ();
   }


   /* Maintain indent & line number */
   protected void handleNewline () {

      line     += 1;
      column    = 0;

      /* Write line number, 5 positions. 
         This works very well! Can be enabled for testing. */
//    int lineNumberWidth = 5;
//    String lineString = String.valueOf (line);
//    int    length     = lineString.length ();
//    if (length < lineNumberWidth) length = lineNumberWidth;
//    byte lineBytes [] = new byte [length];
//    for (int c = 0; c < lineBytes.length; c++)
//       lineBytes[c] = (byte)' ';
//    int begin = (lineNumberWidth - 1) - lineString.length ();
//    if (begin < 0) begin = 0;
//    lineString.getBytes(0, lineString.length (),
//                        lineBytes, begin);
//    super.write (lineBytes, 0, lineBytes.length);
//    column   += lineBytes.length;
//    position += lineBytes.length;

      if (generatingMultilineStringLiteral) {
         super.write (literalStringLineContinuation, 0, literalStringLineContinuation.length);
         column   += literalStringLineContinuation.length;
         position += literalStringLineContinuation.length;
      }
      writeSpaces (indentLevel);
   }

   public void writeSpaces (int len) {
      char spaces [] = new char [len];
      for (int b = 0; b < spaces.length; b++)
         spaces[b] = (char)' ';
      super.write(spaces, 0, spaces.length);

      column   += len;
      position += len;
   }



   public void write(int b) {
      if (   generatingMultilineStringLiteral
          && (b == '\n'))
         write (literalStringDelimiter); // recursive method call!

      super.write (b);
      column++;
      position++;

      if (b == '\n') 
         handleNewline ();
   }


   public void write(char b[], int off, int len) {
      int  startPos  = off;
      int  endPos    = off;
      int  remaining = len;
      char ch        = (char)' ';
      
      do {
         if (remaining <= 0) break;
         
         /* Find next line terminator */
         while (   remaining > 0 
                && (ch = b[endPos]) != (byte) '\n') {
            endPos++;
            remaining--;
         }
         
         if (ch == (byte) '\n') {
            endPos++;    /* skip past line terminator */
            remaining--;
         }
         
         super.write (b, startPos, endPos - startPos);
         column   += endPos - startPos;
         position += endPos - startPos;
         startPos  = endPos;

         /* Maintain indent & line number */
         if (ch == (byte) '\n')
            handleNewline ();

      } while (true);
   }

   public void print (String s) {
      char c[] = new char[s.length()];
      s.getChars(0, s.length(), c, 0);
      write(c,0,c.length);
   }

   public void println (String s) {
      print(s+"\n");
   }

   public void println () {
      print("\n");
   }

   public void indent () {
      indent (indentAmount);
   }

   public void indent (int amount) {
      indentStack.push (indentLevel);
      indentLevel += amount;
      if (column < indentLevel) 
         writeSpaces (indentLevel - column);
   }

   public void setIndent () {
      setIndent (column);
   }

   public void setIndent (int level) {
      indentStack.push (indentLevel);
      indentLevel = level;
      if (column < indentLevel) 
         writeSpaces (indentLevel - column);
   }

   public void unindent () {
      if (indentStack.empty()) return;
      indentLevel = ((Integer)indentStack.pop()).intValue();
   }

   public void startMultilineStringLiteral () {
      if (generatingMultilineStringLiteral) return;

      generatingMultilineStringLiteral = true;
      write (literalStringDelimiter);
   }

   public void stopMultilineStringLiteral () {
      if (!generatingMultilineStringLiteral) return;

      generatingMultilineStringLiteral = false;
      write (literalStringDelimiter);
   }
}
