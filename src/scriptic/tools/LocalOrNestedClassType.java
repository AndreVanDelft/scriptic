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

import java.io.*;

abstract class LocalOrNestedClassType extends ClassType {
   //ConfiningJavaStatement confiningJavaStatement;
   /************
     the constructor may get extra parameters:
       this$n - if the parent is used (say always if not static)
       encompassing locals - used here
       in anonymous new expressions

     These are normally required to be known in compilation pass 3.
   ***********/

   MemberVariable enclosingInstance;

   ClassType parent;

   public ClassType parent() {return parent;}
   public int nestingLevel() {return parent.nestingLevel()+1;}

   public boolean needsReferenceToEnclosingInstance () {return enclosingInstance != null;}

   public void setNeedForParentReference () {
      if (!isStatic()
      &&  enclosingInstance == null) {
          enclosingInstance           = new MemberVariable();
          enclosingInstance.dataType1 = parent;
          enclosingInstance.owner     = this;
          enclosingInstance.name      = ("this$"+parent.className).intern();
          addMemberVariable (enclosingInstance);
      }
   }

   public File sourceFile() {return parent.sourceFile();}

   LocalOrNestedClassType (ClassType parent, String nameWithDots, String clazz) {
     super (nameWithDots, clazz);
     this.parent = parent;
     sourceFile  = parent.sourceFile;
     this.packageNameWithSlashes = parent.packageNameWithSlashes;
     parent.addInnerClass (this);
   }

   Variable copyLocalVariableOrParameter (LocalVariableOrParameter v) {return v;} // for LocalTypeDeclaration
}

