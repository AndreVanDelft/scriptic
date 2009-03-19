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

public class ConstantPoolString extends ConstantPoolItemWithNameRef {
   public ConstantPoolString(ConstantPoolUnicode stringItem) {super (stringItem);}
   public ConstantPoolString(int                  nameIndex) {super (nameIndex);}
   public int tag() {return ConstantPoolStringTag;}

   public static ConstantPoolString readFromStream
                (DataInputStream stream) throws IOException {
      int nameIndex = stream.readUnsignedShort ();
      return new ConstantPoolString (nameIndex);
   }

   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte (tag());
      stream.writeShort(nameIndex);
      return true;
   }
   public String getPresentationName () {return "String";}
}

