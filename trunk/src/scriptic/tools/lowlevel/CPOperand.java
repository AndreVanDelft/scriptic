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

class CPOperand extends InstructionOperand
{
  ConstantPoolItem cpe;
  boolean wide;
  int size(ClassEnvironment e, CodeAttribute code) { if (wide) return 2; else return 1; }
  CPOperand(ConstantPoolItem cpe) { this.cpe = cpe; wide = true; }
  CPOperand(ConstantPoolItem cpe, boolean wide)
  { this.cpe = cpe; this.wide = wide; }

  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    if (wide)
      { out.writeShort(cpe.slot); }  // 'Unsigned' to be added?
    else
      {
        if (cpe.slot > 256)
          { throw new ByteCodingException("exceeded size for small cpidx" + cpe); }
        out.writeByte((byte) (0xff & (cpe.slot)));
      }
  }
  static CPOperand readFrom (ClassEnvironment e, DataInputStream in, LongHolder pos, boolean isWide)
    throws IOException, ByteCodingException {
    if (isWide) {pos.value+=2; return new CPOperand (e.getConstantPoolItem (in.readUnsignedShort()),  true);}
    else        {pos.value++;  return new CPOperand (e.getConstantPoolItem (in.readUnsignedByte ()), false);}
  }
  public String getPresentation (Instruction owner) {return "#"+(cpe==null?"null":""+cpe.slot);}
  public String getCommentString(ClassEnvironment e){
     try {
        return cpe==null? null :cpe.getPresentation(e);
     } catch (RuntimeException err) {
        return err.toString();
     }
  }
}


