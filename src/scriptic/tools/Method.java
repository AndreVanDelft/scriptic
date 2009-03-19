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
import java.util.ArrayList;
import scriptic.tools.lowlevel.*;

////////////////////////////////////////////////////////////////////////////
//
//                             Method
//
////////////////////////////////////////////////////////////////////////////

class Method extends Field implements AttributeOwner {
   public ClassType                  owner;
   protected AttributesContainer     attributesContainer;
   Parameter                         extraParameters[]; // Inner and Nested Classes
   Parameter            			 parameters[];
   String                            returnDimensionSignature;

   Parameter[] allParameters()
   {
     if(extraParameters==null) return parameters;
     if(parameters==null) return extraParameters;
     Parameter[] result = new Parameter[extraParameters.length+parameters.length];
     System.arraycopy(extraParameters, 0, result, 0, extraParameters.length);
     System.arraycopy(parameters     , 0, result, extraParameters.length, parameters.length);
     return result;
   }
   public ClassType                  ownerClass    (CompilerEnvironment env) {return owner;}
// FormalIndex     []                formalIndexes () {return null;}
   public String getAttributeDescription () {
       return attributesContainer==null ? ""
                                        : attributesContainer.getDescription();
   }

   public boolean isConstructor     () {return name.equals(  "<init>");}
   public boolean isClassInitializer() {return name.equals("<clinit>");}

   public DataType        returnType = VoidType.theOne;;
   public RefinementDeclaration source;
   public String getConstructName () {return source!=null?source.getConstructName()
                                           : isConstructor()? "constructor"
                                           :                  "method";}
   public LanguageConstruct source() {return source;}
   private ExceptionsAttribute exceptions;

   public boolean isSynthetic() {
       return attributesContainer==null?false
            : attributesContainer.hasSyntheticAttribute();
   }
   public boolean isDeprecated() {
       return attributesContainer==null?false
            : attributesContainer.hasDeprecatedAttribute();
   }
   public void setSynthetic  () {
     /*
        addAttribute (new   SyntheticAttribute());

        Deactivated since JDK 1.2.2's verifier thinks synthetic methods
        should not override others. It simply throws a CompilerError
        (sun.tools.java.MemberDefinition.checkOverride)
     */
   }
   public void setDeprecated() {addAttribute (new DeprecatedAttribute());}
   public void setDimensionSignature() {
      dimensionSignature = "";
      boolean hasDimension = false;
      Parameter[] allParameters = allParameters();
      if (allParameters != null) {
         for (int i=0; i<allParameters.length; i++) {
            if (!allParameters[i].dataType1.baseType().isNumeric()) continue;
            String s = allParameters[i].dimensionSignature;
            if (s==null) s = "0";
            else hasDimension = true;
            dimensionSignature += s;
         }
      }
      if (returnType.baseType().isNumeric()) {
         String s = returnDimensionSignature;
         if (s==null) s = "0";
         else hasDimension = true;
         dimensionSignature += s;
      }
      if (!hasDimension) dimensionSignature = null;
   }
   public void parseDimensionSignature() {
      if (dimensionSignature==null) return;
      int pos[] = new int[1];
      Parameter[] allParameters = allParameters();
      if (allParameters != null) {
         for (int i=0; i<allParameters.length; i++) {
            if (!allParameters[i].dataType1.baseType().isNumeric()) continue;
            String s = Dimension.eatParameterSignature (dimensionSignature, pos);
            //if (s.equals("0")) s = null;
            allParameters[i].dimensionSignature = s;
         }
      }
      if (returnType.baseType().isNumeric()) {
         String s = Dimension.eatParameterSignature (dimensionSignature, pos);
         // if (s.equals("0")) s = null; ...already done by eatParameterSignature
         returnDimensionSignature = s;
      }
   }

   public void addAttribute (ClassFileAttribute attribute) {
     if (attributesContainer==null)
         attributesContainer = new AttributesContainer(this, owner);
     attributesContainer.add (attribute);
   }

   // free the memory occupied by type declaration, code etc.
   void freeCompiledMembers () {
     attributesContainer = null;
     exceptions          = null; // exceptionTypes remain in exceptionTypes
     source              = null;
     Parameter[] allParameters = allParameters();
     if (allParameters != null)
     for (int i=0; i<allParameters.length; i++) {
        allParameters[i].freeCompiledMembers();
     }
   }

