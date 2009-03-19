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

class LabelOperand extends InstructionOperand
{
  LabelInstruction target;
  Instruction source;
  boolean wide;
  int offset;

  LabelOperand(LabelInstruction l, Instruction source, boolean wide)
  { target = l; this.source = source; this.wide = wide; }
  InstructionOperand copyFor (Instruction source) {
     return new LabelOperand(target, source, wide);
  }
  public LabelInstruction getTarget() {return target;}
  int size(ClassEnvironment e, CodeAttribute code) { if (wide) return 4; else return 2; }
  void incTargetReferenceCount() throws ByteCodingException {target.incReferenceCount(source);}
  void decTargetReferenceCount() {target.decReferenceCount();}
  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    if (wide) { target.writeWideOffset(e, ce, source, out); }
    else      { target.    writeOffset(e, ce, source, out); }
  }
  static LabelOperand readFrom (DataInputStream in, LongHolder pos, boolean isWide)
    throws IOException {
    int offset;
    LabelOperand result;
    if (isWide) {pos.value+=4; offset = in.readInt  (); result = new LabelOperand (null, null, true);}
    else        {pos.value+=2; offset = in.readShort(); result = new LabelOperand (null, null, false);      }
    result.offset = offset;
    return result;
  }
  public String getPresentation (Instruction owner) {
     StringBuffer result = new StringBuffer();
     if (target != null) result.append(target.id);
     else                result.append (owner.pc+offset);
     //if (source != null) result.append("source: ").append...
     //if (wide) result.append (" WIDE");
     return result.toString();
  }
  public String getCommentString (ClassEnvironment e) {return wide?"wide":"";}

  LabelInstruction[] getTargetLabels() {
    LabelInstruction result[] = {target};
    return result;
  }
}
