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

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                RefinementDeclaration               */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

abstract class RefinementDeclaration extends FieldDeclaration {
   public DataTypeDeclaration returnTypeDeclaration;
   public ParameterList       parameterList;
   public int                 headerEndPosition;
   public ScannerPosition     bodyStartPosition;
   public int                 declarationIndex; // as sequence number in typeDeclaration
   public boolean             thisParameterIsUsed;
   public Method              target;
   public Field               targetField () {return target;}
   public Method              codeTarget  () {return target;} // code method for scripts
   public abstract Method     makeTarget  (CompilerEnvironment env);
   public ThrowsClause        throwsClause() {return null;}
   ArrayList<LocalTypeDeclaration> localTypeDeclarations = new ArrayList<LocalTypeDeclaration>();
   /* refinements with localTypeDeclarations won't have their bodies thrown away
    * for later reparsing; the localTypeDeclarations are needed in pass2 and pass3,
    * so we'll keep them.
    * Pass3 and pass3 will process these as well using the Vector
    */

   /* --------------------------- Constructors ------------------------- */

   public RefinementDeclaration () { }

   public RefinementDeclaration (FieldDeclaration declaration) { 
      super (declaration);
   }

   public RefinementDeclaration (RefinementDeclaration declaration) { 
      super (declaration);
      this.parameterList      = declaration.parameterList;
      this.headerEndPosition  = declaration.headerEndPosition;
   }

   public String getHeaderSource (Scanner s) {
      return s.getSource (sourceStartPosition, headerEndPosition);
   }

   public ArrayList<MethodParameterDeclaration> getParameters    () {
      if (parameterList == null)
           return new ArrayList<MethodParameterDeclaration>();
      else return parameterList.parameters;
   }

   /* ---------------------------- Presentation ------------------------ */

   public String getDescription () {
      return new StringBuffer ()
               .append ('[')
               .append (declarationIndex)
               .append ("] ")
               .append (super.getDescription())
               .toString ();
   }

}

