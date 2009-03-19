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

import java.util.HashMap;

class LocalClassType extends LocalOrNestedClassType {

   String uniqueName;
   String nameForFile () {return parent.nameForFile()+'$'+uniqueName;}

   // meant for use by AnonymousLocalClassType
   LocalClassType (ClassType parent, int sequenceNumber) {
     this (parent, sequenceNumber+"", sequenceNumber+"");
   }
   // meant for external use
   LocalClassType (ClassType parent, String clazz) {
     this (parent, parent.localClassSequenceNumber++, clazz);
   }
   private LocalClassType (ClassType parent, int sequenceNumber, String clazz) {
     this (parent, sequenceNumber+"$"+clazz, clazz);
   }
   LocalClassType (ClassType parent, String uniqueName, String clazz) {
     super (parent, parent.getNameWithDots()+'$'+uniqueName, clazz);
     this.uniqueName = uniqueName;
   }

   HashMap<LocalVariableOrParameter, MemberVariable> usedLocalVariablesAndParameters = new HashMap<LocalVariableOrParameter, MemberVariable>(); //LocalVariableOrParameter>>CopyOfLocalVariableOrParameter
   Variable copyLocalVariableOrParameter (LocalVariableOrParameter v) {
     MemberVariable result = (MemberVariable) usedLocalVariablesAndParameters.get(v);
     if (result==null) {
       addMemberVariable (result = new CopyOfLocalVariableOrParameter (v));
       usedLocalVariablesAndParameters.put (v, result);
     }
     return result;
   }
}

