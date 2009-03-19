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

abstract class LocalOrNestedClassContext extends ClassContext {
  LocalOrNestedTypeDeclaration localOrNestedTypeDeclaration;
  LocalOrNestedClassContext (CompilerEnvironment env, ParserErrorHandler parserErrorHandler,
                             LocalOrNestedTypeDeclaration localOrNestedTypeDeclaration) {
      super (env, parserErrorHandler, localOrNestedTypeDeclaration);
      this.localOrNestedTypeDeclaration = localOrNestedTypeDeclaration;
  }
  /* "Inner Classes in Java 1.1" 11/22/96, page 6:
   * Sometimes the combination of inheritance and lexical scoping 
   * can be confusing. For example, if the class E inherited a 
   * field named array from Enumeration, the field would hide 
   * the parameter of the same name in the enclosing scope. 
   * To prevent ambiguity in such cases, Java 1.1 allows 
   * inherited names to hide ones defined in enclosing block 
   * or class scopes, but prohibits them from being used without 
   * explicit qualification.
   */
  Variable resolveVariable (NameExpression e, Context fromContext, boolean fromLocalOrNestedClass)
          throws IOException, ByteCodingException {

      Variable        result = resolveLocalName (e, fromLocalOrNestedClass);
      Variable  parentResult = parent != null
                             ? parent.resolveVariable  (e, this, true)
                             : null;
      if (result ==null) {
          if (parentResult != null) {
              result = parentResult;
              if (result instanceof LocalVariableOrParameter) {
                  if (!result.isFinal()) {
                     parserError (2, "Reference to non-final local variable or parameter "+result.name+" from local type",
                               e.sourceStartPosition,
                               e.sourceEndPosition);
                  }
                  if (parent.isRefinementContext()
                  &&  ((LocalVariableOrParameter)result).owner == ((RefinementContext)parent).owner.target) {
                     LocalOrNestedClassType lnc = (LocalOrNestedClassType) localOrNestedTypeDeclaration.target;
                     result = lnc.copyLocalVariableOrParameter ((LocalVariableOrParameter)result);
                  }
                  else {
                     localOrNestedTypeDeclaration.target.setNeedForParentReference();
                  }
              }
              else {
                localOrNestedTypeDeclaration.target.setNeedForParentReference();
                   // so it will need another 'this' pointer...
              }
          }
      }
      else {
         if (parentResult != null
         &&  result.ownerClass(env) != classType()) {
                 parserError (2, "Reference to inherited field "+result.name
                             +" whereas the name is also in the outer context. Explicit qualification needed",
                               e.sourceStartPosition,
                               e.sourceEndPosition);
         }
         else if (fromContext.isStatic()
              &&  !result    .isStatic()) {
                 parserError (2, "Attempt to use instance field "+result.name
                                                +" from static context",
                                                e.sourceStartPosition,
                                                e.sourceEndPosition);
         }
      }
      return result;
  }
}

