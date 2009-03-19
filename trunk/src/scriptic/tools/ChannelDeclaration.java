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
   /*                        ChannelDeclaration                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class ChannelDeclaration extends BasicScriptDeclaration {

   public int formalIndexCount;
   public boolean isSendChannel;
   public boolean isReceiveChannel;
   public boolean isBidirectionalChannel;
   //public ChannelScript  target;
   //public Field targetField() {return target;}

   public ScriptTemplateVariable  partnerTemplateVariable; 
   public ScriptTemplateGetMethod partnerTemplateGetMethod; 

   /* --------------------------- Constructors ------------------------- */

   public ChannelDeclaration () { }
   public ChannelDeclaration (BasicScriptDeclaration scriptDeclaration) { 
      super (scriptDeclaration);
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isChannelDeclaration () {return true;}
   public int    languageConstructCode () {return ChannelDeclarationCode;}
//   public int      getAllowedModifiers () {return AllowedChannelModifiers;}
   public int          getPartnerIndex () {return 0;}

   /* ---------------------------- Presentation ------------------------ */

   public String getConstructName      () {return "A channel";}
   public String getPresentationName   () {return "channel";}
   public String getDirectionString    () {
      if (isSendChannel)           return ("<<");
      if (isReceiveChannel)        return (">>");
      if (isBidirectionalChannel)  return ("<<>>");
      return new String ();
   }

   public String getPresentation () {
      StringBuffer presentation = new StringBuffer ();
      presentation.append (getPresentationName ());
      presentation.append (' ');
      presentation.append (getName ());

      presentation.append (getDirectionString ());

      if (   parameterList != null 
          && parameterList.parameters.size() > 0)
         presentation.append (parameterList.getPresentation ());
      return presentation.toString ();
   }

   /* ------------------------- Code generation ------------------------ */

   public String getScriptStringName () {
      return   getName () 
             + getDirectionString ()
             + getScriptStringFormalParameters ();
   }

   public String getGeneratedDirectionName () {
      if (isSendChannel)           return "_lchan";
      if (isReceiveChannel)        return "_rchan";
      if (isBidirectionalChannel)  return "_chan";
      return new String ();
   }

   public String getTemplateName () {
      return   getNameSequenceNumber () 
             + getGeneratedDirectionName () 
             + "_template";
   }

   public String getCodeMethodName() {
		return getPackageClassScriptNameSequenceNumber()
             + getGeneratedDirectionName ()
             +"_"+"code";
  }
   public void setFormalIndexCount(int count) {
		formalIndexCount = count;	
	  }
   public int getFormalIndexCount() {
		return formalIndexCount;	
   }
}

