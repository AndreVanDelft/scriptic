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
import java.util.ArrayList;
import java.util.HashMap;

import scriptic.tools.lowlevel.ByteCodingException;


/**
 * Check expressions - determine type and constantness
 */
public class ExpressionChecker extends ScripticParseTreeEnumeratorWithContext
               implements scriptic.tokens.ScripticTokens {

   public ExpressionChecker (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
   }

   /** 
    * answer whether the dimension signatures are equal.
    * null is equivalent for an empty string.
    */
   boolean equalSignatures (String signature1, String signature2) {
     if (signature1         ==null
     ||  signature1.length()==0) {
        return signature2         ==null
            || signature2.length()==0;
     }
     if (signature2==null) return false;
     return signature1.equals (signature2);
   }

   /**
    * See whether the given name is available as a method,
    * with any number of parameters.
    * Useful for messages like 'attempt to reference method as variable'
    */
   public Method findARefinement (DataType d, JavaExpressionWithTarget e) throws CompilerError {
      // JavaExpressionWithTarget for it is a super of FieldAccessExpression and NameExpression
      if (!(d instanceof ClassType)) return null;
      HashMap<String, Object> h = ((ClassType)d).getMethodNamesSignatures(env).get(e.name);
      if (h==null) return null;
      return (Method) h.values().iterator().next();
   }

   /**
    * $15.8 Constructor Invocation Expressions
    * Does not return ConstructorDeclaration, since processed class files
    * don't contain those, but MethodDeclarations instead... 
    */
   public Method resolveConstructorCall (ClassType targetClass,
                                         MethodOrConstructorCallExpression mcall,
                                         boolean isForAllocation)
                              throws ByteCodingException {

    Method result  = null;
    try {
     ArrayList<Method> applicableConstructors = targetClass.findApplicableConstructors (env, classType, mcall, isForAllocation); // step 2

     if (applicableConstructors.isEmpty()) {
         if (mcall.parameterList==null
         ||  mcall.parameterList.parameterExpressions.size()==0) {
            parserError (2, "Class does not have parameterless constructor available",
                          mcall. sourceStartPosition,
                          mcall. sourceEndPosition);
         }
         else {
            parserError (2, "Unresolved constructor call",
                          mcall. sourceStartPosition,
                          mcall. sourceEndPosition);
        }
        return null;
      }
      for (Method m: applicableConstructors) {
         if (result==null)
             result = m;
         else {
             // $15.11.2.2 Choose the Most Specific Method
             if (result.isMoreSpecificThan (env,m     )) ; // just OK
             else if (m.isMoreSpecificThan (env,result)) result = m;
             else {
                parserError (2, "Ambiguous constructor call; both " + m.getPresentation()
                            +" and "                        + result.getPresentation(),
                            mcall. sourceStartPosition,
                            mcall. sourceEndPosition);
             }
         }
      }
      if (result.isDeprecated()) {
          parserError (1, "Warning: call to deprecated constructor",
                         mcall. sourceStartPosition,
                         mcall. sourceEndPosition);
      }
      mcall.setTarget (env, typeDeclaration, result);
      checkParameterDimensions (mcall);

    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (IOException   e) {parserError (2, e.getMessage(), mcall.nameStartPosition, 
                                                            mcall.  nameEndPosition);
    }
    return result;
   }

   /**
    * $15.11 Method Invocation Expressions
    * 
    */
   public Method resolveRefinementCall (RefinementCallExpression mcall,
                                        String refinementType) throws IOException, ByteCodingException {

//System.out.println("resolveRefinementCall: "+((LanguageConstruct)mcall).name);

     Method result = null;
     HashMap<String, Object> applicableRefinements = null;

     // $15.11.1 Compile-Time Step 1: Determine Class or Interface to Search
     // $15.11.2 Compile-Time Step 2: Determine Method Signature
     // $15.11.2.1 Find Methods that are Applicable and Accessible
     // $15.11.2.2 Choose the Most Specific Method
     // $15.11.3 Compile-Time Step 3: Is the Chosen Method Appropriate? 
     // Note: compound names have been parsed as field access expressions,
     // so things are a bit different here...

     JavaExpressionWithTarget a = mcall.accessExpression();

     // if (a.isMethodName()) {    not needed, because set so before...

     ClassType c = classType; // for NameExpression
     if  (a.languageConstructCode()==FieldAccessExpressionCode) {

         FieldAccessExpression f = (FieldAccessExpression) a;

         if (f.primaryExpression.isPackageName()) {
                parserError (2, "Unresolved target for "+refinementType+" call",
                     f.primaryExpression.sourceStartPosition,
                     f.primaryExpression.sourceEndPosition);
                return null;
         }
         else if (f.primaryExpression.isTypeName()) {

             //if (f.primaryExpression.dataType.hasError()) {
             //    // probably already handled
             //   return null;
             //}
             //else
              if (f.primaryExpression.dataType.isInterface()) {
                parserError (2, "Refinements cannot be called at interfaces",
                     f. sourceStartPosition,
                     f. sourceEndPosition);
                return null;
             }
             else {
                 mcall.setStaticMode(); // just a flag for the time being; see below...
                 c = (ClassType)f.primaryExpression.dataType;
             }
         }
         else { // super. or 'ordinary' method call

             if (f.primaryExpression.isSuper()) {
                mcall.setSuperMode(); // will be used below...
             }
             //else {
                // ????????this is not nice here, but I don't see another solution yet...
                // processJavaExpression (fieldDeclaration, f.primaryExpression, null, 0, null, null);
             //}
             else if (f.primaryExpression.dataType==UnresolvedClassOrInterfaceType.one) {
                 // prevent message "Unresolved "+refinementType+" call "
                 return null;
/***************
if (f.primaryExpression.dataType==UnresolvedClassOrInterfaceType.one
||((ClassType)f.primaryExpression.dataType).className.equals("<unresolved>")) {
System.out.println("ERROR: f.primaryExpression.dataType unresolved, in:\n"
+f.                  getDescription()+" ****\n"
+f.primaryExpression.getDescription()+" ////\n"
+f.primaryExpression.qualifiedName ()+" %%%%\n"
+f.primaryExpression.getDescription());
//return null;
}
This kind of error occurs e.g. for a.b.call() where b is not a's field.
So the error will have been flagged earlier
**********/
             }
             //if (f.primaryExpression.dataType.hasError()==null) {
             //    // probably already handled
             //   return null;
             //}
             c = f.primaryExpression.dataType.isArray()
               ? env.javaLangObjectType
               : (ClassType) f.primaryExpression.dataType;
         }
     }
     for (ClassType d=c; d != null; d=d.parent()) {
         applicableRefinements = d.findApplicableMethods (env, classType, mcall); // step 2
         if (!applicableRefinements.isEmpty()) break;
     }
     if (applicableRefinements.isEmpty()) {
    	 applicableRefinements = context.findApplicableStaticallyImportedMethods (env, classType, mcall);
     }
     if (applicableRefinements.isEmpty()) {
         parserError (2, "Unresolved "+refinementType+" call ",
              mcall. sourceStartPosition(),
              mcall. sourceEndPosition  ());
         return null;
     }

     // $15.11.2.2 Choose the Most Specific Method.
     // First take one; class origin prevails a bit to interface
     // so 
     for (Object obj: applicableRefinements.values()) {
         Method r = (Method) obj;
         if (result==null
         || !result.isMoreSpecificThan (env, r)
         ||  result.owner.isInterface()) result = r;
     }
     // now see whether there is a disturbing other one
     for (Object obj: applicableRefinements.values()) {
         Method r = (Method) obj;
         if (result == r) continue;
         if (!result.isMoreSpecificThan (env, r)) {
             // two possible errors...
             if (result.owner.isClass()
             &&  r     .owner.isClass()) {
                parserError (2, "Ambiguous "+refinementType+" call; both " + r.getPresentation()
                            +" and "                       + result.getPresentation(),
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
                break;
             }
             else if (result.returnType != r.returnType) {
                parserError (2, "Ambiguous return type from interface "+refinementType+" call; both "
                            + r.getPresentation() +" and " + result.getPresentation(),
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
                break;
             }
         }
     }
     // also check whether there are not two most specific methods from interfaces
     // with different return types...


     // $15.11.3 Compile-Time Step 3: Is the Chosen Method Appropriate?
     if (a.languageConstructCode()==NameExpressionCode
     &&  context.isStatic()
     &&  !result.isStatic()) {
                parserError (2, "Cannot call instance method from static context",
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
     }
     else if (mcall.isStaticMode() // flags call like Type.Identifier(parameters)
     && !result.isStatic()) {
                parserError (2, "Cannot call instance method at class or interface",
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
     }
     if (result.isDeprecated()) {
          parserError (1, "Warning: call to deprecated method: "+result.getPresentation(),
                      mcall. sourceStartPosition(),
                      mcall. sourceEndPosition  ());
     }

     // check for void calls is not needed here...will come out elsewhere during expression checking


/************* to be changed; look at sourceDeclaration to find out about scripts *********
     // if both parties are scripts, then we may check for channel directives...
     if (result instanceof BasicScriptDeclaration
     &&  mcall  instanceof ScriptCallExpression) {
           if (mcall.isScriptChannelSend()
           ||  mcall.isScriptChannelReceive()) {
              if (result.languageConstructCode() != ChannelDeclarationCode) {
                parserError (2, "Misplaced attempt to use script as channel",
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
              }
              ChannelDeclaration chan = (ChannelDeclaration) result;
              if (chan.   isSendChannel && mcall.isScriptChannelReceive()) {
                parserError (2, "Attempt to receive over send channel",
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
              }
              if (chan.isReceiveChannel && mcall.isScriptChannelSend()) {
                parserError (2, "Attempt to send over receive channel",
                            mcall. sourceStartPosition(),
                            mcall. sourceEndPosition  ());
              }
           }
       }
************* END to be changed *********/

     if      (result.isStatic ()) mcall.setStaticMode();
     else {

        // not static; is 'this' implicitly used? Then flag so
        if (a.languageConstructCode()==NameExpressionCode) {
        }
            if (result.       isPrivate ()) mcall.setSpecialMode();
       else if ( mcall.      isSuperMode()); //setSuperMode has appearently already happened...
       else if (result.owner.isInterface()) mcall.setInterfaceMode();
       else                                 mcall.setVirtualMode();
     }

     mcall.setTarget (env, typeDeclaration, result);
     checkParameterDimensions (mcall);
     return result;
   }

   void checkParameterDimensions (RefinementCallExpression mcall) {

        boolean   hasError = false;
        ArrayList<JavaExpression> ps = mcall.getAllParameters(env);
        HashMap<String,String> bindings = new HashMap<String,String>();

//System.out.println (mcall.getName()+" dimension: "+mcall.target().dimensionSignature);

         
        // Special treatment for System.arraycopy()...
        if (mcall.target().name.equals("arraycopy")
        &&  mcall.target().ownerClass((CompilerEnvironment)env) == ((CompilerEnvironment)env).javaLangSystemType)
        {
            String dimensionSignature0 = null;
            for (int i=0; i<ps.size(); i++) {
                JavaExpression je = ps.get (i);
                //FormalIndexOrParameter p = mcall.target().parameters[i];
                switch(i) {
                case 0: dimensionSignature0 = je.dimensionSignature;
                        break;
                case 2: if (!Dimension.equal(dimensionSignature0, je.dimensionSignature))
                        {
                           parserError (2, "Parameters 1 and 3 must have equal dimension",
                                            je. sourceStartPosition(),
                                            je. sourceEndPosition  ());
                        }
                        break;
                case 1:
                case 4:
                case 5: if (je.dimensionSignature != null) { // the second test is not needed...
                            parserError (2, "Dimensionless number expected",
                                         je. sourceStartPosition(),
                                         je. sourceEndPosition  ());
                        }
                        break;
                }
           }
        }
        else
        {
            for (int i=0; i<ps.size(); i++) {
                JavaExpression je = ps.get (i);
                Parameter p = mcall.target().parameters[i];

        //System.out.println (i+": "+je.name+" dimension: "+je.dimensionSignature
        //              +" formal: "+ p.name+" dimension: "+ p.dimensionSignature);

                if (je.dimensionSignature == Dimension.errorSignature) {
                    hasError = true;
                }
                if (Dimension.equal (je.dimensionSignature, p.dimensionSignature)) {
                    continue;
                }
                if (p.dimensionSignature == null
                && je.dimensionSignature != null) { // the second test is not needed...
                        parserError (2, "Dimensionless number expected",
                                    je. sourceStartPosition(),
                                    je. sourceEndPosition  ());
                        continue;
                }

                if (p.dimensionSignature.charAt(0) == 'V') {
                        if (je.dimensionSignature==null) je.dimensionSignature = "";
                        bindings.put (p.dimensionSignature, je.dimensionSignature);
                        continue;
                }
                String signature = Dimension.resolve (p.dimensionSignature, bindings);
                if (!Dimension.equal (je.dimensionSignature, signature)) {
                        parserError (2, "Incompatible dimension: "
                                +Dimension.getPresentation(je.dimensionSignature)
                    +" formal: " +Dimension.getPresentation(p .dimensionSignature),
                                    je. sourceStartPosition(),
                                    je. sourceEndPosition  ());
                }
            }
        }
        if (hasError) mcall.setDimensionSignature (Dimension.errorSignature);
        else mcall.setDimensionSignature (Dimension.resolve (mcall.target().returnDimensionSignature, bindings));
   }

   public RefinementDeclaration resolveConstructor(TypeDeclaration t, MethodCallParameterList l)
                                                                              throws CompilerError {
       return null;
   }


   protected void processJavaExpression (FieldDeclaration fieldDeclaration, JavaExpression e, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processJavaExpression (fieldDeclaration, e, level, retValue, arg1, arg2);
      if (e.dataType==null) e.dataType = UnresolvedClassOrInterfaceType.one;
   }
   protected void processLayoutExpression          (FieldDeclaration fieldDeclaration, LayoutExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /* Default is to completely skip/ignore the LayoutExpression.
         To do anything with it, this method must be explicitly overridden. */
      processJavaExpression (fieldDeclaration, expression.realExpression, level, retValue, arg1, arg2);
   }

   protected void processBinaryExpression         (FieldDeclaration fieldDeclaration, BinaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /*
      *, /,  %
      + - 
      < > <= >=    (numeric - binary promotion)
      == !=        (numeric - binary promotion, boolean, reference)
      & ^ |        (integer - binary promotion, boolean)
      || &&        (                            boolean)
      <<, >>,  >>> (integral - unary promotion)
      */

      switch (expression.operatorToken) {
          case BooleanOrToken         :
          case BooleanAndToken        :
          case EqualsToken            :
          case NotEqualToken          : 
          case GreaterThanToken       :
          case LessThanToken          :
          case LessOrEqualToken       :
          case GreaterOrEqualToken    : expression.dataType = BooleanType.theOne;
      }
      processJavaExpression (fieldDeclaration, expression. leftExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
      if (!expression. leftExpression.dataType.isResolved()) return;
      if (!expression.rightExpression.dataType.isResolved()) return;
      switch (expression.operatorToken) {
      case BooleanOrToken:
      case BooleanAndToken: // these two are special since they may yield constant values
                            // if only the first value is constant (and it is true for || or false for &&)
          if  (expression. leftExpression.dataType!=BooleanType.theOne)
                  parserError (2, "Boolean operator requires boolean operand",
                        expression. leftExpression.sourceStartPosition,
                        expression. leftExpression.sourceEndPosition);
          if  (expression.rightExpression.dataType!=BooleanType.theOne) 
                  parserError (2, "Boolean operator requires boolean operand",
                        expression.rightExpression.sourceStartPosition,
                        expression.rightExpression.sourceEndPosition);
          if  (expression.leftExpression.constantValue!=null) {
           if (expression.leftExpression.constantValue.isTrue()) {
            if(expression.operatorToken==BooleanOrToken) {
              expression.constantValue =ConstantBoolean.True;
            } else {
              expression.constantValue =expression.rightExpression.constantValue;
            }
           } else if (expression.leftExpression.constantValue.isFalse()) {
            if(expression.operatorToken==BooleanAndToken) {
              expression.constantValue =ConstantBoolean.False;
            } else {
              expression.constantValue =expression.rightExpression.constantValue;
            }
           }
          } else { // it must be ConstantMaybe.theOne ... check the right expression ...
           if  (expression.rightExpression.constantValue!=null) {
                expression.constantValue =ConstantMaybe.theOne;
           }
          }
          return;
      }
      if ( expression.operatorToken==PlusToken
      && ( expression. leftExpression.dataType == env.javaLangStringType
         ||expression.rightExpression.dataType == env.javaLangStringType)) {
           expression                .dataType =  env.javaLangStringType;
           //    ???????? :
           if (expression. leftExpression.dataType != CharType.theOne)
               expression. leftExpression = expression. leftExpression.promoteUnary();
           if (expression.rightExpression.dataType != CharType.theOne)
               expression.rightExpression = expression.rightExpression.promoteUnary();
           if (expression. leftExpression.dimensionSignature != null
           &&  expression. leftExpression.dimensionSignature != Dimension.errorSignature) {
              parserError (2, "Attempt to make string of operand that has dimension: "
                           +Dimension.getPresentation(expression. leftExpression.dimensionSignature),
                           expression.leftExpression.sourceStartPosition,
                           expression.leftExpression.sourceEndPosition);
           }
           if (expression.rightExpression.dimensionSignature != null
           &&  expression.rightExpression.dimensionSignature != Dimension.errorSignature) {
              parserError (2, "Attempt to make string of operand that has dimension: "
                           +Dimension.getPresentation(expression. rightExpression.dimensionSignature),
                           expression.rightExpression.sourceStartPosition,
                           expression.rightExpression.sourceEndPosition);
           }

      } else {
           checkOperandsAndPromote (expression);
      }
      switch (expression.operatorToken) {
          case BooleanOrToken         :
          case BooleanAndToken        : 
          case EqualsToken            :
          case NotEqualToken          : 
          case GreaterThanToken       :
          case LessThanToken          :
          case LessOrEqualToken       :
          case GreaterOrEqualToken    : /*already done*/ break;
          case PlusToken              : if (expression.dataType==env.javaLangStringType) break; // !!!!
          default:                      expression.dataType = expression.leftExpression.dataType; break;
      }

      if  (expression. leftExpression.constantValue==null 
      ||   expression.rightExpression.constantValue==null) return;

      if  (expression. leftExpression.constantValue==ConstantMaybe.theOne 
      ||   expression.rightExpression.constantValue==ConstantMaybe.theOne) {
           expression.                constantValue =ConstantMaybe.theOne;
           return;
      }

      try {
        switch (expression.operatorToken) {
        case LeftShiftToken         :
        case RightShiftToken        :
        case UnsignedRightShiftToken:
            expression.constantValue = expression. leftExpression.constantValue.doShiftOperator(
                                       expression.operatorToken,
                                       expression.rightExpression.constantValue);
            break;
        case PlusToken:
            if (expression.dataType == env.javaLangStringType) {
              expression.constantValue = new ConstantString (
                                           expression. leftExpression.constantValue.makeString()
                                         + expression.rightExpression.constantValue.makeString());
               break;
            }
            // NO break

        default:
            expression.constantValue = expression. leftExpression.constantValue.doBinaryOperator(
                                       expression.operatorToken,
                                       expression.rightExpression.constantValue);
            break;
        }
      } catch (Exception e) {
         parserError (2, e.toString(), expression.sourceStartPosition, expression.sourceEndPosition);
      }
   }


   protected void processAssignmentExpression     (FieldDeclaration fieldDeclaration, AssignmentExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      // in case of a script context, catches NameExpressions on the
      // left hand side of an assignment, and to call 
      // processAssignedNameExpression for that special case;
      // meant for "priority=10" etc
      if (context.isScriptContext()
       && expression.leftExpression instanceof NameExpression)
         processAssignedNameExpression (fieldDeclaration, expression.operatorToken, (NameExpression)expression.leftExpression, level + 1, retValue, arg1, arg2);
      else
         processJavaExpression(fieldDeclaration, expression. leftExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression   (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);

      expression.targetExpression = expression.leftExpression;
      expression.dataType         = expression.targetExpression.dataType;

if (expression. leftExpression.dataType==null) {
System.out.println("expression. leftExpression.dataType==null"+expression. leftExpression.getPresentation());
}
      if (!expression. leftExpression.dataType.isResolved()) return;
      if (!expression.rightExpression.dataType.isResolved()) return;

      if (!expression.targetExpression.canBeAssigned()) {
           parserError (2, "Variable expected",
                         expression.targetExpression.sourceStartPosition,
                         expression.targetExpression.sourceEndPosition);
           return;
      }
      if (expression.targetExpression.isFinal()) {
                  parserError (2, "Cannot assign to final variable",
                                expression.leftExpression.sourceStartPosition,
                                expression.leftExpression.sourceEndPosition);
                  return;
      }
      if (expression.operatorToken == AssignToken) {
           try {
              if (!expression.rightExpression.canBeAssignedTo(env, expression.targetExpression.dataType)) {
                   parserError (2, "Incompatible types for assignment",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                   return;
              }
              expression.rightExpression = expression. rightExpression.convertForAssignmentTo
                                     (env, expression.targetExpression.dataType);

              // dimension checking

              // 0 may be assigned to any dimensioned number...
              if ( expression.rightExpression.dimensionSignature==null
              &&   expression.rightExpression.isZeroLiteralOrPrimitiveCastOfZero()) {
                 expression.dimensionSignature = expression. leftExpression.dimensionSignature;
              }
              else if (!Dimension.equal (expression. leftExpression.dimensionSignature,
                                         expression.rightExpression.dimensionSignature)) {
                    // ...other clashes are prohibited
                    expression.dimensionSignature = Dimension.errorSignature;
                    parserError (2, "Operands of '=' have different dimensions: "
                           +Dimension.getPresentation(expression. leftExpression.dimensionSignature)
                  +" and " +Dimension.getPresentation(expression.rightExpression.dimensionSignature),
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
              }
              else { // equal dimensions
                 expression.dimensionSignature = expression. leftExpression.dimensionSignature;
              }

           } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
           } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                 expression.  sourceEndPosition);
           }
      } else if (expression.operatorToken  == PlusAssignToken
             &&  expression.dataType       == env.javaLangStringType) {
                 expression.rightExpression = expression.rightExpression.promoteUnary();
             if (expression.rightExpression.dimensionSignature != null
             &&  expression.rightExpression.dimensionSignature != Dimension.errorSignature) {
                parserError (2, "Attempt to make string of operand that has dimension: "
                           +Dimension.getPresentation(expression. rightExpression.dimensionSignature),
                             expression.rightExpression.sourceStartPosition,
                             expression.rightExpression.sourceEndPosition);
            }
      } else {

          // $ 15.25.2 Compound Assignment Operators
          // E1 op= E2 is equivalent to E1 = (T)((E1) op (E2))

          checkOperandsAndPromote (expression);
      } 
   }

   void checkOperandsAndPromote (BinaryExpression expression) {

        try {

          // check operand types, perform applicable promotion...

          switch (expression.operatorToken) {
          case EqualsToken            :
          case NotEqualToken          : // both reference OR boolean OR numeric
                 expression. leftExpression = expression. leftExpression.promoteBinary(env, expression.rightExpression.dataType);
                 expression.rightExpression = expression.rightExpression.promoteBinary(env, expression. leftExpression.dataType);

                 if (expression. leftExpression.dataType.isReference()
                 !=  expression.rightExpression.dataType.isReference()
                 || !expression. leftExpression.dataType.isReference()
                 &&  expression. leftExpression.dataType
                 !=  expression.rightExpression.dataType) {
                      parserError (2, "Equality operator expects pair of either numeric, boolean or reference operands",
                                    expression.sourceStartPosition,
                                    expression.sourceEndPosition);
                      return;
                 }
                 if (expression. leftExpression.dataType.isReference()) {
                     String reason1 = expression. leftExpression.dataType.whyCannotBeCastedTo
                                (env, expression.rightExpression.dataType);
                     if (reason1!=null) {
                       String reason2 = expression.rightExpression.dataType.whyCannotBeCastedTo
                                  (env, expression. leftExpression.dataType);
                       if (reason2!=null) {
                         parserError (2, "Operands cannot possibly be equal since their types are incompatible for casting"
                                     + ( reason1.equals(reason2)
                                       ? (" "+reason1)
                                       : (" (from left to right: " + reason1
                                         +"; from right to left: " + reason2+")")
                                       ),
                                      expression.sourceStartPosition,
                                      expression.sourceEndPosition);
                         return;
                       }
                     }
                 }
                 break;

          case GreaterThanToken       :
          case LessThanToken          :
          case LessOrEqualToken       :
          case GreaterOrEqualToken    :
          case PlusToken              :
          case MinusToken             :
          case AsteriskToken          :
          case SlashToken             :
          case PercentToken           : // both numeric

          case               PlusAssignToken:
          case              MinusAssignToken:
          case           AsteriskAssignToken:
          case              SlashAssignToken:
          case            PercentAssignToken:

                 expression. leftExpression = expression. leftExpression.promoteBinary(env, expression.rightExpression.dataType);
                 expression.rightExpression = expression.rightExpression.promoteBinary(env, expression. leftExpression.dataType);
                 if (!expression. leftExpression.dataType.isNumeric()) {
                      parserError (2, "Operator expects numeric operand",
                                    expression.leftExpression.sourceStartPosition,
                                    expression.leftExpression.sourceEndPosition);
                      return;
                 }
                 if (!expression.rightExpression.dataType.isNumeric()) {
                      parserError (2, "Operator expects numeric operand",
                                    expression.rightExpression.sourceStartPosition,
                                    expression.rightExpression.sourceEndPosition);
                      return;
                 }
                 break;

          case AmpersandToken         :
          case VerticalBarToken       :
          case CaretToken             : // both boolean OR integral

          case          AmpersandAssignToken:
          case        VerticalBarAssignToken:
          case              CaretAssignToken:

                 expression. leftExpression = expression. leftExpression.promoteBinary(env, expression.rightExpression.dataType);
                 expression.rightExpression = expression.rightExpression.promoteBinary(env, expression. leftExpression.dataType);
                 if (!expression. leftExpression.dataType.isIntegral()
                 &&  !expression. leftExpression.dataType.isBoolean ()) {
                      parserError (2, "Operator expects boolean or integral operand",
                                    expression.leftExpression.sourceStartPosition,
                                    expression.leftExpression.sourceEndPosition);
                      return;
                 }
                 if (!expression.rightExpression.dataType.isIntegral()
                 &&  !expression.rightExpression.dataType.isBoolean ()) {
                      parserError (2, "Operator expects boolean or integral operand",
                                    expression.rightExpression.sourceStartPosition,
                                    expression.rightExpression.sourceEndPosition);
                      return;
                 }
                 if (expression. leftExpression.dataType
                 !=  expression.rightExpression.dataType) {
                      parserError (2, "Operator expects pair of either boolean or integral operands",
                                    expression.sourceStartPosition,
                                    expression.sourceEndPosition);
                      return;
                 }
                 break;
 
          case LeftShiftToken         :
          case RightShiftToken        :
          case UnsignedRightShiftToken: // both integral
          case          LeftShiftAssignToken:
          case         RightShiftAssignToken:
          case UnsignedRightShiftAssignToken:

                 expression. leftExpression = expression. leftExpression.promoteUnary();
                 expression.rightExpression = expression.rightExpression.promoteUnary();
                 if (!expression. leftExpression.dataType.isIntegral()) {
                      parserError (2, "Shift operator expects integral operand",
                                    expression.leftExpression.sourceStartPosition,
                                    expression.leftExpression.sourceEndPosition);
                      return;
                 }
                 if (!expression.rightExpression.dataType.isIntegral()) {
                      parserError (2, "Shift operator expects integral operand",
                                    expression.rightExpression.sourceStartPosition,
                                    expression.rightExpression.sourceEndPosition);
                      return;
                 }
                 break;
          }

          // check dimensionality:
          switch (expression.operatorToken) {
          case EqualsToken            :
          case NotEqualToken          :
          case GreaterThanToken       :
          case LessThanToken          :
          case LessOrEqualToken       :
          case GreaterOrEqualToken    :
          case PlusToken              :
          case MinusToken             :
          case PercentToken           :
          case PlusAssignToken        :
          case MinusAssignToken       :
          case PercentAssignToken     :

                 if ((  expression. leftExpression.dimensionSignature!=null
                    || !expression. leftExpression.isZeroLiteralOrPrimitiveCastOfZero())
                 &&  (  expression.rightExpression.dimensionSignature!=null
                    || !expression.rightExpression.isZeroLiteralOrPrimitiveCastOfZero())
                 &&  !Dimension.equal (expression. leftExpression.dimensionSignature,
                                       expression.rightExpression.dimensionSignature)) {
                    expression.dimensionSignature = Dimension.errorSignature;
                    parserError (2, "Operands of '"+ expression.name
                                 +"' have different dimensions: "
                           +Dimension.getPresentation(expression. leftExpression.dimensionSignature)
                  +" and " +Dimension.getPresentation(expression.rightExpression.dimensionSignature),
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                 }
                 else switch (expression.operatorToken) {
                 case PlusToken              :
                 case MinusToken             :
                 case PercentToken           :
                 case PlusAssignToken        :
                 case MinusAssignToken       :
                 case PercentAssignToken     :
                    if (expression. leftExpression.dimensionSignature!=null
                    || !expression. leftExpression.isZeroLiteralOrPrimitiveCastOfZero()) {
                      expression.dimensionSignature = expression. leftExpression.dimensionSignature;
                    }
                    else  {
                       expression.dimensionSignature = expression.rightExpression.dimensionSignature;
                    }
                 }
                 break;
          case AsteriskToken          :
                 expression.dimensionSignature = Dimension.product (
                            expression. leftExpression.dimensionSignature,
                            expression.rightExpression.dimensionSignature);
                 break;

          case SlashToken             :
                 expression.dimensionSignature = Dimension.division (
                            expression. leftExpression.dimensionSignature,
                            expression.rightExpression.dimensionSignature);
                 break;

          case           AsteriskAssignToken:
          case              SlashAssignToken:
                 if (expression.rightExpression.dimensionSignature != null
                 &&  expression.rightExpression.dimensionSignature != Dimension.errorSignature) {
                    expression.dimensionSignature = Dimension.errorSignature;
                    parserError (2, "Right-hand operand of '"+ expression.name
                                 +"' should not have dimension; instead, it is: "
                           +Dimension.getPresentation(expression.rightExpression.dimensionSignature),
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                 }
                 else {
                    expression.dimensionSignature = expression. leftExpression.dimensionSignature;
                 }
                 break;


          case AmpersandToken           :
          case VerticalBarToken         :
          case CaretToken               :
          case      AmpersandAssignToken:
          case    VerticalBarAssignToken:
          case          CaretAssignToken:
          case LeftShiftToken               :
          case RightShiftToken              :
          case UnsignedRightShiftToken      :
          case          LeftShiftAssignToken:
          case         RightShiftAssignToken:
          case UnsignedRightShiftAssignToken:
                 if (expression.leftExpression.dimensionSignature != null
                 &&  expression.leftExpression.dimensionSignature != Dimension.errorSignature) {
                    expression.dimensionSignature = Dimension.errorSignature;
                    parserError (2, "Left-hand operand of '"+ expression.name
                                 +"' should not have dimension; instead it is: "
                           +Dimension.getPresentation(expression. leftExpression.dimensionSignature),
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                 }
                 if (expression.rightExpression.dimensionSignature != null
                 &&  expression.rightExpression.dimensionSignature != Dimension.errorSignature) {
                    expression.dimensionSignature = Dimension.errorSignature;
                    parserError (2, "Right-hand operand of '"+ expression.name
                                 +"' should not have dimension; instead it is: "
                           +Dimension.getPresentation(expression.rightExpression.dimensionSignature),
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                 }
                 break;
          }
        } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
        } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                              expression.  sourceEndPosition);
        }
   }

   protected void processConditionalExpression    (FieldDeclaration fieldDeclaration, ConditionalExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    processJavaExpression (fieldDeclaration, expression.conditionExpression, level + 1, retValue, arg1, arg2);
    processJavaExpression (fieldDeclaration, expression.     trueExpression, level + 1, retValue, arg1, arg2);
    processJavaExpression (fieldDeclaration, expression.    falseExpression, level + 1, retValue, arg1, arg2);
    if (!expression.conditionExpression.dataType.isResolved()) return;
    if (!expression.     trueExpression.dataType.isResolved()) return;
    if (!expression.    falseExpression.dataType.isResolved()) return;
    if (!expression.conditionExpression.dataType.isBoolean()) {
         parserError (2, "Conditional operator does not start with boolean operand",
                       expression.conditionExpression.sourceStartPosition,
                       expression.conditionExpression.sourceEndPosition);
    }
    try {

      if (expression. trueExpression.dataType
      ==  expression.falseExpression.dataType) {
          expression.dataType = expression. trueExpression.dataType;
      } else
      if (expression. trueExpression.dataType.isNumeric()
      !=  expression.falseExpression.dataType.isNumeric()) {
         parserError (2, "Conditional operator requires compatible types for second and third operand",
                       expression.conditionExpression.sourceStartPosition,
                       expression.conditionExpression.sourceEndPosition);
         return;

      } else 
      if (expression. trueExpression.dataType.isNumeric()) {
       if(expression. trueExpression.dataType.isByte ()
       && expression.falseExpression.dataType.isShort()
       || expression. trueExpression.dataType.isShort()
       && expression.falseExpression.dataType.isByte ()) {
          expression.dataType = ShortType.theOne;
       } else
       if(expression. trueExpression.dataType.isInt()
       && expression. trueExpression.isConstant()) {
        if (expression.falseExpression.dataType.isByte()
        &&  expression. trueExpression.constantValue.canBeRepresentedAsByte()) {
          expression.dataType = ByteType.theOne;
        } else
        if (expression.falseExpression.dataType.isShort()
        &&  expression. trueExpression.constantValue.canBeRepresentedAsShort()) {
          expression.dataType = ShortType.theOne;
        } else
        if (expression.falseExpression.dataType.isChar()
        &&  expression. trueExpression.constantValue.canBeRepresentedAsChar()) {
          expression.dataType = CharType.theOne;
        }
       } else
       if  (expression.falseExpression.dataType.isInt()
       &&   expression.falseExpression.isConstant()) {
        if (expression. trueExpression.dataType.isByte()
        &&  expression.falseExpression.constantValue.canBeRepresentedAsByte()) {
            expression.dataType = ByteType.theOne;
        } else
        if (expression. trueExpression.dataType.isShort()
        &&  expression.falseExpression.constantValue.canBeRepresentedAsShort()) {
            expression.dataType = ShortType.theOne;
        } else
        if (expression. trueExpression.dataType.isChar()
        &&  expression.falseExpression.constantValue.canBeRepresentedAsChar()) {
            expression.dataType = CharType.theOne;
        }
       }
       if (expression.dataType==null) {
           expression. trueExpression = expression. trueExpression.promoteBinary(env, expression.falseExpression.dataType);
           expression.falseExpression = expression.falseExpression.promoteBinary(env, expression. trueExpression.dataType);
           expression.dataType        = expression.falseExpression.dataType;
       }
      }  // not numeric, so both reference or null
      else if (expression. trueExpression.dataType==NullType.theOne) {
               expression.dataType        = expression.falseExpression.dataType;
      }
      else if (expression.falseExpression.dataType==NullType.theOne) {
               expression.dataType        = expression. trueExpression.dataType;
      }
      else if (expression. trueExpression.dataType.isSubtypeOf (env, 
               expression.falseExpression.dataType)) {
               expression.dataType        = expression.falseExpression.dataType;
      }
      else if (expression.falseExpression.dataType.isSubtypeOf (env, 
               expression. trueExpression.dataType)) {
               expression.dataType        = expression.trueExpression.dataType;
      } else {
         parserError (2, "Conditional operator requires compatible types for second and third operand",
                       expression.conditionExpression.sourceStartPosition,
                       expression.conditionExpression.sourceEndPosition);
         return;
      }

      if (expression.conditionExpression.isTrue()) {
          expression. constantValue = expression. trueExpression.constantValue;
      } else
      if (expression.conditionExpression.isFalse()) {
          expression. constantValue = expression.falseExpression.constantValue;
      }

      // dimension checking
      if (!Dimension.equal (expression. trueExpression.dimensionSignature,
                            expression.falseExpression.dimensionSignature)) {
          expression.dimensionSignature = Dimension.errorSignature;
          parserError (2, "Branch operands of '?:' have different dimensions: "
                           +Dimension.getPresentation(expression. trueExpression.dimensionSignature)
                  +" and " +Dimension.getPresentation(expression.falseExpression.dimensionSignature),
                                expression.  trueExpression.sourceStartPosition,
                                expression. falseExpression.sourceEndPosition);
      }
      else {
           expression.dimensionSignature = expression.trueExpression.dimensionSignature;
      }

    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                expression.  sourceEndPosition);
    }
   }



   protected void processUnaryExpression          (FieldDeclaration fieldDeclaration, UnaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /*
      !            (boolean)
      ~            (integer -  unary promotion)
      ++ --        (numeric - binary promotion with 1)
      +  -         (numeric -  unary promotion)
      */
      // A UnaryExpression with a "++" or "--" operator is much like
      // an assignment; interesting in scripts for "priority++" etc.
      // If the victim is a NameExpression, treat it like
      // the left hand side of an assignment and call 
      // processAssignedNameExpression for that special case.

      if ( context.isScriptContext()
      && ( expression.unaryToken == IncrementToken
        || expression.unaryToken == DecrementToken)
      && ( expression.primaryExpression instanceof NameExpression)) {
         processAssignedNameExpression (fieldDeclaration, expression.unaryToken, (NameExpression)expression.primaryExpression, level + 1, retValue, arg1, arg2);
         if (expression.primaryExpression.dataType==null) {
             expression.primaryExpression.dataType = UnresolvedClassOrInterfaceType.one;
         }
      }
      else
         processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      if (!expression.primaryExpression.dataType.isResolved()) {
         return;
      }
      // check operand types, determine data type...

      switch (expression.unaryToken) {
      case PlusToken  :
      case MinusToken :
             expression.primaryExpression = expression.primaryExpression.promoteUnary();
             if (!expression.primaryExpression.dataType.isNumeric()) {
                  parserError (2, "Operator expects numeric operand",
                                expression.primaryExpression.sourceStartPosition,
                                expression.primaryExpression.sourceEndPosition);
                  return;
             }
             expression.dataType = expression.primaryExpression.dataType;
             break;

      case IncrementToken :
      case DecrementToken :
             if (!expression.primaryExpression.canBeAssigned()) {
                  parserError (2, (expression.unaryToken==IncrementToken? "Increment": "Decrement")
                              + " operator expects variable operand",
                                expression.primaryExpression.sourceStartPosition,
                                expression.primaryExpression.sourceEndPosition);
                  return;
             }
             if (expression.primaryExpression.isFinal()) {
                  parserError (2, (expression.unaryToken==IncrementToken? "Increment": "Decrement")
                              + " operator expects non-final operand",
                                expression.primaryExpression.sourceStartPosition,
                                expression.primaryExpression.sourceEndPosition);
                  return;
             }
             if (!expression.primaryExpression.dataType.isNumeric()) {
                  parserError (2, (expression.unaryToken==IncrementToken? "Increment": "Decrement")
                              + " operator expects numeric operand",
                                expression.primaryExpression.sourceStartPosition,
                                expression.primaryExpression.sourceEndPosition);
                  return;
             }
             // this does not work, since no lvalue would remain:
             // expression.primaryExpression =  expression.primaryExpression.promoteUnary();
             // expression.dataType          = expression.primaryExpression.dataType;
             expression.dataType = expression.primaryExpression.dataType.promoteUnary();
             break;

      case ExclamationToken :
             if (!expression.primaryExpression.dataType.isBoolean()) {
                  parserError (2, "Logical complement operator expects boolean operand",
                                expression.primaryExpression.sourceStartPosition,
                                expression.primaryExpression.sourceEndPosition);
                  return;
             }
             expression.dataType = expression.primaryExpression.dataType;
             break;
      case TildeToken :
             if (!expression.primaryExpression.dataType.isIntegral()) {
                  parserError (2, "Bitwise complement operator expects integral operand",
                                expression.primaryExpression.sourceStartPosition,
                                expression.primaryExpression.sourceEndPosition);
                  return;
             }
             expression.dataType = expression.primaryExpression.dataType;
             break;
      }

      // dimension checking
      switch (expression.unaryToken) {
      case PlusToken  :
      case MinusToken :
             expression.dimensionSignature = expression.primaryExpression.dimensionSignature;
             break;
      case IncrementToken :
      case DecrementToken :
      case ExclamationToken :
      case TildeToken :
            if (expression.primaryExpression.dimensionSignature != null
            &&  expression.primaryExpression.dimensionSignature != Dimension.errorSignature) {
                expression.dimensionSignature = Dimension.errorSignature;
                parserError (2, "Operand of '"+ expression.name
                             +"' should not have dimension; instead it is: "
                           +Dimension.getPresentation(expression.primaryExpression.dimensionSignature),
                             expression.primaryExpression.sourceStartPosition,
                             expression.primaryExpression.sourceEndPosition);
            }
            break;
      }

      // constant value checking

      if  (expression.primaryExpression.constantValue==null) return;
      if  (expression.primaryExpression.constantValue==ConstantMaybe.theOne) {
           expression.                  constantValue =ConstantMaybe.theOne;
           return;
      }

      try {
        switch (expression.unaryToken) {
        case ExclamationToken:
            expression.constantValue = expression.primaryExpression.constantValue.doLogicalComplement();
            break;
        case TildeToken :
        case MinusToken :
            expression.constantValue = expression.primaryExpression.constantValue.doUnaryOperator(
                                       expression.unaryToken);
            break;
        case PlusToken      : // nothing to do...
        case IncrementToken : // impossible... but let it...
        case DecrementToken : // impossible... but let it...
        default:
            break;
        }
      } catch (Exception e) {
         parserError (2, e.toString(), expression.sourceStartPosition, expression.sourceEndPosition);
      }
   }

   protected void processPrimitiveCastExpression  (FieldDeclaration fieldDeclaration, PrimitiveCastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
     expression.dataType = PrimitiveType.forToken (expression.castToken);
     if (!expression.unaryExpression.dataType.isResolved()) return; // presumably already handled
     try {
      String reason = expression.unaryExpression.dataType.whyCannotBeCastedTo (env, expression.dataType);
      if (reason != null) {
                  parserError (2, "Invalid cast: "+reason,
                                expression.sourceStartPosition,
                                expression.sourceEndPosition);
                  return;
      }

      // dimension checking
      if (!Dimension.equal (expression.                dimensionSignature,
                            expression.unaryExpression.dimensionSignature)) {
          expression.dimensionSignature = Dimension.errorSignature;
          parserError (2, "Use dimension cast to convert to a different dimension",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
      }

      // constant value checking
      if (expression.unaryExpression.isConstant()) {
          expression.constantValue = expression.unaryExpression.constantValue.convertTo (expression.castToken);
      }
     } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
     } catch (IOException   e) {parserError (2, e.getMessage(), expression.unaryExpression.sourceStartPosition, 
                                                                 expression.unaryExpression.  sourceEndPosition);
     }
   }

   protected void processCastExpression           (FieldDeclaration fieldDeclaration, CastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
      if (!env.resolveDataTypeDeclaration (typeDeclaration.compilationUnit,
                                          expression.castTypeDeclaration).isResolved()) {
                  parserError (2, "Can not resolve cast type",
                                expression.nameStartPosition,
                                expression.nameEndPosition);
                  return;
      }
      expression.dataType = expression.castTypeDeclaration.dataType;
      try {
        String reason = expression.unaryExpression.dataType.whyCannotBeCastedTo (env, expression.dataType);

        if (reason != null) {
                    parserError (2, "Invalid cast: "+reason,
                                  expression.sourceStartPosition,
                                  expression.sourceEndPosition);
                    return;
        }
        expression.isNarrowing = expression.dataType !=                expression.unaryExpression.dataType
                              && expression.dataType.isSubtypeOf (env, expression.unaryExpression.dataType);
      } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
      } catch (IOException   e) {parserError (2, e.getMessage(), expression.unaryExpression.sourceStartPosition, 
                                                                  expression.unaryExpression.  sourceEndPosition);
      }
   }

   protected void processTypeComparisonExpression (FieldDeclaration fieldDeclaration, TypeComparisonExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.relationalExpression, level + 1, retValue, arg1, arg2);
      expression.dataType = BooleanType.theOne;
      if (expression.relationalExpression.dataType.hasError()) return;
      if (expression.relationalExpression.dataType.isPrimitive()) {
             parserError (2, "instanceof operator cannot be used on primitive type",
                        expression. relationalExpression.sourceStartPosition,
                        expression. relationalExpression.sourceEndPosition);
             return;
      }
      try {
        expression.compareType = (ClassType)
                                 mustResolveDataTypeDeclaration (expression.compareTypeDeclaration);
        if (!expression.compareType.isResolved()) return;
        if (!expression.compareType.isSubtypeOf (env,
             expression.relationalExpression.dataType)
        &&  !expression.relationalExpression.dataType.isSubtypeOf (env, expression.compareType))
               parserError (2, "instanceof operator cannot possibly return true",
                          expression.sourceStartPosition,
                          expression.sourceEndPosition);
        getDimensionSignatureOf (expression.compareTypeDeclaration);
        if (!equalSignatures (expression.relationalExpression  .dimensionSignature,
                              expression.compareTypeDeclaration.dimensionSignature)) {
            parserError (2, "instanceof operator applied with incompatible dimensions",
                         expression.sourceStartPosition,
                         expression.sourceEndPosition);
        }
      } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
      } catch (IOException   e) {parserError (2, e.getMessage(), expression.compareTypeDeclaration.sourceStartPosition, 
                                                                  expression.compareTypeDeclaration.  sourceEndPosition);
      }
   }

   /********** STILL TO FINISH ???? **********/
   // A PostfixExpression with a "++" or "--" operator is much like
   // an assignment. If the victim is a NameExpression, treat it like
   // the left hand side of an assignment and call 
   // processAssignedNameExpression for that special case.
   protected void processPostfixExpression        (FieldDeclaration fieldDeclaration, PostfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
                  processUnaryExpression          (fieldDeclaration, expression, level, retValue, arg1, arg2);
   }

   //not needed, leave it to superclass:
   //protected void processLiteralExpression        (FieldDeclaration fieldDeclaration, LiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   //}

   protected void processNameExpression (FieldDeclaration fieldDeclaration, NameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    try {
      if (expression.isMethodName()) return;
      //if (expression.isAmbiguousName()  // this is according to the spec
      //||  expression.isExpressionName())// but this is our default classification, because it works!
      //{
      if   (expression.isAmbiguousName()) {
            expression.target = context.resolveVariable (expression);
        if (expression.target!=null) {
            if (!expression.target.isAccessibleFor (env, classType, false /*isForAllocation*/)) {
                  parserError (2, "Field is not accessible",
                               expression.sourceStartPosition,
                               expression.sourceEndPosition);
            }
            if (expression.target.isDeprecated()) {
                parserError (1, "Warning: use of deprecated variable: "+expression.target.getPresentation(),
                               expression.sourceStartPosition,
                               expression.sourceEndPosition);
            }
            expression.setExpressionName();

            // dimension checking
            expression.dimensionSignature = expression.target.dimensionSignature;
            if (expression.dimensionSignature != null
            &&  expression.dimensionSignature.charAt(0)=='V') {
                expression.dimensionSignature = "1W1"+expression.dimensionSignature;
            }
        } else {

            // nothing worked. Try types...

            ClassType c = context.findNestedOrLocalClass (expression.name);

            if (c==null) {

                 c =  env.resolveClassNameWithoutSlashes (typeDeclaration.compilationUnit,
                                                               expression.name, false);

            }
            if ((expression.dataType=c)!=null) {
                 expression.setTypeName   ();
                 if (!c.isAccessibleFor (env, classType)) {
                     parserError (2, "Class or interface not accessible",
                           expression.sourceStartPosition,
                           expression.sourceEndPosition);
                 }
            }
            else expression.setPackageName();
        }
      }
      if (expression.isExpressionName()
      &&  expression.target != null) { // NOTE: this could also be true at the top of this method.
                                  // since compiler pass 5 may process variable initializers multiple times
          expression.dataType      = expression.target.getDataType (env);
          expression.constantValue = expression.target.constantValue();
      }

      if (expression.target==null
      && (expression.isExpressionName()
        ||expression.isAmbiguousName ())) {

          ////////////////////////////////////////////////////////////////////
          if (context.isScriptContext()
          &&  expression.isExpressionName()) {
          ////////////////////////////////////////////////////////////////////

              if (expression.specialCode == SpecialCode.asAdaptingParameter) {   // ?!
                    parserError (2, "Invalid usage of \"?!\"",
                                 expression.sourceStartPosition, 
                                 expression.sourceEndPosition);
                 expression.specialCode = SpecialCode.none;
              }

              if (context.checkReservedIdentifier (expression)) {

                 if (expression.specialCode != SpecialCode.none) {
                    parserError (2, "Cannot use \"!?\" embellishments on reserved name \""
                                   + expression.getName() + "\"",
                                 expression.sourceStartPosition, 
                                 expression.sourceEndPosition);
                    expression.specialCode = SpecialCode.none;  // stay out of trouble
                 }

                 if (expression.getName().equals ("duration")) {

                    // Look for the duration signal
                    if (!scriptContext.hasDurationAssignment) {

                       if (scriptContext.processingDurationAssignment)
                          parserError (2,   "Cannot reference \"duration\""
                                       + " while it is being set",
                                       expression.sourceStartPosition, 
                                       expression.sourceEndPosition);
                       else if (scriptContext.processingPriorityAssignment)
                          parserError (2,   "Cannot reference \"duration\""
                                       + " while setting \"priority\"",
                                       expression.sourceStartPosition, 
                                       expression.sourceEndPosition);
                       else
                          parserError (2,   "Reference to \"duration\""
                                       + " is only allowed after it has been set",
                                       expression.sourceStartPosition, 
                                       expression.sourceEndPosition);
                    }
                    //expression.name   = "_n_.getDuration()"; That's in preprocessor; now:
                    expression.target   = NodeDuration.theOne;
                    expression.dataType = DoubleType  .theOne;
                    expression.isExpressionName();
                 }
                 else if (expression.getName().equals ("priority")) {
                    if (!scriptContext.allowPriorityUsage)
                          parserError (2,   "Cannot reference \"priority\" here",
                                       expression.sourceStartPosition, 
                                       expression.sourceEndPosition);
                    expression.target   = env.scripticVmNodePriority;
                    expression.dataType = IntType.theOne;
                    expression.isExpressionName();
                 }
                 else if (expression.getName().equals ("success")) {
                    if (!scriptContext.allowSuccessUsage)
                          parserError (2,   "Cannot reference \"success\" here",
                                       expression.sourceStartPosition, 
                                       expression.sourceEndPosition);
                    expression.target   = env.scripticVmNodeSuccess;
                    expression.dataType = env.javaLangBooleanType;
                    expression.isExpressionName();
                 }
                 else { // expression.getName().equals ("pass")
                    expression.target   = env.scripticVmNodePass;
                    expression.dataType = IntType.theOne;
                    expression.isExpressionName();
                 }
                 return;
              }

              //    i!!  i!  i?!   i?      specialCodes 1..4

              if (expression.specialCode != SpecialCode.none) { // Must resolve to a formal parameter 

                 Variable existingParameter = scriptContext.getFirstLocalName (expression.name);

                 if (existingParameter != null
                 && !existingParameter.isMethodParameter ())
//System.out.println("!existingParameter.isMethodParameter(): "+existingParameter.getDescription());
                     existingParameter  = null;
                 if (existingParameter == null) {
                    parserError (2, "Invalid parameter test (\""
                                   + expression.name
                                   + "\" is not a formal parameter)",
                                 expression.sourceStartPosition, 
                                 expression.sourceEndPosition);
                    expression.specialCode = SpecialCode.none;  // stay out of trouble
                    return;
                 }
                 expression.target   = existingParameter;
                 expression.dataType = BooleanType.theOne;
                 expression.isExpressionName();
                 return;
              }

          ////////////////////////////////////////////////////////////////////
          } // if isScriptContext
          ////////////////////////////////////////////////////////////////////

          expression.target = context.resolveVariable (expression);
          if (expression.target!=null) {
              if (!expression.target.isAccessibleFor (env, classType, false /*isForAllocation*/)) {
                    parserError (2, "Field is not accessible",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
              }
              expression.dataType           = expression.target.getDataType (env);
              expression.constantValue      = expression.target.constantValue();

              // dimension checking
              expression.dimensionSignature = expression.target.dimensionSignature;
              if (expression.dimensionSignature != null
              &&  expression.dimensionSignature.charAt(0)=='V') {
                  expression.dimensionSignature = "1W1"+expression.dimensionSignature;
              }
          }
          else {
            Method m = findARefinement (classType, expression);
            if (m != null) {
                  parserError (2, "Attempt to reference refinement " + m.getPresentation()
                              +" in class " + m.owner.nameWithDots
                              +" as a variable",
                        expression.sourceStartPosition,
                        expression.sourceEndPosition);
            }
            else {
              parserError (2, "Unknown identifier",
                    expression.sourceStartPosition,
                    expression.sourceEndPosition);
            }
          }
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {parserError (2, e.getMessage());
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                expression.  sourceEndPosition);
    }
   }

   /********** STILL TO FINISH **********/
   // this, super, null and some script stuff...
   protected void processSpecialNameExpression    (FieldDeclaration fieldDeclaration, SpecialNameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (expression.getToken()) {
      case  NullToken: expression.dataType = NullType.theOne; break;
      case SuperToken: if (typeDeclaration.getNameWithDots().equals("java.lang.Object")) {
                          parserError (2, "'super' cannot be used in root class java.lang.Object",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                          //expression.dataType = new UnresolvedClassOrInterfaceType();
                          return;
                       }
                       else {
if (typeDeclaration==null) System.out.println ("typeDeclaration==null");
else if (typeDeclaration.target.superclass1==null) System.out.println ("typeDeclaration.target.superclass1==null:"+typeDeclaration.getPresentation());
                          expression.dataType = typeDeclaration.target.superclass1;
                       }
                       break;
      case  ThisToken: if (context.isStatic()) {
                          parserError (2, "'this' cannot be used in static context",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                          return;
                       }
                       expression.dataType = classType; break;

      case ScriptToken:
                       expression.dataType = env.scripticVmNodeType; break;
      }
   }

   protected void processNestedJavaExpression     (FieldDeclaration fieldDeclaration, NestedJavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.subExpression, level + 1, retValue, arg1, arg2);
      expression.dataType           = expression.subExpression.dataType;
      expression.constantValue      = expression.subExpression.constantValue;
      expression.dimensionSignature = expression.subExpression.dimensionSignature;
   }

   protected void processFieldAccessExpression    (FieldDeclaration fieldDeclaration, FieldAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {

      if (expression.primaryExpression.dataType==NullType.theOne) {
          parserError (2, "Invalid expression statement",
                        expression.sourceStartPosition,
                        expression.sourceEndPosition);
          return;
      }
      JavaExpression primaryExpression = expression.primaryExpression;
      primaryExpression.setAmbiguousName();
             // only here and in processMethodCallExpression
             //           and in processScriptCallExpression
             //           and in processClassLiteralExpression
             // is default ExpressionName overruled 
      processJavaExpression (fieldDeclaration, primaryExpression, level + 1, retValue, arg1, arg2);

      if (expression.isMethodName()) {
          if (primaryExpression.isPackageName()) {
               parserError (2,   "Unresolved name",
                            primaryExpression.sourceStartPosition, 
                            primaryExpression.sourceEndPosition);

          } else if (primaryExpression.isExpressionName()) {

              if (!primaryExpression.dataType.isReference()) {
                 parserError (2,   "Reference type expected",
                              primaryExpression.sourceStartPosition, 
                              primaryExpression.sourceEndPosition);
              }

/****** REMOVE THIS ??
              else {
                 expression.target = primaryExpression.dataType.resolveMemberVariable (env, expression);
                 if (expression.target !=null) {
                     expression.dataType = expression.target.dataType;
                                constantvalue...
                 }
                 else {
                    parserError (2,   "Cannot resolve field",
                                 expression.sourceStartPosition, 
                                 expression.sourceEndPosition);
                 }
              }
*********/
          }
          return;
      }

      ////////////////////////////////////////////////////////////////////
      if (context.isScriptContext()) {
      ////////////////////////////////////////////////////////////////////

       if (primaryExpression.isOldIdentifier()) {

         primaryExpression.setExpressionName();
         // Special hack for "old.duration"
         if (expression.name.equals ("duration")) {
            if (scriptContext.hasDurationAssignment) {
                expression.target   = NodeOldDuration.theOne;
                expression.dataType = DoubleType.theOne;
                expression.isExpressionName();
            } else {
               parserError (2,   "Reference to \"old.duration\""
                            + " is only allowed after \"duration\" has been set",
                            expression.sourceStartPosition, 
                            expression.sourceEndPosition);
            }
            return;
         }

         // Look for "old.parameter"
         // have to make a temporary NameExpression, unfortunately...
         NameExpression n = new NameExpression();
         n.name = expression.name;
         Variable existingParameter = context.resolveLocalName (n, false);

         if (existingParameter == null
         || !existingParameter.isMethodParameter ()) {
            parserError (2, "Invalid old parameter reference (\""
                           + expression.name
                           + "\" is not a formal parameter)",
                         expression.sourceStartPosition, 
                         expression.sourceEndPosition);
            return;
         }
         expression.target             = ((ScriptParameter) existingParameter).oldVersion;
         expression.dataType           = existingParameter.getDataType (env);
         return;
       }
      ////////////////////////////////////////////////////////////////////
      } // if isScriptContext
      ////////////////////////////////////////////////////////////////////

      if (expression.isAmbiguousName()
      ||  expression.isExpressionName()) {

        if (primaryExpression.isPackageName()) {

            expression.dataType = env.resolveClass (primaryExpression.qualifiedName().replace('.','/'),
                                                    expression.name,
                                                    false);
            if (expression.dataType!=null) {
              if (expression.isExpressionName()) {
                   parserError (2, "No class expected here",
                         expression.sourceStartPosition,
                         expression.sourceEndPosition);
              }
              else {
                expression.setTypeName   ();
                if (!((ClassType) expression.dataType).isAccessibleFor (env, classType)) {
                     parserError (2, "Class or interface not accessible",
                           expression.sourceStartPosition,
                           expression.sourceEndPosition);
                 }
              }
            }
            else if (expression.isExpressionName()) {
                     parserError (2, "Unresolved field access",
                           expression.nameStartPosition,
                           expression.nameEndPosition);
            }
            else {
               expression.setPackageName();
            }
        }
        else {

          if (primaryExpression.isTypeName()) {

            // a nested type?
            if (primaryExpression.dataType.isClassOrInterface()) { // else array type
              ClassType c = (ClassType) primaryExpression.dataType;
              expression.dataType = (ClassType) c.nestedClassesByName.get (expression.name);
              if (expression.dataType != null) {
                if (expression.isExpressionName()) {
                   parserError (2, "No class expected here",
                         expression.sourceStartPosition,
                         expression.sourceEndPosition);
                }
                else {
                  expression.setTypeName   ();
                  if (!((ClassType) expression.dataType).isAccessibleFor (env, classType)) {
                     parserError (2, "Class or interface not accessible",
                           expression.sourceStartPosition,
                           expression.sourceEndPosition);
                   }
                }
              }
            }
            if (expression.dataType == null) {   // not a nested type. A member variable?
              expression.target = primaryExpression.dataType.
                                                      resolveMemberVariable (env, expression);
              if (expression.target != null) {

                if (!expression.target.isStatic()) {
                   parserError (2, "Illegal reference to non static field from class or interface",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
                }
              }
            }
          }
          else if (primaryExpression.languageConstructCode() == MethodCallExpressionCode) {
            expression.target = primaryExpression.dataType.resolveMemberVariable (env, expression);
          }
          else if (primaryExpression.isExpressionName()
               ||  primaryExpression.languageConstructCode()== SpecialNameExpressionCode
               && (primaryExpression.name.equals("this")
                 ||primaryExpression.name.equals("super"))) {

if (primaryExpression.dataType==null) new Exception ("ERROR: Unexpected dataType==null for: "
                                             + primaryExpression.dataType.getPresentation()
                                             + " at: " + primaryExpression.getPresentation())
.printStackTrace();
else
            expression.target = primaryExpression.dataType.resolveMemberVariable (env, expression);
          }
          else if (primaryExpression.languageConstructCode()== QualifiedThisExpressionCode) {

if (primaryExpression.dataType==null) new Exception ("ERROR: Unexpected dataType==null for: "
                                             + primaryExpression.dataType.getPresentation()
                                             + " at: " + primaryExpression.getPresentation())
.printStackTrace();
else
            expression.target = primaryExpression.dataType.resolveMemberVariable (env, expression);
          } else {
            //            expression.isAmbiguousName
            // && !primaryExpression.isPackageName
            // && !primaryExpression.isTypeName
            // we arrive here for instance with (a).b or with a[i].b ....

            expression.target = primaryExpression.dataType.resolveMemberVariable (env, expression);
          }

          if (expression.target!=null) {
              if (!expression.target.isAccessibleFor (env, classType, false /*isForAllocation*/)) {
                  parserError (2, "Field is not accessible in class or interface "
                              +expression.target.ownerClass(env).nameWithDots,
                               expression.sourceStartPosition,
                               expression.sourceEndPosition);
              }
              expression.dataType           = expression.target.getDataType (env);
              expression.dimensionSignature = expression.target.dimensionSignature;
              expression.constantValue      = expression.target.constantValue();
              expression.setExpressionName();
          } else if (!expression.isTypeName()) {
              Method m = findARefinement (primaryExpression.dataType, expression);
              if (m != null) {
                  parserError (2, "Attempt to reference refinement " + m.getPresentation()
                              +" in class " + m.owner.nameWithDots
                              +" as a variable",
                        expression.sourceStartPosition,
                        expression.sourceEndPosition);
              }
              else {
                  parserError (2, "Invalid field access",
                            expression.sourceStartPosition,
                            expression.sourceEndPosition);
              }
              // ??????? expression.dataType = new UnresolvedClassOrInterfaceType();
              return;
          }
        }
      }
      else { // we won't arrive here, OK??

      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {parserError (2, e.getMessage());
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                expression.  sourceEndPosition);
    }
   }

   protected void processArrayAccessExpression    (FieldDeclaration fieldDeclaration, ArrayAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      processJavaExpression  (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      int oldErrorCount = parserErrorCount();
      processJavaExpressions (fieldDeclaration, expression.indexExpressions, level + 1, retValue, arg1, arg2);
      if (!expression.primaryExpression.dataType.isResolved()) return;

      if (oldErrorCount == parserErrorCount())
      for (int i = 0; i < expression.indexExpressions.size(); i++) {
          JavaExpression exp = expression.indexExpressions.get(i);
          exp = exp.promoteUnary();
          expression.indexExpressions.set(i, exp);
          if (exp.dataType == null) continue;

          if (!exp.dataType.isInt()) {
              parserError (2, "Array index should promote to integer operand",
                            exp.sourceStartPosition,
                            exp.sourceEndPosition);
          }
          if (exp.dimensionSignature!=null
          &&  exp.dimensionSignature!=Dimension.errorSignature) {
              parserError (2, "Array index should not have dimension; instead it is: "
                           +Dimension.getPresentation(exp.dimensionSignature),
                            exp.sourceStartPosition,
                            exp.sourceEndPosition);
          }
      }
      if ( expression.primaryExpression.dataType.noOfArrayDimensions()
         < expression.indexExpressions.size()) {
              parserError (2, "Attempt to access non array by index",
                            expression.sourceStartPosition,
                            expression.sourceEndPosition);
           return;
      }
      expression.dataType = ((ArrayType)expression.primaryExpression.dataType).accessArray (
                            expression.indexExpressions.size());
      expression.dimensionSignature = expression.primaryExpression.dimensionSignature;
   }

   protected void processMethodCallExpression     (FieldDeclaration fieldDeclaration, MethodCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) { 
//System.out.println("processMethodCallExpression 1: "+expression.getDescription()+"\n----------------------------------\n");
    try {

      boolean foundError = false;
      if (expression.parameterList != null) {
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
         foundError = expression.parameterList.hasError();
      }

      boolean isQualifiedSuperExpression = false;
      switch (expression.methodAccessExpression.languageConstructCode()) {

      case QualifiedSuperExpressionCode:
          processJavaExpression (fieldDeclaration, expression.methodAccessExpression, level + 1, retValue, arg1, arg2);
          isQualifiedSuperExpression = true;
          // no break;

      case SpecialNameExpressionCode:
          int token = ((SpecialNameExpression) expression.methodAccessExpression).getToken();

          // possibly recursive...

          ClassType targetClass = classType;
          if (isQualifiedSuperExpression
          ||  token==SuperToken) targetClass = classType.getSuperclass(env);
          expression.dataType =  targetClass;

          if (foundError) break;

          expression.target = resolveConstructorCall (targetClass, expression, false /*isForAllocation*/);

          ConstructorDeclaration cd = (ConstructorDeclaration) fieldDeclaration;
          if (expression.target == cd.target) {
                        parserError (2, "Recursive constructor call not allowed",
                                expression.sourceStartPosition,
                                expression.sourceEndPosition);
          }
          if (expression != cd.otherConstructorInvocation) {
                      parserError (2, "Other constructor invocation should be the first thing in the constructor",
                                expression.sourceStartPosition,
                                expression.sourceEndPosition);
          }
          return;
      }
      if (expression.methodAccessExpression.isJavaExpressionWithTarget()) {
          processJavaExpression (fieldDeclaration, expression.methodAccessExpression, level + 1, retValue, arg1, arg2);
//System.out.println("processMethodCallExpression 2: "+expression.getDescription()
//+" foundError: "+foundError+"\n----------------------------------\n");
      }
      else {
              parserError (3, "Probably internal compiler error, please report...Invalid expression statement",
                            expression.sourceStartPosition,
                            expression.sourceEndPosition);
      }
      if (!foundError) {
//System.out.println("processMethodCallExpression 3: "+expression.getDescription()+"\n----------------------------------\n");
         expression.target = resolveRefinementCall (expression, "method");
//System.out.println("processMethodCallExpression 4: "+expression.getDescription()+"\n----------------------------------\n");
         if (expression.target != null) {
             expression.dataType = expression.target.returnType;
//System.out.println("processMethodCallExpression 5: "+expression.getDescription()+"\n----------------------------------\n");
         }
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {parserError (2, e.getMessage());
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                          expression.  sourceEndPosition);
    }
   }

   //not needed, leave it to superclass:
   //protected void processMethodCallParameterList  (FieldDeclaration fieldDeclaration,
   //                                                MethodOrConstructorCallExpression methodCall,
   //                                                MethodCallParameterList parameterList,
   //                                                int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   //   if (parameterList.parameterExpressions.isEmpty()) return;
   //   processJavaExpressions       (fieldDeclaration, parameterList.parameterExpressions, level + 1, retValue, arg1, arg2);
   //}

   protected void processAllocationExpression     (FieldDeclaration fieldDeclaration, AllocationExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {

      if (expression.enclosingInstance != null) {
         processJavaExpression (fieldDeclaration, expression.enclosingInstance, level + 1, retValue, arg1, arg2);
         if (expression.enclosingInstance.dataType.isResolved()) {

             if (!expression.enclosingInstance.dataType.isClassOrInterface()) {
                parserError (2, "Expression should a reference to enclosing instance ",
                              expression.sourceStartPosition,
                              expression.sourceEndPosition);
             }
             else {
               expression.dataType = (ClassType) ((ClassType)expression.enclosingInstance.dataType)
                                                 .nestedClassesByName.get (expression.name);
               if (expression.dataType==null) {
                  parserError (2, "There is no nested type named \""+expression.name
                              +"\" in "+expression.dataType.getPresentation(),
                                expression.sourceStartPosition,
                                expression.sourceEndPosition);
               }
             }
         }
      }
      for (JavaExpression se: expression.sizeExpressions) {
        if (se != null) {
            processJavaExpression (fieldDeclaration, se, level+1, retValue, arg1, arg2);
        }
      }

      boolean foundError = false;
      if (expression.parameterList != null) {
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
         foundError = expression.parameterList.hasError();
      }
      for (int i = 0; i < expression.sizeExpressions.size(); i++) {
          JavaExpression exp = expression.sizeExpressions.get(i);
          if (exp==null) {
            continue;
          }
          exp = exp.promoteUnary();
          expression.sizeExpressions.set (i, exp);
          if ( exp.dataType == null) {
              foundError = true;
              continue;
          }
          if (!exp.dataType.isInt()) {
              parserError (2, "Array index should promote to integer operand",
                            exp.sourceStartPosition,
                            exp.sourceEndPosition);
          }
          if (exp.dimensionSignature!=null
          &&  exp.dimensionSignature!=Dimension.errorSignature) {
              parserError (2, "Array index should not have dimension; instead it is: "
                           +Dimension.getPresentation(exp.dimensionSignature),
                            exp.sourceStartPosition,
                            exp.sourceEndPosition);
          }
      }

      //  expression.dataTypeDeclaration correctly set in JavaParser1
      mustResolveDataTypeDeclaration (expression.dataTypeDeclaration);
      expression.dataType           = expression.dataTypeDeclaration.dataType;
      getDimensionSignatureOf        (expression.dataTypeDeclaration);
      expression.dimensionSignature = expression.dataTypeDeclaration.dimensionSignature;

      if (!expression.dataType.isResolved()) {
          return;
      }

/*****************
if (expression.dataType==null) {
System.out.println("processAllocationExpression: expression.dataType==null\n"
+expression.dataTypeDeclaration.getPresentation()+"\n"
+expression.dataTypeDeclaration.getDescription());
}
else if (expression.dataType.baseType()==null) {
System.out.println("processAllocationExpression: expression.dataType==null\n"
+expression.dataTypeDeclaration.getPresentation()+"\n"
+expression.dataTypeDeclaration.getDescription());
}
else
************************/

      if (expression.anonymousTypeDeclaration != null) {
         processAnonymousTypeDeclaration (fieldDeclaration, expression.anonymousTypeDeclaration, level + 1, retValue, arg1, arg2);
         if (expression.dataType instanceof ClassType) {
          if(expression.dataType.isInterface()) {
             expression.anonymousTypeDeclaration.target.superclass1 = env.javaLangObjectType;
             expression.anonymousTypeDeclaration.target.addInterface ((ClassType)expression.dataType);
          }
          else {
             expression.anonymousTypeDeclaration.target.superclass1 = (ClassType)expression.dataType;
          }
         }
         else {
             parserError (2, "Anonymous type should inherit from class or interface",
                          expression.sourceStartPosition,
                          expression.sourceEndPosition);
         }
      }
      // some more checking may be required...
      else if (expression.dataType.isInterface()) {
              parserError (2, "Cannot allocate interface type",
                            expression.nameStartPosition,
                            expression.nameEndPosition);
      }
      else if (expression.dataType.isClass()) {
         if (((ClassType)expression.dataType).isAbstract()) {
              parserError (2, "Cannot allocate abstract class type",
                            expression.nameStartPosition,
                            expression.nameEndPosition);
          }
          else if (!foundError) {
            resolveConstructorCall ((ClassType)expression.dataType.baseType(), expression, true /*isForAllocation*/);
          }
      }
      if (expression.arrayInitializer != null) {
         processArrayInitializer (fieldDeclaration, expression.arrayInitializer, level + 1, retValue, arg1, arg2);
         JavaExpression offender = expression.arrayInitializer.eltThatCannotBeAssignedTo(env, expression.dataType);
         if (offender != null) {
                   parserError (2, "Array initializer is incompatible with new type",
                                 offender.sourceStartPosition,
                                 offender.  sourceEndPosition);
                   return;
         }
         expression.arrayInitializer = (ArrayInitializer)
                                        expression.arrayInitializer
                                       .convertForAssignmentTo (env, expression.dataType);
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {parserError (2, e.getMessage());
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                          expression.  sourceEndPosition);
    }
   }

   protected void processClassLiteralExpression (FieldDeclaration fieldDeclaration, ClassLiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      expression.dataType = env.javaLangClassType;
      JavaExpression primaryExpression = expression.primaryExpression;
      if (primaryExpression != null) {
        primaryExpression.setAmbiguousName();
             // only here and in processMethodCallExpression
             //           and in processScriptCallExpression
             //           and in processFieldAccessExpression
             // is default ExpressionName overruled 
        processJavaExpression (fieldDeclaration, primaryExpression, level + 1, retValue, arg1, arg2);
        if (!primaryExpression.isTypeName()) {
              parserError (2, "Type name expected",
                            primaryExpression.sourceStartPosition,
                            primaryExpression.  sourceEndPosition);
        }
      }
   }

   protected void processQualifiedThisExpression (FieldDeclaration fieldDeclaration, QualifiedThisExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      JavaExpression primaryExpression = expression.primaryExpression;
      primaryExpression.setAmbiguousName();
      processJavaExpression (fieldDeclaration, primaryExpression, level + 1, retValue, arg1, arg2);
      if (primaryExpression.languageConstructCode() != NameExpressionCode
      || !primaryExpression.isTypeName()) {
               parserError (2, "Should be name of enclosing type",
                            primaryExpression.sourceStartPosition, 
                            primaryExpression.sourceEndPosition);
               return;
      }
      searchAncestor: {
        for (ClassType c = classType; c.parent()!=null; c=c.parent()) {
            //((LocalOrNestedClassType) c).setNeedForParentReference ();
            if (c.parent().className.equals (primaryExpression.name)) {
                  expression.dataType = c.parent();
                  expression.target   = ((LocalOrNestedClassType) c).enclosingInstance;
                  break searchAncestor;
            }
        }
        parserError (2, "Should be name of enclosing type",
                      primaryExpression.sourceStartPosition, 
                      primaryExpression.sourceEndPosition);
        
      }
  }

  protected void processQualifiedSuperExpression (FieldDeclaration fieldDeclaration, QualifiedSuperExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      JavaExpression primaryExpression = expression.primaryExpression;
      primaryExpression.setAmbiguousName();
      processJavaExpression (fieldDeclaration, primaryExpression, level + 1, retValue, arg1, arg2);

      if (!expression.isMethodName()) {
               parserError (2, "\"super\" may only be qualified in a superclass constructor call",
                            expression.sourceStartPosition, 
                            expression.sourceEndPosition);
               return;
      }
      if (!primaryExpression.isExpressionName()) {
               parserError (2, "Expression expected for enclosing instance",
                            expression.sourceStartPosition, 
                            expression.sourceEndPosition);
               return;
      }
      if (primaryExpression.dataType == null) return;
      if (classType.parent()         == null) {
               parserError (2, "\"super\" may only be qualified in inner classes",
                            expression.sourceStartPosition, 
                            expression.sourceEndPosition);
               return;
      }
      if (typeDeclaration.isStatic()) {
               parserError (2, "\"super\" may only be qualified in non-static nested classes",
                            expression.sourceStartPosition, 
                            expression.sourceEndPosition);
               return;
      }
      try {
        if (!primaryExpression.dataType.canBeAssignedTo (env, classType.parent())) {
               parserError (2, "Instance expression not compatible with enclosing type",
                            expression.sourceStartPosition, 
                            expression.sourceEndPosition);
               return;
        }
      } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
      } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                            expression.  sourceEndPosition);
      }
      //classType.setNeedForParentReference();
      ((ConstructorDeclaration) fieldDeclaration).hasQualifiedSuperCall = true;
  }

  protected void processDimensionCastExpression  (FieldDeclaration fieldDeclaration, DimensionCastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     expression.dimensionSignature = getDimensionSignatureOf (expression.compoundDimensionDeclaration);
     processJavaExpression   (fieldDeclaration, expression.   unaryExpression, level + 1, retValue, arg1, arg2);

     expression.dataType = expression.unaryExpression.dataType;
     expression.constantValue = expression.unaryExpression.constantValue;
   }


   /* **********************************************************
    * Specially for Scriptic
    ************************************************************/

   protected static final String HasDurationAssignment        = "HasDurationAssignment";
   protected static final String ProcessingDurationAssignment = "ProcessingDurationAssignment";
   protected static final String AllowPriorityAssignment      = "AllowPriorityAssignment";
   protected static final String ProcessingPriorityAssignment = "ProcessingPriorityAssignment";

   /* ------------- Name resolving - Assignment targets ------------- */

   protected void processAssignedNameExpression   (FieldDeclaration fieldDeclaration, int offendingToken, NameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    try {
      if (context.checkReservedIdentifier (expression)) {

         if (expression.specialCode != SpecialCode.none) {
            parserError (2, "Cannot use \"!?\" embellishments on reserved name \""
                           + expression.getName() + "\"",
                         expression.sourceStartPosition, 
                         expression.sourceEndPosition);
            expression.specialCode = SpecialCode.none;  // stay out of trouble
         }

         if (expression.getName().equals ("duration")) {
            // Look for the duration signal
            if (scriptContext.processingDurationAssignment) {
               // We're OK.
               // The following line is copied from processNameExpression()!
               // expression.name  = "_n_.duration";    That's in preprocessor; now:
               expression.target   = NodeDuration.theOne;
               expression.dataType = DoubleType  .theOne;

            } else {
               if (offendingToken == AssignToken)
                  parserError (2,   "Assignment to \"duration\""
                               + " is only allowed at the beginning,"
                               + " followed by \":\"",
                               expression.sourceStartPosition, 
                               expression.sourceEndPosition);
               else
                  parserError (2,   "Cannot use \""
                               + Scanner.tokenRepresentation (offendingToken)
                               + "\" with \"duration\"",
                               expression.sourceStartPosition, 
                               expression.sourceEndPosition);
            }

            return;
         }

         if (expression.getName().equals ("priority")) {
            if (scriptContext.processingPriorityAssignment
            ||  scriptContext.     allowPriorityAssignment) {
               // We're OK.
               // The following line is copied from processNameExpression()!
               //expression.name   = "_n_." + expression.name;    That's in preprocessor; now:
               expression.target   = env.scripticVmNodeType.resolveMemberVariable (env, "priority");
               expression.dataType = IntType.theOne;
            } else {
                  parserError (2,   "Changing \"priority\""
                               + " is only allowed at the beginning, "
                               + " followed by \":\"; or"
                               + " in activation code",
                               expression.sourceStartPosition, 
                               expression.sourceEndPosition);
            }

            return;
         }

         if (expression.getName().equals ("success")) {
            if (scriptContext.allowSuccessAssignment) {
               //expression.name = "_n_." + expression.name;    That's in preprocessor; now:
               expression.target = env.scripticVmNodeType.resolveMemberVariable (env, "success");
               expression.dataType          = env.javaLangBooleanType;
            } else {
                  parserError (2,   "Setting \"success\""
                               + " is not allowed here",
                               expression.sourceStartPosition, 
                               expression.sourceEndPosition);
            }
            return;
         }

         // Else: "pass")

         parserError (2,   "\"pass\" variable is read-only",
                         expression.sourceStartPosition, 
                         expression.sourceEndPosition);

         return;
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                          expression.  sourceEndPosition);
    }

      if (expression.specialCode != SpecialCode.none) {

         if (   offendingToken < FirstAssignmentToken
             || offendingToken > LastAssignmentToken)
            parserError (2,   "Cannot use \"!?\" embellishments with \""
                         + Scanner.tokenRepresentation (offendingToken)
                         + "\"",
                         expression.sourceStartPosition, 
                         expression.sourceEndPosition);
         else
            parserError (2, "Cannot use \"!?\" embellishments with an assignment",
                         expression.sourceStartPosition, 
                         expression.sourceEndPosition);

         expression.specialCode = SpecialCode.none;  // stay out of trouble
      }

      // Else: the usual treatment
      processJavaExpression (fieldDeclaration, expression, level, retValue, arg1, arg2);
   }
}
