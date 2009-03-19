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
import scriptic.tools.lowlevel.ByteCodingException;

abstract class JavaExpressionWithTarget extends JavaExpression {
   public Variable target;
   // target==null is OK in case canBeAssigned is called during the parse pass
   public boolean canBeAssigned              () {return target==null||target.canBeAssigned();}
   public boolean isJavaExpressionWithTarget () {return true;}
   public boolean isFinal (CompilerEnvironment env) {
      return target instanceof MemberVariable
          && ((MemberVariable)target).ownerClass(env).isFinal();
   }
   public  boolean isStatic () {return target.isStatic();}

   private static final int  ambiguousName = 0;
   private static final int     methodName = 1;
   private static final int expressionName = 2;
   private static final int    packageName = 3;
   private static final int       typeName = 4;
   private String classificationString() {
       switch (classification) {
       case  ambiguousName: return "ambiguousName";
       case     methodName: return "methodName";
       case expressionName: return "expressionName";
       case    packageName: return "packageName";
       case       typeName: return "typeName";
       }
       return "**UNKNOWN**";
   }
   private int classification = expressionName;
   public void    setMethodName    () {       classification =     methodName;}
   public void    setExpressionName() {       classification = expressionName;}
   public void    setPackageName   () {       classification =    packageName;}
   public void    setTypeName      () {       classification =       typeName;}
   public void    setAmbiguousName () {       classification =  ambiguousName;}
   public boolean  isMethodName    () {return classification ==    methodName;}
   public boolean  isExpressionName() {return classification ==expressionName;}
   public boolean  isPackageName   () {return classification ==   packageName;}
   public boolean  isTypeName      () {return classification ==      typeName;}
   public boolean  isAmbiguousName () {return classification == ambiguousName;}
   public String getDescription() {
      if (isMethodName()) return classificationString();

      StringBuffer result = new StringBuffer();
      result.append(super.getDescription());
      result.append(lineSeparator).append(classificationString());

      if (!isTypeName()) {

        result.append(lineSeparator).append("target: ");
        if (target==null) result.append ("**UNKNOWN**");
        else result.append (target.getDescription());
      }
      return result.toString();
   }
   Instruction   dupReferenceInstruction() throws ByteCodingException {
   return target.dupReferenceInstruction(this);}
   Instruction   dup_xValueInstruction  () throws ByteCodingException {
   return target.dup_xValueInstruction  (this);}
   Instruction []      storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException, IOException {
          return target.storeInstructions (env, constantPoolOwner, this);}
   Instruction []        loadInstructions (CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException, IOException {
          return target. loadInstructions (env, constantPoolOwner, this);}
}

