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



class PrivateScriptVariableDeclaration extends LocalScriptVariableDeclaration {
   public BasicVariableDeclaration targetDeclaration;
   public ScriptPrivateVariable  target;
   public Field targetField() {return target;}

   public BasicVariableDeclaration getUltimateTargetDeclaration () {
      BasicVariableDeclaration ultimateTarget = targetDeclaration;

      while (   ultimateTarget != null
             && ultimateTarget.isPrivateVariable ()) {
         /* No protection for infinite loops! */
         ultimateTarget = ((PrivateScriptVariableDeclaration) ultimateTarget)
                                                   .targetDeclaration;
      }
      return ultimateTarget;
      // ultimateTarget == null || !ultimateTarget.isPrivateVariable
   }

   public boolean isPrivateVariable () {return true;}

   public String getConstructName () {
      if (targetDeclaration != null)
         return targetDeclaration.getConstructName ();
      else
         return "private variable";
   }

   public String getPresentation () {return "private " + getName ();}

   public LocalVariable makeTarget(CompilerEnvironment env) {
       ScriptPrivateVariable result = new ScriptPrivateVariable();
       result.source   = this;
       result.name     = name;
       result.dataType1= targetDeclaration.dataType();
       result.owner    = ((RefinementDeclaration)targetDeclaration.owner).target;
       target          = result;
       return result;
   }
}

