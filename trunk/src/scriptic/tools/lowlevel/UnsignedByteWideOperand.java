/* This file is part of Sawa, the Scriptic-Java compiler
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

class UnsignedByteWideOperand extends InstructionOperand
  implements ClassFileConstants
{
  int val;

  UnsignedByteWideOperand(int n)  { val = n; }
  int size(ClassEnvironment e, CodeAttribute code)
  {
    if (val >= 256) return 3;
    return 1;
  }
  void writePrefix(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException
  {
    if (val > 255) out.writeByte((byte)(INSTRUCTION_wide));
  }
  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException
  {
    if (val > 255) out.writeShort((short)(0xffff & val));
    else           out.writeByte (( byte)(val & 0xff));
  }
  static UnsignedByteWideOperand readFrom (DataInputStream in, LongHolder pos, boolean isWide)
    throws IOException {
    if (isWide) {pos.value+=2; return new UnsignedByteWideOperand (in.readUnsignedShort());}
    else        {pos.value++;  return new UnsignedByteWideOperand (in.readUnsignedByte ());}
  }
  public String getPresentation (Instruction owner) {return "#"+val+ (val<256?"":" WIDE");}
}

