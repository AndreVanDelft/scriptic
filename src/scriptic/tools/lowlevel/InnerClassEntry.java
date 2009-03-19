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

public class InnerClassEntry implements ClassFileConstants {

   public ConstantPoolClass   innerClass;  
   public ConstantPoolClass   outerClass;
   public ConstantPoolUnicode innerName;
   public short    innerClassAccessFlags;

   public InnerClassEntry (ConstantPoolClass   innerClass,  
                           ConstantPoolClass   outerClass,
                           ConstantPoolUnicode innerName,
                           short    innerClassAccessFlags) {
     this.innerClass = innerClass;  
     this.outerClass = outerClass;  
     this.innerName  = innerName;  
     this.innerClassAccessFlags = innerClassAccessFlags;  
   }

   public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
     throws IOException, ByteCodingException
   {
    out.writeShort(innerClass.slot);
    out.writeShort(outerClass.slot);
    out.writeShort(innerName .slot);
    out.writeShort(innerClassAccessFlags);
   }
   public String getDescription () {
      StringBuffer result = new StringBuffer();
      result.append (innerClassAccessFlags).append (" ");
      result.append (outerClass.getName()).append (".");
      result.append (innerName .getName()).append ("(");
      result.append (innerClass.getName()).append (")");
      return result.append (lineSeparator).toString ();
   }
  static InnerClassEntry readFrom (ClassEnvironment e, DataInputStream in)
    throws IOException, ByteCodingException
  {
    int i1 = in.readShort();
    int i2 = in.readShort();
    int i3 = in.readShort();
    int i4 = in.readShort();
    //System.out.println("read inner class entry: "+i1+" "+i2+" "+i3+" "+i4);

    ConstantPoolClass   innerClass = (ConstantPoolClass  ) e.getConstantPoolItem(i1);
    //ConstantPoolItem   it = e.getConstantPoolItem(in.readShort());
    //System.out.println ("readFrom: "+innerClass.getPresentation(e)+" $$$ "+it.getPresentation(e));
    //ConstantPoolClass   outerClass = (ConstantPoolClass  ) it;
    if (i2==0 
    || i3==0) // happens for java.util.Vector in JDK 2.0
    {
        //System.out.println("parameters: "+i1+" "+i2+" "+i3+" "+i4);
        return null;
    }
    ConstantPoolClass   outerClass = (ConstantPoolClass  ) e.getConstantPoolItem(i2);
    ConstantPoolUnicode innerName  = (ConstantPoolUnicode) e.getConstantPoolItem(i3);
    short innerClassAccessFlags    = (short) i4;

    innerClass.getName (e);
    outerClass.getName (e);
    InnerClassEntry result = new InnerClassEntry(innerClass, outerClass, innerName, innerClassAccessFlags);
    return result;
  }

}

