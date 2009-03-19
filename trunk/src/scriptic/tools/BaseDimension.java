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

class BaseDimension extends Dimension {
  BaseUnit unit;

  private BaseDimension (ClassType owner, int modifierFlags, String name)
  {
     this.signature         = "1D1"+owner.nameWithSlashes+"."+name+";";
     this.owner             = owner;
     this.modifierFlags     = modifierFlags;
     this.name              = name;
  }
  BaseDimension (ClassType owner, BaseDimensionDeclaration sourceDeclaration)
  {
     this (owner, sourceDeclaration.modifiers.modifierFlags, sourceDeclaration.name);
     this.sourceDeclaration = sourceDeclaration;
     this.unit              = (BaseUnit) sourceDeclaration.unit.target;
  }
  BaseDimension (ClassType owner, int modifiers, String name, String unitName)
  {
     this (owner, modifiers, name);
     // forget the unitName
  }
  public String getPresentation () {return "dimension "+name+" unit "+(unit==null?"??": unit.name);}
}

