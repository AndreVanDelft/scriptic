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

class MemberVariableDeclaration extends MemberOrLocalVariableDeclaration {
   public JavaExpression initializer;
   public boolean        hasError;
   public MemberVariable target;
   public Field targetField() {return target;}
   public boolean             isDeprecated;

   public final boolean isPublic      () {return owner.isPublic      ();}
   public final boolean isPrivate     () {return owner.isPrivate     ();}
   public final boolean isProtected   () {return owner.isProtected   ();}
   public final boolean isStatic      () {return owner.isStatic      ();}

   public String getConstructName () {return "variable";}

   public String getPresentation () {
      return super.getPresentation () + " in " + owner.typeDeclaration.getPresentation();
   }

   public MemberVariable makeTarget (CompilerEnvironment env) {
      MemberVariable result     = new MemberVariable();
      result.source             = this;
      result.name               = name;
      result.dataType1          = dataType();
      result.dimensionSignature = dataTypeDeclaration.dimensionSignature;
      result.owner              = owner.typeDeclaration.target;
      result.modifierFlags      = owner.modifiers.modifierFlags;
      if (owner.typeDeclaration.isInterface())
          result.modifierFlags |= FinalFlag|StaticFlag|PublicFlag;
      if (isDeprecated) result.setDeprecated();
      target                    = result;
      return result;
   }

}

