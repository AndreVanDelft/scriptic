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

class CastExpression extends JavaExpression {
   public JavaExpression unaryExpression;
   public  DataTypeDeclaration castTypeDeclaration;
   public  boolean             isNarrowing;

   public void    setMethodName    () {       unaryExpression.setMethodName    ();}
   public void    setExpressionName() {       unaryExpression.setExpressionName();}
   public void    setPackageName   () {       unaryExpression.setPackageName   ();}
   public void    setTypeName      () {       unaryExpression.setTypeName      ();}
   public void    setAmbiguousName () {       unaryExpression.setAmbiguousName ();}
   public boolean  isMethodName    () {return unaryExpression.isMethodName     ();}
   public boolean  isExpressionName() {return unaryExpression.isExpressionName ();}
   public boolean  isPackageName   () {return unaryExpression.isPackageName    ();}
   public boolean  isTypeName      () {return unaryExpression.isTypeName       ();}
   public boolean  isAmbiguousName () {return unaryExpression.isAmbiguousName  ();}

   public int languageConstructCode () {return CastExpressionCode;}

   public String getPresentation () {
      return getPresentationName () + " " + castTypeDeclaration.getPresentation();
   }

   public SpecialCode getLastSpecialCodeInExpression () {
      return unaryExpression.getLastSpecialCodeInExpression ();
   }
   public void setLastSpecialCodeInExpression (SpecialCode newCode) {
      unaryExpression.setLastSpecialCodeInExpression (newCode);
   }
}

