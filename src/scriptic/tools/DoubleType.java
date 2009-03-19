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


class DoubleType extends BigNumericType {
   static  DoubleType theOne = new DoubleType();
   private DoubleType() {}
   public boolean       isDouble() {return true;}
   public String    getSignature() {return "D";}
   public int getArrayTypeNumber() {return   7;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {return this==d;}
   public int    getToken() {return DoubleToken;}
   public String getName () {return DoubleRepresentation;}
   public short storeInstructionCode() {return INSTRUCTION_dstore;}
   public short  loadInstructionCode() {return INSTRUCTION_dload ;}
   public short  unaryInstructionCode (int token) {return INSTRUCTION_dneg;}
   public short binaryInstructionCode (int token) {
     switch (token) {
     case      EqualsToken       : return INSTRUCTION_ifeq;
     case    NotEqualToken       : return INSTRUCTION_ifne;
     case GreaterThanToken       : return INSTRUCTION_ifgt;
     case LessThanToken          : return INSTRUCTION_iflt;
     case LessOrEqualToken       : return INSTRUCTION_ifle;
     case GreaterOrEqualToken    : return INSTRUCTION_ifge;

     case PlusAssignToken        :
     case PlusToken              : return INSTRUCTION_dadd;
     case MinusAssignToken       :
     case MinusToken             : return INSTRUCTION_dsub;
     case AsteriskAssignToken    :
     case AsteriskToken          : return INSTRUCTION_dmul;
     case SlashAssignToken       :
     case SlashToken             : return INSTRUCTION_ddiv;
     case PercentAssignToken     :
     case PercentToken           : return INSTRUCTION_drem;
     }
     return 0;
   }
   public short convertToInstructionCode(DataType d) {
       if (d==FloatType.theOne) return INSTRUCTION_d2f;
       if (d== LongType.theOne) return INSTRUCTION_d2l;
                                return INSTRUCTION_d2i;
   }
   public short     returnInstructionCode() {return INSTRUCTION_dreturn;}
   public short  arrayLoadInstructionCode() {return INSTRUCTION_daload;}
   public short arrayStoreInstructionCode() {return INSTRUCTION_dastore;}
   public short    const_0InstructionCode() {return INSTRUCTION_dconst_0;}
   public short    const_1InstructionCode() {return INSTRUCTION_dconst_1;}
   public short        cmpInstructionCode(boolean doGreater) {
                           return doGreater? INSTRUCTION_dcmpl: INSTRUCTION_dcmpg;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangDoubleType;}
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmDoubleHolderType;}
   public String  accessNameForWrapperClass()          {return "doubleValue";}
}

