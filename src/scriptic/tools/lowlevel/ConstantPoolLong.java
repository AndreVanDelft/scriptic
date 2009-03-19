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



public class ConstantPoolLong extends ConstantPoolItem {
   public long longValue;
   public ConstantPoolLong(long longValue) {this.longValue = longValue;}
   public int tag() {return ConstantPoolLongTag;}
   public Instruction loadInstruction (InstructionOwner owner)
    throws ByteCodingException {
      return new Instruction (INSTRUCTION_ldc_2w, this, owner);
   }
   public int noOfSlots () {return 2;}
   public static ConstantPoolLong readFromStream
                (DataInputStream stream) throws IOException {
      long longValue = stream.readLong ();
      return new ConstantPoolLong (longValue);
   }
   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte(tag());
      stream.writeLong (longValue);
      return true;
   }
   public String getPresentationName () {return "long";}
   public String getName () {return String.valueOf (longValue);}
}

