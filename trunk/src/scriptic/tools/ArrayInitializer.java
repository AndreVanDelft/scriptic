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

import java.io.*;
import java.util.ArrayList;
import scriptic.tools.lowlevel.*;

class ArrayInitializer extends JavaExpression {
   public ArrayList<JavaExpression> elementExpressions = new ArrayList<JavaExpression>();
   public int languageConstructCode () {return ArrayInitializerCode;}

   /** Assignment compatibility - $5.2 */
   public boolean canBeAssignedTo(CompilerEnvironment env, DataType target) throws IOException, CompilerError {
       return eltThatCannotBeAssignedTo(env, target)==null;}
   public JavaExpression eltThatCannotBeAssignedTo(CompilerEnvironment env, DataType target) throws IOException, CompilerError {

      if (!target.isArray()) return this;
      ArrayType arrayTypeTarget = (ArrayType) target;

      for (int i = 0; i < elementExpressions.size(); i++) {
          JavaExpression elt = elementExpressions.get(i);
          // nasty; the dataType of sub-arrayinitializers is still to be set here...
          if (elt.languageConstructCode()!=ArrayInitializerCode
          && (elt.languageConstructCode()!=AllocationExpressionCode
             ||((AllocationExpression)elt).arrayInitializer==null)
          || !arrayTypeTarget.accessType.isArray()
          &&  arrayTypeTarget.accessType!=env.javaLangObjectType) {
            if (!elt.canBeAssignedTo (env, arrayTypeTarget.accessType)) {

  System.out.println ("eltThatCannotBeAssignedTo: "+elt.getDescription()
  +"\ntarget: "+arrayTypeTarget.accessType.getDescription());

                 return elt;
            }
          }
      }
      return null;
   }

   /* asserts that canBeAssignedTo gives true! */
   public JavaExpression convertForAssignmentTo(CompilerEnvironment env, DataType target)
            throws IOException, CompilerError
   {
	   ArrayList<JavaExpression> oldElementExpressions = elementExpressions;
      elementExpressions = new ArrayList<JavaExpression>();

      if (target != env.javaLangObjectType)
      for (int i = 0; i < oldElementExpressions.size(); i++) {
          JavaExpression elt = oldElementExpressions.get(i);
          elementExpressions.add (elt.convertForAssignmentTo
                                                 (env, ((ArrayType) target).accessType));
      }
      dataType = target;
      return this;
   }
   Instruction dupReferenceInstruction() throws ByteCodingException {return new Instruction (INSTRUCTION_dup, this);}
   Instruction []     storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException {
      Instruction[] result = new Instruction[1];
      result[0] = new Instruction(((ArrayType)dataType).accessType.arrayStoreInstructionCode(), this);
      return result;
   }

   public String getDescription() {
      StringBuffer result = new StringBuffer();
      result.append(getPresentation());
      result.append(dataType==null?"": " "+dataType.getPresentation());
      result.append('{');
      for (int i=0; i<elementExpressions.size(); i++) {
        if (i>0) result.append(',');
        result.append(elementExpressions.get(i).getDescription());
      }
      result.append('}');
      return result.toString();
   }
}

