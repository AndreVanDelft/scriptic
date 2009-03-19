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

public class SourceFileAttribute extends ClassFileAttribute {

   public String sourceFileName = null;

   public SourceFileAttribute () {}
   public SourceFileAttribute (String sourceFileName) {
       this.sourceFileName = sourceFileName;
   }
   public static String name = "SourceFile";
   public String getName () {return name;}

   public boolean readFromStream (ClassEnvironment e, DataInputStream stream) throws IOException, ByteCodingException {
      if (length != 2) {
         return super.readFromStream (e, stream);
      }
      int sourceFileIndex = stream.readUnsignedShort ();
      sourceFileName = e.getConstantPoolItem(sourceFileIndex).getName(e);
      return true;
   }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort(e.resolveUnicode(getName()).slot);
    out.writeInt(2);
    out.writeShort(e.resolveUnicode(sourceFileName).slot);
  }

  public String getPresentation () {
      if (sourceFileName == null)  return getName ();
      else                         return getName () + " " + sourceFileName;
   }

   public String getDescription () {
      if (sourceFileName == null)
         return getName ();
      else
         return   getName () 
               + " = \"" 
               + sourceFileName
               + "\"";
   }
}

