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



class LongType extends BigNumericType {
   static  LongType theOne = new LongType();
   private LongType() {}
   public boolean isLong        () {return true;}
   public boolean isIntegral    () {return true;}
   public String  getSignature  () {return "J";}
   public int getArrayTypeNumber() {return  11;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {
     return d.isLong   ()
         || d.isFloat  ()
         || d.isDouble ();
   }
   public int    getToken() {return LongToken;}
   public String getName () {return LongRepresentation;}
   public short storeInstructionCode() {return INSTRUCTION_lstore;}
   public short  loadInstructionCode() {return INSTRUCTION_lload ;}
   public short unaryInstructionCode (int token) {
     switch (token) {
     case TildeToken             : return INSTRUCTION_ixor;
     case MinusToken             : return INSTRUCTION_lneg;
     }
     return 0;
   }
   public short binaryInstructionCode (int token) {
     switch (token) {
     case      EqualsToken       : return INSTRUCTION_ifeq;
     case    NotEqualToken       : return INSTRUCTION_ifne;
     case GreaterThanToken       : return INSTRUCTION_ifgt;
     case LessThanToken          : return INSTRUCTION_iflt;
     case LessOrEqualToken       : return INSTRUCTION_ifle;
     case GreaterOrEqualToken    : return INSTRUCTION_ifge;

     case PlusAssignToken        :
     case PlusToken              : return INSTRUCTION_ladd;
     case MinusAssignToken       :
     case MinusToken             : return INSTRUCTION_lsub;
     case AsteriskAssignToken    :
     case AsteriskToken          : return INSTRUCTION_lmul;
     case SlashAssignToken       :
     case SlashToken             : return INSTRUCTION_ldiv;
     case PercentAssignToken     :
     case PercentToken           : return INSTRUCTION_lrem;

     case AmpersandAssignToken   :
     case AmpersandToken         : return INSTRUCTION_land;
     case VerticalBarAssignToken :
     case VerticalBarToken       : return INSTRUCTION_lor;
     case CaretAssignToken       :
     case CaretToken             : return INSTRUCTION_lxor;

     case LeftShiftAssignToken   :
     case LeftShiftToken         : return INSTRUCTION_lshl;
     case RightShiftAssignToken  :
     case RightShiftToken        : return INSTRUCTION_lshr;
     case UnsignedRightShiftAssignToken:
     case UnsignedRightShiftToken: return INSTRUCTION_lushr;
     }
     return 0;
   }
   public short convertToInstructionCode(DataType d) {
       if (d==DoubleType.theOne) return INSTRUCTION_l2d;
       if (d== FloatType.theOne) return INSTRUCTION_l2f;
                                 return INSTRUCTION_l2i;
   }
   public short       cmpInstructionCode(boolean doGreater) {return INSTRUCTION_lcmp;}
   public short     returnInstructionCode() {return INSTRUCTION_lreturn;}
   public short  arrayLoadInstructionCode() {return INSTRUCTION_laload;}
   public short arrayStoreInstructionCode() {return INSTRUCTION_lastore;}
   public short    const_0InstructionCode() {return INSTRUCTION_lconst_0;}
   public short    const_1InstructionCode() {return INSTRUCTION_lconst_1;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangLongType;}
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmLongHolderType;}
   public String  accessNameForWrapperClass()          {return "longValue";}
}

