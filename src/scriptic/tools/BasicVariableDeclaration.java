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
   /*                    BasicVariableDeclaration                    */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

abstract class BasicVariableDeclaration extends LanguageConstruct {
   public FieldDeclaration    owner;
   public DataTypeDeclaration dataTypeDeclaration;
   public int declarationIndex    = -1;
   public int scopeEndPosition;
   public abstract Field targetField();
   public TypeDeclaration typeDeclaration() {return owner.typeDeclaration;}
   public DataType dataType1;
   public DataType dataType() {
      if (dataType1==null) {
          dataType1 = dataTypeDeclaration.dataType.makeArray(extraArrayDimensions());
      }
      return dataType1;
   }
          int extraArrayDimensions() {return 0;}
   public boolean isPrivateVariable() {return false;}
   public boolean  isLocalVariable () {return false;}

   public boolean isFinal       () {return owner.isFinal       ();}

   public BasicVariableDeclaration getUltimateTargetDeclaration () {
      /* used for Scriptic PrivateScriptVariableDeclaration */
      return this;
   }

   public String getDescription () {
      StringBuffer result = new StringBuffer ()
               .append (getPresentation())
               .append (lineSeparator)
               .append (dataTypeDeclaration.getDescription());
      if (targetField() != null) {
         result.append (lineSeparator)
               .append ("target: ")
               .append (targetField().getDescription());
      }
      return result.toString ();
   }


   /* ------------------------- Code generation ------------------------ */

   public void outDeclaration (PreprocessorOutputStream stream) {
      setTargetStartPosition (stream.position);
      dataTypeDeclaration.outDeclaration (stream);
      outSpace (stream);
      outName  (stream);
      setTargetEndPosition (stream.position);
   }

}


