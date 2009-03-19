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

class IincOperand extends InstructionOperand
  implements ClassFileConstants
{
  int vindex, constt;

  IincOperand(int vindex, int constt)
  { this.vindex = vindex; this.constt = constt; }

  int size(ClassEnvironment e, CodeAttribute code)
  {
    if  (vindex >  255
    ||   constt >  127 
    ||   constt < -128)
         return 5;
    else return 2;
  }
  void writePrefix(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException
  {
    if (vindex >  255
    ||  constt >  127
    ||  constt < -128) out.writeByte((byte)INSTRUCTION_wide);
  }
  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException
  {
    if  (vindex >  255
    ||   constt >  127 
    ||   constt < -128)
        {out.writeShort((short)(0xffff & vindex));
         out.writeShort((short)(0xffff & constt));}
    else{out.writeByte (( byte)(0xff   & vindex));
         out.writeByte (( byte)(0xff   & constt));}
  }
  static IincOperand readFrom (DataInputStream in, LongHolder pos, boolean isWide)
    throws IOException {
    int vindex, constt;
    if (isWide) {pos.value+=4;
                 vindex = in.readUnsignedShort();
                 constt = in.readUnsignedShort();}
    else        {pos.value+=2;
                 vindex = in.readUnsignedByte ();
                 constt = in.readUnsignedByte ();}
    return new IincOperand(vindex, constt);
  }
  public String getPresentation (Instruction owner) {return vindex+" "+constt;}
}

