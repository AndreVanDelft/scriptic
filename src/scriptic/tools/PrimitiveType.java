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

abstract class PrimitiveType extends NonArrayType {
   public boolean isPrimitive () {return true;}

   /** Casting compatibility - $5.5
    *  @return null: OK, else reason why not OK
    */
   public String whyCannotBeCastedTo(CompilerEnvironment env, DataType d) throws IOException, CompilerError {
     if (d.isReference()) return "cannot cast to reference type";
     return isSubtypeOf(env, d)
       || d.isSubtypeOf(env, this)? null: "no primitive casting conversion applicable";
   }
   static PrimitiveType forToken (int token) {
      switch (token) {
         case    ByteToken: return    ByteType.theOne; 
         case    CharToken: return    CharType.theOne; 
         case  DoubleToken: return  DoubleType.theOne; 
         case   FloatToken: return   FloatType.theOne; 
         case     IntToken: return     IntType.theOne; 
         case    LongToken: return    LongType.theOne; 
         case   ShortToken: return   ShortType.theOne; 
         case BooleanToken: return BooleanType.theOne; 
         case    VoidToken: return    VoidType.theOne;
      }
      return null;
   }
   public String getSimpleSignature  () {return getSignature();}
   public short storeInstructionCode () {return INSTRUCTION_istore ;}
   public short  loadInstructionCode () {return INSTRUCTION_iload  ;}
   public short returnInstructionCode() {return INSTRUCTION_ireturn;}
}

