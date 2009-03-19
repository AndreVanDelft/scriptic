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

package scriptic.tools;

class ConstantShort extends ConstantSmallOrNormalInteger {
   short value;
   public int intValue() {return value;}
   public ConstantShort (short value) {this.value=value;}
   public DataType dataType(CompilerEnvironment env) {return ShortType.theOne;}
   public boolean isZero   () {return value==0;}
   public boolean isOne    () {return value==1;}
   public ConstantValue promoteUnary() {return new ConstantInt(value);}
   public ConstantValue promoteBinary(DataType other) {
      if (other==null) return this;
      if (other.isLong  ()) return new ConstantLong  (value);
      if (other.isFloat ()) return new ConstantFloat (value);
      if (other.isDouble()) return new ConstantDouble(value);
      return                       new ConstantInt   (value);
   }
   public Object makeObject() {return new Integer(value);}
}

