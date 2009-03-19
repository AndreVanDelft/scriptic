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

import scriptic.tools.lowlevel.ClassEnvironment;
import scriptic.tools.lowlevel.ConstantPoolFieldReference;
import scriptic.tools.lowlevel.FieldInstruction;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

////////////////////////////////////////////////////////////////////////////
//
//                             Variable
//
////////////////////////////////////////////////////////////////////////////

abstract class Variable extends Field {
   public DataType      dataType1;
   public DataType getDataType (CompilerEnvironment env) throws ByteCodingException, IOException {
       if (dataType1==null) {
           dataType1 = DataType.getFromSignature (env, signature, ownerClass(env), false);
           resolveConstantValue(ownerClass(env));
       }
       return dataType1;
   }
   public ConstantValue constantValue() {return null;}
          void   resolveConstantValue(ClassEnvironment env) throws ByteCodingException, IOException {}
   public boolean canHandleIincInstruction() {return false;}
   public boolean canBeAssigned           () {return  true;}
   public boolean isOldParameter          () {return false;}
   public boolean isScriptParameter       () {return false;}
   public boolean isScriptLocalVariable   () {return false;}
   public boolean isDeprecated           () {return false;}

   public void setConfiningJavaStatement(LanguageConstruct c) {}

   public String getSignature (ClassesEnvironment env) {
      return dataType1.getSignature();
   }
   public String getConstructName () {return "variable";}

   public String getSlotPresentation () {return "";}

   public String getDataTypePresentation () {
      String result = dataType1!=null? dataType1.getPresentation(): signature;
      if (dimensionSignature != null) result += '*'+dimensionSignature;
      return result;
   }
   public String getConstantValuePresentation () {return "";}
   public String getPresentation () {
      StringBuffer presentation = new StringBuffer ();
      presentation
               .append (            getConstructName()).append (' ')
               .append (         getSlotPresentation()).append (' ')
               .append (           getModifierString()).append (' ')
               .append (     getDataTypePresentation()).append (' ')
               .append (name);
      return presentation.toString ();
   }
  public String getDescription () {
      return new StringBuffer ()
               .append (getPresentation ()).append (' ')
               .append (getConstantValuePresentation())
               .toString ();
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isMemberVariable () {return false;}
   public boolean isMethodParameter() {return false;}
   public boolean isLocalVariable  () {return false;}
   public boolean isPrivateVariable() {return false;}

   /* ------------------------- Code generation ------------------------ */

   // convert to a constant pool 'fieldReference' for a given constant pool owner.
   // Cache the answer because the constant pool owner may ask more than once.

           String        nameForFieldRef() {return name;}
           DataType  dataTypeForFieldRef(CompilerEnvironment env) throws ByteCodingException, IOException {return getDataType(env);}
   private ClassType createdForConstantPoolOwner;
   private ConstantPoolFieldReference fieldRef;
   public  ConstantPoolFieldReference fieldRef (CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException, IOException {
      if (fieldRef==null  // cashed value per constant pool(owner)
      ||  createdForConstantPoolOwner != constantPoolOwner) {
          createdForConstantPoolOwner  = constantPoolOwner;
          fieldRef =constantPoolOwner.resolveFieldReference (ownerClass(env).nameWithSlashes,
                                                             nameForFieldRef(),
                                                             dataTypeForFieldRef(env).getSignature());
      }
      return fieldRef;
   }

   Instruction dupReferenceInstruction (InstructionOwner instructionOwner) throws ByteCodingException {
        return isStatic()? null: new  Instruction (INSTRUCTION_dup,  instructionOwner);}

   Instruction dup_xValueInstruction (InstructionOwner instructionOwner) throws ByteCodingException {
       int code = isStatic()
                ? dataType1.   dupInstructionCode()
                : dataType1.dup_x1InstructionCode();
       return new Instruction (code, instructionOwner);
   }

   Instruction []     storeInstructions (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
       Instruction [] result = new Instruction [1];
       result[0] = new FieldInstruction ( isStatic()
                                     ? INSTRUCTION_putstatic
                                     : INSTRUCTION_putfield,
                                       fieldRef(env, constantPoolOwner),
                                       getDataType(env).isBig(),
                                       instructionOwner);
       return result;
   }
   Instruction []     loadInstructions  (CompilerEnvironment env, ClassType constantPoolOwner, InstructionOwner instructionOwner)
      throws ByteCodingException, IOException {
       Instruction [] result = new Instruction [1];
       result[0] =  new FieldInstruction ( isStatic()
                                      ? INSTRUCTION_getstatic
                                      : INSTRUCTION_getfield,
                                        fieldRef(env, constantPoolOwner),
                                        getDataType(env).isBig(),
                                        instructionOwner);
       return result;
   }

   Instruction     iincInstruction(int amount, InstructionOwner instructionOwner) throws ByteCodingException {
        return null;
   }
}

