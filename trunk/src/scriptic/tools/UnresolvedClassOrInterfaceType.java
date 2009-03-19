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
import java.util.HashMap;

class UnresolvedClassOrInterfaceType extends ClassType {
   public static UnresolvedClassOrInterfaceType one =
             new UnresolvedClassOrInterfaceType();
   UnresolvedClassOrInterfaceType() {super ("<unresolved>", "<unresolved>");}
   public  boolean  hasError() {return true;}
   public String  getSignature    () {return "?";}
   public boolean isResolved ()      {return false;}
   public boolean isSubtypeOf       (CompilerEnvironment env, DataType d) {return false;}
   public boolean canBeAssignedTo   (CompilerEnvironment env, DataType d) {return  true;}
   public String whyCannotBeCastedTo(CompilerEnvironment env, DataType d) {return    "";}
   public HashMap<String, Object> findApplicableMethods (CompilerEnvironment env,
                                           ClassType callerClass,
                                           RefinementCallExpression c) {
       return new HashMap<String, Object>();
   }
   HashMap<String, HashMap<String, Object>> getMethodNamesSignatures (ClassesEnvironment env) {
       return new HashMap<String, HashMap<String, Object>>();
   }

   public ArrayList<Method> findApplicableConstructors (CompilerEnvironment                 env,
                                             ClassType                  callerClass,
                                             MethodOrConstructorCallExpression    m,
                                             boolean                isForAllocation) {
       return new ArrayList<Method>();
   }
   public Method findMethod (ClassesEnvironment env, String name, String signature) {return null;}
   public MemberVariable resolveMemberVariable (CompilerEnvironment env, String name) {return null;}
   protected final ClassType   getSuperclass (ClassesEnvironment env) {return null;}
   protected final ClassType[] getInterfaces (ClassesEnvironment env) {return new ClassType[0];}
}

