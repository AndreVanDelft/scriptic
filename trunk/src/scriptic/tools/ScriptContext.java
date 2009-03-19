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



class ScriptContext extends RefinementContext {
  boolean        hasDurationAssignment;
  boolean processingDurationAssignment;
  boolean processingPriorityAssignment;
  boolean      allowPriorityAssignment;
  boolean      allowPriorityUsage;
  boolean      allowSuccessAssignment = true;
  boolean      allowSuccessUsage;

  public ScriptContext (CompilerEnvironment env, ParserErrorHandler parserErrorHandler, RefinementDeclaration owner) {
      super (env, parserErrorHandler, owner);
  }
  public boolean isScriptContext () {return true;}

   /* ------------------------------------------------------------------ */

   protected boolean checkReservedIdentifier  (LanguageConstruct construct) {
      String constructName = construct.getName();
      if    (constructName == null) return false;
      return constructName.equals("duration")
          || constructName.equals("priority")
          || constructName.equals("success" )
          || constructName.equals("pass"    );
   }
}
