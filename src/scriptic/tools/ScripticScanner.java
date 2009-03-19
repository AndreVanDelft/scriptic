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

import scriptic.tokens.ScripticRepresentations;



public class ScripticScanner extends JavaScanner 
   implements scriptic.tokens.ScripticTokens, ScripticRepresentations {



	   protected int scanSpecialSymbol () {
		      char ch = charBuffer [charPosition++];

		      if (ch == '{') {  /*  {:  {?  {??  {*  {.  {.. */
		         ch = charBuffer [charPosition++];
		         if (ch == ':') return returnToken (BraceColonOpenToken);
		         if (ch == '*') return returnToken (BraceAsteriskOpenToken);
		         if (ch == '?') return returnToken (BraceQuestionOpenToken);
		         if (ch == '.') {
		             if (charBuffer [charPosition]=='.') {
		                 charPosition++;
			             if (charBuffer [charPosition]=='.') {
			                 charPosition++;
			                 return returnToken (BraceEllipsis3OpenToken);
			             }
		                 return returnToken (BraceEllipsisOpenToken);
		             }
		             return returnToken (BracePeriodOpenToken);
		         }
		         charPosition -= 2;
		         return super.scanSpecialSymbol ();
		      }

		      if (ch == '*') {  /*  *}  *)  *>  */
		         ch = charBuffer [charPosition++];
		         if (ch == '}') return returnToken (BraceAsteriskCloseToken);
		            
		         charPosition -= 2;
		         return super.scanSpecialSymbol ();
		      }

		      if (ch == '@') {
			         return returnToken(AtSignToken);
			  }

		      if (ch == ':') {  /*  :}   */
		          ch = charBuffer [charPosition++];
		          if (ch == '}')
		             return returnToken (BraceColonCloseToken);
		             
		          charPosition -= 2;
		          return super.scanSpecialSymbol ();
		       }

		      if (ch == '?') {  /*  ?}  ???  */
		          ch = charBuffer [charPosition++];
		          if (ch == '}')
			             return returnToken (BraceQuestionCloseToken);
		          if (ch == '?')
		          {
		             return returnToken (DoubleQuestionToken);
		          }  
		          charPosition -= 2;
		          return super.scanSpecialSymbol ();
		      }
		      
		      if (ch == '!') {  /*  :}   */
		          ch = charBuffer [charPosition++];
		          if (ch == '!')
			             return returnToken (DoubleExclamationToken);
		          charPosition -= 2;
		          return super.scanSpecialSymbol ();
		       }

		      if (ch == '.') {  /*  .}  ..  ..}  */
		         ch = charBuffer [charPosition++];
		         if (ch == '.')
		         {
		             if (charBuffer [charPosition] == '}')
		             {
		                 charPosition++;
		                 return returnToken (BraceEllipsisCloseToken);
		             }
		             if (charBuffer [charPosition] == '.')
		             {
		                 charPosition++;
			             if (charBuffer [charPosition] == '}')
			             {
			                 charPosition++;
			                 return returnToken (BraceEllipsis3CloseToken);
			             }
		                 return returnToken (Ellipsis3Token);
		             }
		             return returnToken (EllipsisToken);
		         }
		         if (ch == '}')
		         {
		             return returnToken (BracePeriodCloseToken);
		         }
		            
		         charPosition -= 2;
		         return super.scanSpecialSymbol ();
		      }

		      charPosition--;
		      return super.scanSpecialSymbol ();
		   }

}

