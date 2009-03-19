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

abstract class MethodOrConstructorCallExpression extends JavaExpression
                implements RefinementCallExpression {
   public MethodCallParameterList parameterList;
   public Method target;
   public  Method target()     {return target;}
   public  void    setTarget(CompilerEnvironment env, TypeDeclaration typeDeclaration, Method t)
    throws IOException, CompilerError {
       target=t;
       if (parameterList != null) {
           for (int i=0; i<parameterList.parameterExpressions.size(); i++) {
              JavaExpression je = parameterList.parameterExpressions.get (i);
              Parameter fp = t.parameters[i];
              parameterList.parameterExpressions.set (i,
                     je.convertForAssignmentTo (env, fp.dataType1)); 
           }
       }
   }
//   private Refinement target;

   private int mode = 0;

   public void        setSuperMode() {       mode =       superMode;}
   public void      setVirtualMode() {       mode =     virtualMode;}
   public void      setSpecialMode() {       mode =  specialMode;}
   public void    setInterfaceMode() {       mode =   interfaceMode;}
   public void       setStaticMode() {       mode =      staticMode;}
   public boolean     isStaticMode() {return mode ==     staticMode;}
   public boolean    isSpecialMode() {return mode == specialMode;}
   public boolean      isSuperMode() {return mode ==      superMode;}
   public boolean    isVirtualMode() {return mode ==    virtualMode;}
   public boolean  isInterfaceMode() {return mode ==  interfaceMode;}
   public boolean     isArrayClone() {return false;}
   public String     getModeString() {return getModeString(this);}

   public  boolean isScriptChannelSend   ()  {return false;}
   public  boolean isScriptChannelReceive()  {return false;}

   public MethodCallParameterList   parameterList    () {return parameterList;}
   public JavaExpressionWithTarget  accessExpression () {return methodAccessExpression;}

   public JavaExpressionWithTarget methodAccessExpression; 

   public ArrayList<JavaExpression> getAllParameters (CompilerEnvironment env) {
	   return parameterList()==null? new ArrayList<JavaExpression>()
                                            :parameterList().parameterExpressions;}

   public String getDescription() {
      StringBuffer result = new StringBuffer();
      result.append(super.getDescription());
      result.append(lineSeparator).append("mode: ").append(getModeString());
      result.append(lineSeparator).append("target: ");
      if (target==null) result.append ("**UNKNOWN**");
      else result.append (target.getDescription());
      return result.toString();
   }

   Instruction invokeInstruction (CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException {
       int invokeInstructionCode = 0;
       switch (mode) {
       case     superMode:
       case   specialMode: invokeInstructionCode = INSTRUCTION_invokespecial  ; break;
       case   virtualMode: invokeInstructionCode = INSTRUCTION_invokevirtual  ; break;
       case    staticMode: invokeInstructionCode = INSTRUCTION_invokestatic   ; break;
       case interfaceMode: invokeInstructionCode = INSTRUCTION_invokeinterface; break;
       }
       return target.invokeInstruction (env,
                                        invokeInstructionCode,
                                        constantPoolOwner,
                                        this);
   }
}

