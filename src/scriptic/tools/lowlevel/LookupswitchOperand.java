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

class LookupswitchOperand extends SwitchOperand
{
  int              match[];

  LookupswitchOperand(Instruction s, LabelInstruction def, int m[], LabelInstruction j[]) throws ByteCodingException
  { dflt   = def;
    jmp    = j;
    match  = m;
    source = s;

    if (dflt==null) return;

    dflt.incReferenceCount(source);
    for (int i=0; i<jmp.length; i++) jmp[i].incReferenceCount(source); 
 }

  int size(ClassEnvironment e, CodeAttribute code) throws ByteCodingException
  {
    int sz = 8;			// 4 + 4 + padding + jumptable
    if (((source.pc+1) % 4) != 0)
      {
				// need padding
	sz += (4 - ((source.pc+1) % 4));
      }

    if (jmp != null)
      { sz += 8*(jmp.length); }
    return sz;
  }

  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    int pad;
    if (((source.pc+1) % 4) != 0)
      {				// need padding
	pad = (4 - ((source.pc+1) % 4));
	for (int x=0; x<pad; x++) out.writeByte(0);
      }
    dflt.writeWideOffset(e, ce, source, out);
    if (jmp == null)
      { out.writeInt(0); }
    else
      {
	out.writeInt(jmp.length);
	for (int x=0; x<jmp.length; x++)
	  {
	    out.writeInt(match[x]);
	    jmp[x].writeWideOffset(e, ce, source, out);
	  }
      }
  }
  static LookupswitchOperand readFrom (ClassEnvironment e,
                                       DataInputStream in, LongHolder pos)
    throws ByteCodingException, IOException {
    LabelInstruction dflt    = null;
    int              match[] = null;
    LabelInstruction jmp  [] = null;

    long pad  = 4 - pos.value%4;
    if  (pad != 4) {
      for (int i=0; i<pad; i++) {      // padding...
          in.skip(1);
      }
      pos.value+=pad;
    }

    in.readInt(); // skip the label for the time being...
    pos.value+=4;

    int n = in.readInt();
    pos.value+=4;

    if (n>0) {
      match = new int[n];
      jmp   = new LabelInstruction[n];
      for (int i=0; i<n; i++) {
         match[i] = in.readInt();
         in.readInt();  // skip the label for the time being...
         pos.value+=8;
      }
    }
    return new LookupswitchOperand (null /*source*/, dflt, match, jmp);
  }
  public String getPresentation (Instruction owner) {
    return ""+match.length;
  }
}


