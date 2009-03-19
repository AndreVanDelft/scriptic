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


class FloatType extends NumericType {
   static  FloatType theOne = new FloatType();
   private FloatType() {}
   public boolean isFloat () {return true;}
   public String    getSignature() {return "F";}
   public int getArrayTypeNumber() {return   6;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {
     return d.isFloat  ()
         || d.isDouble ();
   }
   public int    getToken() {return FloatToken;}
   public String getName () {return FloatRepresentation;}
   public short storeInstructionCode() {return INSTRUCTION_fstore;}
   public short  loadInstructionCode() {return INSTRUCTION_fload ;}
   public short  unaryInstructionCode (int token) {return INSTRUCTION_fneg;}
   public short binaryInstructionCode (int token) {
     switch (token) {
     case      EqualsToken       : return INSTRUCTION_ifeq;
     case    NotEqualToken       : return INSTRUCTION_ifne;
     case GreaterThanToken       : return INSTRUCTION_ifgt;
     case LessThanToken          : return INSTRUCTION_iflt;
     case LessOrEqualToken       : return INSTRUCTION_ifle;
     case GreaterOrEqualToken    : return INSTRUCTION_ifge;

     case PlusAssignToken        :
     case PlusToken              : return INSTRUCTION_fadd;
     case MinusAssignToken       :
     case MinusToken             : return INSTRUCTION_fsub;
     case AsteriskAssignToken    :
     case AsteriskToken          : return INSTRUCTION_fmul;
     case SlashAssignToken       :
     case SlashToken             : return INSTRUCTION_fdiv;
     case PercentAssignToken     :
     case PercentToken           : return INSTRUCTION_frem;
     }
     return 0;
   }
   public short convertToInstructionCode(DataType d) {
       if (d==DoubleType.theOne) return INSTRUCTION_f2d;
       if (d==  LongType.theOne) return INSTRUCTION_f2l;
                                 return INSTRUCTION_f2i;
   }
   public short     returnInstructionCode() {return INSTRUCTION_freturn;}
   public short  arrayLoadInstructionCode() {return INSTRUCTION_faload;}
   public short arrayStoreInstructionCode() {return INSTRUCTION_fastore;}
   public short    const_0InstructionCode() {return INSTRUCTION_fconst_0;}
   public short    const_1InstructionCode() {return INSTRUCTION_fconst_1;}
   public short        cmpInstructionCode(boolean doGreater) {
                           return doGreater? INSTRUCTION_fcmpl: INSTRUCTION_fcmpg;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangFloatType;}
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmFloatHolderType;}
   public String  accessNameForWrapperClass()          {return "floatValue";}
}

