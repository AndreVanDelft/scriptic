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


package scriptic.tools.lowlevel;

import java.io.*;

  /*------------------------- ConstantPoolItem -------------------------*/

public abstract class ConstantPoolItem implements ClassFileConstants {
   public int slot = 0;
   public String getPresentationName () {return getClass().getName ();}
   
   public abstract String getName ();
   public          String getName (ClassEnvironment e) throws IOException, ByteCodingException {return getName();}
   public abstract int tag();

   public boolean writeToStream (DataOutputStream stream) throws IOException {
      return false;
   }
   public int noOfSlots () {return 1;}
   static final int PaddedSlotLength = 3;
   public String getPaddedSlotString () {

      String numberString = String.valueOf (slot);
      if (numberString.length() >= PaddedSlotLength)
         return numberString;

      char [ ] numberChars = new char [ PaddedSlotLength ];
      for (int i = 0; i < numberChars.length ; i++)
         numberChars [i] = ' ';
      numberString.getChars (0, numberString.length(),
                             numberChars, 
                             numberChars.length - numberString.length());
      return new String (numberChars);
   }
   public String getPresentation () {
       return getPresentation(null);
   }
   public String getPresentation (ClassEnvironment e) {
     try {
       return getPaddedSlotString () + ": " 
            + getPresentationName () + " " + getName(e);
     }
     catch (IOException      err) {return err.toString();}
     catch (ByteCodingException         err) {return err.toString();}
     catch (RuntimeException err) {return "could not get constant pool item presentation"
                                +lineSeparator+err;}
   }
   public Instruction loadInstruction (InstructionOwner owner)
    throws ByteCodingException {
      if (slot <256) return new Instruction (INSTRUCTION_ldc  , this, owner);
      else           return new Instruction (INSTRUCTION_ldc_w, this, owner);
   }
}


