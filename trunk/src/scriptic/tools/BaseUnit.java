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
import scriptic.tools.lowlevel.ByteCodingException;

class BaseUnit extends MemberVariable {
   BaseUnitDeclaration sourceDeclaration;
//   public boolean isPublic     () {return  true;}
   public boolean isFinal      () {return  true;}
   public boolean canBeAssigned() {return false;}
//   public boolean isStatic     () {return  true;}
//   public boolean isAccessibleFor (CompilerEnvironment env, ClassType clazz, boolean isForAllocation) {return true;}

  BaseUnit (BaseUnitDeclaration sourceDeclaration, ClassType owner, int modifierFlags, String signature) throws ByteCodingException {
    this.sourceDeclaration  = sourceDeclaration;
    this.owner              = owner;
    this.dimensionSignature = signature;
    setConstantValue (new ConstantInt(1));
    name               = sourceDeclaration.name;
    this.modifierFlags = modifierFlags | StaticFlag;
    dataType1          = IntType.theOne;
    addAttribute (new ConstantInt(1).makeAttribute(owner));
  }
}

