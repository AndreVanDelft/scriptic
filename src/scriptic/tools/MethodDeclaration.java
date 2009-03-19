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

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                         MethodDeclaration                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class MethodDeclaration extends RefinementDeclaration {
   public StatementBlock      statements;
   public ThrowsClause        throwsClause;
   public ThrowsClause        throwsClause() {return throwsClause;}

   public int languageConstructCode () {return MethodDeclarationCode;}
   public int getAllowedModifiers   () {return typeDeclaration.isClass()
                                             ? AllowedMethodModifiers
                                             : AllowedInterfaceMethodModifiers;}

   /* ---------------------------- Presentation ------------------------ */

   public String getConstructName () {return "A method";}
   public String getPresentationName () {
      return target.returnType.getPresentation() + " " + getName();
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isMethodDeclaration    () {return true;}

   /** answer whether the declared throwsclause contains a superclass of the given throwable.
    * If so, mark the corresponding ThrownTypeDeclaration as used
    */
   public boolean declaresToThrowFromBody (CompilerEnvironment env, DataType throwable)
          throws IOException, CompilerError {

       if (throwsClause==null) return false;
       for (DataTypeDeclaration exceptionType: throwsClause.exceptionTypeDeclarations) {
          ThrownTypeDeclaration t = (ThrownTypeDeclaration) exceptionType;
          if (t.wantsToThrow (env, throwable)) return true;
       }
       return false;
   }

   public Method makeTarget(CompilerEnvironment env) {
       Method result              = new Method();
       result.source              = this;
       result.name                = name;
       result.owner               = typeDeclaration.target;
       result.modifierFlags       = modifiers.modifierFlags;
       if (typeDeclaration.isInterface())result.modifierFlags |= AbstractFlag|PublicFlag;
       if (returnTypeDeclaration != null) {// else constructor: default VoidType
         result.returnType        = returnTypeDeclaration.dataType;
       }
       result.parameters = new Parameter [parameterList.parameters.size()];
       for (int i=0; i<result.parameters.length; i++) {
           MethodParameterDeclaration parameterDeclaration
             = parameterList.parameters.get(i);
           Parameter parameter  = parameterDeclaration.makeTarget(env, i);
           result.parameters[i] = parameter;
       }
       if (isDeprecated) target.setDeprecated();

       target = result;
       return result;
   }
}

