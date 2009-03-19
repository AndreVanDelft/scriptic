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

import scriptic.tools.lowlevel.*;

////////////////////////////////////////////////////////////////////////////
//
//                             ArrayLength
//
////////////////////////////////////////////////////////////////////////////

class ArrayLength extends MemberVariable {
   public static ArrayLength theOne = new ArrayLength();
   private ArrayLength() {dataType1=IntType.theOne;}
   public DataType getDataType(CompilerEnvironment env) {return dataType1;}
   Instruction []     loadInstructions  (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException {
      Instruction[] result = new Instruction[1];
      result[0] = new  Instruction ( INSTRUCTION_arraylength, instructionOwner);
      return result;
   }
   public boolean isPublic     () {return  true;}
   public boolean isFinal      () {return  true;}
   public boolean canBeAssigned() {return false;}
   public boolean isStatic     () {return false;}
   public boolean isAccessibleFor (CompilerEnvironment env, ClassType clazz, boolean isForAllocation) {return true;}
}

