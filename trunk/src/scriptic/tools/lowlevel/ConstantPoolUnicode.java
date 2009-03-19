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


public class ConstantPoolUnicode extends ConstantPoolItem {
   private String name;
   public ConstantPoolUnicode(String name) {this.name = name.intern();}
   public int tag() {return ConstantPoolUnicodeTag;}
   public static ConstantPoolUnicode readFromStream
                (DataInputStream stream) throws IOException {
      return new ConstantPoolUnicode (stream.readUTF ());
   }
   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte (tag());
      stream.writeUTF  (name);
      return true;
   }
   public String getPresentationName () {return "ConstantPoolUnicode";}
   public String getPresentation () {
      return getPaddedSlotString () + ": \"" 
           + getName()              +   "\"";
   }
   public String getName ()                   {return name;}
   public String getName (ClassEnvironment e) {return name;}
   public String getPresentation (ClassEnvironment e) {
      return getPaddedSlotString () + ": \"" + getName(e) + "\"";
   }

}

