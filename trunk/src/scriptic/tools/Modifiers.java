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

////////////////////////////////////////////////////////////////////////////
//
//                             Modifiers
//
////////////////////////////////////////////////////////////////////////////

class Modifiers implements ModifierFlags {

   public int modifierFlags;

   /* --------------------------- Constructors ------------------------- */

   public Modifiers () { }
   public Modifiers (short     flags) {this.modifierFlags = flags;}
   public Modifiers (Modifiers other) {this.modifierFlags = other.modifierFlags;}

   /* --------------------------- Predicates --------------------------- */

   public int     getAllowedModifiers () {return 0;}
   public boolean isPublic      () {return (modifierFlags &       PublicFlag) > 0;}
   public boolean isPrivate     () {return (modifierFlags &      PrivateFlag) > 0;}
   public boolean isProtected   () {return (modifierFlags &    ProtectedFlag) > 0;}
   public boolean isAbstract    () {return (modifierFlags &     AbstractFlag) > 0;}
   public boolean isStatic      () {return (modifierFlags &       StaticFlag) > 0;}
   public boolean isFinal       () {return (modifierFlags &        FinalFlag) > 0;}
   public boolean isNative      () {return (modifierFlags &       NativeFlag) > 0;}
   public boolean isSynchronized() {return (modifierFlags & SynchronizedFlag) > 0;}
   public boolean isTransient   () {return (modifierFlags &    TransientFlag) > 0;}
   public boolean isInterface   () {return (modifierFlags &    InterfaceFlag) > 0;}

   public String getModifierString () {
      StringBuffer result = new StringBuffer ();

      if (isPublic       ())  result.append ("public ");
      if (isPrivate      ())  result.append ("private ");
      if (isProtected    ())  result.append ("protected ");
      if (isAbstract     ())  result.append ("abstract ");
      if (isStatic       ())  result.append ("static ");
      if (isFinal        ())  result.append ("final ");
      if (isNative       ())  result.append ("native ");
      if (isSynchronized ())  result.append ("synchronized ");
      if (isTransient    ())  result.append ("transient ");

      return result.toString ();
   }
}

