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


   /* --------------------------- Import Statement --------------------- */

class ImportStatement extends LanguageConstruct {
   public boolean importOnDemand;
   public boolean importStatic;
   public String classNameForStaticImport;
   PackageStatement packagePart = new PackageStatement();

   PackageStatement getPackagePart() {return packagePart;}
   public String getPresentationName () {return "import "+packagePart.getName()+'.'+name;} //quick hack...

   public void outSource      (PreprocessorOutputStream stream) {
      setTargetStartPosition (stream.position);
      outToken (stream, ImportToken);
      outSpace (stream);
      if (importStatic)
      {
    	  outToken (stream, ImportToken);
    	  outSpace (stream);
      }
      if (packagePart.getName().length() != 0)
      {
        packagePart.outName  (stream);
        outToken (stream, PeriodToken);
      }
      outName  (stream);
      outToken (stream, SemicolonToken);
      setTargetEndPosition (stream.position);
      outLine  (stream);
   }
}

