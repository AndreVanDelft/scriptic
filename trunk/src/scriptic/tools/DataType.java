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
import java.util.HashMap;

import scriptic.tokens.JavaRepresentations;
import scriptic.tools.lowlevel.*;


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        DataType                                 */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

public abstract class DataType
         implements scriptic.tokens.JavaTokens, JavaRepresentations, ClassFileConstants {
        ArrayType array;
   public boolean isReference       ()                {return false;}
   public boolean isArray           ()                {return false;}
   public boolean isClass           ()                {return false;}
   public boolean isFinal           ()                {return false;}
   public boolean isPublic          ()                {return false;}
   public boolean isInterface       ()                {return false;}
   public boolean isClassOrInterface()                {return false;}
   public boolean isNull            ()                {return false;}
   public boolean isPrimitive       ()                {return false;}
   public boolean isNumeric         ()                {return false;}
   public boolean isIntegral        ()                {return false;}
   public boolean isSmallIntegral   ()                {return false;}
   public boolean isSmallOrNormalInt()                {return false;}
   public boolean isBoolean         ()                {return false;}
   public boolean isVoid            ()                {return false;}
   public boolean isByte            ()                {return false;}
   public boolean isShort           ()                {return false;}
   public boolean isChar            ()                {return false;}
   public boolean isInt             ()                {return false;}
   public boolean isLong            ()                {return false;}
   public boolean isFloat           ()                {return false;}
   public boolean isDouble          ()                {return false;}
   public boolean isBig             ()                {return false;}
   public boolean canBePrimitiveWidenedTo(CompilerEnvironment env, DataType d)
                throws IOException, CompilerError {return false;}
   public boolean isResolved        ()                {return  true;}
   public boolean  hasError         ()                {return false;}
   public int     noOfArrayDimensions()               {return     0;}
   public int     getToken()                          {return     0;}
   public int     getArrayTypeNumber()                {return    -1;}
   public DataType promoteUnary     ()                {return  this;}
   public ClassType wrapperClass (CompilerEnvironment env) {return env.javaLangObjectType;} // take care!
   public ClassType  holderClass (CompilerEnvironment env) {return env.scripticVmObjectHolderType;}
   public String  accessNameForWrapperClass()          {return null;}

   public          byte     slots()                   {return 1;}
   public     Modifiers modifiers()                   {return null;}

   public short      storeInstructionCode()             {return INSTRUCTION_astore;}
   public short       loadInstructionCode()             {return INSTRUCTION_aload ;}
   public short  arrayLoadInstructionCode()             {return INSTRUCTION_aaload;}
   public short arrayStoreInstructionCode()             {return INSTRUCTION_aastore;}
   public short        dupInstructionCode()             {return INSTRUCTION_dup;}
   public short     dup_x1InstructionCode()             {return INSTRUCTION_dup_x1;}
   public short     dup_x2InstructionCode()             {return INSTRUCTION_dup_x2;}
   public short        popInstructionCode()             {return INSTRUCTION_pop;}
   public short     returnInstructionCode()             {return INSTRUCTION_areturn;}
   public short      unaryInstructionCode(int token)    {return     -1;}
   public short     binaryInstructionCode(int token)    {return     -1;}
   public short  convertToInstructionCode(DataType d)   {return     -1;}
   public short        cmpInstructionCode(boolean doGreater){return     -1;}
   public short    const_0InstructionCode()             {return     -1;}
   public short    const_1InstructionCode()             {return     -1;}

   /** resolve the given variable name, and check accessability from the given class */
   public MemberVariable resolveMemberVariable (CompilerEnvironment env, JavaExpression e)
                                                throws CompilerError, IOException {
       return resolveMemberVariable (env, e.name);
   }
   /** resolve the given variable name, and check accessability from the given class */
   public MemberVariable resolveMemberVariable (CompilerEnvironment env, String name)
                                                throws CompilerError, IOException {
       return null;
   }
   /** $15.11.2.1 Find Methods that are Applicable and Accessible
    * @return Hashtable of Methods
 * @throws ByteCodingException 
    */
   public HashMap<String, Object> findApplicableMethods (CompilerEnvironment env,
                                           ClassType callerClass,
                                           RefinementCallExpression c)
                                    throws IOException, ByteCodingException {
       return null;
   }

   /** answer the wider type for an array initializer
    *  so with {'c','d'} {(byte)0} the result will be IntType.makeArray(1)
    *  and with {null}, {{1.3d},null} the result will be DoubleType.makeArray(1)
    */
   public DataType widenForArrayInitializer (CompilerEnvironment env, DataType d) throws IOException, CompilerError {
       if (d.isSubtypeOf (env, this)) return this;
       if (  isSubtypeOf (env, d   )) return d;
       if ( noOfArrayDimensions()
       == d.noOfArrayDimensions()
       && d.baseType().isSmallIntegral()
       &&   baseType().isSmallIntegral()) { // one is char and the other short or byte
          return IntType.theOne.makeArray (d.noOfArrayDimensions());
       }
       return null;
   }

   /** return "java/lang/String" etc...
    * this method should be called either on resolved dataDeclarations
    * or from unresolved ones that originate from class files
    * so that the package part (if any) is known
    */
   public String getNameWithSlashes() {return getName();}

   /** return "java.lang.String" etc...
    * this method should be called either on resolved dataDeclarations
    * or from unresolved ones that originate from class files
    * so that the package part (if any) is known
    */
   public String getNameWithDots() {return getName();}

   public abstract NonArrayType baseType();
   public abstract boolean     isSubtypeOf   (CompilerEnvironment env, DataType d) throws IOException, CompilerError;
   public          boolean canBeAssignedTo   (CompilerEnvironment env, DataType d) throws IOException, CompilerError {return isSubtypeOf(env, d);}
   public abstract String whyCannotBeCastedTo(CompilerEnvironment env, DataType d) throws IOException, CompilerError;
   public abstract String  getName     ();
   public abstract String  getSignature();
   public          String  getNameForClassRef() {return null;}
   public ArrayType makeArray() {
      if (array==null) new ArrayType(this); // sets array
      return array;
   }
   public DataType makeArray(int n) {
      DataType result = this;
      for (int i=0; i<n; i++) result = result.makeArray();
      return result;
   }
   public String getSimpleSignature() {return "Ljava/lang/Object;";}
   /* ---------------------------- Presentation ------------------------ */

   public abstract String getPresentation ();
   public          String getDescription () {return getPresentation();}

   void setNameWithComponents(String s) {}

   // reference to CONSTANT_Class item
   public  ConstantPoolClass classRef (ClassType constantPoolOwner) {return null;}

   /** return a DataType corresponding to the given signature
    *  If the given ClassesEnvironment is null, no attempt is made to load a class type from file
    */
   public static DataType getFromSignature (ClassesEnvironment env,
                                            String             signature,
                                            ClassType          containingClass,
                                            boolean            loadAsWell) {
       return getFromSignature (env, signature, containingClass, new IntHolder(0), loadAsWell);
   }

   /** return a DataType corresponding to the contents at the offset of the
    *  given signature, while incrementing the offset with the number of signaturecharacters.
    *  If the given ClassesEnvironment is null, no attempt is made to load a class type from file
    */
   public static DataType getFromSignature (ClassesEnvironment env,
                                            String             signature,
                                            ClassType          containingClass,
                                            IntHolder          offset,
                                            boolean            loadAsWell ) {

      int noOfArrayBrackets = 0;
      while (offset.value < signature.length() - 1
         &&  signature.charAt(offset.value) == '[') {
         offset.value++;
         noOfArrayBrackets++;

         while (offset.value < signature.length() - 1
            &&  Character.isDigit(signature.charAt(offset.value)))
            offset.value++;
      }

      DataType baseType    = null;
      String signatureName = null;

      if (offset.value >= signature.length())
          throw new RuntimeException("offset ERROR in getFromSignature(\""+signature
                   +"\") value="+offset.value);

      char   signatureChar = signature.charAt(offset.value);
      offset.value++;

      switch (signatureChar) {
         case 'B' : baseType =    ByteType.theOne; break;
         case 'C' : baseType =    CharType.theOne; break; 
         case 'D' : baseType =  DoubleType.theOne; break;
         case 'F' : baseType =   FloatType.theOne; break;
         case 'I' : baseType =     IntType.theOne; break;
         case 'J' : baseType =    LongType.theOne; break;
         case 'S' : baseType =   ShortType.theOne; break;
         case 'Z' : baseType = BooleanType.theOne; break;
         case 'V' : baseType =    VoidType.theOne; break;
         case 'L' :
            int startPos = offset.value;
            while (offset.value < signature.length()
               &&  signature.charAt(offset.value) != ';')
               offset.value++;
            signatureName = signature.substring (startPos, offset.value);
            baseType = ClassType.getFromNameWithSlashes (env, signatureName, containingClass, loadAsWell);
            offset.value++;
            break;
         default:

            throw new RuntimeException("Unknown signature character: "+signatureChar
                                +" in "+signature
                                +"["+(offset.value-1)+"]");

      }
      return baseType.makeArray (noOfArrayBrackets);
   }
}
