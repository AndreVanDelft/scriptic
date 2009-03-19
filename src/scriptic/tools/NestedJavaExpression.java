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

class NestedJavaExpression extends JavaExpression {
   public JavaExpression subExpression;
   public int languageConstructCode () {return NestedJavaExpressionCode;}
   public void    setMethodName    () {       subExpression.setMethodName    ();}
   public void    setExpressionName() {       subExpression.setExpressionName();}
   public void    setPackageName   () {       subExpression.setPackageName   ();}
   public void    setTypeName      () {       subExpression.setTypeName      ();}
   public void    setAmbiguousName () {       subExpression.setAmbiguousName ();}
   public boolean  isMethodName    () {return subExpression.isMethodName     ();}
   public boolean  isExpressionName() {return subExpression.isExpressionName ();}
   public boolean  isPackageName   () {return subExpression.isPackageName    ();}
   public boolean  isTypeName      () {return subExpression.isTypeName       ();}
   public boolean  isAmbiguousName () {return subExpression.isAmbiguousName  ();}
   public boolean  isStringPlus
            (CompilerEnvironment env) {return subExpression.isStringPlus  (env);}
}

