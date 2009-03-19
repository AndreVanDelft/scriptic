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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                           CompilationUnit                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

class CompilationUnit extends LanguageConstruct {

   public Scanner          scanner;
   public PackageStatement packageStatement;
   public ArrayList<ImportStatement>          importStatements            = new ArrayList<ImportStatement>();
   public ArrayList<TypeDeclarationOrComment> typeDeclarationsAndComments = new ArrayList<TypeDeclarationOrComment>();
   public HashMap<Object, Object>          relevantClasses  = new HashMap<Object, Object>(); // "a.b.c" -> Vector["a","b","c"
                                                               // nameExpression -> nameExpression
   public HashMap<String, ClassType>       knownClassTypes  = new HashMap<String, ClassType>(); // class name -> ClassType's
   public HashMap<String, ImportStatement> importedStaticMembers = new HashMap<String, ImportStatement>(); // feature name -> import list entry
   public HashMap<String, ImportStatement> importedClasses  = new HashMap<String, ImportStatement>(); // class name -> import list entry
   public File             sourceFile;

   void freeCompiledMember (TypeDeclaration t) {
      typeDeclarationsAndComments.remove(this);
      if (typeDeclarationsAndComments.isEmpty()) {
          scanner                     = null;
          packageStatement            = null;
          importStatements            = null;
          typeDeclarationsAndComments = null;
          knownClassTypes             = null;
          importedClasses             = null;
          importedStaticMembers       = null;
          sourceFile                  = null;
      }
   }

  /////////////////////////////////////////////////////////////////////////
   /* these are for Scriptic, to cope with scripts like
    *       live  = Customer customer = new Customer (): ...
    */
   protected static HashMap<String, Integer> staticWellknownTypes = new HashMap<String, Integer>();
   protected        HashMap<String, Integer>       wellknownTypes = new HashMap<String, Integer>();

   static {
      String [ ] javaLangTypeNames = { "String",  "StringBuffer",
                                       "Boolean", "Character",
                                       "Double",  "Float",
                                       "Integer", "Long" };

      for (int i = 0; i < javaLangTypeNames.length; i++ ) {
         staticWellknownTypes.put (javaLangTypeNames [i],                i);
         staticWellknownTypes.put ("java.lang." + javaLangTypeNames [i], i);
      }
   }

   /* For the benefit of parsing local script data,
      e.g. "script a = String s : ...", collect a set (actually Hashtable)
      of well-known type names that should be recognized as such
      by the local data parsing routines.

      Currently, the following well-known type names are recognized:
         -  String, StringBuffer, java.lang.String, java.lang.StringBuffer
         -  The arithmetic wrappers from java.lang 
            (Boolean, Character, Double, Float, Integer, Long)
            in both  plain and qualified forms (NOTE: there is no
            Byte or Short)
         -  Any type names from "import" declarations,
            in plain and qualified forms (e.g. Vector and java.util.Vector),
            EXCEPT from import-on-demand (e.g. java.util.*) declarations
         -  The name of the current class being parsed;
            that is, if the function isWellknownType is called at the type declaration
   */
   public boolean isWellknownType (String s) {
       if (staticWellknownTypes.containsKey (s)) return true;
       if (     importedClasses.containsKey (s)) return true;
       return false;
   }

  /////////////////////////////////////////////////////////////////////////

final boolean doTrace=false;
   public void addPossibleRelevantClass (Object relevantClazz) {
      if (relevantClazz instanceof DataTypeDeclaration) {

          DataTypeDeclaration d = (DataTypeDeclaration) relevantClazz;
if (true||doTrace) {
//System.out.println ("addPossibleRelevantClass: DataTypeDeclaration "+ d.getPresentation());
if (d.name.startsWith("com.sun")) 
	new Exception ("FOUND com.sun").printStackTrace();
}
          if (d.baseTypeIsPrimitive()) {
              return;
          }
          if (!relevantClasses.containsKey (d.name)) {
               relevantClasses.put (d.name,relevantClazz);
          }
      }
      else if (relevantClazz instanceof JavaExpression) {
          JavaExpression j = (JavaExpression) relevantClazz;
if (doTrace) System.out.println ("addPossibleRelevantClass: JavaExpression "+ j.getPresentation());

          if (j.isQualifiedName()) {
              if (!relevantClasses.containsKey (j.qualifiedName())) {
                   relevantClasses.put (j.qualifiedName(),relevantClazz);
              }
          }
      }
      else {
if (doTrace) System.out.println ("addPossibleRelevantClass: "+ relevantClazz);
          relevantClasses.put (relevantClazz, relevantClazz);
      }
   }
   public void addImportStatement (ClassesEnvironment classesEnvironment, ImportStatement importStatement) {
       importStatements.add (importStatement);
       if (!importStatement.importOnDemand)
       {
    	   if (importStatement.importStatic) {
               if (importedStaticMembers.containsKey(importStatement.name)) {
                   classesEnvironment.parserError (2, "Multiple static imports for member "+importStatement.name,
                                                     this,
                                                     importStatement.nameStartPosition,
                                                     importStatement.nameEndPosition);
               } else { 
            	   importedStaticMembers.put(importStatement.name, importStatement);
               }
    	   }
	       else
	       {
	           if (importedClasses.containsKey(importStatement.name)) {
	               classesEnvironment.parserError (2, "Multiple imports for class "+importStatement.name,
	                                                 this,
	                                                 importStatement.nameStartPosition,
	                                                 importStatement.nameEndPosition);
	           } else { 
	               importedClasses.put(importStatement.name            , importStatement);
	           }
	       }      
       }
   }

   /* --------------------------- Predicates --------------------------- */

   public boolean hasPackageStatement () {return packageStatement  != null;}
   public boolean hasImportStatements () {return importStatements.size()>0;}

   public String getPackageNameWithDots    () {return packageStatement==null?"":packageStatement.name;}
   public String getPackageNameWithSlashes () {return packageStatement==null?"":packageStatement.name.replace('.','/');}

   /* ---------------------------- Presentation ------------------------ */

   public String getPresentationName () {return "Compilation Unit";}
   public String   getSourceFileName () {return sourceFile==null?"":sourceFile.getName();}
   public String   getSourceFileDirectory () {
     if (sourceFile==null) return "";
     String result = sourceFile.getPath();
     int i;
     i=result.lastIndexOf(File.separatorChar); if(i>=0) return result.substring(0,i);
     i=result.lastIndexOf(':');                if(i>=0) return result.substring(0,i);
     return "";
   }
}

