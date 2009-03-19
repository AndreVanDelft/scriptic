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

import java.util.ArrayList;
import scriptic.tools.lowlevel.*;

class ArrayAccessExpression extends JavaExpression {
   public JavaExpression primaryExpression;
   public ArrayList<JavaExpression> indexExpressions = new ArrayList<JavaExpression>();
   public int languageConstructCode () {return ArrayAccessExpressionCode;}
   public boolean canBeAssigned () {return true;}

   Instruction dupReferenceInstruction() throws ByteCodingException {return new Instruction (INSTRUCTION_dup2, this);}
   Instruction   dup_xValueInstruction() throws ByteCodingException {return new Instruction (dataType.dup_x2InstructionCode(), this);}

   Instruction []     storeInstructions(CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException {
      Instruction[] result = new Instruction[1];
      result[0] = new Instruction (dataType.arrayStoreInstructionCode(), this);
      return result;
   }
   Instruction []     loadInstructions(CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException {
      Instruction[] result = new Instruction[1];
      result[0] = new Instruction (dataType.arrayLoadInstructionCode(), this);
      return result;
   }
}

