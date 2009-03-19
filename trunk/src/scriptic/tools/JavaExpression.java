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

import java.io.IOException;

import scriptic.tools.lowlevel.Instruction;
import scriptic.tools.lowlevel.ByteCodingException;

   /*******************************************************************/
   /**                                                               **/
   /**                         EXPRESSIONS                           **/
   /**                                                               **/
   /*******************************************************************/

class JavaExpression extends LanguageConstruct {
   DataType dataType;
   String dimensionSignature;

   public JavaExpression () { }
   public JavaExpression (JavaExpression anotherExpression) { 
      super (anotherExpression);
   }

   public String getDescription() {
      StringBuffer result = new StringBuffer();
      result.append(name).append("  type: ");
      if (dataType==null) result.append ("**UNKNOWN* *");
      else result.append (dataType.getPresentation());
      if (dimensionSignature!=null) result.append ("\nDimension: ").append (dimensionSignature);
      if (constantValue != null)      
          result.append("\n\nconstant value: ").append (constantValue.makeString());
      return result.toString();
   }

   /* constantValue - often equal to null
    *                 sometimes this or a literal or string
    * Not used for unary or binary promotion
    */
   protected ConstantValue constantValue;
   public void   setConstantValue(byte    value ) {constantValue = new ConstantByte   (value);}
   public void   setConstantValue(char    value ) {constantValue = new ConstantChar   (value);}
   public void   setConstantValue(short   value ) {constantValue = new ConstantShort  (value);}
   public void   setConstantValue(int     value ) {constantValue = new ConstantInt    (value);}
   public void   setConstantValue(long    value ) {constantValue = new ConstantLong   (value);}
   public void   setConstantValue(float   value ) {constantValue = new ConstantFloat  (value);}
   public void   setConstantValue(double  value ) {constantValue = new ConstantDouble (value);}
   public void   setConstantValue(boolean value ) {constantValue = new ConstantBoolean(value);}
   public void   setConstantValue(String  value ) {constantValue = new ConstantString (value);}
   public void   setConstantValue(ConstantValue v){constantValue = v;}

   /* Predicates */
   public boolean isConstant      () {return constantValue!=null;}
   public boolean isTrue          () {return constantValue!=null && constantValue.isTrue ();}
   public boolean isFalse         () {return constantValue!=null && constantValue.isFalse();}
   public boolean isNull          () {return constantValue!=null && constantValue.isNull ();}
   public boolean isZero          () {return constantValue!=null && constantValue.isZero ();}
   public boolean isOne           () {return constantValue!=null && constantValue.isOne  ();}
   public boolean canBeAssigned   () {return false;}
   public boolean isFinal         () {return false;}
   public boolean isThis          () {return false;}
   public boolean isSuper         () {return false;}
   public boolean isPlainName     () {return false;}
   public boolean isOldIdentifier () {return false;}
   public boolean isQualifiedName () {return false;}
   public String    qualifiedName () {return  null;}

   public boolean isStringPlus    (CompilerEnvironment env) {return false;}
   public boolean isJavaExpressionWithTarget  () {return false;}
   public SpecialCode  getLastSpecialCodeInExpression () {return SpecialCode.none;}
   public void setLastSpecialCodeInExpression (SpecialCode newCode) {}

   public void    setMethodName    () {}
   public void    setExpressionName() {}
   public void    setPackageName   () {}
   public void    setTypeName      () {}
   public void    setAmbiguousName () {}
   public boolean  isMethodName    () {return false;}
   public boolean  isExpressionName() {return false;}
   public boolean  isPackageName   () {return false;}
   public boolean  isTypeName      () {return false;}
   public boolean  isAmbiguousName () {return true ;}

   protected boolean isTopLevelExpression = false;
   public boolean  isTopLevelExpression() {return isTopLevelExpression;}
   public void    setTopLevelExpression() {isTopLevelExpression = true;}

   // whether this is '0' or (double) '0' or so
   public boolean  isZeroLiteralOrPrimitiveCastOfZero () {
     if (!isZero()) return false;
     if (languageConstructCode() == LiteralExpressionCode) return true;
     if (languageConstructCode() != PrimitiveCastExpressionCode) return false;
     return ((PrimitiveCastExpression)this)
                .unaryExpression.languageConstructCode() == LiteralExpressionCode;
   }

