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
import  java.io.*;
import  scriptic.tools.lowlevel.*;

class ConstantString extends ConstantNumber {
   String value;
   public ConstantString (String value) {this.value=value;}
   public DataType dataType(CompilerEnvironment env) {return env.javaLangStringType;}
   public Object makeObject() {return value;}
   public ConstantValueAttribute makeAttribute(ClassEnvironment e) throws ByteCodingException
   {
      return new ConstantValueAttribute (e.resolveString (value));
   }
   public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out) throws IOException, ByteCodingException
   {
      new ConstantValueAttribute (e.resolveUnicode (value)).write(e,owner,out);
   }
   public ConstantValue doBinaryOperator (int operatorToken, ConstantValue otherConstant) {
     if (otherConstant==ConstantNull.theOne) return new ConstantBoolean(false);
     ConstantString other = (ConstantString) otherConstant;
     switch (operatorToken) {
     case      EqualsToken       : return new ConstantBoolean(value ==other.value);
     case    NotEqualToken       : return new ConstantBoolean(value !=other.value);
     case PlusToken              : return new ConstantString (value + other.value);
     }
     return null;
   }
   public ConstantPoolItem resolveToConstantPoolItem (ClassEnvironment e) {
     return e.resolveString(value);
   }
   public Instruction []     loadInstructions (ClassType t,InstructionOwner owner) {
    try {
      Instruction[] result = new Instruction[1];
      result[0] = t.resolveString (value).loadInstruction (owner);
      return result;
    } catch (ByteCodingException e) {e.printStackTrace(); return null;}
  }
}

