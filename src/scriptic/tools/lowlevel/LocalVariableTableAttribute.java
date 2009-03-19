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
import java.util.*;

public class LocalVariableTableAttribute extends ClassFileAttribute
{
  public static String name = "LocalVariableTable";
  public String getName () {return name;}

  ArrayList<LocalVarEntry> vars;

  public LocalVariableTableAttribute()
  { vars = new ArrayList<LocalVarEntry>(); }

  public void addEntry(LocalVarEntry e)
  { vars.add(e); }

  int size()
  { return      
      (2 +                      // name_idx
       4 +                      // attr_len
       2 +                      // line table len spec
       10*(vars.size()));       // table
  }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    CodeAttribute ca = (CodeAttribute) owner;
    out.writeShort(e.resolveUnicode(getName()).slot);
    out.writeInt(2 + 10*(vars.size()));
    out.writeShort(vars.size());
    for (LocalVarEntry lv: vars)
      {
	lv.write(e, ca, out);
      }
  }

  public void decode (ClassEnvironment e) {
    if (bytes == null          ) return;
    if (!vars.isEmpty()) return;
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    try {
      int n = in.readShort();  // vars.size()
      for (int i=0; i<n; i++) {
          vars.add (LocalVarEntry.readFrom (e, in));
      }
  
    } catch (Exception exc) {
      System.out.println("LocalVariableTableAttribute: decoding error");
      exc.printStackTrace();
    }
  }
}
