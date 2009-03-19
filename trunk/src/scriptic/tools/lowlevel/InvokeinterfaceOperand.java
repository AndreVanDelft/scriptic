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

class InvokeinterfaceOperand extends InstructionOperand
{
  ConstantPoolItem cpe;
  int nargs;

  InvokeinterfaceOperand(ConstantPoolItem cpe, int nargs)
  { this.cpe = cpe; this.nargs = nargs; }

  int size(ClassEnvironment e, CodeAttribute code) { return 4; }


  void write (ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort(cpe.slot);
    out.writeByte((byte) (0xff & nargs));
    out.writeByte(0);
  }
  static InvokeinterfaceOperand readFrom (ClassEnvironment e, DataInputStream in, LongHolder pos)
    throws IOException, ByteCodingException {
    pos.value+=4;
    InvokeinterfaceOperand result =
        new InvokeinterfaceOperand (e.getConstantPoolItem (in.readShort()),in.readUnsignedByte());
    in.readByte();
    return result;
  }
  public String getPresentation (Instruction owner) {return "#"+(cpe==null?"null":""+cpe.slot)+" "+nargs;}
  public String getCommentString(ClassEnvironment e){return      cpe==null? null :cpe.getPresentation(e);}
}


