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


class ShortType extends SmallIntegralType {
   static  ShortType theOne = new ShortType();
   private ShortType() {}
   public boolean isShort() {return true;}
   public String    getSignature() {return "S";}
   public int getArrayTypeNumber() {return   9;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {
     return d.isNumeric()
        && !d.isByte   ()
        && !d.isChar   ();
   }
   public int    getToken() {return ShortToken;}
   public String getName () {return ShortRepresentation;}
   public short  arrayLoadInstructionCode() {return INSTRUCTION_saload;}
   public short arrayStoreInstructionCode() {return INSTRUCTION_sastore;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangShortType;}
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmShortHolderType;}
   public String  accessNameForWrapperClass()          {return "shortValue";}
}

