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
import java.util.ArrayList;

class CompoundDimension extends Dimension {
  DimensionDeclaration sourceDeclaration;
  CompoundDimension (ClassType owner, CompoundDimensionDeclaration sourceDeclaration)
  {
     this.owner             = owner;
     this.sourceDeclaration = sourceDeclaration;
     this.modifierFlags     = sourceDeclaration.modifiers.modifierFlags;
     this.name              = sourceDeclaration.name;
  }
  CompoundDimension (ClassType owner, int modifierFlags, String name, String signature)
  {
     this.owner             = owner;
     this.modifierFlags     = modifierFlags;
     this.name              = name;
     this.signature         = signature;
  }
  static String getSignature(ArrayList<String> normalSignatures, ArrayList<String> invertSignatures) {
    String result = null;
    for (String s: normalSignatures) {
        result = product (result, s);
     }
    for (String s: invertSignatures) {
        result = division (result, s);
    }
//System.out.println ("CompoundDimension.getSignature: "+result);
    return result;
  }
  public String getPresentation () {return "dimension "+name;}
}

