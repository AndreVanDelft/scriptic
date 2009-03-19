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

import java.io.IOException;

import scriptic.tools.lowlevel.ByteCodingException;

class FieldInitializerContext extends Context {
  public FieldInitializerContext (CompilerEnvironment env, ParserErrorHandler parserErrorHandler, MemberVariableDeclaration owner) {
      super (env, parserErrorHandler, owner.owner.typeDeclaration);
      this.owner = owner;
  }
  public MemberVariableDeclaration owner;
  public boolean isFieldInitializerContext () {return true;}
  public boolean isStatic () {return owner.owner.isStatic();}

  Variable resolveVariable (NameExpression e, Context fromContext, boolean fromLocalOrNestedClass) throws IOException, ByteCodingException {
      Variable result = super.resolveVariable (e, fromContext, fromLocalOrNestedClass);
      if (result != null
      &&  result.isMemberVariable()
      &&  result.isStatic()
      ==         isStatic()) {

          MemberVariable m = (MemberVariable) result;
          if (classType() == m.owner // in same class, so source!=null
          &&  owner.declarationIndex <= m.source.declarationIndex
          &&  owner.owner.typeDeclaration.target == result.ownerClass(env)) {
              parserError (2, "Initializer uses subsequent field "+result.name,
                                                e.sourceStartPosition,
                                                e.sourceEndPosition);
          }
      }
      return result;
  }
}

