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



/** 
 *  This tiny pass does the dimension signatures of the variables
 *  (dimenions of refinements may need those of the variables, so these are
 *   postponed a pass)
 */
public class ScripticCompilerPass5 extends ScripticParseTreeEnumeratorWithContext
               implements scriptic.tokens.ScripticTokens {

   public ScripticCompilerPass5 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }

   /* Main entry point */
   public boolean resolve (TopLevelTypeDeclaration t) {
      processTopLevelTypeDeclaration(t, null, null, null);
      return true;
   }

   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected void processVariableDeclaration  (MultiVariableDeclaration multiVariableDeclaration,
                                               MemberVariableDeclaration variableDeclaration,
                                               int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      getDimensionSignatureOf (variableDeclaration.dataTypeDeclaration);
      
      //variableDeclaration.target may not have been set, in case it is in an anonymous class
      //reparation would take a complete redesign; so we'll just skip it
      if (variableDeclaration.target!=null) {
          variableDeclaration.target.dimensionSignature = 
           variableDeclaration.dataTypeDeclaration.dimensionSignature;
      }
   }
}