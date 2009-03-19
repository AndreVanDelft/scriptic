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
import java.util.Iterator;

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                            ModifierList                         */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class ModifierList implements ModifierFlags, scriptic.tokens.JavaTokens {
   public int    sourceStartPosition;
   public ArrayList<Integer>         tokens    = new ArrayList<Integer> ();
   public ArrayList<Integer>         flags     = new ArrayList<Integer> ();
   public ArrayList<ScannerPosition> positions = new ArrayList<ScannerPosition>();
   public short  modifiers = 0;


   /* Check if the modifiers are allowed for the given construct.
      NOTE: THIS MAY INCOMPLETE; it may not yet check for all possible
            mutually exclusive modifiers (e.g. "public private int i;") */

   public boolean checkModifiers (ModifierLanguageConstruct languageConstruct,
                                  Parser parser) {
      
      int    allowedFlags   = languageConstruct.getAllowedModifiers ();
      String constructName  = languageConstruct.getConstructName ();

      Iterator<Integer>         t =    tokens.iterator();
      Iterator<Integer>         f =     flags.iterator ();
      Iterator<ScannerPosition> p = positions.iterator ();
      while (t.hasNext()) {
         int token = t.next();
         int flag  = f.next();
         ScannerPosition position  = p.next();

         if ((flag & allowedFlags) == 0)
            parser.parserError (2, (new StringBuffer ())
                                       .append (constructName)
                                       .append (" cannot be ")
                                       .append (parser.scanner.quotedTokenRepresentation (token))
                                       .toString (),
                                position);
      }
      do { // bogus loop
        if (hasConflict (languageConstruct, parser,   PrivateFlag,   PrivateToken,      PublicFlag,      PublicToken)) break;
        if (hasConflict (languageConstruct, parser,   PrivateFlag,   PrivateToken,   ProtectedFlag,   ProtectedToken)) break;
        if (hasConflict (languageConstruct, parser, ProtectedFlag, ProtectedToken,      PublicFlag,      PublicToken)) break;
        if (hasConflict (languageConstruct, parser,  VolatileFlag,  VolatileToken,       FinalFlag,       FinalToken)) break;
        if (hasConflict (languageConstruct, parser,  AbstractFlag,  AbstractToken,       FinalFlag,       FinalToken)) break;
        if (hasConflict (languageConstruct, parser,  AbstractFlag,  AbstractToken,      NativeFlag,      NativeToken)) break;
        if (hasConflict (languageConstruct, parser,  AbstractFlag,  AbstractToken,SynchronizedFlag,SynchronizedToken)) break;
        if (hasConflict (languageConstruct, parser,  AbstractFlag,  AbstractToken,      StaticFlag,      StaticToken)) break;
        if (hasConflict (languageConstruct, parser,  AbstractFlag,  AbstractToken,     PrivateFlag,     PrivateToken)) break;
      } while (false);
      return true;  /* Can quite safely continue with this kind of error */
   }

   boolean hasConflict (ModifierLanguageConstruct languageConstruct,
                        Parser parser,
                        int flag1, int flag1Token,
                        int flag2, int flag2Token) {
       if ((modifiers & flag1) != 0
       &&  (modifiers & flag2) != 0) {
            Scanner scanner = null;
            if (languageConstruct instanceof TypeDeclaration)
                    scanner = ((TypeDeclaration)languageConstruct).compilationUnit.scanner;
            else if (languageConstruct instanceof RefinementDeclaration) 
                    scanner = ((RefinementDeclaration)languageConstruct).typeDeclaration.compilationUnit.scanner;
            parser.parserError (2, (new StringBuffer ()
                                       .append (languageConstruct.name)
                                       .append (" cannot be both" + Scanner.tokenRepresentation(flag1Token))
                                       .append (" and"            + Scanner.tokenRepresentation(flag1Token)))
                                       .toString (),
                                new ScannerPosition (scanner,
                                                     languageConstruct.nameStartPosition,
                                                     languageConstruct.nameEndPosition ));
            return true;
       }
       return false;
   }
}

