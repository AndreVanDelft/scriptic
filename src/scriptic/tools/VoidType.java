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


class VoidType extends PrimitiveType {
   static  VoidType theOne = new VoidType();
   private VoidType() {}
   public boolean isVoid () {return true;}
   public byte   slots()    {return 0;}
   public String  getSignature() {return "V";}
   public int    getToken() {return VoidToken;}
   public String getName () {return VoidRepresentation;}
   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) {return this==d;}
   public short returnInstructionCode()     {return INSTRUCTION_return;}
   public short    popInstructionCode()     {return -1;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangVoidType;}
}
