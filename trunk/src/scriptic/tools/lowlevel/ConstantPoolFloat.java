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


public class ConstantPoolFloat extends ConstantPoolItem {
   public float floatValue;
   public ConstantPoolFloat(float floatValue) {this.floatValue = floatValue;}
   public int tag() {return ConstantPoolFloatTag;}

   public static ConstantPoolFloat readFromStream
                (DataInputStream stream) throws IOException {
      float floatValue = stream.readFloat ();
      return new ConstantPoolFloat (floatValue);
   }
   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte(tag());
      stream.writeFloat (floatValue);
      return true;
   }
   public String getPresentationName () {return "float";}
   public String getName () {return String.valueOf (floatValue);}
}

