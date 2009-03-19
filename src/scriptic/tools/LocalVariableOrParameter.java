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
//                             LocalVariableOrParameter
//
////////////////////////////////////////////////////////////////////////////

abstract class LocalVariableOrParameter extends Variable {
   public Method owner;
   public int slot;
   public String getSlotPresentation () {
      return new StringBuffer ()
               .append ("[")
               .append (slot)
               .append ("] ")
               .toString ();
   }
   public boolean isAccessibleFor (CompilerEnvironment env, ClassType t, boolean isForAllocation) {return true;}

   /* ----------------------------- Predicates ------------------------- */

   public boolean isStatic() {return false;}
   public boolean isFinal () {
      if (source()==null) return false;
      return ((BasicVariableDeclaration)source()).isFinal();
   }
   Instruction dupReferenceInstruction (InstructionOwner instructionOwner) throws ByteCodingException {return null;}

   Instruction[] loadReferenceInstructions (FieldDeclaration fieldDeclaration, CompilerEnvironment env,
                                            ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
     return null;
   }
}


