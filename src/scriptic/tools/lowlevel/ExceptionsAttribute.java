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

public class ExceptionsAttribute extends ClassFileAttribute
{
  public static String name = "Exceptions";
  public String getName () {return name;}

  public ArrayList<String> cps;

  public ExceptionsAttribute() {cps = new ArrayList<String>();}
  public void addException(String  s)  { cps.add(s); length+=2; }

  public void resolve(ClassEnvironment e, AttributeOwner owner) { // only for tracing...?
    try {
      if (bytes == null) {
// this just fills bytes...for tracing only
          length = 2 + cps.size()*2;
          ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
          write (e, owner, new DataOutputStream(byteStream));
          bytes = new byte[length];
          System.arraycopy (byteStream.toByteArray(),2+4,bytes,0,length);
      } else if (cps.size()==0) {
        // this is more decoder stuff...
          DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
          in.readShort();
          for (int i = 0; i<(bytes.length-2)/2; i++)
          {
             int slot = in.readShort();
             cps.add (e.getConstantPoolItem(slot).getPresentation());
          }
      } else {
      }
    } catch (Exception ex) {
System.out.println("Internal Error");
ex.printStackTrace();
    }
  }

   // Generic description -- hex dump+ascii dump
   public String getDescription () {
      StringBuffer result = new StringBuffer(super.getDescription());
      String lineSeparator = System.getProperty ("line.separator", "\r\n");
      result.append(lineSeparator);
      for (String s: cps) {
          result.append (s)
                .append (lineSeparator);
      }
      result.append (lineSeparator);
      return result.toString ();
   }

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    if (bytes != null) {
      out.write (bytes);
    } else {
      out.writeShort(e.resolveUnicode(getName()).slot);
      out.writeInt  (2+cps.size()*2);
      out.writeShort(cps.size());
      for (String s: cps) {
          out.writeShort(e.resolveClass(s).slot);
      }
    }
  }
  public boolean readFromStream (ClassEnvironment e, DataInputStream stream) throws IOException, ByteCodingException {
     int size = stream.readShort();
     for (int i=0; i<size; i++) {
        int index = stream.readShort();
        cps.add (e.getConstantPoolItem(index).getName(e));
     }
     return true;
  }
}
