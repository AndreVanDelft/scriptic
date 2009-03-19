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



class ScriptCallParameter extends JavaExpression
         implements scriptic.tokens.ScripticParseTreeCodes {

   public JavaExpression expression;
   public boolean isForcing    = false;
   public boolean isOutput     = false;
   public boolean isAdapting   = false;

   public MethodParameterDeclaration formalParameter = null; // when isAdapting
   public int     formalParameterIndex = -1;                 // when isAdapting
   public int     actualIndex  = 0;
   public Parameter target;       // when isOutput
                                                             // or isAdapting

   public ScriptCallParameter () { }
   public ScriptCallParameter (JavaExpression anotherExpression) { 
      super (anotherExpression);
   }

   public String getPresentationName () {
      if (   !isForcing
          && !isOutput
          && !isAdapting)
         return super.getPresentationName ();

      StringBuffer presentation = new StringBuffer ();
      presentation.append (super.getPresentationName ());
      presentation.append ("(");
      if (isForcing)    presentation.append ("forcing");
      if (isOutput)     presentation.append ("output");
      if (isAdapting)   presentation.append ("adapting");
      presentation.append (")");
      return presentation.toString ();
   }
}

