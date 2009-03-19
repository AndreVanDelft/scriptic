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

import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

////////////////////////////////////////////////////////////////////////////
//
//                             Parameter
//
////////////////////////////////////////////////////////////////////////////

class Parameter extends LocalVariableOrParameter {
   public BasicVariableDeclaration source;
   public LanguageConstruct        source() {return source;}
   public String getConstructName () {return "parameter";}
   public boolean isMethodParameter() {return true;}

   public int      declarationIndex;
   public Method   owner;
   /* ----------------------------- Presentation ------------------------- */

   public String getPresentation () {
      if (name==null) return getDataTypePresentation();
      StringBuffer presentation = new StringBuffer ();
      presentation
               .append ( getDataTypePresentation()).append (' ')
               .append (name);
      return presentation.toString ();
   }
   
   // free the memory occupied by type declaration, code etc.
   void freeCompiledMembers () {source = null;}

   Instruction dup_xValueInstruction (InstructionOwner instructionOwner) throws ByteCodingException {
       return new Instruction (dataType1.dupInstructionCode(), instructionOwner);
   }
   Instruction []     storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner) throws ByteCodingException, IOException {
      Instruction[] result = new Instruction[1];
      result[0] = new  Instruction (getDataType(env).storeInstructionCode(), slot, source());
      return result;
   }
   Instruction []     loadInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner) throws ByteCodingException, IOException {
      Instruction[] result = new Instruction[1];
      result[0] = new  Instruction (getDataType(env).loadInstructionCode(), slot, instructionOwner);
      return result;
   }
}

