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
//                             ShadowOfMemberVariable
// created for private MemberVariables that are referenced by inner classes
////////////////////////////////////////////////////////////////////////////

class ShadowOfMemberVariable extends MemberVariable {
   MemberVariable original;
   ShadowGetMethod shadowGetMethod;
   ShadowSetMethod shadowSetMethod;
   ShadowOfMemberVariable (MemberVariable original, ShadowGetMethod sg, ShadowSetMethod ss) {
     this.original        = original;
     this.shadowGetMethod = sg;
     this.shadowSetMethod = ss;
     this.name            = "shadow of "+original.name;
     this.modifierFlags   = original.modifierFlags & StaticFlag;
   }
   Instruction [] storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException {
       Instruction [] result = new Instruction [1];
       result[0] = shadowGetMethod.invokeInstruction (env, original.isStatic()
                                                         ? INSTRUCTION_invokestatic
                                                         : INSTRUCTION_invokespecial,
                                                           constantPoolOwner,
                                                           instructionOwner);
       return result;
   }
   Instruction []     loadInstructions  (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
       Instruction [] result = new Instruction [1];
       result[0] = shadowSetMethod.invokeInstruction (env, original.isStatic()
                                                         ? INSTRUCTION_invokestatic
                                                         : INSTRUCTION_invokespecial,
                                                           constantPoolOwner,
                                                           instructionOwner);
       return result;
   }

}
