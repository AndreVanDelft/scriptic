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
import scriptic.tools.lowlevel.ConstantPoolItem;

class ConstantInt extends ConstantSmallOrNormalInteger {
   public int value;
   public int intValue() {return value;}
   public ConstantInt (int value) {this.value=value;}
   public DataType dataType(CompilerEnvironment env) {return IntType.theOne;}
   public boolean isZero   () {return value==0;}
   public boolean isOne    () {return value==1;}
   public boolean canBeRepresentedAsByte () {return value>=-0x0080 && value<0x0080;}
   public boolean canBeRepresentedAsChar () {return value>=-0x8000 && value<0x8000;}
   public boolean canBeRepresentedAsShort() {return value>=-0x8000 && value<0x8000;}
   public ConstantValue promoteBinary(DataType other) {
      if (other==null) return this;
      if (other.isLong  ()) return new ConstantLong  (value);
      if (other.isFloat ()) return new ConstantFloat (value);
      if (other.isDouble()) return new ConstantDouble(value);
      return this;
   }
   public Object makeObject() {return new Integer(value);}
   public ConstantValue doUnaryOperator (int operatorToken) {
     switch (operatorToken) {
     case TildeToken             : return new ConstantInt    (~ value);
     case MinusToken             : return new ConstantInt    (- value);
     }
     return null;
   }
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue otherConstant) {
     ConstantInt other = (ConstantInt) otherConstant;
     switch (operatorToken) {
     case      EqualsToken       : return new ConstantBoolean(value ==other.value);
     case    NotEqualToken       : return new ConstantBoolean(value !=other.value);
     case GreaterThanToken       : return new ConstantBoolean(value > other.value);
     case LessThanToken          : return new ConstantBoolean(value < other.value);
     case LessOrEqualToken       : return new ConstantBoolean(value <=other.value);
     case GreaterOrEqualToken    : return new ConstantBoolean(value >=other.value);

     case PlusToken              : return new ConstantInt    (value + other.value);
     case MinusToken             : return new ConstantInt    (value - other.value);
     case AsteriskToken          : return new ConstantInt    (value * other.value);
     case SlashToken             : return new ConstantInt    (value / other.value);
     case PercentToken           : return new ConstantInt    (value % other.value);

     case AmpersandToken         : return new ConstantInt    (value & other.value);
     case VerticalBarToken       : return new ConstantInt    (value | other.value);
     case CaretToken             : return new ConstantInt    (value ^ other.value);
     }
     return null;
   }
   public ConstantValue doShiftOperator (int operatorToken, ConstantValue otherConstant) {
     int otherValue = otherConstant instanceof ConstantInt
                    ?       ((ConstantInt )otherConstant).value
                    : (int) ((ConstantLong)otherConstant).value;
     switch (operatorToken) {
     case LeftShiftToken         : return new ConstantInt    (value <<otherValue);
     case RightShiftToken        : return new ConstantInt    (value >>otherValue);
     case UnsignedRightShiftToken: return new ConstantInt    (value>>>otherValue);
     }
     return null;
   }
   public ConstantPoolItem resolveToConstantPoolItem (ClassType t) {
     return t.resolveInteger(value);
   }
}

