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
import scriptic.tools.lowlevel.ClassEnvironment;
import scriptic.tools.lowlevel.ClassFileConstants;
import scriptic.tools.lowlevel.ConstantValueAttribute;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

public abstract class ConstantValue implements ClassFileConstants, scriptic.tokens.JavaTokens {
   public boolean isNull   () {return false;}
   public boolean isTrue   () {return false;}
   public boolean isFalse  () {return false;}
   public boolean isZero   () {return false;}
   public boolean isOne    () {return false;}
   public boolean isNumeric() {return false;}
   public Object makeObject() {return null;}
   public String makeString() {return makeObject().toString();}
   public DataType dataType(CompilerEnvironment env) {return null;}
   public ConstantValue stringPlus (ConstantValue c) {
       return new ConstantString(makeString() + c.makeString());
   }
   public ConstantValueAttribute makeAttribute(ClassEnvironment e) throws ByteCodingException {return null;}
   public ConstantValue doOperator(int operator, ConstantValue c) {return null;}
   public ConstantValue promoteUnary ()                {return this;}
   public ConstantValue promoteBinary(DataType other) {return this;}
   public ConstantValue convertTo          (int primitiveTypeToken)               {return this;} 
   public ConstantValue  doLogicalComplement ()                                   {return null;}
   public ConstantValue  doUnaryOperator (int operatorToken)                      {return null;}
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue other) {return null;}
   public ConstantValue  doShiftOperator (int operatorToken, ConstantValue other) {return null;}
   public boolean canBeRepresentedAsByte () {return false;} // only relevant for ConstantInts
   public boolean canBeRepresentedAsChar () {return false;} // only relevant for ConstantInts
   public boolean canBeRepresentedAsShort() {return false;} // only relevant for ConstantInts
   public abstract Instruction []     loadInstructions (ClassType t,InstructionOwner owner) throws ByteCodingException;

/************ not needed; already in MemberVariable ...
   public static ConstantValue getFromAttribue (ConstantValueAttribute attribute, DataType d) {
      if (attribute==null) return null;
      ConstantPoolItem c = attribute.constantPoolItem;
      switch (d.getToken()) {
         case    ByteToken: return new ConstantByte   ((byte) ((ConstantPoolInteger)c).   intValue); 
         case    CharToken: return new ConstantChar   ((char) ((ConstantPoolInteger)c).   intValue); 
         case  DoubleToken: return new ConstantDouble (       ((ConstantPoolDouble )c).doubleValue); 
         case   FloatToken: return new ConstantFloat  (       ((ConstantPoolFloat  )c). floatValue); 
         case     IntToken: return new ConstantInt    (       ((ConstantPoolInteger)c).   intValue); 
         case    LongToken: return new ConstantLong   (       ((ConstantPoolLong   )c).  longValue); 
         case   ShortToken: return new ConstantShort  ((short)((ConstantPoolInteger)c).   intValue); 
         case BooleanToken: return new ConstantBoolean(       ((ConstantPoolInteger)c).   intValue!=0); 
      }
      return null;
   }
******************************/
}

