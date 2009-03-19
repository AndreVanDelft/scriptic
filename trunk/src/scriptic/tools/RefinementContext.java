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
import java.util.HashMap;

abstract class RefinementContext extends Context {
  RefinementContext (CompilerEnvironment env, ParserErrorHandler parserErrorHandler, RefinementDeclaration owner) {
      super (env, parserErrorHandler, owner.typeDeclaration);
      this.owner = owner;
  }
  public RefinementDeclaration owner;
  protected ArrayList<LocalVariableOrParameter>    localDataDeclarations = new ArrayList<LocalVariableOrParameter>();
  protected HashMap<String, ArrayList<LocalVariableOrParameter>> currentNameEnvironment  
      = new HashMap<String, ArrayList<LocalVariableOrParameter>> ();
  protected int       nextLocalDataSlotNumber = 0;
  public boolean isRefinementContext () {return true;}
  HashMap<String, BasicVariableDeclaration> parameterNames = new HashMap<String, BasicVariableDeclaration>();
  public boolean isStatic () {return owner.isStatic();}

   /* -------------------- Name environment routines ------------------- */

   protected void pushLocalName (LocalVariableOrParameter declaration) {
	   ArrayList<LocalVariableOrParameter> declarations = currentNameEnvironment.get (declaration.name);
      if (declarations == null) {
         declarations = new ArrayList<LocalVariableOrParameter>();
         currentNameEnvironment.put (declaration.name, declarations);
      }
      declarations.add(declaration);
   }

   protected void popLocalName (String name) {
	   ArrayList<LocalVariableOrParameter> declarations =currentNameEnvironment.get (name);
      if (declarations == null) 
         return;
      declarations.remove (declarations.size() - 1);
      if (declarations.isEmpty ())
         currentNameEnvironment.remove (name);
   }

   protected LocalVariableOrParameter getLocalName (String name) {
	   ArrayList<LocalVariableOrParameter> declarations = currentNameEnvironment.get (name);
      if (declarations == null)
         return null;
      else
         return declarations.get(declarations.size() - 1);
   }

   /* getFirstLocalName -- used to get at formal parameters */
   protected LocalVariableOrParameter getFirstLocalName (String name) {
	  ArrayList<LocalVariableOrParameter> declarations = currentNameEnvironment.get (name);
      if (declarations == null)
         return null;
      else
         return declarations.get(0);
   }

   /* ------------------ Declaration collection routines --------------- */

   protected void pushLocalDeclaration (LocalVariableOrParameter declaration) {
    //declaration.declarationIndex    = localDataDeclarations.size ();
      declaration.slot = nextLocalDataSlotNumber;
      localDataDeclarations.add (declaration);
      pushLocalName (declaration);
      nextLocalDataSlotNumber++;
   }

   protected void popLocalDeclaration (LocalVariableOrParameter declaration, LanguageConstruct endOfScope) {
      popLocalName (declaration.name);
    //declaration.scopeEndPosition = endOfScope.sourceEndPosition;
      declaration.setConfiningJavaStatement (endOfScope);

      // Only free up the script local data slot
      // if the popped declaration actually OCCUPIED a script local data slot!
      if (declaration.slot >= 0)
         nextLocalDataSlotNumber--;
   }

   protected void popLocalDeclarations (ArrayList<LocalVariableOrParameter> declarations, LanguageConstruct endOfScope) {
      for (LocalVariableOrParameter declaration: declarations) {
         popLocalDeclaration (declaration, endOfScope);
      }
   }
  Variable resolveLocalName (NameExpression e, boolean fromLocalOrNestedClass) throws CompilerError {
      return getLocalName(e.name);
  }
}
