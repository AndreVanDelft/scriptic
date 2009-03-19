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

class NestedScriptExpression extends ScriptExpression
                          implements scriptic.tokens.ScripticTokens {
   /* This class represents nested scripts "(...)", 
      launched scripts "<....>", threaded scripts "(*...*)"
      and thread-launched scripts "<*....*>". The distinction
      is made with the starting delimiter (which will also be
      the name). The ending delimiter is also stored to ease
      Script String generation. */

   public ScriptExpression subExpression;
   public int startingDelimiter;
   public int endingDelimiter;
   public int languageConstructCode () {return   NestedScriptExpressionCode;}
}

