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

import scriptic.tools.lowlevel.*;

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                           ModifierFlags                         */
   /*                                                                 */
   /*-----------------------------------------------------------------*/



interface ModifierFlags extends ClassFileConstants {

   public final int AllowedClassModifiers           = PublicFlag 
                                                    | ProtectedFlag
                                                    | AbstractFlag
                                                    | FinalFlag;
                                             
   public final int AllowedInterfaceModifiers       = PublicFlag 
                                                    | ProtectedFlag
                                                    | InterfaceFlag;

   public final int AllowedNestedClassModifiers     = PublicFlag 
                                                    | StaticFlag
                                                    | AbstractFlag
                                                    | FinalFlag;
                                             
   public final int AllowedNestedInterfaceModifiers = PublicFlag 
                                                    | StaticFlag
                                                    | InterfaceFlag;

   public final int AllowedVariableModifiers        = PublicFlag 
                                                    | ProtectedFlag
                                                    | PrivateFlag
                                                    | StaticFlag
                                                    | FinalFlag
                                                    | TransientFlag
                                                    | VolatileFlag;

   public final int AllowedConstructorModifiers     = PublicFlag 
                                                    | ProtectedFlag
                                                    | PrivateFlag;

   public final int AllowedMethodModifiers          = PublicFlag 
                                                    | ProtectedFlag
                                                    | PrivateFlag
                                                    | StaticFlag
                                                    | AbstractFlag
                                                    | FinalFlag
                                                    | NativeFlag
                                                    | SynchronizedFlag;

   public final int AllowedInterfaceMethodModifiers = PublicFlag 
                                                    | AbstractFlag;

   public final int AllowedScriptModifiers          = PublicFlag 
                                                    | ProtectedFlag
                                                    | PrivateFlag
                                                    | StaticFlag
                                                    | AbstractFlag
                                                    | FinalFlag
                                                    | SynchronizedFlag;

   public final int AllowedInterfaceScriptModifiers = PublicFlag 
                                                    | AbstractFlag;

/******
   public final int AllowedCommunicationModifiers   = AllowedScriptModifiers;
   public final int AllowedChannelModifiers         = AllowedScriptModifiers;
   public final int AllowedInterfaceCommunicationModifiers = AllowedInterfaceScriptModifiers;
   public final int AllowedInterfaceChannelModifiers       = AllowedInterfaceScriptModifiers;
******/
   public final int AllowedDimensionModifiers       = PublicFlag 
                                                    | ProtectedFlag
                                                    | PrivateFlag;
   public final int AllowedInterfaceDimensionModifiers = PublicFlag;
}

