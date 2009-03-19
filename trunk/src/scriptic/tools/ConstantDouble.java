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

class ConstantDouble extends ConstantNumber {
   double value;
   public ConstantDouble (double value) {this.value=value;}
   public DataType dataType(CompilerEnvironment env) {return DoubleType.theOne;}
   public boolean isZero   () {return value==0;}
   public boolean isOne    () {return value==1;}
   public Object makeObject() {return new Double(value);}
   public ConstantValueAttribute makeAttribute(ClassEnvironment e) throws ByteCodingException
   {
      return new ConstantValueAttribute (e.resolveDouble (value));
   }
   public ConstantValue convertTo (int primitiveTypeToken) {
      switch (primitiveTypeToken) {
      case   ByteToken: return new ConstantByte  (( byte)value);
      case   CharToken: return new ConstantChar  (( char)value);
      case  ShortToken: return new ConstantShort ((short)value);
      case    IntToken: return new ConstantInt   ((  int)value);
      case   LongToken: return new ConstantLong  (( long)value);
      case  FloatToken: return new ConstantFloat ((float)value);
      }
      return this;
   }
   public ConstantValue doUnaryOperator (int operatorToken) {
     if (operatorToken==MinusToken) return new ConstantDouble (- value);
     return null;
   }
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue otherConstant) {
     ConstantDouble other = (ConstantDouble) otherConstant;
     switch (operatorToken) {
     case      EqualsToken       : return new ConstantBoolean(value ==other.value);
     case    NotEqualToken       : return new ConstantBoolean(value !=other.value);
     case GreaterThanToken       : return new ConstantBoolean(value > other.value);
     case LessThanToken          : return new ConstantBoolean(value < other.value);
     case LessOrEqualToken       : return new ConstantBoolean(value <=other.value);
     case GreaterOrEqualToken    : return new ConstantBoolean(value >=other.value);

     case PlusToken              : return new ConstantDouble (value + other.value);
     case MinusToken             : return new ConstantDouble (value - other.value);
     case AsteriskToken          : return new ConstantDouble (value * other.value);
     case SlashToken             : return new ConstantDouble (value / other.value);
     case PercentToken           : return new ConstantDouble (value % other.value);
     }
     return null;
   }
   public ConstantPoolItem resolveToConstantPoolItem (ClassType t) {
     return t.resolveDouble(value);
   }
   public Instruction []     loadInstructions (ClassType t,InstructionOwner owner) {
      Instruction[] result = new Instruction[1];
      result[0] = Instruction.loadDoubleInstruction (t, value, owner);
      return result;
   }
}

