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
   /*                   CommunicationDeclaration                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class CommunicationDeclaration extends BasicScriptDeclaration {

   public ArrayList<CommunicationPartnerDeclaration> partners = new ArrayList<CommunicationPartnerDeclaration>();
   //public CommunicationScript   target;
   //public Field targetField() {return target;}

   /* --------------------------- Constructors ------------------------- */

   public CommunicationDeclaration () { }

   public CommunicationDeclaration (BasicScriptDeclaration scriptDeclaration) { 
      super (scriptDeclaration);

      /* The parameter lists are declared in the partners.
         Remove the info from here. */
      this.parameterList = null;
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isCommunicationDeclaration () {return true;}
   public int     languageConstructCode      () {return CommunicationDeclarationCode;}
//   public int     getAllowedModifiers        () {return AllowedCommunicationModifiers;}

   /* ---------------------------- Presentation ------------------------ */

   public String getConstructName    () {return "A communication";}
   public String getPresentationName () {return "communication";}
   public String getPartnerNames     () {return getPartnerNamesSeparated (',');}

   public String getNameSignature () {
      StringBuffer presentation = new StringBuffer ();
      boolean firstTime = true;

      for (BasicScriptDeclaration bsd: partners) {
         CommunicationPartnerDeclaration partner 
               = (CommunicationPartnerDeclaration) bsd;

         if (firstTime)
            firstTime = false;
         else
            presentation.append (",");
         presentation.append (partner.getNameSignature());
      }
      return presentation.toString ();
   }


   public String getPresentation () {
      StringBuffer presentation = new StringBuffer ();
      presentation.append (getPresentationName ());
      presentation.append (' ');

      presentation.append (getNameSignature ());
      return presentation.toString ();
   }


 
   public ArrayList<MethodParameterDeclaration> getParameters () {
	   ArrayList<MethodParameterDeclaration> result = new ArrayList<MethodParameterDeclaration>();

      for (BasicScriptDeclaration bsd: partners) {
          CommunicationPartnerDeclaration partner 
                = (CommunicationPartnerDeclaration) bsd;
          result.addAll (partner.getParameters ());
      }
      return result;
   }

   /** return the parameter index of a given parameter,
    *  as applicable for isForced etc.
    *  Example: for a[10 i](int p) b[](int q, int r)
    *       the result for parameters p, q and r would be: 0, 1 and 2
    */
   int paramIndexOf (NormalOrOldScriptParameter p) {
      int result = 0;
      for (BasicScriptDeclaration bsd: partners) {
          CommunicationPartnerDeclaration partner 
                = (CommunicationPartnerDeclaration) bsd;
         ArrayList<MethodParameterDeclaration> partnerParameters = partner.getParameters ();
         int i = partnerParameters.indexOf (p.source);
         if (i>=0) return result+i;
         result += partnerParameters.size();
      }
      throw new RuntimeException ("Internal Error: parameter "+p.name+" not found");
   }

   /* ------------------------- Code generation ------------------------ */

   public String getPartnerNamesSeparated (char separator) {

      StringBuffer partnerNames = new StringBuffer ();
      boolean firstTime = true;
      for (BasicScriptDeclaration bsd: partners) {
          CommunicationPartnerDeclaration partner 
                = (CommunicationPartnerDeclaration) bsd;

         if (firstTime)
            firstTime = false;
         else
            partnerNames.append (separator);
         partnerNames.append (partner.getName());
      }
      return partnerNames.toString ();
   }

   public String getScriptStringName () {return getPartnerNamesSeparated (',');} // "a,b,c"

   /* "a_b_c" or "a_b_c_nnn" */
   public String getTemplateBaseName () {
      //String className = typeDeclaration.getName ().replace ('.', '_');
      String baseName  = getPartnerNamesSeparated ('_');

      if (getSequenceNumber () >= 0)
           return baseName + "_" + getSequenceNumberString();
      else return baseName;
   }

   /* "a_b_c_comm_template"    */
   /* "a_b_c_script_code"      */
   /* "a_b_c_comm_local_000"   */
   public String getTemplateName  () {return getTemplateBaseName() + "_comm_template";}
   public String getCodeProcName  () {return getTemplateBaseName() + "_script_code";}
   public String getLocalDataClassName(BasicVariableDeclaration dataDeclaration) {
      StringBuffer result = new StringBuffer ();
      result
         .append (getTemplateBaseName ())
         .append ("_comm_local_")
         .append (getZeroPaddedString (dataDeclaration.declarationIndex));
      return result.toString ();
   }
   public ArrayList<CommunicationPartnerDeclaration> getPartners () {return partners;}
}

