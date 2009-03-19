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
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

class ConstantNull extends ConstantValue {
   public static final ConstantNull theOne = new ConstantNull();
   public String makeString() {return "null";}
   private ConstantNull() {}
   public boolean isNull() {return true;}
   public Instruction []     loadInstructions (ClassType t,InstructionOwner owner) throws ByteCodingException {
      Instruction[] result = new Instruction[1];
      result[0] = new Instruction (INSTRUCTION_aconst_null, owner);
      return result;
   }
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue otherConstant) {
     switch (operatorToken) {
     case      EqualsToken: return new ConstantBoolean(otherConstant==this);
     case    NotEqualToken: return new ConstantBoolean(otherConstant!=this);
     }
     return null;
   }
}

