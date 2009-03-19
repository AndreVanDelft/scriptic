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

class MultiarrayOperand extends InstructionOperand
{
  ConstantPoolItem cpe;
  int sz;

  MultiarrayOperand(ConstantPoolItem cpe, int sz)
  { this.cpe = cpe; this.sz = sz; }
  int size(ClassEnvironment e, CodeAttribute code) { return 3; }
  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort(cpe.slot);
    out.writeByte((byte)(0xff & sz));
  }
  static MultiarrayOperand readFrom (ClassEnvironment e, DataInputStream in, LongHolder pos)
    throws IOException, ByteCodingException {
    pos.value+=3;
    return new MultiarrayOperand(e.getConstantPoolItem (in.readShort()), in.readUnsignedByte());
  }
  public String getPresentation (Instruction owner) {return "#"+(cpe==null?"null":""+cpe.slot)+" "+sz;}
  public String getCommentString(ClassEnvironment e){return      cpe==null? null :cpe.getPresentation(e);}
}