   public void setExceptionsAttribute () {
     if (exceptionTypes==null) return;
     exceptions = new ExceptionsAttribute();
     for (int i=0; i<exceptionTypes.length; i++) {
         exceptions.addException(exceptionTypes[i].nameWithSlashes);
     }
     addAttribute (exceptions);
   }

   protected ClassType[] exceptionTypes;

   public  ClassType[] exceptionTypes (ClassesEnvironment env) {

       if (exceptionTypes==null) {
           if (attributesContainer != null
           &&  (exceptions = attributesContainer.getExceptions()) != null) {

               exceptionTypes = new ClassType[exceptions.cps.size()];
               for (int i=0; i<exceptionTypes.length; i++) {
                  exceptionTypes[i] = env.resolveClassNameWithSlashes(exceptions.cps.get(i), false);
               }
           }
           else exceptionTypes = new ClassType[0];
       }
       return exceptionTypes;
   }


   protected int nextVariableSlot = 0;
      /** set slot number for given variable or parameter.
       *  and increments nextSlotNumber appropriately
       */

   public void setNextSlot (LocalVariableOrParameter v) {
         v.slot = nextVariableSlot;
         nextVariableSlot += v.dataType1.slots(); // not so for scripts...
   }

   public void parseSignatureIfNeeded (ClassesEnvironment env) {
       if (parameters==null) {
           parseSignature (env, false);
       }
   }
   public  int parameterSlots() {
     int result = 0;

     Parameter[] allParameters = allParameters();
     for (int i=0; i<allParameters.length; i++) {
        result += allParameters[i].dataType1.slots();
     }
     return result;
   }

   private int deltaStackSize = -12345678;
   public  int deltaStackSize() {
      if (deltaStackSize== -12345678) {
        deltaStackSize = isStatic()? 0: -1;
        Parameter[] allParameters = allParameters();
        if (allParameters!=null) {
           for (int i=0; i<allParameters.length; i++) {
              deltaStackSize -= allParameters[i].dataType1.slots();
           }
        }
        deltaStackSize += returnType.slots();
      }
      return deltaStackSize;
   }

   public boolean isMoreSpecificThan (CompilerEnvironment env, Method other)
              throws ByteCodingException, IOException {
       if (!returnType.isSubtypeOf (env, other.returnType)) return false;
       parseSignatureIfNeeded (env);
       Parameter[]      allParameters =       allParameters();
       Parameter[] allOtherParameters = other.allParameters();
       for (int i=0; i<parameters.length; i++) {
    	   Parameter p =      allParameters[i];
    	   Parameter q = allOtherParameters[i];
           if (!p.getDataType(env).isSubtypeOf (env, q.getDataType(env))) return false;
       }
       return true;
   }

   public boolean equalParameters (ClassesEnvironment env, Method other) {
      /* This is a little shortcut. 
         Why is there no equals() method in class Vector? */
      return this.getParameterSignature (env).equals 
                     (other.getParameterSignature(env));
   }

/*
   public boolean hasParameterError () {
      for (int i=0; i<parameters.size(); i++) {
         MethodParameterDeclaration parameter
            = (MethodParameterDeclaration)parameters.elementAt(i);
         if (parameter.getDataType(env)==null
         || !parameter.getDataType(env).isResolved()) {
             return true;
         }
      }
      return false;
   }
*/

   public boolean isApplicableFor (CompilerEnvironment env, RefinementCallExpression c)
              throws ByteCodingException, IOException {
       //MethodCallParameterList actualParameters = c.parameterList  ();
       //ScriptCallFormalIndexList  actualIndexes = c.formalIndexList();
       //int nActualParameters = actualParameters==null? 0: actualParameters.parameterExpressions.size();
       //int nActualIndexes    =    actualIndexes==null? 0: actualIndexes   .parameterExpressions.size();
       
       ArrayList<JavaExpression> allActualParameters = c.getAllParameters (env);
       parseSignatureIfNeeded (env);

/*****
       if (this instanceof Script
       &&  c    instanceof ScriptCallExpression) {
           if (formalIndexes.length != nActualIndexes
           ||     parameters.length != nActualParameters) return false;
       }
instead of this, look at source declaration

       FormalIndex              formalIndexes[] = formalIndexes ();
       // for scripts these include scriptic.vm.Node and the indexes...

****/

       // NOT: FormalIndexOrParameter[] allParameters = allParameters();
       // because in case of a call to a constructor of a local or nested class,
       // the extra parameters are not in the RefinementCallExpression,
       // so don't consider those now
       if (parameters == null) return allActualParameters.isEmpty();
       if (parameters.length != allActualParameters.size()) return false;

       for (int i=0; i<parameters.length; i++) {
           JavaExpression q = allActualParameters.get(i);
           if (!q.dataType.isSubtypeOf (env, parameters[i].getDataType(env))) return false;
       }
       return true;
   }

