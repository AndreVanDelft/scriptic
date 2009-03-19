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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

////////////////////////////////////////////////////////////////////////////
//
//                             AttributesContainer
//
////////////////////////////////////////////////////////////////////////////

public class AttributesContainer implements ClassFileConstants {

   public int                attributeCount;
   public ClassFileAttribute attributes[];
   public HashMap<String, ClassFileAttribute> attributeTable;
   public AttributeOwner     owner;
   public ClassEnvironment   classEnv;

   public AttributesContainer (AttributeOwner owner, ClassEnvironment classEnv) {
      this.owner     = owner;
      this.classEnv  = classEnv;
      attributeTable = new HashMap<String, ClassFileAttribute>();
   }

   public               CodeAttribute                 getCode() {return (              CodeAttribute) attributeTable.get(              CodeAttribute.name);}
   public      ConstantValueAttribute        getConstantValue() {return (     ConstantValueAttribute) attributeTable.get(     ConstantValueAttribute.name);}
   public          DimensionAttribute            getDimension() {return (         DimensionAttribute) attributeTable.get(         DimensionAttribute.name);}
   public         DimensionsAttribute           getDimensions() {return (        DimensionsAttribute) attributeTable.get(        DimensionsAttribute.name);}
   public         ExceptionsAttribute           getExceptions() {return (        ExceptionsAttribute) attributeTable.get(        ExceptionsAttribute.name);}
   public       InnerClassesAttribute         getInnerClasses() {return (      InnerClassesAttribute) attributeTable.get(      InnerClassesAttribute.name);}
   public    LineNumberTableAttribute      getLineNumberTable() {return (   LineNumberTableAttribute) attributeTable.get(   LineNumberTableAttribute.name);}
   public  LinePositionTableAttribute    getLinePositionTable() {return ( LinePositionTableAttribute) attributeTable.get( LinePositionTableAttribute.name);}
   public LocalVariableTableAttribute   getLocalVariableTable() {return (LocalVariableTableAttribute) attributeTable.get(LocalVariableTableAttribute.name);}
   public         SourceFileAttribute           getSourceFile() {return (        SourceFileAttribute) attributeTable.get(        SourceFileAttribute.name);}
   public                     boolean   hasSyntheticAttribute() {return null !=                       attributeTable.get(         SyntheticAttribute.name);}
   public                     boolean  hasDeprecatedAttribute() {return null !=                       attributeTable.get(        DeprecatedAttribute.name);}

   public void add (ClassFileAttribute attribute) {
     if (attributes == null) {
         attributes = new ClassFileAttribute[1];
         attributeCount = 0;
     }
     else if (attributeCount >= attributes.length) {
         ClassFileAttribute[] old = attributes;
         attributes = new ClassFileAttribute[attributeCount*2];
         System.arraycopy (old, 0, attributes, 0, old.length);
     }
     attributes [attributeCount] = attribute;
     attributeTable.put (attribute.getName(), attribute);
     attributeCount++;
   }
   void deleteCodeAttribute() {
       ClassFileAttribute code = getCode();
       if (code != null) deleteAttribute (code);
   }
   void deleteAttribute (ClassFileAttribute a) {
       attributeTable.remove (a.getName());
       ClassFileAttribute[] old = attributes;
       attributes = new ClassFileAttribute[--attributeCount];
       int i=0,j=0;
       while (i<attributeCount) {
          if (old[j]!=a) {attributes[i++]=old[j];}
          j++;
       }
   }

   public String getDescription () {
      StringBuffer result = new StringBuffer ();

      if (attributes != null
      &&  attributeCount > 0) {
         result
            .append ("ATTRIBUTES" ).append (lineSeparator)
            .append ("===========").append (lineSeparator);

         try {
           for (int i = 0; i < attributeCount; i++) {
              attributes [i].decode (classEnv);
              result.append (attributes [i].getDescription()).append (lineSeparator);
           }
         } catch (IOException e) {
              result.append ("DECODING ERROR: "+e).append (lineSeparator);
         }
      }
      return result.toString ();
   }

   public static AttributesContainer readFromStream (AttributeOwner     owner,
                                                     ClassEnvironment   e,
                                                     DataInputStream stream) throws IOException, ByteCodingException {
      AttributesContainer result = new AttributesContainer(owner, e);
      result.attributeCount = stream.readUnsignedShort ();
      if (result.attributeCount > 0) result.readFromStream (stream, result.attributeCount);
      return result;
   }

   public AttributesContainer readFromStream (DataInputStream stream,
                                              int attributeCount) throws IOException, ByteCodingException {

      attributes     = new ClassFileAttribute [attributeCount];

      for (int i=0; i<attributeCount; i++) {

         int nameIndex = stream.readUnsignedShort ();
         int length    = stream.readInt ();
         String name   = classEnv.getConstantPoolItem (nameIndex).getName(classEnv);

         ClassFileAttribute attribute;

              if(              CodeAttribute.name.equals(name)) attribute = new               CodeAttribute(owner, classEnv);
         else if(     ConstantValueAttribute.name.equals(name)) attribute = new      ConstantValueAttribute();
         else if(        DeprecatedAttribute.name.equals(name)) attribute = new         DeprecatedAttribute();
         else if(         DimensionAttribute.name.equals(name)) attribute = new          DimensionAttribute();
         else if(        DimensionsAttribute.name.equals(name)) attribute = new         DimensionsAttribute();
         else if(        ExceptionsAttribute.name.equals(name)) attribute = new         ExceptionsAttribute();
         else if(      InnerClassesAttribute.name.equals(name)) attribute = new       InnerClassesAttribute();
         else if(   LineNumberTableAttribute.name.equals(name)) attribute = new    LineNumberTableAttribute();
         else if( LinePositionTableAttribute.name.equals(name)) attribute = new  LinePositionTableAttribute();
         else if(LocalVariableTableAttribute.name.equals(name)) attribute = new LocalVariableTableAttribute();
         else if(        SourceFileAttribute.name.equals(name)) attribute = new         SourceFileAttribute();
         else if(         SyntheticAttribute.name.equals(name)) attribute = new          SyntheticAttribute();
         else                                                   attribute = new            GenericAttribute(name);

         attribute.length = length;
         attribute.readFromStream (classEnv, stream);
         attributes [i] = attribute;
         attributeTable.put (name, attribute);
      }
      return this;
   }

   public void write(ClassEnvironment e, DataOutputStream out) throws IOException, ByteCodingException
   {
      out.writeShort(attributeCount);
      for (int i=0; i<attributeCount; i++)
      {
	attributes[i].write(e, owner, out);
      }
   }
   public void resolve (ClassEnvironment e, AttributeOwner owner)
   {
      for (int i=0; i<attributeCount; i++)
      {
	attributes[i].resolve(e, owner);
      }
   }
   public int size()
   {
      int result = 0;
      for (int i=0; i<attributeCount; i++)
      {
	result += attributes[i].size();
      }
      return result;
   }
}


