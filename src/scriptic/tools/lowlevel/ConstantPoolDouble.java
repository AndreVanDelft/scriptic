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


public class ConstantPoolDouble extends ConstantPoolItem {
   public double doubleValue;
   public ConstantPoolDouble(double doubleValue) {this.doubleValue = doubleValue;}
   public int tag() {return ConstantPoolDoubleTag;}
   public Instruction loadInstruction (InstructionOwner owner)
    throws ByteCodingException {
      return new Instruction (INSTRUCTION_ldc_2w, this, owner);
   }
   public int noOfSlots () {return 2;}
   public static ConstantPoolDouble readFromStream
                (DataInputStream stream) throws IOException {
      double doubleValue = stream.readDouble ();
      return new ConstantPoolDouble (doubleValue);
   }
   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte(tag());
      stream.writeDouble (doubleValue);
      return true;
   }
   public String getPresentationName () {return "double";}
   public String getName () {return String.valueOf (doubleValue);}
}

