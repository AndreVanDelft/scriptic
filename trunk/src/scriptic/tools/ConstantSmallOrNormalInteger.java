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
import scriptic.tools.lowlevel.ClassEnvironment;
import scriptic.tools.lowlevel.ConstantValueAttribute;
import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.InstructionOwner;
import scriptic.tools.lowlevel.ByteCodingException;

abstract class ConstantSmallOrNormalInteger extends ConstantNumber {
   abstract public int intValue();
   public Instruction []     loadInstructions (ClassType t,InstructionOwner owner) {
      Instruction[] result = new Instruction[1];
      result[0] = Instruction.loadIntegerInstruction (t, intValue(), owner);
      return result;
   }
   public ConstantValueAttribute makeAttribute(ClassEnvironment e) throws ByteCodingException
   {
      return new ConstantValueAttribute (e.resolveInteger (intValue()));
   }
   public ConstantValue convertTo (int primitiveTypeToken) {
      if (primitiveTypeToken==dataType(null).getToken()) return this;
      switch (primitiveTypeToken) {
      case   ByteToken: return new ConstantByte  ( (byte)intValue());
      case   CharToken: return new ConstantChar  ( (char)intValue());
      case  ShortToken: return new ConstantShort ((short)intValue());
      case    IntToken: return new ConstantInt   (       intValue());
      case   LongToken: return new ConstantLong  (       intValue());
      case  FloatToken: return new ConstantFloat (       intValue());
      case DoubleToken: return new ConstantDouble(       intValue());
      }
      return this;
   }
}
