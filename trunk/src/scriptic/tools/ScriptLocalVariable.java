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

import scriptic.tools.lowlevel.FieldInstruction;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

////////////////////////////////////////////////////////////////////////////
//
//                             ScriptLocalVariable
//
////////////////////////////////////////////////////////////////////////////

class ScriptLocalVariable extends LocalVariable {
   public boolean isScriptLocalVariable() {return true;}
   public String getConstructName () {return "script local variable";}

   public ClassType ownerClass(CompilerEnvironment env) throws ByteCodingException, IOException 
   {return getDataType(env).holderClass(env);}

       String    nameForFieldRef() {return "value";}
       DataType  dataTypeForFieldRef(CompilerEnvironment env) throws ByteCodingException, IOException {
           DataType result = getDataType(env);
           if  ( !result.isPrimitive())
                  result = env.javaLangObjectType;
           return result;
       }
   int getSlotNumberForNodeParameter() {
	   	return owner.isStatic()? 0: 1;
   }

   Instruction dupReferenceInstruction (InstructionOwner instructionOwner) throws ByteCodingException {
        return new Instruction (INSTRUCTION_dup,instructionOwner);}

   Instruction []     storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
      Instruction[] result = new Instruction[1];
      result[0] = new FieldInstruction (INSTRUCTION_putfield,
                                     fieldRef(env, constantPoolOwner),
                                     getDataType(env).isBig(),
                                     instructionOwner);
      return result;
   }
   Instruction []     loadInstructions  (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
      Instruction[] result = new Instruction[1];
      result[0] = new FieldInstruction (INSTRUCTION_getfield,
                                     fieldRef(env, constantPoolOwner),
                                     getDataType(env).isBig(),
                                     instructionOwner);
      return result;
   }
   Instruction[] loadReferenceInstructions (FieldDeclaration fieldDeclaration, CompilerEnvironment env,
                                            ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
       // _n.localData[slot]
       MemberVariable localDataMember = env.scripticVmNodeType.resolveMemberVariable (env, "localData");
       Instruction[] loadLocalDataMemberInstructions = localDataMember.loadInstructions (env, constantPoolOwner, instructionOwner);
       Instruction[] result = new Instruction [4+loadLocalDataMemberInstructions.length];
       int i=0;
       result[i++] = new Instruction (INSTRUCTION_aload, getSlotNumberForNodeParameter(), instructionOwner);
       for (int j=0; j<loadLocalDataMemberInstructions.length; j++) {
         result[i++] = loadLocalDataMemberInstructions [j];
       }
       result[i++] = Instruction.loadIntegerInstruction (constantPoolOwner, slot, instructionOwner);
       result[i++] = new Instruction (INSTRUCTION_aaload, instructionOwner );
       result[i++] = new Instruction (INSTRUCTION_checkcast,
                                    constantPoolOwner.resolveClass (getDataType(env).holderClass(env)),
                                    instructionOwner);
       return result;
   }
}


