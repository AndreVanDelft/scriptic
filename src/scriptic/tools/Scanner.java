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
import static scriptic.tokens.Representations.*;

public abstract class Scanner extends BasicScanner {

   /* Public variables - Read only! */
   public int     token;
   public Object  tokenValue;
   public int     tokenStartPosition, tokenEndPosition;
   public int     previousTokenEndPosition;

   public ParserErrorHandler errorHandler;

   public void setTokenEndPosition(int pos) {
       previousTokenEndPosition = tokenEndPosition;
       tokenEndPosition = pos;
   }

   public void setErrorHandler (ParserErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
   }
   
   public abstract int next ();
  
   public static String tokenRepresentation (int token) {
      String result;
      if (   token < 0 
          || token >= TokenRepresentations.length
          || (result = TokenRepresentations [token]) == null)
         return new String ();
      return result;
   }
   
   
   public String quotedTokenRepresentation (int token) {
      String repr = tokenRepresentation (token);
      if (repr.length() <= 0) return repr;
      return "\"" + repr + "\"";
   }


   public String tokenName (int token) {
      String result;
      if (   token < 0 
          || token >= TokenNames.length
          || (result = TokenNames [token]) == null)
         return new String ();
      return result;
   }
   

   public ScannerPosition getPosition () {
      ScannerPosition scannerPosition    = new ScannerPosition (this);
      scannerPosition.token              = this.token;
      scannerPosition.tokenValue         = this.tokenValue;
      scannerPosition.tokenStartPosition = this.tokenStartPosition;
      scannerPosition.tokenEndPosition   = this.tokenEndPosition;

      scannerPosition.charPosition       = this.charPosition;
      scannerPosition.lineStartPosition  = this.lineStartPosition;
      scannerPosition.lineNumber         = this.lineNumber;
      return scannerPosition;
   }


   public void setPosition (ScannerPosition scannerPosition) {
      this.token              = scannerPosition.token;
      this.tokenValue         = scannerPosition.tokenValue;
      this.tokenStartPosition = scannerPosition.tokenStartPosition;
      this.tokenEndPosition   = scannerPosition.tokenEndPosition;
      this.previousTokenEndPosition = scannerPosition.tokenEndPosition; // not very good, but...
      this.charPosition       = scannerPosition.charPosition;
      this.lineStartPosition  = scannerPosition.lineStartPosition;
      this.lineNumber         = scannerPosition.lineNumber;
   }
}

