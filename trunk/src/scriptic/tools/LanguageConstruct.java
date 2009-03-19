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
import scriptic.tools.lowlevel.InstructionOwner;



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                         LanguageConstruct                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

public class LanguageConstruct
  implements scriptic.tokens.JavaTokens, JavaParseTreeCodes,
             InstructionOwner, scriptic.tools.lowlevel.ClassFileConstants {
   public int     sourceStartPosition, sourceEndPosition;
   public int     nameStartPosition, nameEndPosition;
 //public int     targetStartPosition, targetEndPosition;
   public String  name;
 //public static final String lineSeparator = System.getProperty ("line.separator", "\r\n");

   public static int instances;

   public int     sourceStartPosition() {return sourceStartPosition;}
   public int       sourceEndPosition() {return   sourceEndPosition;}
   public int       nameStartPosition() {return   nameStartPosition;}
   public int         nameEndPosition() {return     nameEndPosition;}
   public int                    slot() {return                  -1;}

   public void setTargetStartPosition(int i) {}   //  this.targetStartPosition = i;
   public void setTargetEndPosition  (int i) {}   //  this.targetEndPosition   = i;
   public int  getTargetStartPosition() {return 0;} // targetStartPosition;
   public int  getTargetEndPosition  () {return 0;} // targetEndPosition;

   /* --------------------------- Constructors ------------------------- */

   public LanguageConstruct () {instances++;}

   public LanguageConstruct (LanguageConstruct anotherConstruct) {
      this(); 
      this.sourceStartPosition  = anotherConstruct.sourceStartPosition;
      this.sourceEndPosition    = anotherConstruct.sourceEndPosition;
      this.nameStartPosition    = anotherConstruct.nameStartPosition;
      this.nameEndPosition      = anotherConstruct.nameEndPosition;
      this.name                 = anotherConstruct.name;
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isClass                     () {return false;}
   public boolean isInterface                 () {return false;}
   public boolean isVariableDeclaration       () {return false;}
   public boolean isInitializerBlock         () {return false;}
   public boolean isMethodDeclaration         () {return false;}
   public boolean isConstructorDeclaration    () {return false;}
   public boolean isAnyKindofScriptDeclaration() {return false;}
   public boolean isScriptDeclaration         () {return false;}
   public boolean isCommunicationDeclaration  () {return false;}
   public boolean isChannelDeclaration        () {return false;}
   public int     languageConstructCode       () {return -1;}
   public boolean hasCompoundName () {
      if (name == null) return false;
      else              return name.indexOf ('.') >= 0;
   }

   /* ---------------------------- Presentation ------------------------ */

   public String getName        () {if (name == null) return ""; return name;}
   public String getNameWithDots() {return getName();}
   public String getDescription () {return "";}

   public String getConstructName    () {return new String ();}
   public String getPresentationName () {
       String result = getClass().getName();
       int i = result.lastIndexOf('.');
       if (i >= 0) return result.substring(i+1);
       return result;
   }
   
   public String getPresentation () {
      return getPresentationName() + " " + getNameWithDots();
   }
   
   public String getSource (Scanner s) {
      if (sourceStartPosition >= sourceEndPosition)
         return   getPresentation () 
                + " -- source positions not set"; 

      return s.getSource (sourceStartPosition, sourceEndPosition);
   }

   public String getHeaderSource (Scanner s) {return getSource (s);}

   public static String getModeString(RefinementCallExpression e) {
      return e.   isStaticMode()? "static"
           : e.  isSpecialMode()? "special"
           : e.    isSuperMode()? "super"
           : e.  isVirtualMode()? "virtual"
           : e.isInterfaceMode()? "interface"
           :                     "**UNKNOWN**";
   }

   /* ------------------------- Code generation ------------------------ */

   public void outNameList        (PreprocessorOutputStream stream,
		   						   ArrayList<? extends LanguageConstruct> elements,
                                   boolean firstParameter) {

      boolean firstTime = firstParameter;
      for (LanguageConstruct element: elements) {

         if (firstTime)
            firstTime = false;
         else {
            outToken (stream, CommaToken);
            outSpace (stream);
         }
         element.outName (stream);
      }
   }

   public void outDeclarationList (PreprocessorOutputStream stream,
		   						   ArrayList<? extends LanguageConstruct> elements,
                                   boolean firstParameter) {

      boolean firstTime = firstParameter;
      for (LanguageConstruct element: elements) {

         if (firstTime)
            firstTime = false;
         else {
            outToken (stream, CommaToken);
            outSpace (stream);
         }
         element.outDeclaration (stream);
      }
   }

   public void outSource      (PreprocessorOutputStream stream) {}
   public void outDeclaration (PreprocessorOutputStream stream) {}
   public void outHeader      (PreprocessorOutputStream stream) {}
   public void outName        (PreprocessorOutputStream stream) {outString (stream, getName ());}
   protected void outToken    (PreprocessorOutputStream stream, int token) {
      stream.print (Scanner.tokenRepresentation (token));
   }

   protected void outString(PreprocessorOutputStream stream,String s) {stream.print (s);}
   protected void outSpace (PreprocessorOutputStream stream)          {stream.write((int)' ');}
   protected void outLine  (PreprocessorOutputStream stream)          {stream.println ();}
}

