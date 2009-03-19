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

import java.io.*;
import scriptic.tools.lowlevel.*;

abstract class LiteralExpression extends JavaExpression {

   public boolean isConstant() {return true;}

   public static LiteralExpression makeNew(CompilerEnvironment env, int token, Object tokenValue) throws NumberFormatException {
      LiteralExpression result = null;
      switch (token) {
      case   IntegerLiteralToken: result = new IntegerLiteralExpression(tokenValue); break;
      case      LongLiteralToken: result = new    LongLiteralExpression(tokenValue); break;
      case     FloatLiteralToken: result = new   FloatLiteralExpression(tokenValue); break;
      case    DoubleLiteralToken: result = new  DoubleLiteralExpression(tokenValue); break;
      case    StringLiteralToken: result = new  StringLiteralExpression(env,tokenValue); break;
      case CharacterLiteralToken: result = new    CharLiteralExpression(tokenValue); break;
      case   BooleanLiteralToken: result = new BooleanLiteralExpression(tokenValue); break;
      }
      return result;
   }

   public abstract int getLiteralToken();
   public String        getAsString() {return constantValue.makeString();}
   public int languageConstructCode() {return LiteralExpressionCode;}

   public String getPresentation () {
      return getName () + " \"" + getAsString() + "\"";
   }
   public void writeString(ClassEnvironment e, DataOutputStream out) throws IOException, ByteCodingException {
       out.writeChars(constantValue.makeString());
   }
}


