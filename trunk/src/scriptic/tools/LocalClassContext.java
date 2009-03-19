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


class LocalClassContext extends LocalOrNestedClassContext {
  LocalClassContext (CompilerEnvironment env, ParserErrorHandler parserErrorHandler, LocalTypeDeclaration typeDeclaration) {
      super (env, parserErrorHandler, typeDeclaration);
  }
  public ClassType findNestedOrLocalClass (String name) {

/********************************
ERROR: localTypesContext not set... ignore for the time being:

    for (Enumeration e=((LocalTypeDeclaration)typeDeclaration).localTypesContext.elements();
                     e.hasMoreElements(); ) {
        LocalTypeDeclaration lt = (LocalTypeDeclaration) e.nextElement();
        if (lt.name.equals (name) ) {        // NOT OK FOR compound names...

            // REPARATION: compare with first name component;
            // if found then construct nameWithDots (and '$') and check in environment...

            return lt.target;
        }
    }
***********************/

    return super.findNestedOrLocalClass (name);
  }
}
