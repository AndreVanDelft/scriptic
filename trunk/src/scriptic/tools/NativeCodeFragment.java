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

class NativeCodeFragment extends ScriptExpression {
   /* This class represents plain code fragments "{...}",
      native-code script call field accesses "{...}.scriptname",
      threaded code fragments "{*...*}",
      unsure code fragments "{?...?}" 
      and tiny code fragments "{:...:}" */

   public int languageConstructCode () {return NativeCodeFragmentCode;}
   public ArrayList<JavaStatement> statements = new ArrayList<JavaStatement>();
   public int startingDelimiter;
   public int endingDelimiter;

   public boolean isPlainCodeFragment   () {return startingDelimiter==BraceOpenToken;}
   public boolean isThreadedCodeFragment() {return startingDelimiter==BraceAsteriskOpenToken;}
   public JavaStatement lastStatement() {
     if (statements.size() > 0) {
        return statements.get(statements.size() - 1);
     }
     return null;
   }
   public boolean isEmpty() {return statements.size()==0;}

   public JavaStatement durationAssignment = null;
   public JavaStatement priorityAssignment = null;

   public String getPresentationName () {
      if (   durationAssignment == null
          && priorityAssignment == null)
         return super.getPresentationName ();

      StringBuffer presentation = new StringBuffer ();
      boolean firstTime         = true;
      presentation.append (super.getPresentationName ());
      presentation.append ("(");
      if (durationAssignment != null) {
         if (firstTime) firstTime = false; else presentation.append (", ");
         presentation.append ("sets duration");
      }
      if (priorityAssignment != null) {
         if (firstTime) firstTime = false; else presentation.append (", ");
         presentation.append ("sets priority");
      }
      presentation.append (")");
      return presentation.toString ();
   }
}

