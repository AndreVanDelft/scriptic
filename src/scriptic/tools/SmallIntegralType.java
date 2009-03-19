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


abstract class SmallIntegralType extends SmallOrNormalIntType {
   public boolean isSmallIntegral () {return true;}
   public DataType promoteUnary   () {return IntType.theOne;}
   public short convertToInstructionCode(DataType d) {
       if (d==  LongType.theOne) return INSTRUCTION_i2l;
       if (d== FloatType.theOne) return INSTRUCTION_i2f;
       if (d==DoubleType.theOne) return INSTRUCTION_i2d;
       return -1;
   }
}
