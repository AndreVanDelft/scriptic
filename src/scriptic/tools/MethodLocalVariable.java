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

import java.io.IOException;

import scriptic.tools.lowlevel.*;

////////////////////////////////////////////////////////////////////////////
//
//                             MethodLocalVariable
//
////////////////////////////////////////////////////////////////////////////

class MethodLocalVariable extends LocalVariable {
   boolean isOnHeap; // true if used in locally nested class
   public  boolean canHandleIincInstruction() {return !isOnHeap;}

   public void setConfiningJavaStatement(LanguageConstruct c) {
       if (c instanceof ConfiningJavaStatement) {
	}
   }
   public String getConstructName () {return "local variable";}

   Instruction dup_xValueInstruction (InstructionOwner instructionOwner) throws ByteCodingException {
       return new Instruction (dataType1.dupInstructionCode(), instructionOwner);
   }
   Instruction []     storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner) 
   throws ByteCodingException, IOException {
      Instruction[] result = new Instruction[1];
      result[0] = new  Instruction (getDataType(env).storeInstructionCode(), slot, source());
      return result;
   }
   Instruction []     loadInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner) 
   throws ByteCodingException, IOException {
      Instruction[] result = new Instruction[1];
      result[0] = new Instruction (getDataType(env).loadInstructionCode(), slot, instructionOwner);
      return result;
   }
   Instruction     iincInstruction(int amount, InstructionOwner instructionOwner) throws ByteCodingException {
        return new IincInstruction (slot, amount, instructionOwner);
   }
}

