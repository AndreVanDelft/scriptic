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

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                    MethodParameterDeclaration                   */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class MethodParameterDeclaration extends BasicVariableDeclaration {
   public boolean isMethodParameter () {return true;}
   public String getConstructName   () {return "parameter";}
   public Field targetField() {return target;}
   public Parameter                   target;
   public boolean                     isFinal;
   public boolean                     isFinal() {return isFinal;}

   public Parameter makeTarget (CompilerEnvironment env, int i) {
      Parameter result        = new Parameter();
      result.source           = this;
      result.name             = name;
      result.dataType1        = dataType();
      result.declarationIndex = i;
      result.owner            = (Method) owner.targetField();
      target                  = result;
      return result;
   }
   public ScriptParameter makeTargetForScript (CompilerEnvironment env, int i) {
      ScriptParameter result  = new ScriptParameter();
      result.source           = this;
      result.name             = name;
      result.dataType1        = dataType();
      result.declarationIndex = i;
      result.owner            = (Method) owner.targetField();
      target                  = result;

      // Create fake parameter declaration to be referenced by the "old.p"
      OldParameter oldie        = new OldParameter ();
      oldie.name                = "old_" + name;
      oldie.dataType1           = result.dataType1;
      oldie.owner               = result.owner;
      oldie.declarationIndex    = i;
    //oldie.slot                = existingParameter.slot;
      oldie.source              = this;
      result.oldVersion         = oldie;
      return result;
   }
}

