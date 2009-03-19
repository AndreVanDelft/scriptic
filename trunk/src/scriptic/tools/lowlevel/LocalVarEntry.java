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

public class LocalVarEntry
{
  LabelInstruction start, end;
  String name, sig;
  int slot;

  public LocalVarEntry
  (LabelInstruction startLabel, LabelInstruction endLabel, String name, String sig, int slot)
  {
    start     = startLabel;
    end       = endLabel;
    this.name = name;
    this.sig  = sig;
    this.slot = slot;
  }
  public LocalVarEntry() {}

  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    start.writeOffset(e, ce,  null, out);
    end  .writeOffset(e, ce, start, out); // This is the *length*,
			                  // not another offset

    out.writeShort(e.resolveUnicode(name).slot);
    out.writeShort(e.resolveUnicode(sig ).slot);
    out.writeShort((short)slot);
  }

  static LocalVarEntry readFrom (ClassEnvironment e, /*CodeAttribute ce,*/ DataInputStream in)
    throws IOException, ByteCodingException
  {
    LocalVarEntry result = new LocalVarEntry();
    in.readShort();
    in.readShort();
    result.name = e.getConstantPoolItem (in.readShort()).getName(e);
    result.sig  = e.getConstantPoolItem (in.readShort()).getName(e);
    result.slot = in.readShort();
    return result;
  }
}
