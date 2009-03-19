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

import java.io.DataOutputStream;
import java.io.IOException;

public class LineNumberTableAttribute extends ClassFileAttribute
{
  public static String name = "LineNumberTable";
  public String getName () {return name;}
  int nEntries;
  int lines[], pcs[];

  public LineNumberTableAttribute()
  { pcs = new int[2]; lines = new int[2];}


  public void addEntry(int pc, int line) {
    if (nEntries >= pcs.length) {
        int old[]=  pcs;  pcs=new int[  pcs.length*2]; System.arraycopy(old,0,  pcs,0,old.length);
            old  =lines;lines=new int[lines.length*2]; System.arraycopy(old,0,lines,0,old.length);
    }
    pcs  [nEntries] = pc;
    lines[nEntries] = line;
    nEntries++;
  }

  int size()
  { return 2  		// name_idx
         + 4  		// attr_len
         + 2  		// line table len spec
         + 4*nEntries;	// table
  }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException {
    out.writeShort(e.resolveUnicode(getName()).slot);
    out.writeInt(2 + 4*nEntries);
    out.writeShort(nEntries);
    for (int i=0; i<nEntries; i++) {
	out.writeShort(  pcs[i]);
	out.writeShort(lines[i]);
    }
  }

  public void decode (ClassEnvironment e) {
  }
}
