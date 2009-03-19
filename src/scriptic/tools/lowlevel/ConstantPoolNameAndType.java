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

public class ConstantPoolNameAndType extends ConstantPoolItemWithNameRef {
   int signatureIndex;
   public ConstantPoolNameAndType(ConstantPoolUnicode nameItem,
                                  ConstantPoolUnicode  sigItem) {
       super (nameItem);
       this.signatureIndex =  sigItem.slot;
   }
   public ConstantPoolNameAndType(int nameIndex, int signatureIndex) {
       super (nameIndex);
       this.signatureIndex = signatureIndex;
   }
   public int tag() {return ConstantPoolNameAndTypeTag;}

   public static ConstantPoolNameAndType readFromStream
                (DataInputStream stream) throws IOException {
      int nameIndex      = stream.readUnsignedShort ();
      int signatureIndex = stream.readUnsignedShort ();
      return new ConstantPoolNameAndType (nameIndex,signatureIndex);
   }
   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte (tag());
      stream.writeShort(     nameIndex);
      stream.writeShort(signatureIndex);
      return true;
   }
   public String getPresentation    () {return super.getPresentation () + "," + signatureIndex;}
   public String getPresentationName() {return "name/type";}
}


