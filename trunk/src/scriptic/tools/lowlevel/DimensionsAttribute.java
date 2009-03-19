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

public class DimensionsAttribute extends ClassFileAttribute implements ClassFileConstants {

   public int    dimensionModifiers[];
   public String dimensionNames    [];
   public String signaturesAndUnits[];

   public DimensionsAttribute () {}
   public DimensionsAttribute (int dimensionModifiers[], String dimensionNames[],
                                                         String signaturesAndUnits[]) {
       this.dimensionModifiers = dimensionModifiers;
       this.dimensionNames     = dimensionNames;
       this.signaturesAndUnits = signaturesAndUnits;
   }
   public static final String name = "scriptic.Dimensions";
   public String getName () {return name;}

   public boolean readFromStream (ClassEnvironment e, DataInputStream stream) throws IOException, ByteCodingException {
     int size           = stream.readShort();
     dimensionModifiers = new int    [size];
     dimensionNames     = new String [size];
     signaturesAndUnits = new String [size];
     for (int i=0; i<size; i++) {
       dimensionModifiers[i] = stream.readInt           ();
       int      nameIndex    = stream.readUnsignedShort ();
       int signatureIndex    = stream.readUnsignedShort ();
       dimensionNames    [i] = e.getConstantPoolItem(     nameIndex).getName(e);
       signaturesAndUnits[i] = e.getConstantPoolItem(signatureIndex).getName(e);
     }
     return true;
   }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
     out.writeShort(e.resolveUnicode(getName()).slot);
     out.writeInt  (2+dimensionNames.length*8);
     out.writeShort(  dimensionNames.length  );
     for (int i=0; i<dimensionNames.length; i++) {
       out.writeInt  (                 dimensionModifiers[i]);
       out.writeShort(e.resolveUnicode(dimensionNames    [i]).slot);
       out.writeShort(e.resolveUnicode(signaturesAndUnits[i]).slot);
     }
  }

  public String getPresentation () {
     StringBuffer result = new StringBuffer();
     for (int i=0; i<dimensionNames.length; i++) {
       result.append( "modifiers ").append(dimensionModifiers[i])
             .append(" dimension ").append(dimensionNames    [i])
             .append(" - "        ).append(signaturesAndUnits[i])
             .append(lineSeparator);
     }
     return result.toString();
   }
   public String getDescription () {return getPresentation();}
}

