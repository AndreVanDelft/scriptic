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

import scriptic.tools.lowlevel.ClassFileConstants;

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Type (= class or interface) Declaration            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


public abstract class TypeDeclaration extends TypeDeclarationOrComment
                   implements ClassFileConstants, ModifierFlags {
   public  ClassType    target;
   public SuperclassDeclaration superclass;
   public ArrayList<ImplementsDeclaration> interfaces = new ArrayList<ImplementsDeclaration>();
   public ArrayList<FieldDeclaration> fieldDeclarations = new ArrayList<FieldDeclaration>();
   public boolean         hasScripts;

   public abstract TopLevelTypeDeclaration topLevelTypeDeclaration();
   public abstract ClassType makeNewClassType();

   public String toString() {return name;}

  public boolean isInDefaultPackage() {return compilationUnit.packageStatement != null;}
  public String getPresentationName() {return  modifiers.isInterface()?    "interface":   "class";}
  public String getConstructName   () {return  modifiers.isInterface()? "An interface": "A class";}
  public boolean isClass           () {return !modifiers.isInterface();}
  public boolean isInterface       () {return  modifiers.isInterface();}
  public boolean isTopLevelType    () {return !isStatic();}
  public abstract boolean isInsideMethod();

  public int languageConstructCode () {return TopLevelTypeDeclarationCode;}
  public int getAllowedModifiers () {
    return modifiers.isInterface()? AllowedInterfaceModifiers: AllowedClassModifiers;
  }
  public boolean isWellknownType (String s) {
    return s.equals (name)
        || compilationUnit.isWellknownType (s);
  }

  // free the memory occupied by type declaration, code etc.
  void freeCompiledMembers () {
    superclass        = null;
    interfaces        = null;
    fieldDeclarations = null;
    compilationUnit.freeCompiledMember (this);
  }

  public PackageStatement getPackageStatement() {return compilationUnit.packageStatement;}

   protected String          fullNameWithDots;
   public  String          fullNameWithDots() {
       if (fullNameWithDots==null) {
           if (compilationUnit.packageStatement != null) {
               fullNameWithDots = compilationUnit.packageStatement.name + '.' + name;
           }
           else {
               fullNameWithDots = name;
           }
       }
       return fullNameWithDots;
   }

   public String getDescription () {
      StringBuffer result = new StringBuffer ();
      result.append ( getShortDescription())
            .append (getFieldsDescription());
      if (target!=null)
        result.append (lineSeparator)
              .append (target.getDescription(null));
      return result.toString ();
   }

   public String getShortDescription () {
      StringBuffer result = new StringBuffer ();
      result
         .append (getModifierString())
         .append (getPresentation ());
      if (superclass != null)
         result
            .append (" extends ")
            .append (superclass.getName());
      result.append (lineSeparator);

      for (int i = 0; i < interfaces.size(); i++) {
         if (i==0) result.append ("      implements ");
         else      result.append (", ");
         result.append (interfaces.get(i).getName());
      }
      return result.toString ();
   }

   public String getFieldsDescription () {
      StringBuffer result = new StringBuffer ();
      for (int i = 0; i < fieldDeclarations.size(); i++) {
         if (i==0) result.append (lineSeparator)
                         .append ("Fields")        .append (lineSeparator)
                         .append ("==============");
         result.append (lineSeparator);
         FieldDeclaration f = fieldDeclarations.get(i);
         if (f instanceof MultiVariableDeclaration)
              result.append (((MultiVariableDeclaration)f).getDescription ());
         else result.append (f.getPresentation());
      }
      return result.toString ();
   }
   public TypeDeclaration getParentTypeDeclaration () {return null;}
}

