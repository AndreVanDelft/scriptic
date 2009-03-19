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

public class DimensionAttribute extends ClassFileAttribute {

   public String signature = null;

   public DimensionAttribute () {}
   public DimensionAttribute (String signature) {
       this.signature = signature;
   }
   public static final String name = "scriptic.Dimension";
   public String getName () {return name;}

   public boolean readFromStream (ClassEnvironment e, DataInputStream stream) throws IOException, ByteCodingException {
      int signatureIndex = stream.readUnsignedShort ();
      signature = e.getConstantPoolItem(signatureIndex).getName(e);
      return true;
   }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort(e.resolveUnicode(getName()).slot);
    out.writeInt(2);
    out.writeShort(e.resolveUnicode(signature).slot);
  }

  public String getPresentation () {
      if (signature==null) return getName ();
      else                 return getName () + " " + signature;
   }
   public String getDescription () {
      if (signature==null) return getName ();
      else                 return getName () + " = \"" + signature + "\"";
   }
}