   public  CodeAttribute code() {return attributesContainer==null?null:attributesContainer.getCode();}
   public void finishCodeForWriting() throws ByteCodingException
   {
     if (owner==null) System.out.println ("owner==null: "+getDescription());
     if (code()!=null) code().finishForWriting(owner);
   }

   public String getParameterSignature (ClassesEnvironment env) {
      StringBuffer result = new StringBuffer();
      result.append('(');
      parseSignatureIfNeeded (env);
      Parameter[] allParameters = allParameters();
      for (int i=0; i<allParameters.length; i++) {
         result.append(allParameters[i].getSignature(env));
      }
      result.append(')');
      return result.toString ();
   }

   public String getSignature (ClassesEnvironment env) {
      if (signature == null) {
          signature = getParameterSignature(env) + returnType.getSignature();
      }
      return signature;
   }

   public String getNameSignature (ClassesEnvironment env) {
                 return name + getParameterSignature (env);}

   public String getMnemonicCode () {
     if (code()==null) return "";
     return code().getMnemonicCode(owner);
   }

   // forget for a while about the return dimension...
   public String getReturnTypePresentation() {return returnType.getPresentation();}
   public String getChannelArrows   () {return "";}
   public String getPresentation    () {

      StringBuffer result = new StringBuffer ();
      result.append (getModifierString        ()).append (' ')
            .append (getReturnTypePresentation()).append (' ')
            .append (name);

      Parameter[] allParameters = allParameters();

      result.append (getChannelArrows());
      result.append('(');
      if (parameters != null)
      for (int i=0; i<parameters.length; i++) {
         if   (i>0) result.append(',');
         result.append (parameters[i].getPresentation());
      }
      result.append(')');

      if (exceptionTypes != null
      &&  exceptionTypes.length > 0) {

         result.append(" throws");
         for (int i=0; i<exceptionTypes.length; i++) {
            result.append(" ");
            result.append (exceptionTypes[i].getPresentation());
         }
      }

      return result.toString ();
   }

   /* ----------------------------- CodeGeneration ------------------------- */

   // convert to a constant pool 'methodReference' for a given constant pool owner.
   // Cache the answer because the constant pool owner may ask more than once.

   private ClassType createdForConstantPoolOwner;
   private ConstantPoolItem methodRef;
   public  ConstantPoolItem methodRef (ClassType constantPoolOwner) {
      if (methodRef==null  // cashed value per constant pool(owner)
      ||  createdForConstantPoolOwner != constantPoolOwner) {
          createdForConstantPoolOwner  = constantPoolOwner;
          if (owner.isInterface()) {
              methodRef =constantPoolOwner.resolveInterfaceMethodReference (owner.getNameWithSlashes(),
                                                             name,
                                                             signature);
          }
          else {
              methodRef =constantPoolOwner.resolveMethodReference (owner.getNameWithSlashes(),
                                                             name,
                                                             signature);
          }
      }
      return methodRef;
   }

   Instruction invokeInstruction (CompilerEnvironment env,
                                  int invokeInstructionCode,
                                  ClassType constantPoolOwner,
                                  InstructionOwner instructionOwner)
     throws ByteCodingException {
          parseSignatureIfNeeded (env);
          if (invokeInstructionCode==INSTRUCTION_invokeinterface) {
           // equally OK: if (typeDeclaration.isInterface())    ???
             return new InvokeinterfaceInstruction (methodRef(constantPoolOwner),
                                                    parameterSlots()+(isStatic()?0:1),
                                                    deltaStackSize(),
                                                    instructionOwner );
          }
          else {
             return new InvokeInstruction (invokeInstructionCode,
                                           methodRef(constantPoolOwner),
                                           deltaStackSize(),
                                           instructionOwner );
          }
   }

