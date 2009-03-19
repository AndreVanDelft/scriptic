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

package scriptic.tools.lowlevel;

import java.io.*;

public abstract class ClassFileAttribute {
          byte[] bytes;
   public int    length;
   public abstract String getName ();
   public String getPresentationName () {return "attribute";}
   public String getPresentation () {
      return getPresentationName() + " " + getName();
   }
   public boolean readFromStream (ClassEnvironment e, DataInputStream stream)
                                             throws IOException, ByteCodingException {
      bytes = new byte [length];
      stream.read (bytes);
      return true;
   }

   // Generic description -- hex dump
   public String getDescription () {
      StringBuffer result = new StringBuffer ();
      String lineSeparator = System.getProperty ("line.separator", "\r\n");

      result.append (getName ()).append (" = { ");
      int indent      = result.length() * 2;
      char spaces [ ] = new char [indent];
      for (int b = 0; b < spaces.length; b++)
         spaces[b] = ' ';
      String indentString = new String (spaces);

      if (bytes != null) {
         for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
               result.append(", ");
               if (i % 16 == 0)
                  result.append(lineSeparator).append(indentString);
            }
            result
               .append (Character.forDigit ((bytes[i] >> 4) & 0x000F, 16))
               .append (Character.forDigit ((bytes[i]     ) & 0x000F, 16));
         }
      }
      result.append(" };").append(lineSeparator);
      return result.toString ();
   }
  int size() { return (2 + 4 + length); }
  public void resolve (ClassEnvironment e, AttributeOwner owner) {}
  public void decode (ClassEnvironment e) throws IOException {}

  public abstract void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException;
}

