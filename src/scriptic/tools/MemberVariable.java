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

package scriptic.tools;

import java.io.*;
import scriptic.tools.lowlevel.*;

////////////////////////////////////////////////////////////////////////////
//
//                             MemberVariable
//
////////////////////////////////////////////////////////////////////////////

class MemberVariable extends Variable
                  implements ClassFileConstants, AttributeOwner {
   public ClassType       owner;
          AttributesContainer        attributesContainer;
   public String getAttributeDescription () {
        return attributesContainer==null?""
             : attributesContainer.getDescription();}
   public ClassType                  ownerClass(CompilerEnvironment env) {return owner;}
   public ShadowOfMemberVariable shadow; // hack for private members that are accessed by inner classes

   private ConstantValue constantValue;
   public  ConstantValue constantValue() {return constantValue;}
   public  void       setConstantValue(ConstantValue c) {constantValue=c;}
   public  boolean      isConstant    () {return constantValue!=null;}
   public  boolean      isStatic      () {
if (owner==null){
new Exception("MemberVariable.owner==null: "+name).printStackTrace();
}
return super.isStatic()||owner!=null&&owner.isInterface();}
   public  boolean      isFinal      () {
if (owner==null){
new Exception("MemberVariable.owner==null: "+name).printStackTrace();
}
return super.isFinal()||owner!=null&&owner.isInterface();}


   public  String getConstructName () {return "field";}
   public MemberVariableDeclaration  source;
   public LanguageConstruct          source() {return source;}
   public boolean                    isSynthetic() {
       return attributesContainer==null?false
            : attributesContainer.hasSyntheticAttribute();
   }
   public boolean                    isDeprecated() {
       return attributesContainer==null?false
            : attributesContainer.hasDeprecatedAttribute();
   }
   public void setSynthetic  () {addAttribute (new   SyntheticAttribute());}
   public void setDeprecated() {addAttribute (new DeprecatedAttribute());}
   public void addAttribute (ClassFileAttribute attribute) {
     if (attributesContainer==null)
         attributesContainer = new AttributesContainer(this, owner);
     attributesContainer.add (attribute);
   }

   public void write(CompilerEnvironment env, ClassEnvironment e, DataOutputStream out)
                          throws IOException, ByteCodingException
   {
     out.writeShort(modifierFlags);
     out.writeShort(e.resolveUnicode(name)          .slot);
     out.writeShort(e.resolveUnicode(getSignature(env)).slot);

     if (dimensionSignature!=null) {
        addAttribute (new DimensionAttribute(dimensionSignature));
     }
     if (attributesContainer == null) {
          out.writeShort(0);
     }
     else {
        attributesContainer.write(e, out);
//System.out.println("variable.write: "+attributesContainer.getDescription());

     }
   }

   /* ----------------------------- Predicates ------------------------- */

   public boolean isMemberVariable () {return true;}

   /* -----------------------------   I/O   ------------------------- */

   public void readFromStream (ClassesEnvironment env,
                               ClassType containingClass,
                               DataInputStream stream) throws IOException, ByteCodingException {
       owner                      = containingClass;
       modifierFlags              = stream.readUnsignedShort ();
       int nameIndex              = stream.readUnsignedShort ();
       int signatureIndex         = stream.readUnsignedShort ();
try {
       name                       = containingClass.getConstantPoolItem (nameIndex).getName(containingClass);
       signature                  = containingClass.getConstantPoolItem (signatureIndex).getName(containingClass);
    // dataType1                  = DataType.getFromSignature (env, signature, owner, false);
       attributesContainer        = AttributesContainer.readFromStream (this, owner, stream);
       DimensionAttribute da      = attributesContainer.getDimension();
       if (da!=null) dimensionSignature = da.signature;

//System.out.println ("readFromStream: "+name
//+"\nattributesContainer: "+attributesContainer.getDescription()
//+"\ngetConstantValuePresentation: "+getConstantValuePresentation()
//);

} catch (RuntimeException e) {
System.out.println (e+" in readFromStream: "
+"\nmodifierFlags = "+modifierFlags
+"\nnameIndex = "+nameIndex
+"\nsignatureIndex = "+signatureIndex
+"\nownerClass = "+owner.getShortDescription()
+"\ncontainingClass = "+containingClass.getShortDescription()); e.printStackTrace();
}
   }

   public String getConstantValuePresentation () {
        return !isConstant()?"":"= "+constantValue.makeString();
   }
   public void resolveConstantValue(ClassEnvironment env) throws ByteCodingException, IOException {
       if (attributesContainer == null) return;
       ConstantValueAttribute cva = attributesContainer.getConstantValue();
       if (cva != null) {
             ConstantPoolItem   c = cva.constantPoolItem;
             switch (dataType1.getToken()) {
             case StringLiteralToken:  
                               constantValue = new ConstantString  (        ((ConstantPoolString)c).getName(env)); break;
             case    ByteToken: constantValue = new ConstantByte   (( byte)((ConstantPoolInteger)c).   intValue); break; 
             case    CharToken: constantValue = new ConstantChar   (( char)((ConstantPoolInteger)c).   intValue); break; 
             case   ShortToken: constantValue = new ConstantShort  ((short)((ConstantPoolInteger)c).   intValue); break; 
             case     IntToken: constantValue = new ConstantInt    (       ((ConstantPoolInteger)c).   intValue); break; 
             case BooleanToken: constantValue = new ConstantBoolean(       ((ConstantPoolInteger)c).   intValue==0?false:true); break;
             case  DoubleToken: constantValue = new ConstantDouble (       ((ConstantPoolDouble )c).doubleValue); break; 
             case   FloatToken: constantValue = new ConstantFloat  (       ((ConstantPoolFloat  )c). floatValue); break; 
             case    LongToken: constantValue = new ConstantLong   (       ((ConstantPoolLong   )c).  longValue); break;
             default:
                System.out.println("ERROR in ConstantValue: illegal dataType token: "+ dataType1.getToken()
                                  +lineSeparator+getPresentation());
                Thread.dumpStack();                                  
             }
             //System.out.println("ConstantValue: "+ (constantValue()==null? null: constantValue().makeString()));
//System.out.println ("resolveConstantValue: "+name+" value: "+constantValue.makeString());
         }
   }
   // hack for private members that are accessed by inner classes
   public ShadowOfMemberVariable getShadow() {
      if (shadow==null) {
          ShadowGetMethod sg = new ShadowGetMethod (this);
          ShadowSetMethod ss = new ShadowSetMethod (this);
          owner.addMethod (sg);
          owner.addMethod (ss);
          shadow = new ShadowOfMemberVariable (this, sg, ss);
      }
      return shadow;
   }
}


