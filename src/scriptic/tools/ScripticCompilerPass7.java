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
import scriptic.tools.lowlevel.ClassFileConstants;
import scriptic.tools.lowlevel.ByteCodingException;

/**
 * Resolve field declarations etc
 * Various checks on inheritance and abstractness
 * Checks expression types and constantness
 * Reparses methods and scripts first
 *
 * arg1: reserved for localDeclarations
 *        (collect those that must be popped at end of block)
 * arg2: passes switch statement
 *       and variable declarations for array initializers (level restarts at 0)
 */
public class ScripticCompilerPass7 extends ExpressionChecker
               implements scriptic.tokens.ScripticTokens {

   ScripticParser parser; // for reparsing method bodies

   public ScripticCompilerPass7 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }

  // hack for lack of passes 3,4,5,6 in anonymous types...we'll do only pass 3...
   protected void processAnonymousTypeDeclaration (FieldDeclaration field, AnonymousTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     ScripticCompilerPass3 scripticCompilerPass3 = new ScripticCompilerPass3(scanner,env);
     scripticCompilerPass3.resolveAnonymousTypeDeclaration(t);
     super.processAnonymousTypeDeclaration (field, t, level, retValue, arg1, arg2);
   }
   /* Main entry point */
   public boolean resolve (TopLevelTypeDeclaration t) {
      parser = new ScripticParser (scanner, t.compilationUnit.sourceFile, env);
      processTopLevelTypeDeclaration(t, null, null, null);
      return true;
   }

   private void endOfProcessTypeDeclaration () {
      if   (classType.isClass()) {
        if(!classType.isAbstract()) // check for abstract methods...
        {
          checkForUnimplementedAbstractMethods (classType);
        }
        else { // abstract...check for duplicate abstract methods with same name and signature
               // but different return types
          checkConflictingAbstractMethods (classType, new HashMap<Method, HashMap<String, Method>>());
        }
      }
      if (classType.defaultConstructor != null) {
         processConstructorDeclaration (classType.defaultConstructor, 0, null, null, null);
      }
      // check for inner class cycles
      if (typeDeclaration.name != null)
      for (ClassType parent = classType.parent(); parent!=null; parent=parent.parent()) {
         if (parent.className.equals (typeDeclaration.name)) {
              env.parserError (2, classType.nameWithDots + " attempts embedding in class with same name", typeDeclaration);
              return;
         }
      }
   }
   void endOfProcessTopLevelTypeDeclaration     () {endOfProcessTypeDeclaration();}
   void endOfProcessLocalOrNestedTypeDeclaration() {endOfProcessTypeDeclaration();}

   protected void processLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      LocalOrNestedClassType lnc = (LocalOrNestedClassType) t.target;
      LocalClassType          lc = null;
      int nrExtraParametersForLocals = 0;
      if (t.target instanceof LocalClassType) {
         lc = (LocalClassType) t.target;
         nrExtraParametersForLocals = lc.usedLocalVariablesAndParameters.size();
      }

      // add constructor parameters ... this$0, val$... etc.

      for (int i=0; i<lnc.methods.length; i++) { //handle all constructors
         Method m = lnc.methods[i];
         if (!m.name.equals ("<init>")) {
            continue;
         }
         ConstructorDeclaration cdecl = (ConstructorDeclaration) m.source;

         int nrExtraParametersForEnclosingInstance = 0;
         if (lnc.enclosingInstance != null
         && !cdecl.hasQualifiedSuperCall) {
           nrExtraParametersForEnclosingInstance = 1;
         }
         int nrExtraParameters = nrExtraParametersForLocals
                               + nrExtraParametersForEnclosingInstance;
         if (nrExtraParameters==0) continue;

         m.extraParameters = new Parameter [nrExtraParameters];

         int currentIndex = 0; // of extra parameters
         if (nrExtraParametersForEnclosingInstance == 1) {
              // insert the enclosing this parameter
              Parameter thisParam = new Parameter();
              thisParam.name      = "this$0";
              thisParam.dataType1 = lnc.parent();
              thisParam.slot      = m.nextVariableSlot++;
              m.extraParameters[currentIndex++] = thisParam;
         }

         if (lc != null) {
            for (MemberVariable v: lc.usedLocalVariablesAndParameters.values()) {

               Parameter p = new Parameter();
               p.name      = v.name;
               p.dataType1 = v.dataType1;
               p.slot      = m.nextVariableSlot;
               m.nextVariableSlot += v.dataType1.slots();
               m.extraParameters[currentIndex++] = p;
            }
         }

         m.signature = null;
         m.getSignature(env);
      }
      super.processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
   }


   /**
    * check that classType provides proper implementations
    * for all abstract fields in the given ClassType or parent thereof
    */
   protected void checkForUnimplementedAbstractMethods (ClassType s) {

     if (s.methods != null) {

        if (classType != s) { // in first call, '==' holds
          for (int i=0; i<s.methods.length; i++) {

            Method abstractMethod = s.methods[i];
            abstractMethod.parseSignatureIfNeeded (env);

            if ( s.isInterface() ) {
                   if ( abstractMethod.isClassInitializer()) continue;
            } else if (!abstractMethod.isAbstract        ()) continue;

            try {
              Method r = classType.resolveAbstractMethodImplementation (env, abstractMethod);
              if (r == null) {

                 env.parserError (2, "Class " + classType.nameWithDots + " should be declared abstract;"
                                + " it does not implement " + abstractMethod.getPresentation()
                                + " declared in "           + (s.isClass()? "class ": "interface ")
                                + s.nameWithDots,
                                  typeDeclaration);

                 classType.modifiers.modifierFlags |= ModifierFlags.AbstractFlag; // it should not be instanciated...
              }
              else if (r.isStatic()) {
                 env.parserError (2, "Attempt to implement abstract refinement " + abstractMethod.getPresentation()
                                 + " in class "                                + s.nameWithDots
                                 + " using a static refinement in class "      + classType.nameWithDots,
                                   typeDeclaration);
              }
            } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
            } catch (java.io.IOException e) {parserError (2, e.getMessage(), typeDeclaration.nameStartPosition, 
                                                                        typeDeclaration.  nameEndPosition);
            }
          }
        }
     }
     if (s.getSuperclass(env) != null
     &&  s.superclass1.isAbstract()) {
        checkForUnimplementedAbstractMethods (s.superclass1);
     }
     for (int i=0; i < s.getInterfaces(env).length; i++) {
        checkForUnimplementedAbstractMethods (s.interfaces1[i]);
     }
   }

   /**
    * fill signaturesByName with abstract methods in the given TypeDeclaration or parents
    * thereof. Report an error when an abstract method is encountered with a name/signature
    * already in signaturesByName but with a different return type.
    * Also, check for throws clause and for staticness
    */
   protected void checkConflictingAbstractMethods (ClassType c, HashMap<Method, HashMap<String, Method>> signaturesByName) {

     if (c.methods != null) {
        for (int i=0; i<c.methods.length; i++) {

            Method abstractMethod = c.methods[i];
            if   (!abstractMethod.isAbstract()
            ||    !c.isInterface            () ) {
                  continue;
            }
            abstractMethod.parseSignatureIfNeeded (env);

            HashMap<String, Method> signatures = signaturesByName.get (abstractMethod.name);
            if (signatures==null) {
                signatures = new HashMap<String, Method>();
                signaturesByName.put(abstractMethod, signatures);
            }
            String parameterSignature = abstractMethod.getParameterSignature(env);
            Method m = signatures.get (parameterSignature);
            if (m != null) {

               m.parseSignatureIfNeeded (env);

               if (             m.returnType
               !=  abstractMethod.returnType)
               {
                  env.parserError (2,  typeDeclaration.getPresentationName()   + " "
                                 + classType.nameWithDots + " inherits conflicting abstract refinements: "
                                 + abstractMethod.getPresentation()
                                 + " declared in " + (abstractMethod.owner.isClass()? "class ": "interface ")
                                 + abstractMethod.owner.nameWithDots
                                 + " and "
                                 + m.getPresentation()
                                 + " declared in " + (m.owner.isClass()? "class ": "interface ")
                                 + m.owner.nameWithDots,
                                 typeDeclaration);
                  continue;
               }
               try {
                 if (m.exceptionTypes (env)!=null
                 && (!             m.throwsClauseCoveres (env, abstractMethod)
                   ||!abstractMethod.throwsClauseCoveres (env, m)))
                 {
                  env.parserError (2,  typeDeclaration.getPresentationName()   + " "
                                 + classType.nameWithDots + " inherits conflicting abstract refinements: "
                                 + abstractMethod.getPresentation()
                                 + " declared in " + (abstractMethod.owner.isClass()? "class ": "interface ")
                                 + abstractMethod.owner.nameWithDots
                                 + " has a different throws declaration as "
                                 + m.getPresentation()
                                 + " declared in " + (m.owner.isClass()? "class ": "interface ")
                                 + m.owner.nameWithDots,
                                 typeDeclaration);
                    continue;
                 }
               } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
               } catch (java.io.IOException e) {parserError (2, e.toString());
               }
               if (m.isStatic() != abstractMethod.isStatic())
               {
                  env.parserError (2,  typeDeclaration.getPresentationName()   + " "
                                 + classType.nameWithDots + " inherits conflicting abstract refinements: "
                                 + (abstractMethod.isStatic()? "static": "instance") + " refinement "
                                 +  abstractMethod.getPresentation()
                                 + " declared in " + (abstractMethod.owner.isClass()? "class ": "interface ")
                                 + abstractMethod.owner.nameWithDots
                                 + " whereas " + (m.isStatic()? "a static": "an instance")
                                 + " version exists in " + (m.owner.isClass()? "class ": "interface ")
                                 + m.owner.nameWithDots,
                                 typeDeclaration);
                  continue;
               }
            }
            signatures.put (parameterSignature, abstractMethod);
        }
     }
     if (null != c.getSuperclass(env)) {
         checkConflictingAbstractMethods (c.superclass1, signaturesByName);
     }
     for (int i=0; i < c.getInterfaces(env).length; i++) {
         checkConflictingAbstractMethods (c.interfaces1[i], signaturesByName);
     }
   }


   /**
    * find a Method in the given ClassType or parent
    * thereof, with name/signature equal to the given refinement, but some conflicting property:
    *  it has a different return type; it is final; static instead of instance vv.;
    *  it is more public; it has a narrower throws clause
    *  or it is deprecated (not so bad)
    */
   protected void checkConflictingRefinementInAncestor (ClassType c, RefinementDeclaration refinement) {

     //if (c.getMethodNamesSignatures(env) != null) {

        if (classType != c) { // false in first call

          HashMap<String, Object> signatures = c.getMethodNamesSignatures(env).get(refinement.name);
          if (signatures != null) {

            Method r = (Method) signatures.get(refinement.target.getSignature(env));
            if (r != null) {

             //8.4.6.3 Requirements in Overriding and Hiding

             r.parseSignatureIfNeeded (env);
             try{

              if (r.isFinal()) {
                env.parserError (2, "Error: attempt to override final refinement "
                                + r.getPresentation()
                                + " in class "+ r.owner.nameWithDots,
                                 refinement);
                return;
              }
              else if (r.isStatic() != refinement.isStatic()) {
                env.parserError (2, "Error: attempt to define "
                              + (refinement.isStatic()? "static": "instance")
                              + " refinement "+refinement.getPresentation()
                              + " while "
                              + (r.isStatic()? "static": "instance")
                              + " ancestor version exists in "+r.owner.nameWithDots,
                                 refinement);
                return;
              }
              else if (r.isPublic   () && !refinement.isPublic()
                   ||  r.isProtected() && !refinement.isPublic() && !refinement.isProtected()
                   || !r.isPrivate  () &&  refinement.isPrivate() ) {

                env.parserError (2, "Error: attempt to define refinement "
                                + refinement.getPresentation()
                                + " while more public "
                                + "ancestor version exists in "+r.owner.nameWithDots,
                                 refinement);
                return;
              }
              // if (r.isPrivate ()) return ???????????????

              else if (!r.isPrivate ()
                   &&   r.returnType != refinement.target.returnType ) {

                env.parserError (2, "Error: attempt to define refinement "
                                + refinement.getPresentation()
                                + " while ancestor version exists in "+r.owner.nameWithDots
                                + " with return type "+r.returnType.getPresentation(),
                                 refinement);
                return;
              }
              else if (!r.isPrivate  ()
                   &&  refinement.throwsClause() != null) {

                  ArrayList<DataTypeDeclaration> v = refinement.throwsClause().exceptionTypeDeclarations;
                  boolean foundError = false;
                  for (DataTypeDeclaration exceptionType: v) {
                     if (!r.throwsClauseCoveres (env, (ClassType) exceptionType.dataType)) {
                        parserError (2, "Error: thrown exception " + exceptionType.getNameWithDots()
                                       +" has not been declared in throws clause of ancestor refinement in "
                                       +r.owner.nameWithDots
                                       +ClassFileConstants.lineSeparator+refinement.getPresentation()
                                       +ClassFileConstants.lineSeparator+r
                                       +ClassFileConstants.lineSeparator+r         .getPresentation(),
                                       exceptionType.nameStartPosition,
                                       exceptionType.  nameEndPosition);
                        foundError = true;
                     }
                  }
                  if (foundError) return;
              }
              else if (r.isDeprecated ()) {

                env.parserError (1, "Warning: attempt to define refinement "
                                  + refinement.getPresentation()
                                  + " while deprecated ancestor version exists in "+r.owner.nameWithDots,
                                   refinement);
                return;
              }
             } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
             } catch (java.io.IOException e) {parserError (2, e.getMessage(), refinement.sourceStartPosition, 
                                                                         refinement.  sourceEndPosition);
             }
            }
          }
        }
