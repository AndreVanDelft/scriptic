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

class BaseDimensionDeclaration extends DimensionDeclaration {
  public int languageConstructCode () {return BaseDimensionDeclarationCode;}
  BaseUnitDeclaration unit;
  BaseDimension    target;
  public String getPresentation () {return "dimension "+name+" unit "+unit.name;}
  public String getDescription () {return getPresentation();}
  void makeTarget(ClassType classType) throws ByteCodingException  {
     target = new BaseDimension (classType, this);
     if (typeDeclaration.isInterface())
        target.modifierFlags |= PublicFlag;
     target.modifierFlags |= FinalFlag;
     unit.makeTarget(classType, target.modifierFlags, target.signature);
     target.unit = (BaseUnit) unit.target;
  }
}

