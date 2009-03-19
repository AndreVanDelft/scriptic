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
import scriptic.tools.lowlevel.ClassEnvironment;
import scriptic.tools.lowlevel.ConstantPoolItem;
import scriptic.tools.lowlevel.ConstantValueAttribute;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

class ConstantLong extends ConstantNumber {
   long value;
   public ConstantLong (long value) {this.value=value;}
   public DataType dataType(CompilerEnvironment env) {return LongType.theOne;}
   public boolean isZero   () {return value==0;}
   public boolean isOne    () {return value==1;}

   public ConstantValue convertWideningTo (int primitiveTypeToken) {
      if (primitiveTypeToken== ByteToken
      ||  primitiveTypeToken==ShortToken) return this;
      return convertTo (primitiveTypeToken);}
   public ConstantValue convertTo (int primitiveTypeToken) {
      switch (primitiveTypeToken) {
      case   ByteToken: return new ConstantByte  (( byte)value);
      case   CharToken: return new ConstantChar  (( char)value);
      case  ShortToken: return new ConstantShort ((short)value);
      case    IntToken: return new ConstantInt   ((  int)value);
      case  FloatToken: return new ConstantFloat (       value);
      case DoubleToken: return new ConstantDouble(       value);
      }
      return this;
   }
   public ConstantValue promoteBinary(DataType other) {
      if (other==null) return this;
      if (other.isFloat ()) return new ConstantFloat (value);
      if (other.isDouble()) return new ConstantDouble(value);
      return this;
   }
   public Object makeObject() {return new Long(value);}
   public ConstantValueAttribute makeAttribute(ClassEnvironment e) throws ByteCodingException
   {
      return new ConstantValueAttribute (e.resolveLong (value));
   }
   public ConstantValue doUnaryOperator (int operatorToken) {
     switch (operatorToken) {
     case TildeToken             : return new ConstantLong   (~ value);
     case MinusToken             : return new ConstantLong   (- value);
     }
     return null;
   }
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue otherConstant) {
     ConstantLong other = (ConstantLong) otherConstant;
     switch (operatorToken) {
     case      EqualsToken       : return new ConstantBoolean(value ==other.value);
     case    NotEqualToken       : return new ConstantBoolean(value !=other.value);
     case GreaterThanToken       : return new ConstantBoolean(value > other.value);
     case LessThanToken          : return new ConstantBoolean(value < other.value);
     case LessOrEqualToken       : return new ConstantBoolean(value <=other.value);
     case GreaterOrEqualToken    : return new ConstantBoolean(value >=other.value);

     case PlusToken              : return new ConstantLong   (value + other.value);
     case MinusToken             : return new ConstantLong   (value - other.value);
     case AsteriskToken          : return new ConstantLong   (value * other.value);
     case SlashToken             : return new ConstantLong   (value / other.value);
     case PercentToken           : return new ConstantLong   (value % other.value);
     }
     return null;
   }
   public ConstantValue doShiftOperator (int operatorToken, ConstantValue otherConstant) {
     int otherValue = otherConstant instanceof ConstantInt
                    ?       ((ConstantInt )otherConstant).value
                    : (int) ((ConstantLong)otherConstant).value;
     switch (operatorToken) {
     case LeftShiftToken         : return new ConstantLong   (value <<otherValue);
     case RightShiftToken        : return new ConstantLong   (value >>otherValue);
     case UnsignedRightShiftToken: return new ConstantLong   (value>>>otherValue);
     }
     return null;
   }
   public ConstantPoolItem resolveToConstantPoolItem (ClassType t) {
     return t.resolveLong(value);
   }
   public Instruction []     loadInstructions (ClassType t,InstructionOwner owner) {
      Instruction[] result = new Instruction[1];
      result[0] = Instruction.loadLongInstruction (t, value, owner);
      return result;
   }
}

