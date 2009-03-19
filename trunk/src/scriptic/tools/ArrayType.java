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
import java.util.HashMap;
import scriptic.tools.lowlevel.*;

class ArrayType extends DataType {
   private int      noDimensions;
   private NonArrayType baseType;
   public  DataType   accessType;
   public int    noOfArrayDimensions() {return noDimensions;}
   public boolean isReference       () {return true;}
   public boolean isArray           () {return true;}
   public boolean isResolved        () {return baseType.isResolved();}

   public ArrayType (DataType accessType) {
       this.accessType  = accessType;
       this.baseType    = accessType.baseType();
       noDimensions     = accessType.noOfArrayDimensions()+1;
       accessType.array = this;
   }
   public NonArrayType baseType() {return   baseType;}
   public DataType  accessArray() {return accessType;}
   public DataType  accessArray(int n) {
       DataType result = this;
       for (int i=0; i<n; i++) {
           if (!(result instanceof ArrayType)) break;
           result = ((ArrayType)result).accessArray();
       }
       return result;
   }
   public String  getName() {
     StringBuffer result = new StringBuffer();
     result.append(baseType.getName());
     for (int i=0; i<noDimensions; i++) 
       result.append("[]");
     return result.toString();
   }
   public String  getNameForClassRef() {return getSignature();}
   public String  getSignature() {
       StringBuffer result = new StringBuffer();
       for (int i=0; i<noDimensions; i++) result.append('[');
       result.append(baseType.getSignature());
       return result.toString();
   }

   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) throws IOException, CompilerError {
     if (this        == d                      ) return  true;
     if (noDimensions < d.noOfArrayDimensions()) return false;
     if (noDimensions > d.noOfArrayDimensions()) return d.baseType()==env.javaLangObjectType;
     if (baseType.isPrimitive()                ) return d.baseType()==baseType;
     return baseType.isSubtypeOf(env, d.baseType());
   }

   /** Casting compatibility - $5.5
    *  @return null: OK, else reason why not OK
    */
   public String whyCannotBeCastedTo(CompilerEnvironment env, DataType d) throws IOException, CompilerError {
     if (!d.isReference()) return "only one reference type involved";

     if (d.isClass    ()) return d==env.javaLangObjectType
                               ? null: "array is only compatible with class java.lang.Object";
     if (d.isInterface()) return d==env.javaLangCloneableType
                               ? null: "array is only compatible with interface java.lang.Cloneable";
     if (noDimensions  < d.noOfArrayDimensions()) return "arrays are incompatible";
     if (noDimensions == d.noOfArrayDimensions()) {
         if(baseType.isPrimitive()) return baseType==d.baseType()? null
                                                                 : "arrays are incompatible";
         else if (d.baseType().isPrimitive()) return "arrays are incompatible";
         return baseType.isSubtypeOf(env, d.baseType())? null: "arrays are incompatible";
     }
     return d==env.javaLangObjectType
         || d==env.javaLangCloneableType
          ? null
          : "arrays are incompatible";
   }

   /** $15.11.2.1 Find Methods that are Applicable and Accessible
    * @return Hashtable of Methods
    */
   public HashMap<String, Object> findApplicableMethods (CompilerEnvironment env,
                                           ClassType callerClass,
                                           RefinementCallExpression c)
                                    throws IOException, ByteCodingException {
      return env.javaLangObjectType.findApplicableMethods (env, callerClass, c);
   }
   /** resolve the given variable name, and check accessability from the given class */
   public MemberVariable resolveMemberVariable (CompilerEnvironment env, String name)
                                                          throws CompilerError, IOException {
       if (name.equals("length")) return ArrayLength.theOne;
       return null; 
   }

   /* ---------------------------- Presentation ------------------------ */

   public String getPresentation () {
      StringBuffer result = new StringBuffer ();
      result.append (baseType.getPresentation ());
      for (int i = 0; i < noDimensions; i++)
         result.append ("[]");
      return result.toString ();
   }

   // reference for this to CONSTANT_Class item in another classType's constant pool
   public  ConstantPoolClass classRef (ClassType constantPoolOwner) {
      return constantPoolOwner.resolveClass (getNameForClassRef());
   }
}
