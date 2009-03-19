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


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                       LocalVariableDeclaration                  */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class LocalVariableDeclaration extends MemberOrLocalVariableDeclaration {
   public JavaExpression initializer;
   public boolean isLocalVariable () {return true;}
   public String getConstructName () {return "local variable";}
   public Field targetField() {return target;}
   public LocalVariable               target;
   public boolean                     isFinal;
   public boolean                     isFinal() {return isFinal;}

   public LocalVariable makeTarget (CompilerEnvironment env) {
      LocalVariable result      = new MethodLocalVariable();
      result.source             = this;
      result.name               = name;
      result.dataType1          = dataType();
      result.dimensionSignature = dataTypeDeclaration.dimensionSignature;
      result.owner              = (Method) owner.targetField();
      target                    = result;
      return result;
   }
}

