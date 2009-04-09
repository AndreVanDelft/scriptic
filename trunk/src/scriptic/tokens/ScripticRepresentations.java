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



public interface ScripticRepresentations extends JavaRepresentations {

   public final static String BraceColonOpenRepresentation            =  "{:";
   public final static String BraceColonCloseRepresentation           =  ":}";
   public final static String BraceQuestionOpenRepresentation         =  "{?";
   public final static String BraceQuestionCloseRepresentation        =  "?}";
   public final static String BracePeriodOpenRepresentation           =  "{.";
   public final static String BracePeriodCloseRepresentation          =  ".}";
   public final static String BraceEllipsisOpenRepresentation         =  "{..";
   public final static String BraceEllipsisCloseRepresentation        =  "..}";
   public final static String BraceEllipsis3OpenRepresentation        =  "{...";
   public final static String BraceEllipsis3CloseRepresentation       =  "...}";

   public final static String BraceAsteriskOpenRepresentation         =  "{*";
   public final static String BraceAsteriskCloseRepresentation        =  "*}";

   public final static String EllipsisRepresentation                  =  "..";
   public final static String Ellipsis3Representation                 =  "...";
   public final static String DoubleQuestionRepresentation            =  "??";
   public final static String DoubleExclamationRepresentation         =  "!!";
   public final static String AtSignRepresentation                    =  "@";
   public final static String HashRepresentation                      =  "#";

   /* Extra keywords */
   public final static String ScriptRepresentation                    = "script";
   public final static String ScriptsRepresentation                   = "scripts";

   public final static String REP_SendChannel                  = "script<<";
   public final static String REP_ReceiveChannel               = "script>>";
   public final static String REP_Channel	               = "script<<>>";
   public final static String REP_ScriptCall                   = "call ";
   public final static String REP_SendCall	               = "send ";
   public final static String REP_ReceiveCall                  = "receive ";
   public final static String REP_CommRequest                  = "commReq ";
   public final static String REP_ChanRequest                  = "chanReq ";
   public final static String REP_ZERO                         = "()";
   public final static String REP_ONE                          = "(-)";
   public final static String REP_NEUTRAL                      = "{:}";
   public final static String REP_SOME                         = "some";
   public final static String REP_CommRoot	               = "CommRoot";
   public final static String REP_Root	                       = "Root";
   public final static String REP_RootScriptCall               = "RootScriptCall";
   public final static String REP_Launched	               = "<>";

}

