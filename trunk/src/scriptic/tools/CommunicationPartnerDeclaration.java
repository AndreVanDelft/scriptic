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


import java.io.IOException;
import java.util.ArrayList;

import scriptic.tools.lowlevel.SourceFileAttribute;

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*            CommunicationPartnerDeclaration                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class CommunicationPartnerDeclaration extends BasicScriptDeclaration {

   public ArrayList<CommunicationDeclaration> communications = new ArrayList<CommunicationDeclaration>();
	
   public int   languageConstructCode () {return CommunicationRequestCode;}
   public CommunicationPartnerDeclaration firstOccurrence = null;
   public int partnerIndex = 0;
   //public CommunicationPartnerScript  target;
   //public Field targetField() {return target;}

   /* --------------------------- Constructors ------------------------- */

   public CommunicationPartnerDeclaration () { }
   public CommunicationPartnerDeclaration (BasicScriptDeclaration scriptDeclaration) { 
      super (scriptDeclaration);
   }

   /* ----------------------------- Predicates ------------------------- */

   public int getSequenceNumber () {
      if (firstOccurrence == null)
           return sequenceNumber;
      else return firstOccurrence.getSequenceNumber ();
   }

   public int getPartnerIndex () {return partnerIndex;}

   /* ---------------------------- Presentation ------------------------ */

   public String getPresentationName () {return "communication partner";}
   public String getPresentation     () {
      StringBuffer presentation = new StringBuffer ();
      presentation.append (getPresentationName ());
      presentation.append (' ');
      presentation.append (getName ());

      if (   parameterList != null 
          && parameterList.parameters.size() > 0)
         presentation.append (parameterList.getPresentation());
      return presentation.toString ();
   }

   public ArrayList<MethodParameterDeclaration> getParameters () {
      if (parameterList==null) return new ArrayList<MethodParameterDeclaration>();
      return parameterList.parameters;
   }

   /* ------------------------- Code generation ------------------------ */

   public String getPartnerScriptStringName () {
      return "," + getName () + getScriptStringFormalParameters ();
   }

}

