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

class ShortOperand extends InstructionOperand
{
  int offset;
  ShortOperand(int n) { offset = n; }
  int size(ClassEnvironment e, CodeAttribute code) { return 2; }
  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException
  { out.writeShort((short)offset); }
  static ShortOperand readFrom (DataInputStream in, LongHolder pos)
    throws IOException {
    pos.value+=2; return new ShortOperand (in.readShort());
  }
  public String getPresentation (Instruction owner) {return "#"+offset;}
}

