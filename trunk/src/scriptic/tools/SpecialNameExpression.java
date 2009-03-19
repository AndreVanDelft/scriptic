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

import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.ByteCodingException;

class SpecialNameExpression extends JavaExpressionWithTarget {
              // Target just needed for this() and super() constructor calls
   public boolean isConstant() {return token== NullToken;}
   public boolean isThis    () {return token== ThisToken;}
   public boolean isSuper   () {return token==SuperToken;}
   int token; /* ThisToken, SuperToken or NullToken */

   public SpecialNameExpression (int token) {
      this.token = token;
      if (token==NullToken) {constantValue = ConstantNull.theOne;}
   }
   public int getToken() {return token;}
   public int languageConstructCode () {return SpecialNameExpressionCode;}

   Instruction []     loadInstructions(CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException {
       Instruction[] result = null;
       switch (token) {
       case ThisToken:
       case SuperToken: result=new Instruction[1]; result[0]=new Instruction (INSTRUCTION_aload_0    , this); return result;
       case  NullToken: result=new Instruction[1]; result[0]=new Instruction (INSTRUCTION_aconst_null, this); return result;
       }
       return null;
   }
}
