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
   /*                     ModifierLanguageConstruct                   */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

abstract class ModifierLanguageConstruct extends LanguageConstruct
                                      implements ModifierFlags {

   public Modifiers modifiers;

   /* --------------------------- Constructors ------------------------- */

   public ModifierLanguageConstruct () { }
   public ModifierLanguageConstruct (ModifierLanguageConstruct anotherConstruct) { 
      super ((LanguageConstruct) anotherConstruct);
      this.modifiers = anotherConstruct.modifiers;
   }

   public void  setModifiers (short flags) {this.modifiers = new Modifiers(flags);}
   public int  getAllowedModifiers () {return 0;}
   public String getModifierString () {return modifiers.getModifierString ();}

   public final boolean isPublic      () {return modifiers.isPublic      ();}
   public final boolean isPrivate     () {return modifiers.isPrivate     ();}
   public final boolean isProtected   () {return modifiers.isProtected   ();}
   public final boolean isAbstract    () {return modifiers.isAbstract    ();}
   public final boolean isStatic      () {return modifiers.isStatic      ();}
   public final boolean isFinal       () {return modifiers.isFinal       ();}
   public final boolean isNative      () {return modifiers.isNative      ();}
   public final boolean isSynchronized() {return modifiers.isSynchronized();}
   public final boolean isTransient   () {return modifiers.isTransient   ();}

   /* ------------------------- Code generation ------------------------ */

   public void outModifiers (PreprocessorOutputStream stream) {outModifiers (stream, 0xFFFFFFFF);}
   public void outModifiers (PreprocessorOutputStream stream,int mask) {
      int  processedModifiers = modifiers.modifierFlags & mask;
      if ((processedModifiers &      PublicFlag)>0) {outToken(stream,      PublicToken); outSpace(stream);}
      if ((processedModifiers &   ProtectedFlag)>0) {outToken(stream,   ProtectedToken); outSpace(stream);}
      if ((processedModifiers &     PrivateFlag)>0) {outToken(stream,     PrivateToken); outSpace(stream);}
      if ((processedModifiers &      StaticFlag)>0) {outToken(stream,      StaticToken); outSpace(stream);}
      if ((processedModifiers &    AbstractFlag)>0) {outToken(stream,    AbstractToken); outSpace(stream);}
      if ((processedModifiers &       FinalFlag)>0) {outToken(stream,       FinalToken); outSpace(stream);}
      if ((processedModifiers &   TransientFlag)>0) {outToken(stream,   TransientToken); outSpace(stream);}
      if ((processedModifiers &    VolatileFlag)>0) {outToken(stream,    VolatileToken); outSpace(stream);}
      if ((processedModifiers &      NativeFlag)>0) {outToken(stream,      NativeToken); outSpace(stream);}
      if ((processedModifiers &SynchronizedFlag)>0) {outToken(stream,SynchronizedToken); outSpace(stream);}
   }
}

