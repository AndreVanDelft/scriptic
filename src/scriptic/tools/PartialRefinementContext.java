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

// very temporary context to allow parameter dimensions be used by subsequent parameters
// 
class PartialRefinementContext extends Context {
  private ArrayList<Parameter> params = new ArrayList<Parameter>();
  void addParameter (Parameter p) {params.add(p);}

  public boolean isStatic() {return false;}
  PartialRefinementContext (CompilerEnvironment env, ParserErrorHandler parserErrorHandler, TypeDeclaration typeDeclaration) {
    super (env, parserErrorHandler, typeDeclaration);
  }

  Variable resolveLocalName (NameExpression e, boolean fromLocalOrNestedClass) throws CompilerError {
     for (int i=0; i<params.size(); i++) {
        Parameter p = params.get(i);
        if (p.name.equals (e.name)) {
//new Exception ("PartialRefinementContext.resolveLocalName: "+e.name+" >> "+p.dimensionSignature).printStackTrace();
           return p;
        }
     }
//System.out.println ("PartialRefinementContext.could not resolveLocalName: "+e.name);
     return null;
  }
}
