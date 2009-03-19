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


class CharType extends SmallIntegralType {
   static  CharType theOne = new CharType();
   private CharType() {}
   public boolean isChar() {return true;}
   public String    getSignature() {return "C";}
   public int getArrayTypeNumber() {return   5;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {
     return d.isNumeric()
        && !d.isByte   ()
        && !d.isShort  ();
   }
   public int    getToken() {return CharToken;}
   public String getName () {return CharRepresentation;}
   public short  arrayLoadInstructionCode() {return INSTRUCTION_caload;}
   public short arrayStoreInstructionCode() {return INSTRUCTION_castore;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangCharType;}
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmCharHolderType;}
   public String  accessNameForWrapperClass()          {return "charValue";}
}

