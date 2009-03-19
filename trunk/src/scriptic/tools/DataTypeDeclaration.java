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


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        DataTypeDeclaration                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


class DataTypeDeclaration extends LanguageConstructWithNameComponents {
   public  int        noOfArrayDimensions;
   public  int        primitiveTypeToken;
   public  String     dimensionSignature;
   public  DataType   dataType;
   public  DimensionDeclaration dimensionDeclaration;

   public  boolean      isVoidType() {return primitiveTypeToken == VoidToken;}
   public  boolean      isArray   () {return noOfArrayDimensions > 0;}
   public  boolean     isClassType() {return noOfArrayDimensions ==0 && baseTypeIsClass    ();}
   public  boolean isPrimitiveType() {return noOfArrayDimensions ==0 && baseTypeIsPrimitive();}
   public  boolean baseTypeIsClass    () {return !baseTypeIsPrimitive();}
   public  boolean baseTypeIsPrimitive() {
      switch (primitiveTypeToken) {
         case    VoidToken:
         case BooleanToken:
         case    CharToken:
         case    ByteToken:
         case   ShortToken:
         case     IntToken:
         case    LongToken:
         case   FloatToken:
         case  DoubleToken: return true;
      }
      return false;
   }

   // like in importStatements:
   PackageStatement packagePart;

   PackageStatement getPackagePart() {
      if (packagePart==null) {
        packagePart = new PackageStatement();
        packagePart.nameStartPosition = nameStartPosition;
        packagePart.nameEndPosition   = nameEndPosition;

        StringBuffer b = new StringBuffer();
        for (int i=0; i<nameComponents.size()-1; i++) {
          if (i>0) b.append('.');
          String s = nameComponents.get(i);
          b.append(s);
          packagePart.addNameComponent(s);
        }
        packagePart.name = b.toString();
      }
      return packagePart;
   }

   /* --------------------------- Constructors ------------------------- */

   public DataTypeDeclaration () {}
   public DataTypeDeclaration (int primitiveTypeToken) {
      this.primitiveTypeToken = primitiveTypeToken;
   }

   public DataTypeDeclaration (DataTypeDeclaration anotherDeclaration) { 
      super ((LanguageConstruct) anotherDeclaration);
      this.noOfArrayDimensions = anotherDeclaration.noOfArrayDimensions;
      this.primitiveTypeToken  = anotherDeclaration.primitiveTypeToken;
      this.dimensionSignature  = anotherDeclaration.dimensionSignature;
      this.dataType            = anotherDeclaration.dataType;
      this.name                = anotherDeclaration.name;
      this.nameComponents      = anotherDeclaration.nameComponents;
   }
 
   /* ---------------------------- Presentation ------------------------ */

   public String getPresentation () {
      StringBuffer presentation = new StringBuffer ();
      presentation.append (getName ());
      if (dimensionSignature != null)
         presentation.append ('*').append (dimensionSignature);

      if (noOfArrayDimensions > 0) {
        presentation.append (' ');
        for (int i = 0; i < noOfArrayDimensions; i++)
           presentation.append ("[ ]");
      }
      return presentation.toString ();
   }

   public String getDescription () {
      if (dataType==null) return getPresentation();
      return dataType.getDescription();
   }

   /* ------------------------- Code generation ------------------------ */

   public void outDeclaration (PreprocessorOutputStream stream) {
      setTargetStartPosition (stream.position);
      outName  (stream);

      if (noOfArrayDimensions > 0) {
         outSpace (stream);
         for (int i = 0; i < noOfArrayDimensions; i++) {
            outToken (stream, BracketOpenToken);
            outSpace (stream);
            outToken (stream, BracketCloseToken);
         }
      }
      setTargetEndPosition (stream.position);
   }
}