//     }
     if (null != c.getSuperclass(env)) {
        checkConflictingRefinementInAncestor (c.superclass1, refinement);
     }
     for (int i=0; i<c.getInterfaces(env).length; i++) {
        checkConflictingRefinementInAncestor (c.interfaces1[i], refinement);
     }
   }

   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected void processVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, MemberVariableDeclaration variable, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if (variable.initializer != null
      && !variable.hasError
      && !variable.target.isConstant()) {
          pushContext (new FieldInitializerContext (env, this, variable));
          processJavaExpression (variable.owner, variable.initializer, 0, null, null, null);
          popContext();

          try {
              if(!variable.initializer.canBeAssignedTo(env, variable.dataType())) {
                   parserError (2, "Incompatible types for initialization",
                                 variable.sourceStartPosition,
                                 variable.sourceEndPosition);
                   variable.hasError = true;
                   return;
              }
              variable.initializer = variable.initializer.convertForAssignmentTo (env, variable.dataType());

              // dimension checking

              // 0 may be assigned to any dimensioned number...
              if ( variable.initializer.dimensionSignature==null
              &&   variable.initializer.isZeroLiteralOrPrimitiveCastOfZero()) {
                 // just OK;
              }
              else if (!Dimension.equal (variable.target.dimensionSignature,
                                         variable.initializer.dimensionSignature)) {
                    // ...other clashes are prohibited
                    parserError (2, "Variable and initializer have different dimensions"
+": "+Dimension.getPresentation(variable.target.dimensionSignature)
+" and "+Dimension.getPresentation(variable.initializer.dimensionSignature),
                                 variable.initializer.sourceStartPosition,
                                 variable.initializer.sourceEndPosition);
              }

          } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
          } catch (java.io.IOException e) {parserError (2, e.getMessage(), variable.initializer.nameStartPosition, 
                                                                      variable.initializer.nameEndPosition);
          }
      }
   }

   protected void processMethodDeclaration (MethodDeclaration method, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
/*
    $8.4.6.4 Inheriting Methods with the Same Signature
    If one of the inherited methods is not abstract, then there are two subcases: 
          If the method that is not abstract is static, a compile-time error occurs. 
          Otherwise, the method that is not abstract is considered to override,
             and therefore to implement, all the other methods on behalf of the class
             that inherits it. A compile-time error occurs if, comparing the method
             that is not abstract with each of the other of the inherited methods,
             for any such pair, either they have different return types or one has a
             return type and the other is void.
          Moreover, a compile-time error occurs if the inherited method that is not
             abstract has a throws clause that conflicts (§8.4.4) with that of any
          other of the inherited methods. 
    If none of the inherited methods is not abstract, then the class is necessarily
     an abstract class and is considered to inherit all the abstract methods.
     A compile-time error occurs if, for any two such inherited methods, either they have
     different return types or one has a return type and the other is void.
     (The throws clauses do not cause errors in this case.)

***/
      checkConflictingRefinementInAncestor (classType, method);
/////      parser.reparseMethodBody (method);
      super.processMethodDeclaration (method, level, retValue, arg1, arg2);
   }

   protected void processThrowsClause (MethodDeclaration methodDeclaration, ThrowsClause throwsClause, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      for (int i=0; i<methodDeclaration.target.exceptionTypes.length; i++) {
         DataTypeDeclaration exceptionType = throwsClause.exceptionTypeDeclarations.get(i);
         try {
             if (!exceptionType.dataType.isSubtypeOf (env, env.javaLangThrowableType)) {
               parserError (2, "Element "+exceptionType.name
                           +" in throws clause is no descendant of class Throwable",
                            exceptionType.nameStartPosition, 
                            exceptionType.nameEndPosition);
             }
         } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
         } catch (java.io.IOException e) {parserError (2, e.getMessage(), exceptionType.nameStartPosition, 
                                                                     exceptionType.nameEndPosition);
         }
    }
   }

   // Hmmm...try hard to see whether the constructor starts with calling
   // this(...) or super(...). Then move that call to otherConstructorInvocation

   protected void processConstructorDeclaration (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
//////////   parser.reparseMethodBody (constructorDeclaration);

      if (constructorDeclaration.statements            == null
      ||  constructorDeclaration.statements.statements == null) {
               parserError (2, "Internal error: constructor statements null",
                            constructorDeclaration.sourceStartPosition, 
                            constructorDeclaration.sourceEndPosition); 
               return;
      }
      if (!constructorDeclaration.statements.statements.isEmpty()) {
        JavaStatement firstStatement = constructorDeclaration.statements.statements.get(0);
        if (firstStatement  instanceof   ExpressionStatement) {
          ExpressionStatement      es = (ExpressionStatement) firstStatement;
          if (es.expression instanceof   MethodCallExpression) {
              MethodCallExpression mc = (MethodCallExpression) es.expression;
              JavaExpression       je =                        mc.methodAccessExpression;
              if (je.isThis ()
                ||je.isSuper()
                ||je.languageConstructCode()==QualifiedThisExpressionCode) {
                mc.setSpecialMode();
                constructorDeclaration.statements.statements.remove(0);
                constructorDeclaration.otherConstructorInvocation = mc; // at last...
                if (!constructorDeclaration.statements.statements.isEmpty()) {
                  firstStatement = constructorDeclaration.statements.statements.get(0);
                  constructorDeclaration.statements.  nameStartPosition = 
                  constructorDeclaration.statements.sourceStartPosition = 
                                     firstStatement.sourceStartPosition;
                }
              }
          }
        }
      }
      if (constructorDeclaration.otherConstructorInvocation == null) {
         //insert super call....
         SpecialNameExpression superName  = new SpecialNameExpression(SuperToken);
         MethodCallExpression superCall   = new MethodCallExpression();
         superCall.name                   = "super";
         superCall.methodAccessExpression = superName;
         superCall.setSpecialMode();
         constructorDeclaration.otherConstructorInvocation = superCall;
         superName.nameStartPosition = superName.sourceStartPosition = 
         superCall.nameStartPosition = superCall.sourceStartPosition =
                            constructorDeclaration.nameStartPosition;
         superName.  nameEndPosition = superName.  sourceEndPosition =
         superCall.  nameEndPosition = superCall.  sourceEndPosition =
                          constructorDeclaration.    nameEndPosition;
      }
      constructorDeclaration.target.nextVariableSlot = 1; // not in pass 7, since extra parameters
                                                          // may be inserted soon.
      super.processConstructorDeclaration (constructorDeclaration, level, retValue, arg1, arg2);
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                           Little routines                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processParameterDeclaration  (RefinementDeclaration declaration, MethodParameterDeclaration parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      RefinementContext rc = (RefinementContext) context;
      rc.pushLocalName (parameter.target);
      parameter.scopeEndPosition = declaration.sourceEndPosition;
      if (rc.parameterNames.containsKey (parameter.name)) {
               parserError (2, "Duplicate parameter name \""
                              + parameter.name 
                              + "\"",
                            parameter.nameStartPosition, 
                            parameter.nameEndPosition); 
      }
      rc.parameterNames.put (parameter.name, parameter);
      parameter.owner      = declaration;
   }

   protected void processReturnStatement (RefinementDeclaration refinement, ReturnStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (refinement.isInitializerBlock()) {
          parserError (2, "Cannot return from initializer",
                       statement.sourceStartPosition, 
                       statement.sourceEndPosition); 
      }
      else if (statement.returnExpression !=  null) {
       processJavaExpression (refinement, statement.returnExpression, level + 1, retValue, arg1, arg2);
       try {
         if (refinement.target.returnType.isVoid())
               parserError (2, "Cannot return value in "
                           + (refinement.isMethodDeclaration()? "void method": "script"),
                            statement.sourceStartPosition, 
                            statement.sourceEndPosition); 

         if (statement.returnExpression.dataType.isResolved()) {  // otherwise already de
           if (statement.returnExpression.dataType.isVoid())
               parserError (2, "Void expression in return statement",
                            statement.sourceStartPosition, 
                            statement.sourceEndPosition); 

           else if (!statement.returnExpression.canBeAssignedTo (env, refinement.target.returnType))
               parserError (2, "Incompatible return type: " + statement.returnExpression.dataType.getPresentation(),
                            statement.sourceStartPosition, 
                            statement.sourceEndPosition); 

           else statement.returnExpression =
                statement.returnExpression.convertForAssignmentTo (env, refinement.target.returnType);

           if ( statement.returnExpression.dimensionSignature==null
           &&   statement.returnExpression.isZeroLiteralOrPrimitiveCastOfZero()) {
                 // just OK;
           }
           else
           if (!Dimension.equal (statement.returnExpression.dimensionSignature,
                                 refinement.target.returnDimensionSignature)) {
                    parserError (2, "Return expression and refinement have different dimensions"
+": "+Dimension.getPresentation(statement.returnExpression.dimensionSignature)
+" and "+Dimension.getPresentation(refinement.target.returnDimensionSignature)
,
                                 statement.returnExpression.sourceStartPosition,
                                 statement.returnExpression.sourceEndPosition);
           }
         }
       } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
       } catch (java.io.IOException e) {parserError (2, e.getMessage(), statement.nameStartPosition, 
                                                                   statement.  nameEndPosition);
       }
      } else if (!refinement.target.returnType.isVoid())
            parserError (2, "Should return a value",
                         statement.sourceStartPosition, 
                         statement.sourceEndPosition); 
   }

   protected void processThrowStatement (RefinementDeclaration refinement, ThrowStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     processJavaExpression (refinement, statement.throwExpression, level + 1, retValue, arg1, arg2);
     try {
       if (!statement.throwExpression.dataType.isSubtypeOf (env, env.javaLangThrowableType)) {
            parserError (2, "Thrown value should have subtype of Throwable",
                         statement.sourceStartPosition, 
                         statement.sourceEndPosition); 
       }
     } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
     } catch (java.io.IOException e) {parserError (2, e.getMessage(), refinement.nameStartPosition, 
                                                                 refinement.  nameEndPosition);
     }
   }


   /* JAVA SCOPING RULES. The following Java constructs
      confine their contained declarations to the end of their own scopes:
      
         StatementBlock
         NestedStatement
         ForStatement
         SwitchStatement
         TryStatement 
               (the "try" part, each "catch" block separately,
                and the "finally" part) */

   protected void processStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	  ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();
      processJavaStatements (refinement, statementBlock.statements, level + 1, retValue, localDeclarations, arg2);
      context.popLocalDeclarations (localDeclarations, statementBlock);
   }

   protected void processNestedStatement             (RefinementDeclaration refinement, NestedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	  ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();
      processJavaStatements (refinement, statement.statements, level + 1, retValue, localDeclarations, arg2);
      context.popLocalDeclarations (localDeclarations, statement);
   }

   protected void processExpressionStatement         (RefinementDeclaration refinement, ExpressionStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processExpressionStatement (refinement, statement, level, retValue, arg1, arg2);
      if (statement.specialCode == SpecialCode.none) {
          statement.expression.setTopLevelExpression();
      }
      else {
          // statement.specialCode == SpecialCode.singleSuccessTest or doubleSuccessTest
          // Success testing expression "xxxx ??;"
          // Code to be generated essentially like "if (!_n_.setSuccess(xxxx)) return;"
          if (statement.expression.dataType != BooleanType.theOne) {
               parserError (2, "Boolean type expected",
                            statement.sourceStartPosition, 
                            statement.  sourceEndPosition); 
          }
      }
   }

   protected void processForStatement                (RefinementDeclaration refinement, ForStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	   ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();

      processJavaStatements (refinement, statement.initStatements, level + 1, retValue, localDeclarations, arg2);
      if (statement.conditionExpression != null) {
         processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, localDeclarations, arg2);
         if (statement.conditionExpression.dataType.isResolved()
         && !statement.conditionExpression.dataType.isBoolean()) {
               parserError (2, "Boolean type expected",
                            statement.conditionExpression.sourceStartPosition, 
                            statement.conditionExpression.sourceEndPosition); 
         }
      }
      processJavaStatements (refinement, statement.loopStatements, level + 1, retValue, localDeclarations, arg2);
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, localDeclarations, arg2);

      context.popLocalDeclarations (localDeclarations, statement.statement);
   }

   protected void processSwitchStatement (RefinementDeclaration refinement, SwitchStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression  (refinement, statement.switchExpression, level + 1, retValue, arg1, arg2);
      if (!statement.switchExpression.dataType.isSmallIntegral()
      &&   statement.switchExpression.dataType!=IntType.theOne    ) {
            parserError (2, "Integer or subtype expected",
                         statement.switchExpression.sourceStartPosition, 
                         statement.switchExpression.sourceEndPosition);
            statement.switchExpression.dataType = IntType.theOne;
            statement.switchExpression.constantValue = null;
      }
      if (statement.switchExpression.dimensionSignature != null
      &&  statement.switchExpression.dimensionSignature != Dimension.errorSignature) {
          parserError (2, "Switch expression should not have dimension; instead it is: "
                           +Dimension.getPresentation(statement.switchExpression.dimensionSignature),
                       statement.switchExpression.sourceStartPosition,
                       statement.switchExpression.sourceEndPosition);
      }

	  ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();
      processJavaStatements  (refinement, statement.statements, level + 1, retValue, localDeclarations, statement);
      context.popLocalDeclarations (localDeclarations, statement);
   }

   protected void processCaseTag (RefinementDeclaration refinement, CaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     processJavaExpression (refinement, statement.tagExpression, level + 1, retValue, arg1, arg2);

     SwitchStatement switchStatement = (SwitchStatement) arg2;

     // checks: isConstant, canBeAssignedTo en no duplicates.

     try {

      if (!statement.tagExpression.isConstant()) {
            parserError (2, "Constant expression expected",
                         statement.tagExpression.sourceStartPosition, 
                         statement.tagExpression.sourceEndPosition);
            return;
      }
      if (switchStatement.switchExpression.dataType.isSmallOrNormalInt()) {

          if (!statement.tagExpression.canBeAssignedTo (env, switchStatement.switchExpression.dataType)) {
            parserError (2, "Not assignable to type of switch expression ("
                        +switchStatement.switchExpression.dataType.getNameWithDots()+")",
                         statement.tagExpression.sourceStartPosition, 
                         statement.tagExpression.sourceEndPosition);
            return;
          }
      }
      if (!statement.tagExpression.dataType.canBeAssignedTo(env, IntType.theOne)) {
            parserError (2, "Integer type expected",
                         statement.tagExpression.sourceStartPosition, 
                         statement.tagExpression.sourceEndPosition);
            return;
      }
      int tagValue = statement.tagValue();

      insertTheTagIntoTheSortedVector:
      {
    	 int i = 0;
         for (CaseTag c: switchStatement.caseTags) {
            int otherValue = c.tagValue();
            if (tagValue < otherValue) {
                switchStatement.caseTags.add (i, statement);
                break insertTheTagIntoTheSortedVector;
            }
            else if (tagValue == otherValue) {
                parserError (2, "Duplicate tag value",
                              statement.tagExpression.sourceStartPosition, 
                              statement.tagExpression.sourceEndPosition);
            }
            i++;
         }
         switchStatement.caseTags.add (statement);
      }
     } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
     } catch (java.io.IOException e) {parserError (2, e.getMessage(), switchStatement.nameStartPosition, 
                                                                 switchStatement.  nameEndPosition);
     }
   }

   protected void processDefaultCaseTag (RefinementDeclaration refinement, DefaultCaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      SwitchStatement switchStatement = (SwitchStatement) arg2;
      if (switchStatement.defaultTag!=null) {
        parserError (2, "Duplicate default tag",
                     statement.sourceStartPosition, 
                     statement.sourceEndPosition);
      } else {
         switchStatement.defaultTag        = statement;
         switchStatement.hasPureDefaultTag = true;
      }
   }


   protected void processTryStatement                (RefinementDeclaration refinement, TryStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      processTryBlock        (refinement, statement.tryBlock, level    , retValue, arg1, arg2);
      processJavaStatements  (refinement, statement.catches , level + 1, retValue, arg1, arg2);

      if (!statement.finalStatements.isEmpty ()) {
  	     ArrayList<LocalVariableOrParameter> finallyDeclarations = new ArrayList<LocalVariableOrParameter>();
         processJavaStatements  (refinement, statement.finalStatements, level + 1, retValue, finallyDeclarations, arg2);
         context.popLocalDeclarations (finallyDeclarations, statement.finalStatements.get(statement.finalStatements.size()-1));
      }
   }

   protected void processCatchBlock                  (RefinementDeclaration refinement, CatchBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

	ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter> (1);
    processLocalVariableDeclaration (refinement, statement, statement.catchVariable, level + 1, retValue, localDeclarations, arg2);

    DataType d = statement.catchVariable.dataType();

    try {
      if (!d.isSubtypeOf (env, env.javaLangThrowableType)) {
            parserError (2, "Catch variable should be subtype of java.lang.Throwable",
                         statement.catchVariable.sourceStartPosition, 
                         statement.catchVariable.sourceEndPosition);
      }
      else if (d.isSubtypeOf (env, env.javaLangRuntimeExceptionType)
           ||  d == env.javaLangExceptionType
           ||  d == env.javaLangThrowableType) {
               statement.catchesAnything = true;
      }
    } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), statement.nameStartPosition, 
                                                                statement.  nameEndPosition);
    }
    processJavaStatements (refinement, statement.statements, level + 1, retValue, localDeclarations, arg2);
    context.popLocalDeclarations (localDeclarations, statement);
   }

   protected void processIfStatement                 (RefinementDeclaration refinement, IfStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      if (statement.conditionExpression.dataType.isResolved()
      && !statement.conditionExpression.dataType.isBoolean()) {
            parserError (2, "Boolean type expected",
                         statement.conditionExpression.sourceStartPosition, 
                         statement.conditionExpression.sourceEndPosition); 
      }
      processJavaStatement  (refinement, statement.trueStatement, level + 1, retValue, arg1, arg2);
      if (statement.falseStatement != null)
         processJavaStatement  (refinement, statement.falseStatement, level + 1, retValue, arg1, arg2);
   }

   protected void processWhileStatement              (RefinementDeclaration refinement, WhileStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      if (statement.conditionExpression.dataType.isResolved()
      && !statement.conditionExpression.dataType.isBoolean()) {
            parserError (2, "Boolean type expected",
                         statement.conditionExpression.sourceStartPosition, 
                         statement.conditionExpression.sourceEndPosition); 
      }
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
   }

   protected void processDoStatement                 (RefinementDeclaration refinement, DoStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      if (statement.conditionExpression.dataType.isResolved()
      && !statement.conditionExpression.dataType.isBoolean()) {
            parserError (2, "Boolean type expected",
                         statement.conditionExpression.sourceStartPosition, 
                         statement.conditionExpression.sourceEndPosition); 
      }
   }

   protected void processSynchronizedStatement       (RefinementDeclaration refinement, SynchronizedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (refinement, statement.synchronizedExpression, level + 1, retValue, arg1, arg2);
      if (!statement.synchronizedExpression.dataType.isReference()) {
            parserError (2, "Reference type expected",
                         statement.synchronizedExpression.sourceStartPosition, 
                         statement.synchronizedExpression.sourceEndPosition); 
      }
      processNestedStatement  (refinement, statement, level, retValue, arg1, arg2);
   }


   /* ------------------------------------------------------------------ */

   protected void processArrayInitializer (FieldDeclaration fieldDeclaration, ArrayInitializer expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processArrayInitializer       (fieldDeclaration, expression, level, retValue, arg1, arg2);

      boolean seenElement = false;
      for (JavaExpression je: expression.elementExpressions) {
         if (je.dimensionSignature == null
         &&  je.isZeroLiteralOrPrimitiveCastOfZero()) continue;
         if (seenElement) {
            if(!Dimension.equal (je.dimensionSignature, expression.dimensionSignature)) {
               parserError (2, "Incompatible dimension for array element: "
                           +Dimension.getPresentation(        je.dimensionSignature)
                 + " and " +Dimension.getPresentation(expression.dimensionSignature),
                            je.nameStartPosition, 
                            je.nameEndPosition); 
             
            }
         }
         else {
           seenElement = true;
           expression.dimensionSignature = je.dimensionSignature;
         }
      }
   }

   protected void processLocalVariableDeclaration  (RefinementDeclaration refinement, JavaStatement statement, LocalVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      mustResolveDataTypeDeclaration  (variableDeclaration.dataTypeDeclaration);
      getDimensionSignatureOf (variableDeclaration.dataTypeDeclaration);

      ArrayList<LocalVariable> localDeclarations  = (ArrayList<LocalVariable>) arg1;
      variableDeclaration.owner = refinement; // bit of a hack, but owner was not set already
                                        // to do this during the parse pass would require some reorganisations...
      if (context.checkReservedIdentifier (variableDeclaration)) {
               parserError (2, "Redeclaration of reserved name \""
                              + variableDeclaration.getName()
                              + "\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition); 
               return;
      }

      Variable existingVariable 
            = context.getLocalName (variableDeclaration.getName());
      if (existingVariable != null) {
               parserError (2, "Redeclaration of "
                              + existingVariable.getConstructName ()
                              + " name \""
                              + variableDeclaration.getName()
                              + "\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition); 
      }

      variableDeclaration.makeTarget     (env);

      /* Process the initializer before the new declaration takes effect */
      if (variableDeclaration.initializer != null) {
         processJavaExpression (refinement, variableDeclaration.initializer, level+1, retValue, arg1, arg2);

         if (variableDeclaration.initializer.dataType.isResolved()
         ||  variableDeclaration.initializer.languageConstructCode()==ArrayInitializerCode) {

           try {
              if (!variableDeclaration.initializer.canBeAssignedTo (env, variableDeclaration.dataType())) {
                   parserError (2, "Incompatible types for initialization",
                                 variableDeclaration.sourceStartPosition,
                                 variableDeclaration.sourceEndPosition);
              } else {
                  variableDeclaration.initializer =
                  variableDeclaration.initializer.convertForAssignmentTo (env, variableDeclaration.dataType());
              }

              // dimension checking

              // 0 may be assigned to any dimensioned number...
              if ( variableDeclaration.initializer.dimensionSignature==null
              &&   variableDeclaration.initializer.isZeroLiteralOrPrimitiveCastOfZero()) {
                 // just OK;
              }
              else if (!Dimension.equal (variableDeclaration.target     .dimensionSignature,
                                         variableDeclaration.initializer.dimensionSignature)) {
                    // ...other clashes are prohibited
                    parserError (2, "Variable and initializer have different dimensions"
+": "+Dimension.getPresentation(variableDeclaration.target.dimensionSignature)
+" and "+Dimension.getPresentation(variableDeclaration.initializer.dimensionSignature),
                                 variableDeclaration.initializer.sourceStartPosition,
                                 variableDeclaration.initializer.sourceEndPosition);
              }

            } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
            } catch (java.io.IOException e) {parserError (2, e.getMessage(), variableDeclaration.initializer.nameStartPosition, 
                                                                        variableDeclaration.initializer.  nameEndPosition);
            }
         }

      }

      /* Add the declaration to the environment, but DO NOT add it to the
         collection of the script's local declarations and DO NOT assign a
         declaration index */
      context.pushLocalName (variableDeclaration.target);
//for testing:
//pushLocalDeclaration ((BasicVariableDeclaration) variableDeclaration);
//NOTE: This causes declarationIndexes and slotNumbers to be assigned,
//      making the generated code unusable
      localDeclarations.add (variableDeclaration.target);


   }

   /*******************************************************************/
   /**                                                               **/
   /**                      SCRIPT DECLARATIONS                      **/
   /**                                                               **/
   /*******************************************************************/


   protected void processScriptDeclaration (ScriptDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    checkConflictingRefinementInAncestor (classType, decl);
    //parser.reparseScriptBody(decl);

    classType.addMethod (decl.makeTarget (env));

    if (!typeDeclaration.isInterface()
    &&  !decl.isNative  ()   // illegal, but non-fatal
    &&  !decl.isAbstract()) {
      classType.addMemberVariable (decl. templateVariable = new ScriptTemplateVariable (env, decl));
      classType.addMethod         (decl.templateGetMethod = new ScriptTemplateGetMethod(env, decl.templateVariable));
      classType.addMethod         (decl.       codeMethod = new ScriptCodeMethod       (env, decl));
    }
    super.processScriptDeclaration (decl, level, retValue, arg1, arg2);
   }
   protected void processCommunicationDeclaration (CommunicationDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    checkConflictingRefinementInAncestor (classType, decl);
    //parser.reparseScriptBody(decl);
    if (!typeDeclaration.isInterface()
    &&  !decl.isNative  ()   // illegal, but non-fatal
    &&  !decl.isAbstract()) {
      classType.addMemberVariable (decl. templateVariable = new ScriptTemplateVariable (env, decl));
      classType.addMethod         (decl.templateGetMethod = new ScriptTemplateGetMethod(env, decl.templateVariable));
      classType.addMethod         (decl.       codeMethod = new ScriptCodeMethod  (env, decl));
    }
    super.processCommunicationDeclaration (decl, level, retValue, arg1, arg2);
   }

   protected void processCommunicationPartner   (CommunicationDeclaration decl,
                                                 CommunicationPartnerDeclaration partner, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if (partner.firstOccurrence != null) return;
      classType.addMethod (partner.makeTarget (env));

      if (typeDeclaration.isInterface()
      ||  decl.isNative  ()   // illegal, but non-fatal
      ||  decl.isAbstract()) {
          return;
      }
      classType.addMemberVariable (partner. templateVariable = new ScriptPartnerTemplateVariable (env, partner));
      classType.addMethod         (partner.templateGetMethod = new ScriptPartnerTemplateGetMethod(env, partner.templateVariable));

      super.processCommunicationPartner (decl, partner, level, retValue, arg1, arg2);

      // this must be the last thing because it may damage the expression tree a bit...
   }


   protected void processChannelDeclaration (ChannelDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    checkConflictingRefinementInAncestor (classType, decl);
    //parser.reparseScriptBody(decl);
    if (!typeDeclaration.isInterface()
    &&  !decl.isNative  ()   // illegal, but non-fatal
    &&  !decl.isAbstract()) {
      classType.addMemberVariable (decl.        templateVariable = new        ScriptTemplateVariable (env, decl));
      classType.addMemberVariable (decl. partnerTemplateVariable = new ScriptPartnerTemplateVariable (env, decl));
      classType.addMethod         (decl.       templateGetMethod = new        ScriptTemplateGetMethod(env, decl.       templateVariable));
      classType.addMethod         (decl.partnerTemplateGetMethod = new ScriptPartnerTemplateGetMethod(env, decl.partnerTemplateVariable));
      classType.addMethod         (decl.              codeMethod = new ScriptCodeMethod              (env, decl));
    }
    super.processChannelDeclaration (decl, level, retValue, arg1, arg2);
   }


   /*******************************************************************/
   /**                                                               **/
   /**                      SCRIPT EXPRESSIONS                       **/
   /**                                                               **/
   /*******************************************************************/



   //protected ArrayList localDataDeclarations   = new ArrayList (1);


/**

The purpose of the script expression processing methods below is to collect
all script local name declarations (parameters, local variables, 
private variables), assign declarationIndexes and slotNumbers,
and resolve all references according to Scriptic scope rules.

Here's how it works. The scriptExpression parse tree
is traversed recursively in its natural order (which corresponds to textual
order in the source code). Every declaration is added to the 
"localDataDeclarations" vector as it is encountered; this vector will end up
containing the complete set of declarations when finished. In addition,
every declaration is added to "currentNameEnvironment". This is a Hashtable
which maps the name (a String) of the declaration to a Vector
containing all declarations of that name; the last element of the vector
is the "current" declaration. The intention is for "currentNameEnvironment"
to contain the currently visible declarations at any point during the recursive
traversal. The issue, then, is to remove declarations from 
"currentNameEnvironment" where appropriate.

Scriptic scope rules require that declarations sometimes live beyond the parse subtree
they are declared in. So we need to pass the declarations around among methods.
For this purpose, we use generic argument "arg1" as a Vector. 
Declaration-containing nodes add the declarations to this vector. Other nodes
can then use and/or manipulate this argument to remove them when required.

E.g., to confine declarations occurring in a certain construct (like ScriptExpression)
to that construct, pass a new Vector as the "arg1" to the traversal of its subtree,
and afterward remove from the environment all the declarations that have accumulated
in that vector. 

For a more creative use of this mechanism, see InfixExpression.

Another issue is to keep track of special situations in the environment
as a result of certain script constructs. For example: if a code fragment
contains an assignment to "duration" in its attribute setting section,
Java expressions within the scope of that code fragment may refer to
the initial duration value by "old.duration", and to the object that
represents the remaining duration by "script.this".

We keep track of these special situations with the second generic argument,
"arg2". This is a Hashtable containing Strings as keys and arbitrary objects
as values. Currently the following situations are signalled by
the presence of the corresponding key (with an arbitrary, non-null value)
in this Hashtable:

   Key value                         Purpose
   ============================   ======================================
   HasDurationAssignment          The current expression is within the
                                  scope of a code fragment that has a
                                  "duration" assignment.
                                  Allow references to "old.duration",
                                  "duration" and "script.this"

   ProcessingDurationAssignment   The current expression is (part of)
                                  an actual "duration" assignment expression
                                  itself. This is used to present slightly
                                  better worded error messages.

   AllowPriorityAssignment        The current expression is within the
                                  scope of a code fragment in which direct,
                                  straight assignments to "priority"
                                  are allowed. This applies to activation
                                  and tiny code fragments.

   ProcessingPriorityAssignment   The current expression is (part of)
                                  an actual "priority" assignment expression
                                  itself. This is used to present slightly
                                  better worded error messages.
*/

   protected void processTopLevelScriptExpression (BasicScriptDeclaration scriptDeclaration, ScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      //localDataDeclarations   = new ArrayList ();
 
      /* Create initial environment, consisting of parameters. */

      int declarationIndex = 0;
      if (scriptDeclaration.parameterList != null) {
        for (MethodParameterDeclaration parameter: scriptDeclaration.parameterList.parameters) {
            scriptContext.pushLocalName (parameter.target);
            parameter.scopeEndPosition = expression.sourceEndPosition;
            parameter.declarationIndex = declarationIndex;
            declarationIndex++;
        }
      }

      /* Do the stuff */
      ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();
      processScriptExpression (scriptDeclaration, expression, 0, null, localDeclarations, arg2);

      /* Pop leftover declarations, in order to set their scopeEndPosition */
      scriptContext.popLocalDeclarations (localDeclarations, expression); 

      /* Save the results for posterity */
      //scriptDeclaration.localDataDeclarations = localDataDeclarations;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                       Declaration scoping                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


   /* ----------- Declaration scoping - Script Expression ----------- */

   // Scope of declarations in a script expression
   // is confined to that expression.

   protected void processScriptExpression (BasicScriptDeclaration scriptDeclaration, ScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	  ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();
      super.processScriptExpression (scriptDeclaration, expression, level, retValue, localDeclarations, arg2);
      scriptContext.popLocalDeclarations (localDeclarations, expression);
   }

   private void checkAnchorExpressionIsCodeInvokerType(JavaExpression anchorExpression, String forConstruct)
   {
 	  try
 	  {
          if (!anchorExpression.dataType.isSubtypeOf (env, env.scripticVmCodeInvokerType))
          {
              parserError (2, "Anchor expression should implement scriptic.vm.CodeInvoker for "+forConstruct,
            		  anchorExpression.sourceStartPosition, 
            		  anchorExpression.sourceEndPosition);
          }
 	  }
 	  catch (CompilerError e)
 	  {
           parserError (2, "Unexpected CompilerError",
         		  anchorExpression.sourceStartPosition, 
         		  anchorExpression.sourceEndPosition); 
 	  }
 	  catch (IOException e)
 	  {
           parserError (2, "Unexpected IO exception",
         		  anchorExpression.sourceStartPosition, 
         		  anchorExpression.sourceEndPosition); 
 	  }
   }

   /* ------------ Declaration scoping - Code Fragments ------------- */

   // Scope of declarations in a code fragment
   // is confined to that code fragment. Assignment to "duration"
   // is treated as a declaration of the corresponding constructs for
   // the remainder of the code fragment. Other manipulations of
   // the boolean flags in ScripticContext are mostly for the purpose of
   // enabling better error messages.

   protected void processNativeCodeFragment          (BasicScriptDeclaration scriptDeclaration, NativeCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	   ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();

	  if (expression.anchorExpression != null) {
         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, localDeclarations, arg2);
         checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "code fragment");
 	  }
      if (expression.durationAssignment != null) {
         scriptContext.processingDurationAssignment = true;
         processJavaStatement  (scriptDeclaration, expression.durationAssignment, level + 1, retValue, localDeclarations, arg2);
         scriptContext.processingDurationAssignment = false;
      }
      if (expression.priorityAssignment != null) {
         scriptContext.processingPriorityAssignment = true;
         processJavaStatement  (scriptDeclaration, expression.priorityAssignment, level + 1, retValue, localDeclarations, arg2);
         scriptContext.processingPriorityAssignment = false;
      }

      // Signal presence of "duration"
      if (expression.durationAssignment != null) {
         scriptContext.hasDurationAssignment = true;
      }

      // For a tiny code fragment, signal permission to assign "priority"
      if (expression.startingDelimiter == BraceColonOpenToken)
         scriptContext.allowPriorityAssignment = true;

      processJavaStatements    (scriptDeclaration, expression.statements, level + 1, retValue, localDeclarations, arg2);

      // Clean up the "duration" signal.
      // NOTE: This would fail if the specialSituationsEnvironment
      //       already contained the signal before entering this method...
      //       which is currently impossible because code fragments
      //       cannot be nested.
      scriptContext.  hasDurationAssignment = false;
      scriptContext.allowPriorityAssignment = false;

      scriptContext.popLocalDeclarations (localDeclarations, expression);
   }

   protected void processEventHandlingCodeFragment   (BasicScriptDeclaration scriptDeclaration, EventHandlingCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();
      if (expression.anchorExpression == null) {
          parserError (2, "Anchor expression (@...:) missing for event handling code fragment",
        		  expression.sourceStartPosition, 
        		  expression.sourceEndPosition); 
      }
      else
      {
          processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, localDeclarations, arg2);
          if (!expression.anchorExpression.dataType.isReference())
          {
              parserError (2, "Anchor expression should have a reference type for event handling code fragment",
            		  expression.anchorExpression.sourceStartPosition, 
            		  expression.anchorExpression.sourceEndPosition); 
          }
      }
      processJavaStatements (scriptDeclaration, expression.statements      , level + 1, retValue, localDeclarations, arg2);
      scriptContext.popLocalDeclarations (localDeclarations, expression);
   }

   protected void processActivationCode          (BasicScriptDeclaration scriptDeclaration, ActivationCode expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "activation code");
      }
      // Signal permission to assign "priority" in the 
      // activation code fragment
      scriptContext.allowPriorityAssignment = true;
      processScriptExpression  (scriptDeclaration, expression.activationCode, level + 1, retValue, arg1, arg2);
      scriptContext.allowPriorityAssignment = false;

      processScriptExpression  (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processDeactivationCode          (BasicScriptDeclaration scriptDeclaration, DeactivationCode expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "deactivation code");
      }
      // Signal permission to use "success" in the deactivation code fragment
      processScriptExpression  (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);

      scriptContext.allowSuccessUsage = true;
      processScriptExpression  (scriptDeclaration, expression.deactivationCode, level + 1, retValue, arg1, arg2);
      scriptContext.allowSuccessUsage = false;

   }


   /* ------------------------------------------------------------------ */

   protected void processScriptCallExpression (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    try {
  	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "script call");
      }
      boolean foundError = false;

      if (expression.parameterList != null) {
         processScriptCallParameterList (scriptDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
         foundError |= expression.parameterList.hasError();
      }

      processJavaExpression (scriptDeclaration, expression.scriptAccessExpression, level + 1, retValue, arg1, arg2);

/******** this part is obsolete; it never should have worked or so .... ??????????
      if (expression.scriptAccessExpression.isSuper()) {
             expression.setSuperMode(); // will be used below...
      }
      else if (expression.scriptAccessExpression.dataType==UnresolvedClassOrInterfaceType.one) {
           // prevent message "Unresolved "+refinementType+" call "
//if (expression.target==null)
throw new RuntimeException ("processScriptCallExpression, UnresolvedClassOrInterfaceType: "
+expression.name
+" access: "+expression.scriptAccessExpression.name);
//              return;
      }
      if (!foundError)
************/
/******************
if (foundError)
throw new RuntimeException ("processScriptCallExpression, foundError: "
+expression.name);
*******************/
      expression.target = resolveRefinementCall (expression, "script");

    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e)      {parserError (2, e.getMessage());
    } catch (IOException   e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                          expression.  sourceEndPosition);
    }
   }


   /* ------------------------------------------------------------------ */

   protected void processScriptCallParameter      (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, ScriptCallParameter parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression       (scriptDeclaration, parameter.expression, level + 1, retValue, arg1, arg2);

      parameter.dataType = parameter.expression.dataType;

      if (parameter.isOutput) {
        if (parameter.expression instanceof JavaExpressionWithTarget) {
            JavaExpressionWithTarget argument = (JavaExpressionWithTarget) parameter.expression;
            if (argument.target==null) {
               //already handled  ??
               return;
            }
            if (argument.target.isMemberVariable()   // parameter.target would be wrong here...
            &&  ((MemberVariable)argument.target).isFinal()) {
                   parserError (2, "Output parameter should be assignable",
                           parameter.sourceStartPosition, 
                           parameter.sourceEndPosition);
            }
        }
        else if (!(parameter.expression instanceof ArrayAccessExpression)) {
           parserError (2, "Output parameter should be assignable",
                         parameter.sourceStartPosition, 
                         parameter.sourceEndPosition);
        }
      }

      if (parameter.isAdapting) { // then must also be a formal parameter
        if (!(parameter.expression instanceof JavaExpressionWithTarget)) {
                parserError (2, "Invalid adapting parameter (must be a formal parameter name)",
                         parameter.sourceStartPosition, 
                         parameter.sourceEndPosition);
                return;
        }
        JavaExpressionWithTarget argument = (JavaExpressionWithTarget) parameter.expression;

        if (argument.target==null) {
           //already handled
           return;
        }

        if (argument.target.isMethodParameter ()) {
parameter.target = (Parameter) argument.target;
         //       parameter.formalParameter      = (Parameter) argument.target;
         //       parameter.formalParameterIndex = argument.declarationIndex;
            } else {
                parserError (2, "Invalid adapting parameter \""
                           + argument.getName()
                           + "\" (must be a formal parameter name)",
                         parameter.sourceStartPosition, 
                         parameter.sourceEndPosition);
           }

      }
   }

   /* ----------- Declaration scoping - Script Local Data ----------- */

   // Scope of script local data (including "private" declarations)
   // extends beyond the parse subtree they occur in. For example:
   // in "int i: a; b; c", the scriptTerm of the "int i" declaration
   // is only the "a" (in accordance with the Scriptic grammar rules!),
   // but obviously the scope of "int i" extends to
   // b and c as well. So in processing the declaration, we shouldn't call
   // "this.processScriptExpression (...expression.scriptTerm...)",
   // because that method calls popLocalDeclarations(). (See above)

   protected void processScriptLocalDataDeclaration  (BasicScriptDeclaration scriptDeclaration, ScriptLocalDataDeclaration expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      // Copied from superclass
      for (MemberOrLocalVariableDeclaration mvd: expression.variables.variables) {
    	  LocalScriptVariableDeclaration obj = (LocalScriptVariableDeclaration) mvd;
         processScriptLocalVariable (scriptDeclaration, obj, level + 1, retValue, arg1, arg2);
      }

      // call "super" instead of "this", so that the declaration
      // (contained in arg1) will be properly scoped
      super.processScriptExpression (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processPrivateScriptDataDeclaration (BasicScriptDeclaration scriptDeclaration, PrivateScriptDataDeclaration expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      // Copied from superclass
      for (PrivateScriptVariableDeclaration pv: expression.variables) {
         processPrivateScriptVariable (scriptDeclaration, pv, level + 1, retValue, arg1, arg2);
      }

      // call "super" instead of "this", so that the declaration
      // (contained in arg1) will be properly scoped
      super.processScriptExpression (scriptDeclaration, expression.scriptTerm, level + 1, retValue, arg1, arg2);
   }


   /* ------------ Declaration scoping - Infix Operators ------------ */

   protected void processInfixExpression (BasicScriptDeclaration scriptDeclaration, InfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      // As should be clear by now, the trick to implementing scope rules
      // is manipulating the "arg1" argument and calling
      // "popLocalDeclarations()" as appropriate.

	   ArrayList<LocalVariableOrParameter> localDeclarations = new ArrayList<LocalVariableOrParameter>();  /* Declarations passed from operand to next */

      boolean                    firstTime = true;

      for (ScriptExpression se: expression.expressions) {

         if (firstTime) {
            firstTime = false;

            /* FIRST operand -> pass declarations on to other operands of the SAME operator */
          //super.processScriptExpression (scriptDeclaration, se, level + 1, retValue, localDeclarations, arg2);

          // To pass declarations on to other operands AND parent:
            super.processScriptExpression (scriptDeclaration, se, level + 1, retValue, arg1, arg2);
         } else {

            /* OTHER operands -> only valid in the operand itself (default behaviour of this.processScriptExpression()) */
          //this.processScriptExpression (scriptDeclaration, se, level + 1, retValue, arg1, arg2);

          // To pass on to subsequent operands of the SAME operator:
            super.processScriptExpression (scriptDeclaration, se, level + 1, retValue, localDeclarations, arg2);
         }
      }

      /* Remove collected declarations */
      scriptContext.popLocalDeclarations (localDeclarations, expression);
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                     Declaration processing                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processScriptLocalVariable  (BasicScriptDeclaration scriptDeclaration, LocalScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	   ArrayList<LocalVariable> localDeclarations = (ArrayList<LocalVariable>) arg1;

      variableDeclaration.owner = scriptDeclaration; // bit of a hack, but owner was not set already
                                        // to do this during the parse pass would require some reorganisations...
                                        // just like in processScriptLocalVariable
      if (context.checkReservedIdentifier (variableDeclaration)) {
               parserError (2, "Redeclaration of reserved name \""
                              + variableDeclaration.getName()
                              + "\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition); 
               return;
      }

      Variable existingDeclaration = scriptContext.getLocalName (variableDeclaration.getName());
      if (existingDeclaration != null) {
               parserError (2, "Redeclaration of "
                              + existingDeclaration.getConstructName ()
                              + " name \""
                              + variableDeclaration.getName()
                              + "\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition); 
      }

      /* Process the initializer before the new declaration takes effect */
      if (variableDeclaration.initializer != null)
         processJavaExpression (scriptDeclaration, variableDeclaration.initializer, level + 1, retValue, arg1, arg2);

      mustResolveDataTypeDeclaration  (variableDeclaration.dataTypeDeclaration);
      getDimensionSignatureOf (variableDeclaration.dataTypeDeclaration);
      variableDeclaration.makeTarget (env);

      /* Use the new declaration in any case, even if it was in error.
         This probably better reduces the cascade of error messages. */
      scriptContext.pushLocalDeclaration (variableDeclaration.target);
      localDeclarations.add (variableDeclaration.target);
   }

   /* ------------------------------------------------------------------ */

   protected void processPrivateScriptVariable  (BasicScriptDeclaration scriptDeclaration, PrivateScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	   ArrayList<LocalVariable> localDeclarations = (ArrayList<LocalVariable>) arg1;

      if (context.checkReservedIdentifier (variableDeclaration)) {
               parserError (2, "Reserved name \""
                              + variableDeclaration.getName()
                              + "\" cannot be declared \"private\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition); 
               return;
      }

      LocalVariableOrParameter existingVariable = scriptContext.getLocalName (variableDeclaration.getName());
      if (existingVariable == null) {
               parserError (2, "There is no existing local declaration of \""
                              + variableDeclaration.getName()
                              + "\"",
                            variableDeclaration.nameStartPosition, 
                            variableDeclaration.nameEndPosition); 
      } else {
         variableDeclaration.dataTypeDeclaration = ((BasicVariableDeclaration)
                                                       existingVariable.source()).dataTypeDeclaration;
      }
      variableDeclaration.targetDeclaration = (BasicVariableDeclaration) existingVariable.source();
      variableDeclaration.makeTarget     (env);
      variableDeclaration.target.targetVariable = existingVariable;
      scriptContext.pushLocalDeclaration (variableDeclaration.target);
      localDeclarations.add       (variableDeclaration.target);
   }

   /* ------------------------------------------------------------------ */

   protected void processIfScriptExpression      (BasicScriptDeclaration scriptDeclaration, IfScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processIfScriptExpression  (scriptDeclaration, expression, level, retValue, arg1, arg2);
	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "if-expression");
      }
      if (!expression.condition.dataType.isBoolean()) {
            parserError (2, "Boolean type expected",
                         expression.condition.sourceStartPosition, 
                         expression.condition.sourceEndPosition);
      }
   }

   protected void processWhileScriptExpression   (BasicScriptDeclaration scriptDeclaration, WhileScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processWhileScriptExpression  (scriptDeclaration, expression, level, retValue, arg1, arg2);
	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "while-expression");
      }
      if (!expression.condition.dataType.isBoolean()) {
            parserError (2, "Boolean type expected",
                         expression.condition.sourceStartPosition, 
                         expression.condition.sourceEndPosition);
      }
   }

   protected void processForScriptExpression    (BasicScriptDeclaration scriptDeclaration, ForScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "for-expression");
      }
      if (expression.initExpression != null) {
          expression.initExpression.setTopLevelExpression();
          processJavaExpression (scriptDeclaration, expression.initExpression, level + 1, retValue, arg1, arg2);
      }
      if (expression.condition != null) {
         processJavaExpression (scriptDeclaration, expression.condition, level + 1, retValue, arg1, arg2);
         if (!expression.condition.dataType.isBoolean()) {
              parserError (2, "Boolean type expected",
                           expression.condition.sourceStartPosition, 
                           expression.condition.sourceEndPosition);
         }
      }
      if (expression.loopExpression != null) {
          expression.loopExpression.setTopLevelExpression();
          processJavaExpression (scriptDeclaration, expression.loopExpression, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processSwitchScriptExpression  (BasicScriptDeclaration scriptDeclaration, SwitchScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processSwitchScriptExpression  (scriptDeclaration, expression, level, retValue, this, arg2);
	  if (expression.anchorExpression != null) {
	         processJavaExpression (scriptDeclaration, expression.anchorExpression, level + 1, retValue, arg1, arg2);
		     checkAnchorExpressionIsCodeInvokerType(expression.anchorExpression, "switch-expression");
      }

      if (!expression.switchExpression.dataType.isSmallIntegral()
      &&   expression.switchExpression.dataType!=IntType.theOne    ) {
            parserError (2, "Integer or subtype expected",
                         expression.switchExpression.sourceStartPosition, 
                         expression.switchExpression.sourceEndPosition);
            expression.switchExpression.dataType = IntType.theOne;
            expression.switchExpression.constantValue = null;
      }
      if (expression.switchExpression.dimensionSignature != null
      &&  expression.switchExpression.dimensionSignature != Dimension.errorSignature) {
          parserError (2, "Switch expression should not have dimension; instead it is: "
                           +Dimension.getPresentation(expression.switchExpression.dimensionSignature),
                       expression.switchExpression.sourceStartPosition,
                       expression.switchExpression.sourceEndPosition);
      }
   }

   protected void processCaseTagScriptExpression (ScriptDeclaration scriptDeclaration, CaseTagScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processCaseTagScriptExpression  (scriptDeclaration, expression, level, retValue, arg1, arg2);
      SwitchScriptExpression switchScriptExpression = (SwitchScriptExpression) arg1;

     // checks: isConstant, canBeAssignedTo en no duplicates.

     for (JavaExpression tag: expression.tags) {

         if (tag instanceof SpecialNameExpression
         && ((SpecialNameExpression) tag).getToken() == DefaultToken) {

            if (switchScriptExpression.hasDefaultTag) {
               parserError (2, "Duplicate default tag",
                     tag.sourceStartPosition, 
                     tag.sourceEndPosition);
            }
            else {
               switchScriptExpression.hasDefaultTag = true;
            }
         } else {
           try {
             if (!tag.isConstant()) {
                parserError (2, "Constant expression expected",
                           tag.sourceStartPosition, 
                           tag.sourceEndPosition);
                return;
             }
             if (switchScriptExpression.switchExpression.dataType.isSmallOrNormalInt()) {

               if (!tag.canBeAssignedTo (env, switchScriptExpression.switchExpression.dataType)) {
                 parserError (2, "Not assignable to type of switch expression ("
                        +switchScriptExpression.switchExpression.dataType.getNameWithDots()+")",
                         tag.sourceStartPosition, 
                         tag.sourceEndPosition);
                 return;
               }
             }
             if (!tag.dataType.canBeAssignedTo(env, IntType.theOne)) {
                 parserError (2, "Integer type expected",
                              tag.sourceStartPosition, 
                              tag.sourceEndPosition);
                 return;
             }
             int tagValue = ((ConstantSmallOrNormalInteger) tag.constantValue).intValue();;
 
             if (switchScriptExpression.tagValues.contains (tagValue)) {
                parserError (2, "Duplicate tag value",
                              tag.sourceStartPosition, 
                              tag.sourceEndPosition);
             }
             else {
                switchScriptExpression.tagValues.add (tagValue);
             }
           } catch (CompilerError       err) {parserError (2, err.getMessage(), err.start, err.end);
           } catch (java.io.IOException err) {parserError (2, err.getMessage(), tag.nameStartPosition, 
                                                                             tag.  nameEndPosition);
           }
         }
      }
   }
}


