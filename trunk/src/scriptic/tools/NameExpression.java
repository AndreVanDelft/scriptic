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

import scriptic.tools.lowlevel.*;

class NameExpression extends JavaExpressionWithTarget {
   public SpecialCode specialCode = SpecialCode.none;    // hooks for e.g. Scriptic
   public Object specialObject  = null;

   Instruction dupForAssignmentInstruction() throws ByteCodingException {
      if (target instanceof LocalVariableOrParameter)
          return new Instruction (INSTRUCTION_dup, this);
      return dup_xValueInstruction();
   }

   public NameExpression() {
      super();
      setExpressionName(); // will be undone in ExpressionChecker for method call and field access...
   }
   public SpecialCode  getLastSpecialCodeInExpression ()            {return specialCode;}
   public void setLastSpecialCodeInExpression (SpecialCode newCode) {specialCode = newCode;}
   public int languageConstructCode () {return NameExpressionCode;}
   public boolean       isPlainName () {return true;}
   public boolean   isQualifiedName () {return true;}
   public boolean   isOldIdentifier () {return name.equals("old");}
   public String qualifiedName () {return name;}
   public String getPresentationName () {
      String result = super.getPresentationName();
      if (specialCode == SpecialCode.none) return result;
      return result + " (special " + specialCode + ")";
   }
}

