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
import java.io.FilenameFilter;

/* STRICTLY PRELIMINARY. Only supports patterns of the form
   'head*tail'. The head and tail are case sensitive! */
   
class FilePattern implements FilenameFilter {
   String  pattern;
   String  head, tail;
   boolean containsPattern;
   int     minimumLength;

   public FilePattern (String pattern) {
      int headIndex, tailIndex;
      
      if (pattern == null) this.pattern = new String ();
      else                 this.pattern = pattern;

      headIndex = this.pattern.indexOf ('*');
      tailIndex = this.pattern.lastIndexOf ('*');
      containsPattern = (headIndex >= 0);
      if (containsPattern) {
         this.head = this.pattern.substring (0, headIndex);
         this.tail = this.pattern.substring (tailIndex + 1);
         this.minimumLength = this.head.length() + this.tail.length();
      }
   }


   public boolean accept(File dir, String name) {
      if (containsPattern)
           return name.length() >= minimumLength
               && name.startsWith (head)
               && name.  endsWith (tail);
      else return name.    equals (pattern);
   }
   
   public boolean hasPattern() {return this.containsPattern;}
   public String  toString  () {return pattern;}
}

