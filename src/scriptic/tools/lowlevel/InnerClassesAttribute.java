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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class InnerClassesAttribute extends ClassFileAttribute {

   public ArrayList<InnerClassEntry> entries;
   public InnerClassesAttribute () {entries = new ArrayList<InnerClassEntry>();}
   public static String name = "InnerClasses";
   public String getName () {return name;}

  public void add (InnerClassEntry ie) {entries.add(ie);}
  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort (e.resolveUnicode(getName()).slot);
    out.writeInt   (2+entries.size()*8);
    out.writeShort ((short)entries.size());
    for (int i=0; i<entries.size(); i++) {
       entries.get(i).write (e, owner, out);
    }
  }
  public boolean readFromStream (ClassEnvironment e, DataInputStream in)
    throws IOException, ByteCodingException
  {
    /* these two lines also fill bytes - for debugging... */
    super.readFromStream (e, in);
    in = new DataInputStream (new ByteArrayInputStream (bytes));

    int size = in.readShort();
    for (int i=0; i<size; i++)
      {
        InnerClassEntry entry = InnerClassEntry.readFrom (e, in);
        if (entry != null) // beware java.util.Vector in JDK 2.0; see InnerClassEntry.readFrom
        {
            entries.add(entry);
        }
      }
    return true;
  }
  public String getPresentation () {return getName ();}
  public String getDescription  () {
    String result = super.getDescription();
    for (InnerClassEntry ice: entries) {
       result += ice.getDescription ();
    }
    return result;
  }
}

