/* This file is part of the Scriptic Virtual Machine
 * Copyright (C) 2009 Andre van Delft
 *
 * The Scriptic Virtual Machine is free software: 
 * you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.tokens;

public interface ScripticTokens extends JavaTokens {

   /* Extra keywords */
   public final static int ScriptToken                   =  FirstKeywordToken + 58;
   public final static int ScriptsToken                  =  FirstKeywordToken + 59;


   /* Special symbols */
   /* {:  :}  {?  ?}  {. .}  {.. ..} {... ...}  {*  *}  (*  *)  <*  *>  ..  ??  !!  */
   public final static int FirstScripticToken            = 150; 
   
   public final static int BraceColonOpenToken           = FirstScripticToken + 20;
   public final static int BraceColonCloseToken          = FirstScripticToken + 21;
   public final static int BraceQuestionOpenToken        = FirstScripticToken + 22;
   public final static int BraceQuestionCloseToken       = FirstScripticToken + 23;
   public final static int BracePeriodOpenToken          = FirstScripticToken + 24;
   public final static int BracePeriodCloseToken         = FirstScripticToken + 25;
   public final static int BraceEllipsisOpenToken        = FirstScripticToken + 26;
   public final static int BraceEllipsisCloseToken       = FirstScripticToken + 27;
   public final static int BraceEllipsis3OpenToken        = FirstScripticToken + 28;
   public final static int BraceEllipsis3CloseToken       = FirstScripticToken + 29;
 
   public final static int BraceAsteriskOpenToken        = FirstScripticToken + 30;
   public final static int BraceAsteriskCloseToken       = FirstScripticToken + 31;

   public final static int EllipsisToken                 = FirstScripticToken + 36;
   public final static int Ellipsis3Token                = FirstScripticToken + 37;
   public final static int DoubleQuestionToken           = FirstScripticToken + 38;
   public final static int DoubleExclamationToken        = FirstScripticToken + 39;
   public final static int AtSignToken                   = FirstScripticToken + 40;

}

