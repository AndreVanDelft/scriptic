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
import scriptic.tools.lowlevel.ConstantValueAttribute;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

class ConstantBoolean extends ConstantValue {
   public boolean value;
   public ConstantBoolean (boolean value) {this.value=value;}
   public DataType dataType     () {return BooleanType.theOne;}
   public boolean isTrue () {return  value;}
   public boolean isFalse() {return !value;}
   public static ConstantBoolean True  = new ConstantBoolean( true);
   public static ConstantBoolean False = new ConstantBoolean(false);
   public Object makeObject() {return new Boolean(value);}
   public ConstantValueAttribute makeAttribute(ClassEnvironment e) throws ByteCodingException
   {
      return new ConstantValueAttribute (e.resolveInteger (value?1:0));
   }
   public ConstantValue doLogicalComplement () {return new ConstantBoolean(!value);}
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue otherConstant) {
     ConstantBoolean other = (ConstantBoolean) otherConstant;
     switch (operatorToken) {
     case      EqualsToken       : return new ConstantBoolean(value ==other.value);
     case    NotEqualToken       : return new ConstantBoolean(value !=other.value);
     case       CaretToken       : return new ConstantBoolean(value ^ other.value);
     case   AmpersandToken       : return new ConstantBoolean(value & other.value);
     case VerticalBarToken       : return new ConstantBoolean(value | other.value);
     }
     return null;
   }
   public Instruction []     loadInstructions (ClassType t,InstructionOwner owner) {
      Instruction[] result = new Instruction[1];
      result[0] = Instruction.loadBooleanInstruction (t, value, owner);
      return result;
   }
}


