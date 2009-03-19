/* This file is part of Sawa, the Scriptic-Java compiler
 * Copyright (C) 2009 Andre van Delft
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


public abstract class ConstantPoolClassMemberReference extends ConstantPoolItem {
   int classIndex;
   int nameAndTypeIndex;
   ConstantPoolClass classItem;
   ConstantPoolNameAndType nameAndTypeItem;

   public ConstantPoolClassMemberReference () {}
   public ConstantPoolClassMemberReference(ConstantPoolClass       classItem,
                                    ConstantPoolNameAndType nameAndTypeItem) {
       this.classItem        =       classItem;
       this.classIndex       =       classItem.slot;
       this.nameAndTypeItem  = nameAndTypeItem;
       this.nameAndTypeIndex = nameAndTypeItem.slot;
   }
   public ConstantPoolClassMemberReference(int classIndex, int nameAndTypeIndex) {
       this.classIndex       =       classIndex;
       this.nameAndTypeIndex = nameAndTypeIndex;
   }

   public boolean writeToStream (DataOutputStream stream) throws IOException {
      stream.writeByte (tag());
      stream.writeShort(classIndex);
      stream.writeShort(nameAndTypeIndex);
      return true;
   }
   private ConstantPoolClass classItem(ClassEnvironment e) throws IOException, ByteCodingException {
       if (classItem==null) {
           classItem = (ConstantPoolClass) e.getConstantPoolItem (classIndex);
       }
       return classItem;
   }
   private ConstantPoolNameAndType nameAndTypeItem(ClassEnvironment e) throws IOException, ByteCodingException {
       if (nameAndTypeItem==null) {
           nameAndTypeItem = (ConstantPoolNameAndType) e.getConstantPoolItem (nameAndTypeIndex);
       }
       return nameAndTypeItem;
   }
   public String getName() {
     if (classItem==null||nameAndTypeItem==null) {
    	 return "????";
     }
     return  classItem.getName() + "."
     + nameAndTypeItem.getName();
   }
   public String getName (ClassEnvironment e) throws IOException, ByteCodingException {
     return  classItem(e).getName(e) + "."
     + nameAndTypeItem(e).getName(e);
   }
   public String getPresentation (ClassEnvironment e) {
     try {
       return super.getPresentation(e)
       + ' ' + e.getConstantPoolItem(nameAndTypeItem.signatureIndex).getName(e);
     } catch (IOException      err) {return err.toString();
     } catch (ByteCodingException         err) {return err.toString();
     } catch (RuntimeException err) {return err.toString();
     }
   }
}


