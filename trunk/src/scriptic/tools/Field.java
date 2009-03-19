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

import java.io.DataInputStream;
import java.io.IOException;

import scriptic.tools.lowlevel.ByteCodingException;

////////////////////////////////////////////////////////////////////////////
//
//                             Field
//
////////////////////////////////////////////////////////////////////////////

abstract class Field extends Modifiers implements scriptic.tokens.JavaTokens {
      // refinement, member variable, parameter, local variable...

   public String                  name;
   public String                  signature;
   public String                  dimensionSignature;
   public abstract LanguageConstruct source();
   public abstract String getConstructName ();
   public ClassType                ownerClass(CompilerEnvironment env) throws ByteCodingException, IOException {return null;}

   public boolean isConstructor() {return false;}
   public DataType getDataType (CompilerEnvironment env) throws ByteCodingException, IOException {return null;} // meaningful for variables   only

   /* ------------------------- Predicates ------------------------ */

   /** answer whether this is accessible for the given class.
    *  Assume this is a member variable or a refinement
    */
   public boolean isAccessibleFor (CompilerEnvironment env, ClassType clazz, boolean isForAllocation)
              throws ByteCodingException, IOException {
       // $6.6.1 Determining Accessibility
       // $6.6.2 Details on protected Access; same package: already handled
       ClassType ownerClass = ownerClass(env);

try {
       if (ownerClass         ==           clazz ) return  true;
       if (            isPrivate               ()) return false;
       if (!ownerClass.isAccessibleFor(env,clazz)) return false;
       if (            isPublic                ()) return  true;
       if (    ownerClass.packageNameWithSlashes
           .equals (clazz.packageNameWithSlashes)) return  true;
       if (!           isProtected             ()) return false;
       if (            isForAllocation           ) return false; // $6.6.2

       return clazz.isSubtypeOf (env, ownerClass );
}
catch (Exception e) 
{
  return false;
}
   }

   /* ------------------------- Presentation ------------------------ */

   public String signature                () {return signature==null? "": signature;}
   public String getPresentation          () {return getSignaturePresentation() + name;}
   public String getSignaturePresentation () {return getSignaturePresentation (signature);}
   public String getSignaturePresentation (String formalSignature) {
      if (   formalSignature == null
          || formalSignature.length() == 0)
         return new String ();

      int position = 0;
      int length   = formalSignature.length();
      int noOfArrayBrackets = 0;
      while (position < length - 1
         &&  formalSignature.charAt(position) == '[') {
         position++;
         noOfArrayBrackets++;

         while (position < length - 1
            &&  Character.isDigit(formalSignature.charAt(position)))
            position++;
      }

      String signatureName = null;
      char   signatureChar = formalSignature.charAt(position);
      position++;
      switch (signatureChar) {
         case 'B' : signatureName = "byte";    break;
         case 'C' : signatureName = "char";    break;
         case 'D' : signatureName = "double";  break;
         case 'F' : signatureName = "float";   break;
         case 'I' : signatureName = "int";     break;
         case 'J' : signatureName = "long";    break;
         case 'S' : signatureName = "short";   break;
         case 'Z' : signatureName = "boolean"; break;
         case 'V' : signatureName = "void";    break;
         case 'L' :
            int startPos = position;
            while (position < length
               &&  formalSignature.charAt(position) != ';')
               position++;

            signatureName = formalSignature.substring (startPos, position);
            break;
         default:
            signatureName = formalSignature.substring (position - 1);
      }

      signatureName += " ";
      for (int i = 0; i < noOfArrayBrackets; i++) {
         signatureName += "[ ]";
      }
      return signatureName;
   }

   public String getMnemonicCode () {return "";}
   public String getDescription () {
      return new StringBuffer ()
               .append (getPresentation ())
               .append ("   :   ")
               .append (signature ())
               .append (lineSeparator)
               .append (getMnemonicCode())
               .append (lineSeparator)
               .append (getAttributeDescription ())
               .toString ();
   }

   public String getAttributeDescription () {return "";}

   /* -------------------------   I/O   ------------------------ */

   public void readFromStream (DataInputStream stream) throws IOException {}

   /* ------------------------- Code generation ------------------------ */

}