   public final JavaExpression promoteUnary()
   {
       if (dataType.isSmallIntegral()) {
              PrimitiveCastExpression result = new PrimitiveCastExpression();
              result.unaryExpression     = this;
              result.nameStartPosition   = nameStartPosition;
              result.nameEndPosition     = nameEndPosition;
              result.sourceStartPosition = sourceStartPosition;
              result.sourceEndPosition   = sourceEndPosition;
              result.dimensionSignature  = dimensionSignature;
              result.castToken           = IntToken;
              result.dataType            = IntType.theOne;
              result.constantValue       = constantValue==null? null: constantValue.promoteUnary();
              return result;
       }
       return this;
   }
   public final JavaExpression promoteBinary(CompilerEnvironment env, DataType other) throws IOException, CompilerError
   {
       if (!   other.isPrimitive()) return this;
       if (!dataType.isPrimitive()) return this;
       if (!dataType.isSubtypeOf (env, other)) return this;

       DataType newDataType = null;

       if  (dataType == other) {
         if(dataType.isSmallIntegral()) return promoteUnary();
         else return this;
       }

       switch (other.getToken()) {
       case   ByteToken:
       case   CharToken:
       case  ShortToken: 
       case    IntToken: return promoteUnary();

       case   LongToken:                        newDataType =   LongType.theOne; //NO break...
       case  FloatToken: if (newDataType==null) newDataType =  FloatType.theOne; //NO break...
       case DoubleToken: if (newDataType==null) newDataType = DoubleType.theOne;
              PrimitiveCastExpression result = new PrimitiveCastExpression();
              result.unaryExpression     = this;
              result.nameStartPosition   = nameStartPosition;
              result.nameEndPosition     = nameEndPosition;
              result.sourceStartPosition = sourceStartPosition;
              result.sourceEndPosition   = sourceEndPosition;
              result.dimensionSignature  = dimensionSignature;
              result.castToken           = other.getToken();
              result.dataType            = newDataType;
              result.constantValue       = constantValue==null
                                         ? null
                                         : constantValue.promoteBinary(other);
              return result;
       }
       return this;
   }
   
   /** Assignment compatibility - $5.2 */
   public boolean canBeAssignedTo(CompilerEnvironment env, DataType target) throws IOException, CompilerError {
     if (dataType.isSubtypeOf (env, target)) return true;
     if (dataType.isInt() && isConstant()) {
       if (target.isByte ()) return constantValue.canBeRepresentedAsByte ();
       if (target.isChar ()) return constantValue.canBeRepresentedAsChar ();
       if (target.isShort()) return constantValue.canBeRepresentedAsShort();
     }
     return false;
   }

   /* asserts that canBeAssignedTo gives true! */
   public JavaExpression convertForAssignmentTo(CompilerEnvironment env, DataType target)
    throws IOException, CompilerError {
       if (!dataType.isPrimitive()) return this;
       if (!  target.isPrimitive()) return this;
       if ( dataType.isSubtypeOf(env, target)) return convertPrimitiveWideningTo(target);
       if ( dataType.isInt() && isConstant()) {
              PrimitiveCastExpression result = new PrimitiveCastExpression();
              result.unaryExpression     = this;
              result.nameStartPosition   = nameStartPosition;
              result.nameEndPosition     = nameEndPosition;
              result.sourceStartPosition = sourceStartPosition;
              result.sourceEndPosition   = sourceEndPosition;
              result.dimensionSignature  = dimensionSignature;
              result.castToken           = target.getToken();
              result.dataType            = target;
              result.constantValue       = constantValue==null
                                         ? null
                                         : constantValue.convertTo(target.getToken());
              return result;
       }
       return this;
   }

   /* asserts that dataType.isSubtypeOf(target.dataType)! */
   public JavaExpression convertPrimitiveWideningTo (DataType target)
   {
       if (!dataType.isPrimitive()) return this;
       if (!  target.isPrimitive()) return this;
       if ( dataType.getToken() != target.getToken()) {
              PrimitiveCastExpression result = new PrimitiveCastExpression();
              result.unaryExpression     = this;
              result.nameStartPosition   = nameStartPosition;
              result.nameEndPosition     = nameEndPosition;
              result.sourceStartPosition = sourceStartPosition;
              result.sourceEndPosition   = sourceEndPosition;
              result.dimensionSignature  = dimensionSignature;
              result.castToken           = target.getToken();
              result.dataType            = target;
              result.constantValue       = constantValue==null
                                         ? null
                                         : constantValue.convertTo(target.getToken());
              return result;
       }
       return this;
   }
   Instruction []     storeInstructions(CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException, IOException {throw new ByteCodingException("Illegal attempt on storeInstructions");}
   Instruction []     loadInstructions(CompilerEnvironment env, ClassType constantPoolOwner) throws ByteCodingException, IOException {throw new ByteCodingException("Illegal attempt on loadInstructions");}
   Instruction          newInstruction(ClassType constantPoolOwner) throws ByteCodingException {throw new ByteCodingException("Illegal attempt on newInstruction");}
   Instruction dupReferenceInstruction() throws ByteCodingException {throw new ByteCodingException("Illegal attempt on dupReferenceInstruction");}
   Instruction   dup_xValueInstruction() throws ByteCodingException {return new Instruction (dataType.dup_x1InstructionCode(), this);}
   Instruction dupForAssignmentInstruction() throws ByteCodingException {return dup_xValueInstruction();}
}
