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


class IntType extends SmallOrNormalIntType {
   static  IntType theOne = new IntType();
   private IntType() {}
   public boolean isInt() {return true;}
   public String    getSignature() {return "I";}
   public int getArrayTypeNumber() {return  10;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {
     return d.isInt    ()
         || d.isLong   ()
         || d.isFloat  ()
         || d.isDouble ();
   }
   public int    getToken() {return IntToken;}
   public String getName () {return IntRepresentation;}
   public short unaryInstructionCode (int token) {
     switch (token) {
     case TildeToken             : return INSTRUCTION_ixor;
     case MinusToken             : return INSTRUCTION_ineg;
     }
     return 0;
   }
   public short binaryInstructionCode (int token) {
     switch (token) {
     case      EqualsToken       : return INSTRUCTION_if_icmpeq;
     case    NotEqualToken       : return INSTRUCTION_if_icmpne;
     case GreaterThanToken       : return INSTRUCTION_if_icmpgt;
     case LessThanToken          : return INSTRUCTION_if_icmplt;
     case LessOrEqualToken       : return INSTRUCTION_if_icmple;
     case GreaterOrEqualToken    : return INSTRUCTION_if_icmpge;

     case PlusAssignToken        : 
     case PlusToken              : return INSTRUCTION_iadd;
     case MinusAssignToken       :
     case MinusToken             : return INSTRUCTION_isub;
     case AsteriskAssignToken    :
     case AsteriskToken          : return INSTRUCTION_imul;
     case SlashAssignToken       :
     case SlashToken             : return INSTRUCTION_idiv;
     case PercentAssignToken     :
     case PercentToken           : return INSTRUCTION_irem;

     case AmpersandAssignToken   :
     case AmpersandToken         : return INSTRUCTION_iand;
     case VerticalBarAssignToken :
     case VerticalBarToken       : return INSTRUCTION_ior;
     case CaretAssignToken       :
     case CaretToken             : return INSTRUCTION_ixor;

     case LeftShiftAssignToken   :
     case LeftShiftToken         : return INSTRUCTION_ishl;
     case RightShiftAssignToken  :
     case RightShiftToken        : return INSTRUCTION_ishr;
     case UnsignedRightShiftAssignToken:
     case UnsignedRightShiftToken: return INSTRUCTION_iushr;
     }
     return 0;
   }
   public short convertToInstructionCode(DataType d) {
       if (d==  ByteType.theOne) return INSTRUCTION_int2byte;
       if (d== ShortType.theOne) return INSTRUCTION_int2short;
       if (d==  CharType.theOne) return INSTRUCTION_int2char;
       if (d==  LongType.theOne) return INSTRUCTION_i2l;
       if (d== FloatType.theOne) return INSTRUCTION_i2f;
       /*presumeably....*/       return INSTRUCTION_i2d;
   }
   public short  arrayLoadInstructionCode() {return INSTRUCTION_iaload;}
   public short arrayStoreInstructionCode() {return INSTRUCTION_iastore;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangIntType;}
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmIntHolderType;}
   public String  accessNameForWrapperClass()          {return "intValue";}
}