   public void write(CompilerEnvironment env, ClassEnvironment e, DataOutputStream out) throws IOException, ByteCodingException
   {
     out.writeShort(modifierFlags&0xFFFF);
     out.writeShort(e.resolveUnicode(name)     .slot);
     out.writeShort(e.resolveUnicode(signature).slot);
     if (dimensionSignature!=null) {
        addAttribute (new DimensionAttribute(dimensionSignature));
     }
     if (attributesContainer == null)
          out.writeShort(0);
     else {
        attributesContainer.write(e, out);
        //if (!env.doKeepMethodBodies)
        //   attributesContainer.deleteCodeAttribute(); cleanup comes later
     }
   }

   /** fill the parameterList and result type of the method.
    *  If the given ClassesEnvironment is null,
    *   no attempt is made to load a class type from file
    */
   public void parseSignature (ClassesEnvironment env,
                               boolean            loadAsWell) {
      ArrayList<Parameter> parameterList = new ArrayList<Parameter>();
      int length             = signature.length();
      IntHolder offset       = new IntHolder(1); // signature starts with '('

      for (int i=0; offset.value < length-2; i++) {

         char signatureChar = signature.charAt(offset.value);
         if (signatureChar == ')') {
            break;
         }
         Parameter parameter        = new Parameter();
         parameter.dataType1        = DataType.getFromSignature (env, signature, owner,
                                                                 offset, loadAsWell);
         parameter.owner            = this;
       //parameter.declarationIndex = i;
         parameterList.add (parameter);
      }
      offset.value++;
      parameters = new Parameter [parameterList.size()];
      for (int i=0; i<parameters.length; i++)
                      parameters[i] = parameterList.get(i);
      returnType = DataType.getFromSignature (env, signature, owner, offset, loadAsWell);
/*************
if (returnType instanceof ClassType) {
ClassType c=(ClassType)returnType;
System.out.println (
"//  "+returnType.getNameWithSlashes()+lineSeparator+
"..  "+returnType.getNameWithDots   ()+lineSeparator+
"nam "+returnType.getName           ()+lineSeparator+

"ps  "+c.packageNameWithSlashes+lineSeparator+
"pd  "+c.packageNameWithDots ()+lineSeparator+
"cn  "+c.className             +lineSeparator+
"sig "+signature);
   }
******************/

      parseDimensionSignature();
   }


   public void readFromStream (ClassesEnvironment env,
                               ClassType containingClass,
                               DataInputStream stream) throws IOException, ByteCodingException {
      owner               = containingClass;
      modifierFlags       = stream.readUnsignedShort ();
      int nameIndex       = stream.readUnsignedShort ();
      int signatureIndex  = stream.readUnsignedShort ();
/*******
System.out.println("Method.readFromStream"
+lineSeparator+"modifierFlags: "+modifierFlags
+lineSeparator+"nameIndex: "+nameIndex
+lineSeparator+"signatureIndex: "+signatureIndex
+lineSeparator+containingClass.getShortDescription()
+lineSeparator+containingClass.getConstantsDescription()
);
*******/
      name                = owner.getConstantPoolItem (     nameIndex).getName(owner);
      signature           = owner.getConstantPoolItem (signatureIndex).getName(owner);
      attributesContainer = AttributesContainer.readFromStream (this, owner, stream);
      DimensionAttribute da = attributesContainer.getDimension();
      if (da!=null) {
         dimensionSignature = da.signature;
      }
      if (env==null||env.doParseAllSignatures) getParameterSignature(env);
   }

   /** answer whether the throws clause covers a given classType */
   public boolean throwsClauseCoveres (CompilerEnvironment env, ClassType c)
              throws CompilerError, IOException {

     ClassType exceptionTypes[] = exceptionTypes(env);
     for (int i=0; i<exceptionTypes.length; i++) {

         if (c.isSubtypeOf (env, exceptionTypes[i])) {
             return true;
         }
      }

System.out.println (name+".throwsClauseCoveres NOT: "+c.className);
System.out.println (this);
for (int i=0; i<exceptionTypes.length; i++) 
System.out.println (exceptionTypes[i].nameWithDots);
System.out.println ("============================");

      return false;
   }

   /** answer whether the throws clause coveres another completely... */
   public boolean throwsClauseCoveres (CompilerEnvironment env, Method other)
              throws CompilerError, IOException {

      ClassType otherExceptionTypes[] = other.exceptionTypes(env);
      if (otherExceptionTypes != null) {
        for (int i=0; i<otherExceptionTypes.length; i++) {
          if (!throwsClauseCoveres (env, otherExceptionTypes[i])) {
             return false;
          }
        }
      }
      return true;
   }
}
