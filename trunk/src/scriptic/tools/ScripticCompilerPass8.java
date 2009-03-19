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
import java.util.List;

import scriptic.tools.lowlevel.*;


/**
 * local varable 0 == this for instance functions
 *
 * process (expression) loads 'reference' on stack:
 *   if arg1!=loadReferenceOnly: load expression.value
 *   if arg1==loadReferenceOnly: load (Array   access: arrayRef, index
 *                               Instance field: ObjectRef
 *                               default       : - )
 * better: boolean isLValue() ...?
 *
 * Expression,target  reference>>stack load       store       dupRef      dup_xValue
 * ----------------------------------------------------------------------------------------
 * this, super                         dt.load_0  dt.store_0              dt.dup_x1Instruction
 * Local variable                      dt.load#   dt.store#               dt.dup_x1Instruction
 * Static   field                      getstatic# putstatic#              dt.dup_x1Instruction
 * Instance field     ObjectRef        getfield#  putfield#   dup         dt.dup_x1Instruction
 * Array   access     Arrayref, index  dt.aload   dt.astore   dup2        dt.dup_x2Instruction
 *
 *
 * Code for expr++                    Comment
 * ---------------------------------------------------
 * process (arg1=loadReferenceOnly)   load 'reference'
 * expr.dupRef                        dup that
 * expr.load                          load the value
 * expr.dup_xValue                    insert as result value
 * const_1                            1
 * add                                compute
 * expr.store
 *
 * expr.dupRef shorthand for dupReferenceInstruction ()
 * Instruction FieldAccessExpression.dupReferenceInstruction () {
 *   return new Instruction (dataType.dupInstruction(), this);
 * }
 * etc.
 */





public class ScripticCompilerPass8 extends ByteCodeEmitter {

   public ScripticCompilerPass8 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }
   
   Object loadReferenceOnly = new Object(); // flag as arg1 to load reference only

   protected ArrayList<JavaStatement> statementStack; // nothing to do with stack size...

   /* Main entry point */
   public boolean resolve (TopLevelTypeDeclaration t) {
      constantPoolOwner = t.target;
      processTopLevelTypeDeclaration (t, null, null, null);
      return true;
   }

   protected void processLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      ClassType savedConstantPoolOwner = constantPoolOwner;
      constantPoolOwner = t.target;
      super.processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
      ClassType c = t.target;
      c.         addInnerClassEntry (c);
      c.parent().addInnerClassEntry (c);
      constantPoolOwner = savedConstantPoolOwner;
   }

   private void endOfProcessTypeDeclaration () {
      if (classType.defaultConstructor != null) {
         processConstructorDeclaration (classType.defaultConstructor, 0, null, null, null);
      }
      generateClinitMethod();
   }
   void endOfProcessTopLevelTypeDeclaration     () {endOfProcessTypeDeclaration();}
   void endOfProcessLocalOrNestedTypeDeclaration() {endOfProcessTypeDeclaration();}

   private void generateClinitMethod() {
      Method clinit       = new Method();
      codeAttribute       = new CodeAttribute(clinit,classType);
      clinit.owner        = classType;
      clinit.addAttribute ( codeAttribute);

      for (FieldDeclaration f: typeDeclaration.fieldDeclarations)
      {
          if (!f.isStatic()
          &&  !typeDeclaration.isInterface()) continue;

          if (f.languageConstructCode()==MultiVariableDeclarationCode) {

             for (Object obj: ((MultiVariableDeclaration)f).variables)
             {
                 MemberVariableDeclaration m = (MemberVariableDeclaration) obj;

                 if (m.initializer!=null)  {   //  || !v.initializer.isConstant())

                    if (m.target.isConstant()) continue;    // already done in pass 6
                    processJavaExpression(m.initializer);
                    try {
                        emit (m.target.storeInstructions (env, constantPoolOwner, m));
                    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, m);
                    } catch (IOException e) {parserError (2, e.getMessage());}
                 }
             }
          }
          else if (f.languageConstructCode()==InitializerBlockCode) {

            InitializerBlock initializerBlock = (InitializerBlock) f;
            if (initializerBlock.statements != null) {
                initializerBlock.target      = clinit;
                statementStack = new ArrayList<JavaStatement>();
                processStatementBlock (initializerBlock, initializerBlock.statements, 0, null, null, null);
                if (codeAttribute.varSize() < (short) initializerBlock.target.nextVariableSlot)
                    codeAttribute.setVarSize(initializerBlock.target.nextVariableSlot);
            }
          }
      }
      if (!codeAttribute.isEmpty()) {
           clinit.name         = "<clinit>";
           clinit.signature    = "()V";
           clinit.returnType   = VoidType.theOne;
           clinit.modifierFlags= StaticFlag;
           emitCode (INSTRUCTION_return, typeDeclaration);
           HashMap<String, Object> signatures  = new HashMap<String, Object>();
           signatures.put("()V", clinit);
           classType.getMethodNamesSignatures(env).put(clinit.name, signatures);
           classType.addMethod (clinit);
      }
   }

   private void generateInitializersCodeForConstructor (ConstructorDeclaration constructorDeclaration) {

      for (FieldDeclaration f: typeDeclaration.fieldDeclarations)
      {

//System.out.println ("generateInitializersCodeForConstructor: "+f.getPresentation());
          if (f.isStatic()
          ||  typeDeclaration.isInterface()) continue;

          if (f.languageConstructCode()==MultiVariableDeclarationCode) {

             for (Object obj: ((MultiVariableDeclaration)f).variables)
             {
                 MemberVariableDeclaration m = (MemberVariableDeclaration) obj;

                 if (m.initializer!=null)  {   //  || !v.initializer.isConstant())
                    emitLoadThis (m);
                    processJavaExpression(m.initializer);
                    try {
                        emit (m.target.storeInstructions (env, constantPoolOwner, m));
                    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, m);
                    } catch (IOException e) {parserError (2, e.getMessage());
                    }
                 }
             }
          }
          else if (f.languageConstructCode()==InitializerBlockCode) {

            InitializerBlock initializerBlock = (InitializerBlock) f;
            if (initializerBlock.statements != null) {
                statementStack = new ArrayList<JavaStatement>();
                initializerBlock.target = constructorDeclaration.target;
                processStatementBlock (initializerBlock, initializerBlock.statements, 0, null, null, null);
                if (codeAttribute.varSize() < (short) initializerBlock.target.nextVariableSlot)
                    codeAttribute.setVarSize(initializerBlock.target.nextVariableSlot);
                initializerBlock.target = null; // nothing would make sense;
            }
          }
      }
   }

   // A statement stack records statement hierarchy of:
   //   LabeledStatement
   //    SwitchStatement
   //       ForStatement
   //     WhileStatement
   //        DoState ment
   //     CatchBlock
   //       TryBlock
   // This is relevant for:
   //   checking duplicate label in hierarchy
   //   resolving normal    break: from nearest loop or switch
   //   resolving normal continue: from nearest loop
   //   resolving labeled    break: from labeled statement
   //   resolving labeled continue: from labeled statement that holds loop
   //   adding appropriate code to handle finally clauses for break, continue, return, throw

   protected void pushStatement(BreakableStatementInterface s) {
        statementStack.add ((JavaStatement)s);
   }

   protected void popStatement(BreakableStatementInterface s) {
        statementStack.remove(statementStack.size()-1);
   }

   protected LabeledStatement findLabel (String name) {
     for (int i = statementStack.size()-1; i>=0; i--) {
         JavaStatement s = statementStack.get(i);
         if (s.languageConstructCode()==LabeledStatementCode
         &&  s.name.equals(name)) {
            return (LabeledStatement) s;
         }
     }
     return null;
   }

   /** process a jump statement. If it is a:
    *  return: the return value is already on the operand stack, if applicable
    *  throw : the throw value is already on the operand stack
    *  break or continue with label: the label is looked up in the statement stack
    *  break or continue without label: the appropriate loop or switch statement is looked up
    */
   protected void processJumpingStatement (RefinementDeclaration refinement, JumpingStatement jumpingStatement) {

      TemporaryVariable temporaryVariable = null;
      LanguageConstruct languageConstructForTemporaryVariable = null;
    try {
      for (int i = statementStack.size()-1; i>=0; i--) {

         ConfiningJavaStatement s = (ConfiningJavaStatement) statementStack.get(i);
         switch (s.languageConstructCode()) {

         case       CatchBlockCode:
         case         TryBlockCode:
              {
                  LabelInstruction finallyLabel = ((TryOrCatchBlock) s).tryStatement.finallyStartLabel;
                  if (finallyLabel==null) {
                      break;
                  }

                  // jumpingStatement returns value? store the value; otherwise:
                  // VERIFIER ERROR: Inconsistent stack height 1 != 2 etc.

                  if (temporaryVariable==null) {
                    if (jumpingStatement.languageConstructCode()==ReturnStatementCode) {
                      ReturnStatement r = (ReturnStatement) jumpingStatement;
                      if (r.returnExpression != null) {
                             temporaryVariable = new TemporaryVariable (refinement.target, r.returnExpression.dataType);
                             temporaryVariable.name = "temporaryReturnValue";
                             languageConstructForTemporaryVariable = r.returnExpression;
                             emitStoreLocal(temporaryVariable, languageConstructForTemporaryVariable);
                      }
                    }
                    else if (jumpingStatement.languageConstructCode()==ThrowStatementCode) {
                      ThrowStatement t = (ThrowStatement) jumpingStatement;
                      temporaryVariable = new TemporaryVariable (refinement.target, t.throwExpression.dataType);
                      temporaryVariable.name = "temporaryThrowValue";
                      languageConstructForTemporaryVariable = t.throwExpression;
                      emitStoreLocal(temporaryVariable, languageConstructForTemporaryVariable);
                    }
                  }
                  emitJSR (finallyLabel, jumpingStatement);
              }
              break;

         case LabeledStatementCode:
              if (jumpingStatement.hasTargetLabel()
              &&  jumpingStatement.getName().equals (s.getName())) {
                  LabeledStatement lab = (LabeledStatement) s;
                  if (jumpingStatement.languageConstructCode()==ContinueStatementCode)
                  {
                      if (!(lab.statement instanceof JavaLoopStatement)) {
                          parserError (2, "Continue label target is not a loop statement", 
                                        jumpingStatement.sourceStartPosition,
                                        jumpingStatement.sourceEndPosition);
                      }
                      emitGoto (lab.startLabel, jumpingStatement);
                  }
                  else {
                      emitGoto (lab.endLabel, jumpingStatement);
                      ((BreakableStatementInterface)s).setTargetOfBreak();
                      lab.isTargetOfBreak = true;
                  }
                  return;
              }
              break;

         case     ForStatementCode:
         case   WhileStatementCode:
         case      DoStatementCode:
              if (jumpingStatement.languageConstructCode()==ContinueStatementCode
              && !jumpingStatement.hasTargetLabel()) {
                  emitGoto (((JavaLoopStatement)s).nextLabel, jumpingStatement);
                  return;
              }
              // NO break;

         case  SwitchStatementCode:
              if (jumpingStatement.languageConstructCode()==BreakStatementCode
              && !jumpingStatement.hasTargetLabel()) {
                  ((BreakableStatementInterface)s).setTargetOfBreak();
                  emitGoto (s.endLabel, jumpingStatement);
                  return;
              }
              break;

         }
      }
      if (temporaryVariable!=null) {
          emitLoadLocal (temporaryVariable, languageConstructForTemporaryVariable);
      }
      switch (jumpingStatement.languageConstructCode()) {
      case    ReturnStatementCode:  emitReturn ((ReturnStatement) jumpingStatement); break;
      case     ThrowStatementCode:  emitThrow  (jumpingStatement); break;
      case     BreakStatementCode:  
    	  {
    		  if (refinement instanceof BasicScriptDeclaration)
		      {
		          emitLoadNodeParameter (refinement, jumpingStatement, refinement.isStatic());
		          Method breakIteration_method = 
		        	  env.mustResolveMethod (env.scripticVmNodeType, "breakIteration", "()V");
		          MethodCallExpression breakIteration_methodCallExpression = new MethodCallExpression();
		          breakIteration_methodCallExpression.target = breakIteration_method;
		          breakIteration_methodCallExpression.setVirtualMode();
		          
		          try {
		        	  emit (breakIteration_methodCallExpression.invokeInstruction (env, constantPoolOwner));
		          }
		          catch (ByteCodingException je)
		          {
		        	  env.handleByteCodingException (je, typeDeclaration, jumpingStatement);
		          }
		      }
	          emitCode (INSTRUCTION_return, jumpingStatement);
	          break;
	      }
      case  ContinueStatementCode:
              parserError (2, "No target for "
                          +(jumpingStatement.hasTargetLabel()?"labeled ":"")
                          +(jumpingStatement.languageConstructCode()==BreakStatementCode?"break ":"continue ")
                          +"statement", 
                           jumpingStatement.sourceStartPosition,
                           jumpingStatement.sourceEndPosition);
               break;
      }
    } 
    catch (IOException e) {parserError (2, e.getMessage());}
   }


   /** Somewhere in the current statement at top of the statement stack,
    *  a throwable is thrown.
    *  Ignore if it is a RunTimeException.
    *  Check whether this is caught by a catch block.
    *  If so, set the catch block's boolean catchesAnything 
    *  Else check whether the refinement declares the throwable type in the throws clause
    *  If so, set the boolean catchesAnything of the throws clause element
    */
   protected void checkMethodCallThrows (FieldDeclaration fieldDeclaration,
                                         MethodOrConstructorCallExpression methodCall) {
       if (methodCall.isArrayClone()) return;

       ClassType exceptionTypes[] = methodCall.target.exceptionTypes(env);
       for (int i=0; i<exceptionTypes.length; i++) {
           throwableIsThrown (fieldDeclaration, exceptionTypes[i], methodCall);
       }
   }


   /** Somewhere from within the current statement at top of the statement stack,
    *  a throwable is thrown.
    *  Ignore if it is a RunTimeException.
    *  Check whether this is caught by a catch block.
    *  If so, set the catch block's boolean catchesAnything 
    *  Else check whether the refinement declares the throwable type in the throws clause
    *  If so, set the boolean catchesAnything of the throws clause element
    *
    *  We do this in this pass, because we need the statement stack
    */
   protected void throwableIsThrown (FieldDeclaration   fieldDeclaration,
                                     ClassType          throwable,
                                     LanguageConstruct  thrower) {
     try {
       if (throwable.isSubtypeOf (env, env.javaLangRuntimeExceptionType)) {
           return;
       }
       for (int i = statementStack.size()-1; i>=0; i--) {
  
           ConfiningJavaStatement s = (ConfiningJavaStatement) statementStack.get(i);
  
           if (s.languageConstructCode() == TryBlockCode)
           {
               TryStatement tryStatement = ((TryBlock) s).tryStatement;
               for (JavaStatement js: tryStatement.catches) {
                    CatchBlock c = (CatchBlock) js;
                    if (c.wantsToCatch (env, throwable)==2 ) { // 2 means: will allways be caught
                        return;
                    }
                }
           }
       }
       if ( fieldDeclaration.isVariableDeclaration()) {
                 parserError (2, "Exception " + throwable.getNameWithDots()
                             + " thrown inside variable initializer", 
                            thrower.sourceStartPosition,
                            thrower.sourceEndPosition);
       }
       else if (!fieldDeclaration.     isMethodDeclaration()
            &&  !fieldDeclaration.isConstructorDeclaration()) {
               parserError (2, "Exception " + throwable.getNameWithDots()
                           + " thrown; should be caught in catch block",
                            thrower.sourceStartPosition,
                            thrower.sourceEndPosition);
       }
       else {
          if (!fieldDeclaration.declaresToThrowFromBody (env, throwable)) {
               parserError (2, "Exception " + throwable.getNameWithDots()
                           + " thrown; should be caught in catch block or declared in throws clause",
                            thrower.sourceStartPosition,
                            thrower.sourceEndPosition);
          }
       }
     } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
     } catch (java.io.IOException e) {parserError (2, e.getMessage(), thrower.nameStartPosition, 
                                                                 thrower.  nameEndPosition);
     }
   }

   private void setCodeAttribute(Method method) {
      codeAttribute. setVarSize (method.nextVariableSlot);
      method.addAttribute (codeAttribute);
   }

   // members are done in generateClinitMethod and generateInitializersCodeForConstructor...
   protected void processMultiVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {}

   protected void processInitializerBlock     (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      // handled in code for <init> and <clinit> parts
   }



   protected void processVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, MemberVariableDeclaration variable, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

     if (variable.target.shadow != null) {
       try{
         Method m = variable.target.shadow.shadowGetMethod;
         codeAttribute = new CodeAttribute(m, classType);
         m.addAttribute (codeAttribute);

         // load this
         // getfield
         // return value

         if (!variable.isStatic()) {
            emitLoadThis (variable);
         }
         emit (variable.target.loadInstructions (env, classType, variable));
         emitCode (variable.target.dataType1.returnInstructionCode(), variable);
         codeAttribute.setVarSize (m.isStatic()? 0: 1);

         m = variable.target.shadow.shadowSetMethod;
         codeAttribute = new CodeAttribute(m, classType);
         m.addAttribute (codeAttribute);

         // load this
         // load parameter
         // putfield
         // return

         int slot = 0;
         if (!variable.isStatic()) {
            emitLoadThis (variable);
            slot = 1;
         }
         emit     (new Instruction (variable.target.getDataType(env).loadInstructionCode(), slot, variable));
         emit     (variable.target.storeInstructions (env, classType, variable));
         emitCode (INSTRUCTION_return, variable);
         codeAttribute.setVarSize (m.isStatic()? 0: 1);
      } catch (ByteCodingException            e) {env.handleByteCodingException (e, typeDeclaration, variable);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), variable.sourceStartPosition, 
                                                                       variable.  sourceEndPosition);
      }
    }
  }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                       Method Declaration                        */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processParameterDeclaration  (RefinementDeclaration declaration, MethodParameterDeclaration parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!(declaration instanceof BasicScriptDeclaration))
          declaration.target.setNextSlot (parameter.target);
   }

   protected void processMethodDeclaration (MethodDeclaration methodDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      codeAttribute  = new CodeAttribute(methodDeclaration.target,constantPoolOwner);
      statementStack = new ArrayList<JavaStatement>();
      methodDeclaration.target.nextVariableSlot = methodDeclaration.isStatic()? 0: 1;

      if (methodDeclaration.parameterList != null)
         processParameterList  ((RefinementDeclaration)methodDeclaration, methodDeclaration.parameterList, level + 1, retValue, arg1, arg2);

      if (methodDeclaration.statements != null) { // else abstract...
if (methodDeclaration.isAbstract()
||  classType.isInterface())
parserError (3, "INTERNAL ERROR: abstract method has statements", 
                              methodDeclaration.sourceStartPosition,
                              methodDeclaration.sourceEndPosition);

         processStatementBlock (methodDeclaration, methodDeclaration.statements, level + 1, retValue, arg1, arg2);
         
         if (methodDeclaration.statements.canCompleteNormally()) {
            if (methodDeclaration.target.returnType.isVoid())
                 emitReturn (methodDeclaration);
            else parserError (2, "Method must return value", 
                              methodDeclaration.sourceStartPosition,
                              methodDeclaration.sourceEndPosition);
         }
         setCodeAttribute (methodDeclaration.target);
         if (!env.doKeepMethodBodies)methodDeclaration.statements = null;
      }
      if (methodDeclaration.throwsClause != null)
         processThrowsClause   (methodDeclaration, methodDeclaration.throwsClause, level + 1, retValue, arg1, arg2);

   }

   protected void processConstructorDeclaration     (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
/*
System.out.println("constructorDeclaration pass 7: "
                +  constructorDeclaration.getName()
                +  constructorDeclaration.target.getParameterSignature(env));
*/
      LocalClassType         lc  = classType instanceof LocalClassType? (LocalClassType)classType: null;
      LocalOrNestedClassType lnc = classType instanceof LocalOrNestedClassType?
                                   (LocalOrNestedClassType)classType: null;

      codeAttribute  = new CodeAttribute(constructorDeclaration.target,constantPoolOwner);
      statementStack = new ArrayList<JavaStatement>();
      // constructorDeclaration.target.nextVariableSlot = 1; was done in pass 6; could be >1! (this$0 etc)

      boolean hasQualifiedSuperCall = constructorDeclaration.otherConstructorInvocation != null
                                   && constructorDeclaration.otherConstructorInvocation.
                                           methodAccessExpression.languageConstructCode()
                                      ==  QualifiedSuperExpressionCode;
      try {
        if (hasQualifiedSuperCall) {
          emitLoadThis (constructorDeclaration);
          processJavaExpression ( constructorDeclaration,
                                  ((QualifiedSuperExpression)constructorDeclaration.otherConstructorInvocation.methodAccessExpression).
                                    primaryExpression,
                                  level + 1, retValue, arg1, arg2);
          emit (lnc.enclosingInstance.storeInstructions(env, classType, constructorDeclaration));
        }

        // parameter list; hack for the extraParameters:
        // they get setNextSlot explicitly here...no alternative
        if (lnc != null
        && constructorDeclaration.target.extraParameters != null)
        {
          for (int i=0; i<constructorDeclaration.target.extraParameters.length; i++) {
            constructorDeclaration.target.setNextSlot (constructorDeclaration.target.extraParameters[i]);
          }
        }  
        if (constructorDeclaration.parameterList != null) {
           // set the slots:
           processParameterList  ((RefinementDeclaration)constructorDeclaration, constructorDeclaration.parameterList, level + 1, retValue, arg1, arg2);
        }

        if (constructorDeclaration.otherConstructorInvocation != null // else Object ...
        && !hasQualifiedSuperCall) {

          processMethodCallExpression (constructorDeclaration,
                                       constructorDeclaration.otherConstructorInvocation, level + 1, retValue, arg1, arg2);
        }
        if (constructorDeclaration.otherConstructorInvocation              == null
        ||  constructorDeclaration.otherConstructorInvocation.target.owner != classType) {

          if (classType instanceof LocalOrNestedClassType) { // store the extra variables
            int nEnclosingInstanceParameters = 0;
            Parameter parameters[] = constructorDeclaration.target.extraParameters;

            if (lnc.enclosingInstance!=null) {
              nEnclosingInstanceParameters = 1;
              emitLoadThis (constructorDeclaration);
              emit (parameters[0].loadInstructions (env, classType, constructorDeclaration));
              emit (lnc.enclosingInstance.storeInstructions (env, classType, constructorDeclaration));
            }
            if (lc != null) {
              int i=nEnclosingInstanceParameters;
              for (MemberVariable v: lc.usedLocalVariablesAndParameters.values()) {
                emitLoadThis (constructorDeclaration);
                emit (parameters[i].loadInstructions (env, classType, constructorDeclaration));
                emit (v.storeInstructions (env, classType, constructorDeclaration));
              }
            }
          }
          generateInitializersCodeForConstructor(constructorDeclaration);
        }
      } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, constructorDeclaration);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), constructorDeclaration.sourceStartPosition, 
                                                                       constructorDeclaration.  sourceEndPosition);
      }
      if (constructorDeclaration.statements != null) {
         processStatementBlock (constructorDeclaration, constructorDeclaration.statements, level + 1, retValue, arg1, arg2);
      }
      if (constructorDeclaration.statements == null
      ||  constructorDeclaration.statements.canCompleteNormally()) emitReturn (constructorDeclaration);

      if (constructorDeclaration.throwsClause != null)
         processThrowsClause   (constructorDeclaration, constructorDeclaration.throwsClause, level + 1, retValue, arg1, arg2);

      setCodeAttribute(constructorDeclaration.target);
      if (!env.doKeepMethodBodies) constructorDeclaration.statements = null;
   }

   protected void processThrowsClause (MethodDeclaration method, ThrowsClause throwsClause, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      method.target.setExceptionsAttribute();
   }

   /*******************************************************************/
   /**                                                               **/
   /**                          STATEMENTS                           **/
   /**                                                               **/
   /*******************************************************************/

   protected void processJavaStatements (RefinementDeclaration refinement, ArrayList<JavaStatement> statements, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      boolean previousCanCompleteNormally = true;

      // we have to remove empty statements,
      // otherwise we'll get unreachable messages on 'return;;' ...
      for (int i = statements.size()-1; i>=0; i--) {
    	 JavaStatement statement = statements.get (i);
         if (statement.languageConstructCode() == EmptyStatementCode)
             statements.remove(i);
      }

      for (JavaStatement statement: statements) {
         processJavaStatement (refinement, statement, level, retValue, arg1, arg2);
         switch (statement.languageConstructCode()) {
         case     CatchBlockCode:
         case DefaultCaseTagCode:
         case        CaseTagCode: break;
         default                : if (!previousCanCompleteNormally) {
                                       parserError (2, "Statement unreachable", 
                                                     statement.sourceStartPosition,
                                                     statement.  sourceEndPosition);
                                  }
         }
         previousCanCompleteNormally = statement.canCompleteNormally();
      }
   }

   protected void processExpressionStatement (RefinementDeclaration refinement, ExpressionStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (statement.specialCode == SpecialCode.none) {
        processJavaExpression (refinement, statement.expression, level + 1, retValue, arg1, arg2);
        int instructionCode = statement.expression.dataType.popInstructionCode();
        if (instructionCode>=0) emitCode (instructionCode, statement);
      }
      else {
        // statement.specialCode == 1
        // Success testing expression "xxxx ??"
        // Code to be generated "if (_n_.setSuccessTrueOrNull(xxxx)); else return;"
        try {
          emitLoadNodeParameter (refinement, statement, refinement.isStatic());
          processJavaExpression (refinement, statement.expression, level + 1, retValue, arg1, arg2);
          
          String methodName = statement.specialCode == SpecialCode.singleSuccessTest
			? "setSuccess": "setSuccessTrueOrNull";

          Method setSuccessTrueOrNull_method = 
        	  env.mustResolveMethod (env.scripticVmNodeType, methodName, "(Z)Z");
          MethodCallExpression setSuccessTrueOrNull_methodCallExpression = new MethodCallExpression();
          setSuccessTrueOrNull_methodCallExpression.target = setSuccessTrueOrNull_method;
          setSuccessTrueOrNull_methodCallExpression.setVirtualMode();
          emit (setSuccessTrueOrNull_methodCallExpression.invokeInstruction (env, constantPoolOwner));

          LabelInstruction  endLabel = new LabelInstruction("??.end", statement);
          emitIfTrue (endLabel, statement);
          emitCode   (INSTRUCTION_return, statement);
          emit       (endLabel);
        } catch (ByteCodingException            e) {env.handleByteCodingException (e, typeDeclaration, statement);
        }
      }
   }




   protected void processLocalVariableDeclaration  (RefinementDeclaration refinement, JavaStatement statement, LocalVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      //variableDeclaration.target.rangeStart = new LabelInstruction("local.var", variableDeclaration);
      //emit (variableDeclaration.target.rangeStart);
      // rangeEnd known through variable.confiningJavaStatement
      refinement.codeTarget().setNextSlot (variableDeclaration.target);
      if (variableDeclaration.initializer != null) {
       try {
         processJavaExpression (refinement, variableDeclaration.initializer, level + 1, retValue, arg1, arg2);
         emit (variableDeclaration.target.storeInstructions (env, constantPoolOwner, variableDeclaration));
       } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, statement);
       } catch (IOException e) {parserError (2, e.getMessage());
       }
      }
   }

   protected void processLabeledStatement (RefinementDeclaration refinement, LabeledStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      statement.startLabel = new LabelInstruction ("labeled.start"+statement.name, statement);
      statement.  endLabel = new LabelInstruction ("labeled.end"  +statement.name, statement);

      LabeledStatement conflict = findLabel (statement.name);
      if (conflict != null) {
         parserError (2, "Label same has with one defined higher in the statement hierarchy", 
                      statement.sourceStartPosition,
                      statement.sourceEndPosition);
      }
      pushStatement(statement);
      emit         (statement.startLabel);
      processJavaStatement (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      emit         (statement.endLabel);
      emitCode     (INSTRUCTION_nop, statement);
      popStatement (statement);
   }

   protected void processIfStatement                 (RefinementDeclaration refinement, IfStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      LabelInstruction elseLabel = new LabelInstruction("if.else", statement.falseStatement);
      LabelInstruction  endLabel = new LabelInstruction("if.end" , statement);
      if (!statement.conditionExpression.isConstant()) {
         processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
         emitIfFalse (elseLabel, statement.conditionExpression);
      }

      if (!statement.conditionExpression.isFalse()) {
         processJavaStatement  (refinement, statement.trueStatement, level + 1, retValue, arg1, arg2);
         emitGoto (endLabel, statement.trueStatement);
      }
      emit (elseLabel);
      if (statement.falseStatement != null
      && !statement.conditionExpression.isTrue()) {
         processJavaStatement  (refinement, statement.falseStatement, level + 1, retValue, arg1, arg2);
      }
      emit     (endLabel);
      emitCode (INSTRUCTION_nop, statement);
   }

   protected void processSwitchStatement             (RefinementDeclaration refinement, SwitchStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      
      if (!statement.statements.isEmpty()) {
         JavaStatement firstStatement = statement.statements.get(0);
         if (!(firstStatement instanceof CaseOrDefaultTag)) {
               parserError (2, "Statement unreachable: "+firstStatement.getPresentation()+"  "+firstStatement.getDescription(), 
                            firstStatement.sourceStartPosition,
                            firstStatement.sourceEndPosition);
         }
      }

      pushStatement(statement);
      statement.startLabel = new LabelInstruction ("switch.start", statement);
      statement.  endLabel = new LabelInstruction ("switch.end"  , statement);
      emit (statement.startLabel);

      //The following would be nonsense:
      //if (statement.switchExpression.isConstant()) {
      //    lookup the correct statements; do those and return...
      //}

      // caseTags is an ordered Vector with the case tags.
      // inspect min and max value, decide whether to go for a tableSwitch or lookupSwitch

      boolean doTableSwitch = false;
      int n = statement.caseTags.size();
      int min = 0;
      int max = 0;
      if (n > 0) {
          min = statement.caseTags.get(0  ).tagValue();
          max = statement.caseTags.get(n-1).tagValue();
          if (max-min <= n + 1
          ||  max-min <= n * 1.2) {
              doTableSwitch = true;
              n = max-min+1;  /// beware...
          }
      }
      if  (statement.defaultTag==null) {
           statement.defaultTag = new DefaultCaseTag ();
           statement.defaultTag.label = statement.endLabel;
      }
      processJavaExpression  (refinement, statement.switchExpression, level + 1, retValue, arg1, arg2);

      if (n==0) {
          emitPop  (statement.switchExpression.dataType, statement);
      }
      else {

        LabelInstruction targets[] = new LabelInstruction[n];

        try {

          if (doTableSwitch) {

              TableswitchInstruction instruction = new TableswitchInstruction (statement);
              emit (instruction);

              // generate the code and the labels
              processJavaStatements  (refinement, statement.statements, level + 1, retValue, arg1, arg2);

              int j=0;
              for (int i=0; i<n; i++) {
                 if ( i + min   == statement.caseTags.get(j  ).tagValue())
                      targets[i] = statement.caseTags.get(j++).label;
                 else targets[i] = statement.defaultTag.label;
              }
if (j!=statement.caseTags.size())
parserError (2, "Internal error when computing tableswitch");

              instruction.setContents (min, max, statement.defaultTag.label, targets);
          }
          else {
              LookupswitchInstruction instruction = new LookupswitchInstruction (statement);
              emit (instruction);

              // generate the code and the labels
              processJavaStatements  (refinement, statement.statements, level + 1, retValue, arg1, arg2);

              int tags[] = new int[n];
              for (int i=0; i<n; i++) targets[i] = statement.caseTags.get(i).label;
              for (int i=0; i<n; i++) tags   [i] = statement.caseTags.get(i).tagValue();

              instruction.setContents (statement.defaultTag.label, tags, targets);
          }
        } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, statement);}
      }
      emit        (statement.endLabel);
      emitCode    (INSTRUCTION_nop, statement);
      popStatement(statement);
   }

   protected void processCaseTag                     (RefinementDeclaration refinement, CaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      emit (statement.label = new LabelInstruction ("case "+statement.tagValue(), statement));
   }

   protected void processDefaultCaseTag              (RefinementDeclaration refinement, DefaultCaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      emit (statement.label = new LabelInstruction ("default", statement));
   }

   protected void processWhileStatement              (RefinementDeclaration refinement, WhileStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     if (statement.conditionExpression.isFalse()) {
         parserError (2, "Code unreachable", 
                      statement.statement.sourceStartPosition,
                      statement.statement.sourceEndPosition);
     }
     processDoStatement (refinement, statement, level, retValue, arg1, arg2);
   }

   protected void processDoStatement                 (RefinementDeclaration refinement, DoStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushStatement(statement);
      statement.            nextLabel =
      statement.           startLabel = new LabelInstruction("while.start"    , statement);
      statement.             endLabel = new LabelInstruction("while.end"      , statement);
      LabelInstruction conditionLabel = new LabelInstruction("while.condition", statement.conditionExpression);
      if (statement.languageConstructCode()==WhileStatementCode) {
          emitGoto (conditionLabel, statement);
      }

      emit (statement.startLabel);

      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);

      emit (conditionLabel);
      if (!statement.conditionExpression.isConstant()) {
        processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
        emitIfTrue (statement.startLabel, statement);
      }
      else if (statement.conditionExpression.isTrue()) {
          emitGoto (statement.startLabel, statement);
      }
      emit        (statement.endLabel);
      emitCode    (INSTRUCTION_nop, statement);
      popStatement(statement);
   }

   protected void processForStatement                (RefinementDeclaration refinement, ForStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if (statement.conditionExpression != null
      &&  statement.conditionExpression.isFalse()) {
          parserError (2, "Code unreachable", 
                       statement.statement.sourceStartPosition,
                       statement.statement.sourceEndPosition);
      }

      JavaStatement bodyStartStatement = null;
      if (!statement.loopStatements.isEmpty()) {
          bodyStartStatement = statement.loopStatements.get(0);
      }

      pushStatement(statement);
      statement.           startLabel = new LabelInstruction ("for.start"    , statement);
      statement.            nextLabel = new LabelInstruction ("for.next"     , statement);
      statement.             endLabel = new LabelInstruction ("for.end"      , statement);
      LabelInstruction conditionLabel = new LabelInstruction ("for.condition", statement.conditionExpression);
      LabelInstruction bodyStartLabel = new LabelInstruction ("for.body"     , bodyStartStatement);

      emit (statement.startLabel);

      processJavaStatements (refinement, statement.initStatements, level + 1, retValue, arg1, arg2);

      emitGoto (conditionLabel, statement);
      emit     (bodyStartLabel);

      processJavaStatement  (refinement, statement.    statement , level + 1, retValue, arg1, arg2);

      emit     (statement.nextLabel);

      processJavaStatements (refinement, statement.loopStatements, level + 1, retValue, arg1, arg2);

      emit (conditionLabel);

      if (statement.conditionExpression != null
      && !statement.conditionExpression.isConstant()) {
        processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
        emitIfTrue (bodyStartLabel, statement);
      }
      else if (statement.conditionExpression == null
           ||  statement.conditionExpression.isTrue()) {
          emitGoto (bodyStartLabel, statement);
      }
      emit        (statement.endLabel);
      emitCode    (INSTRUCTION_nop, statement);
      popStatement(statement);
   }

   protected void processBreakStatement    (RefinementDeclaration refinement, BreakStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJumpingStatement (refinement, statement);
   }

   protected void processContinueStatement (RefinementDeclaration refinement, ContinueStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJumpingStatement (refinement, statement);
   }

   protected void processReturnStatement (RefinementDeclaration refinement, ReturnStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (statement.returnExpression !=  null)
         processJavaExpression (refinement, statement.returnExpression, level + 1, retValue, arg1, arg2);
      processJumpingStatement (refinement, statement);
   }

   protected void processThrowStatement (RefinementDeclaration refinement, ThrowStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (refinement, statement.throwExpression, level + 1, retValue, arg1, arg2);
      throwableIsThrown (refinement,
                         (ClassType) statement.throwExpression.dataType,
                         statement.throwExpression);
      processJumpingStatement (refinement, statement);
   }

   protected void processSynchronizedStatement (RefinementDeclaration refinement, SynchronizedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {
      statement.       startLabel = new LabelInstruction ("synchronized.start", statement);
      statement.         endLabel = new LabelInstruction ("synchronized.end"  , statement);
      LabelInstruction enterLabel = new LabelInstruction ("monitor.enter"     , statement);
      LabelInstruction exitLabel  = new LabelInstruction ("monitor.exit"      , statement);
      LabelInstruction catchLabel = new LabelInstruction ("monitor.catch"     , statement, LabelInstruction.CATCH_TYPE);
      emit (statement.startLabel);

      processJavaExpression (refinement, statement.synchronizedExpression, level + 1, retValue, arg1, arg2);

      TemporaryVariable temporaryVariable    = new TemporaryVariable(refinement.target, env.javaLangObjectType);
      temporaryVariable.name                 = "synchronizer";
      emitStoreLocal(temporaryVariable, statement.synchronizedExpression);
      emitLoadLocal (temporaryVariable, statement.synchronizedExpression);
      emitCode (INSTRUCTION_monitorenter, statement);

      emit (enterLabel);

      processNestedStatement  (refinement, statement, level, retValue, arg1, arg2);

      emit (exitLabel);
      emitLoadLocal (temporaryVariable, statement.synchronizedExpression);
      emitCode      (INSTRUCTION_monitorexit, statement);
      emitGoto      (statement.endLabel     , statement);

      emit          (catchLabel);
      emitLoadLocal (temporaryVariable, statement.synchronizedExpression);
      emitCode      (INSTRUCTION_monitorexit, statement);
      emitCode      (INSTRUCTION_athrow     , statement);
      emit          (statement.endLabel);
      emitCode      (INSTRUCTION_nop, statement);

      codeAttribute.addCatchtableEntry (enterLabel, exitLabel, catchLabel, null);
    } 
    catch (IOException e) {parserError (2, e.getMessage());}
   }

   protected void processTryStatement                (RefinementDeclaration refinement, TryStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {
      LabelInstruction defaultCatchHandlerLabel = null;

      statement. startLabel = new LabelInstruction("try-catch-finally.start", statement, LabelInstruction.TRY_START_TYPE);
      statement.   endLabel = new LabelInstruction("try-catch-finally.end"  , statement);
      statement.tryEndLabel = new LabelInstruction("try.end"                , statement, LabelInstruction.  TRY_END_TYPE);
     
      if (statement.finalStatements != null
      && !statement.finalStatements.isEmpty()) {
          JavaStatement firstFinallyStatement = statement.finalStatements.get(0);
          statement.finallyStartLabel      = new LabelInstruction("finally.start", firstFinallyStatement);
             defaultCatchHandlerLabel      = new LabelInstruction("default.catch", firstFinallyStatement, LabelInstruction.  CATCH_TYPE);
          statement.targetAfterTryAndCatch = new LabelInstruction("jsr.finally"  , firstFinallyStatement, LabelInstruction.TRY_END_TYPE);
              // targetAfterTryAndCatch marks the end of the managed area of the any-catch-handler 
      }
      else statement.targetAfterTryAndCatch = statement.endLabel;

      emit            (statement.startLabel);
      processTryBlock (refinement, statement.tryBlock, level + 1, retValue, arg1, arg2);
      emit            (statement.tryEndLabel);
      emitGoto        (statement.targetAfterTryAndCatch, statement);

      processJavaStatements  (refinement, statement.catches, level + 1, retValue, arg1, arg2);
      // like the try block, each of these catch handlers ends
      // "emitGoto statement.targetAfterTryAndCatch"

      if (defaultCatchHandlerLabel != null) { // so we've also a 'finally' part...
          
          emit        (statement.targetAfterTryAndCatch);
          emitJSR     (statement.finallyStartLabel, statement);
          emitGoto    (statement.         endLabel, statement);

          // add the default catch handler
          TemporaryVariable temporaryVariable = new TemporaryVariable(refinement.target, env.javaLangObjectType);
          temporaryVariable.name              = "default catch handler";
          emit          (defaultCatchHandlerLabel);
          emitStoreLocal(temporaryVariable          , statement);
          emitJSR       (statement.finallyStartLabel, statement);
          emitLoadLocal (temporaryVariable          , statement);
          emitThrow     (statement);
          codeAttribute.addCatchtableEntry (statement.startLabel,
                                            statement.targetAfterTryAndCatch,
                                            defaultCatchHandlerLabel,
                                            null);

          // finally, finally
          emit (statement.finallyStartLabel);

          temporaryVariable       = new TemporaryVariable(refinement.target, env.javaLangObjectType);
          temporaryVariable.name  = "finally save area";
          emitStoreLocal(temporaryVariable, statement);
          processJavaStatements  (refinement, statement.finalStatements, level + 1, retValue, arg1, arg2);
          emitRet(temporaryVariable, statement);
      }
      emit     (statement.endLabel);
      emitCode (INSTRUCTION_nop, statement);
    }
    catch (IOException e) {parserError (2, e.getMessage());}
   }

   protected void processTryBlock (RefinementDeclaration refinement, TryBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
       pushStatement(statement);
       processNestedStatement (refinement, statement, level, retValue, arg1, arg2);
       popStatement(statement);
   }


   protected void processCatchBlock (RefinementDeclaration refinement, CatchBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {
      pushStatement(statement);
      processLocalVariableDeclaration (refinement, statement, statement.catchVariable, level + 1, retValue, arg1, arg2);

      for (JavaStatement js: statement.tryStatement.catches) {
         CatchBlock c = (CatchBlock) js;
         if (c == statement) break;
         try {
           if (statement.catchVariable.dataType().isSubtypeOf (env, c.catchVariable.dataType())) {
              parserError (2, "Catch variable's type is subtype of earlier catch variable", 
                           statement.catchVariable.sourceStartPosition,
                           statement.catchVariable.  sourceEndPosition);
             statement.catchesAnything = true; // prevent the next message...
           }
         } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
         } catch (java.io.IOException e) {parserError (2, e.getMessage(), statement.catchVariable.nameStartPosition, 
                                                                     statement.catchVariable.  nameEndPosition);
         }
      }
        if (!statement.catchesAnything
        &&  !statement.catchVariable.dataType().isSubtypeOf (env, env.javaLangRuntimeExceptionType)) {
            parserError (2, "Catch variable cannot possibly be thrown inside try part", 
                         statement.catchVariable.sourceStartPosition,
                         statement.catchVariable.  sourceEndPosition);
        }

      LabelInstruction catchLabel = new LabelInstruction("catch", statement, LabelInstruction.CATCH_TYPE);

      emit (catchLabel);
      emitStoreLocal (statement.catchVariable.target, statement.catchVariable);

      ConstantPoolClass cpc = statement.catchVariable.dataType().classRef(constantPoolOwner);

      /* add the catch table entry now, before processing the catch clause.
       * This is needed in order to have the right order in the catch table
       * for nested try-catch statements
       * Otherwise stack size checking will go wrong: that requires the
       * stack size of the try-start when processing the catch clause.
       */
      codeAttribute.addCatchtableEntry (statement.tryStatement.startLabel,
                                        statement.tryStatement.tryEndLabel, catchLabel, cpc);
      processJavaStatements (refinement, statement.statements, level + 1, retValue, arg1, arg2);
      emitGoto (statement.tryStatement.targetAfterTryAndCatch, statement);

      popStatement(statement);
    } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {handleByteCodingException(e);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), statement.nameStartPosition, 
                                                                statement.  nameEndPosition);
    }
   }


   /*******************************************************************/
   /**                                                               **/
   /**                         EXPRESSIONS                           **/
   /**                                                               **/
   /*******************************************************************/

   protected void processJavaExpression (FieldDeclaration fieldDeclaration, JavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
       if (expression.isConstant()) {
           emitLoadConstant (expression);
       } else {
           super.processJavaExpression (fieldDeclaration, expression, level, retValue, arg1, arg2);
       }
   }

   /*-------------------------------------------------------------------------------*/

   protected void emitStringBufferAppends (FieldDeclaration fieldDeclaration, JavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2)
                 throws ByteCodingException {

      if (expression.languageConstructCode()==NestedJavaExpressionCode) {
         emitStringBufferAppends (fieldDeclaration,
                                  ((NestedJavaExpression)expression).subExpression,
                                  level + 1, retValue, arg1, arg2);
         return;
      }
      BinaryExpression b = (BinaryExpression) expression;

      if (b. leftExpression.isStringPlus (env)) {
         emitStringBufferAppends (fieldDeclaration, b.leftExpression, level + 1, retValue, arg1, arg2);
      }
      else {
         processJavaExpression (fieldDeclaration, b.leftExpression, level + 1, retValue, arg1, arg2);
         Method m = env.getJavaLangStringBufferAppend (b.leftExpression.dataType);
         if (m == null) return; // error flagged by env
         emit (m.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                         constantPoolOwner,
                                         b.leftExpression));
      }
      if (b.rightExpression.isStringPlus (env)) {
         emitStringBufferAppends (fieldDeclaration, b.rightExpression, level + 1, retValue, arg1, arg2);
      }
      else {
         processJavaExpression (fieldDeclaration, b.rightExpression, level + 1, retValue, arg1, arg2);

         Method m = env.getJavaLangStringBufferAppend (b.rightExpression.dataType);
         if (m == null) return; // error flagged by env
         emit (m.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                         constantPoolOwner,
                                         b.rightExpression));
      }
   }

   protected void emitConcatUsingTemporaryStringBuffer (FieldDeclaration fieldDeclaration,
                                                        BinaryExpression expression,
                                                        int level,
                                                        ReturnValueHolder retValue,
                                                        Object arg1,
                                                        Object arg2)  throws ByteCodingException {
         emit (new Instruction(INSTRUCTION_new,
                               env.javaLangStringBufferType.classRef(constantPoolOwner),
                               expression));
         emitCode (INSTRUCTION_dup, expression);

         Method m = env.getJavaLangStringBufferInit();
         if (m == null) return; // error flagged by env
         emit (m.invokeInstruction (env, INSTRUCTION_invokespecial,
                                         constantPoolOwner,
                                         expression));

         emitStringBufferAppends (fieldDeclaration, expression, level + 1, retValue, arg1, arg2);

         m = env.getJavaLangStringBufferToString();
         if (m == null) return; // error flagged by env

         emit (m.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                         constantPoolOwner,
                                         expression));
   }

   /*-------------------------------------------------------------------------------*/

   protected void processBinaryExpression         (FieldDeclaration fieldDeclaration, BinaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if   (expression.operatorToken==BooleanAndToken) {

        if (expression. leftExpression.isTrue()) { // isFalse has already been handled
                 processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
                 return;
        }
        processJavaExpression (fieldDeclaration, expression.leftExpression, level + 1, retValue, arg1, arg2);
        if (expression.rightExpression.isTrue()) {
                 return;
        }
        if (expression.rightExpression.isFalse()) {
            emitPop (BooleanType.theOne, expression);
            emitLoadFalse (expression);
            return;
        }
        LabelInstruction  trueLabel = new LabelInstruction("&&.true", expression);
        LabelInstruction   endLabel = new LabelInstruction("&&.end" , expression);
        emitIfTrue    (trueLabel, expression);
        emitLoadFalse (expression);
        emitGoto      (endLabel, expression);
        emit          (trueLabel);
        processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
        emit          (endLabel);
        return;
      }

      if   (expression.operatorToken==BooleanOrToken) {

        if (expression. leftExpression.isFalse()) { // isTrue has already been handled
                 processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
                 return;
        }
        processJavaExpression (fieldDeclaration, expression.leftExpression, level + 1, retValue, arg1, arg2);
        if (expression.rightExpression.isFalse()) {
                 return;
        }
        if (expression.rightExpression.isTrue()) {
            emitPop      (BooleanType.theOne, expression);
            emitLoadTrue (expression);
            return;
        }
        LabelInstruction falseLabel = new LabelInstruction("||.false", expression);
        LabelInstruction   endLabel = new LabelInstruction("||.end"  , expression);
        emitIfFalse   (falseLabel, expression);
        emitLoadTrue  (expression);
        emitGoto      (endLabel, expression);
        emit          (falseLabel);
        processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
        emit          (endLabel);
        return;
      }

try {
      if (expression.dataType == env.javaLangStringType) { // must be '+'
         emitConcatUsingTemporaryStringBuffer (fieldDeclaration, expression, level + 1, retValue, arg1, arg2);
         return;
      }

      processJavaExpression (fieldDeclaration, expression. leftExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);

      switch (expression.operatorToken) {
        case      EqualsToken       : 
        case    NotEqualToken       :  
        case GreaterThanToken       :  
        case LessThanToken          :  
        case LessOrEqualToken       :  
        case GreaterOrEqualToken    :  
          if (expression.leftExpression.dataType.isInt    ()
          ||  expression.leftExpression.dataType.isBoolean()) {
                  emitCompareIntegers   (expression.operatorToken, expression);
          } else if (expression.leftExpression.dataType.isReference()) {
                  emitCompareReferences (expression.operatorToken, expression);
          } else {emitCompare (expression.leftExpression.dataType, expression.operatorToken, expression);
          }
          break;

        default:
          emitCode  (expression.dataType.binaryInstructionCode (expression.operatorToken), expression);
      }
}catch (Exception e) {
System.out.println (e.getMessage()+"\n"
+"LEFT: "+expression. leftExpression.dataType.getPresentation()+"\n"
+"RIGHT:"+expression.rightExpression.dataType.getPresentation()+"\n"
+"MAIN: "+expression.                dataType.getPresentation()+"\n"
);
e.printStackTrace();
}
   }
   /*-------------------------------------------------------------------------------*/

   protected void processAssignmentExpression     (FieldDeclaration fieldDeclaration, AssignmentExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {
      processJavaExpression (fieldDeclaration, expression.targetExpression, level + 1, retValue, loadReferenceOnly /*arg1*/, arg2);

      if (expression.operatorToken==AssignToken) {
          processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
      }
      else if (expression.dataType == env.javaLangStringType) { // must be +=
         // ERROR (?) : emit(expression.targetExpression.dupReferenceInstruction());
         emitConcatUsingTemporaryStringBuffer (fieldDeclaration, expression, level + 1, retValue, arg1, arg2);
      }
      else {
          if      (!expression.isTopLevelExpression())
          emit     (expression.targetExpression.dupReferenceInstruction());
          processJavaExpression (fieldDeclaration, expression. leftExpression, level + 1, retValue, arg1, arg2);
          processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
          emitCode (expression.leftExpression.dataType.binaryInstructionCode(expression.operatorToken),expression);
          // still possibly needed: casting to expression.dataType
          // e.g. for short s=0; s+=1.23e45;
          emitPrimitiveCast (expression, expression.leftExpression.dataType);
      }
      if       (expression.isTopLevelExpression())
                expression.dataType = VoidType.theOne;
      else emit(expression.targetExpression.dupForAssignmentInstruction ());
      emit     (expression.targetExpression.storeInstructions (env, constantPoolOwner));
     } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
     } catch (IOException e) {parserError (2, e.getMessage());}
   }

   /*-------------------------------------------------------------------------------*/

   protected void processConditionalExpression    (FieldDeclaration fieldDeclaration, ConditionalExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      LabelInstruction elseLabel = new LabelInstruction("'?:'.else", expression.falseExpression);
      LabelInstruction  endLabel = new LabelInstruction("'?:'.end ", expression);

      if (!expression.conditionExpression.isConstant()) {
         processJavaExpression (fieldDeclaration, expression.conditionExpression, level + 1, retValue, arg1, arg2);
         emitIfFalse (elseLabel, expression.conditionExpression);
      }

      if (!expression.conditionExpression.isFalse()) {
         processJavaExpression  (fieldDeclaration, expression.trueExpression, level + 1, retValue, arg1, arg2);
         emitGoto (endLabel, expression.trueExpression);
      }
      emit (elseLabel);
      if (!expression.conditionExpression.isTrue()) {
         processJavaExpression  (fieldDeclaration, expression.falseExpression, level + 1, retValue, arg1, arg2);
      }
      emit    (endLabel);
   }

   /*-------------------------------------------------------------------------------*/

   protected void iincLocalVariable (LocalVariable variable,
                                     int amount,
                                     LanguageConstruct instructionOwner) {
     try {
       emit (variable.iincInstruction(amount, instructionOwner));
     } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, instructionOwner);}
   }

   /*-------------------------------------------------------------------------------*/

   protected void processUnaryExpression          (FieldDeclaration fieldDeclaration, UnaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {
      switch (expression.unaryToken) {
      case PlusToken  :
           processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
           break;

      case TildeToken: // becomes xor...
           processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
           emitLoadIntegerAs (-1, expression.dataType, expression);
           emitCode (expression.dataType.binaryInstructionCode (CaretToken), expression);
           break;

      case MinusToken:
           processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
           emitCode (expression.dataType.unaryInstructionCode (expression.unaryToken), expression);
           break;

      case ExclamationToken:
           {
             LabelInstruction elseLabel = new LabelInstruction("bool.else", expression.primaryExpression);
             LabelInstruction  endLabel = new LabelInstruction("bool.end ", expression);
             processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
             emitIfFalse     (elseLabel, expression);
             emitLoadInteger (        0, expression);
             emitGoto        ( endLabel, expression);
             emit            (elseLabel);
             emitLoadInteger (        1, expression);
             emit            ( endLabel);
           }
           break;

      case IncrementToken:
      case DecrementToken:
           {
             if (expression.primaryExpression instanceof JavaExpressionWithTarget) {
                 JavaExpressionWithTarget primary = (JavaExpressionWithTarget) expression.primaryExpression;
                 if (primary.dataType==IntType.theOne
                 &&  primary.target.canHandleIincInstruction()) {
                     iincLocalVariable ((MethodLocalVariable) primary.target,
                                        expression.unaryToken==IncrementToken? 1: -1,
                                        expression);
                     if       (!expression.isTopLevelExpression())
                         emit (primary.loadInstructions (env, constantPoolOwner));
                     else      expression.dataType = VoidType.theOne;
                     return;
                }
             }

             // still to optimize for int locals?
             int token = expression.unaryToken==IncrementToken
                       ?  PlusToken
                       : MinusToken;
             processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, loadReferenceOnly/*arg1*/, arg2);
             emit     (expression.primaryExpression.        dupReferenceInstruction());
             emit     (expression.primaryExpression.                loadInstructions (env, constantPoolOwner));
             emitCode (expression.dataType.const_1InstructionCode()     ,expression);
             emitCode (expression.dataType. binaryInstructionCode(token),expression);
             if      (!expression.isTopLevelExpression())
             emit     (expression.primaryExpression.dup_xValueInstruction()); // the resulting value...
             if       (expression.dataType != expression.primaryExpression.dataType) // was promoted...
             emitCode (expression.dataType.convertToInstructionCode(expression.primaryExpression.dataType), expression);
             emit     (expression.primaryExpression.storeInstructions (env, constantPoolOwner));
             if       (expression.isTopLevelExpression())
                       expression.dataType = VoidType.theOne;
             break;
           }
      }
     } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
     } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                   expression.  sourceEndPosition);
     }
   }

   /*-------------------------------------------------------------------------------*/

   protected void processPostfixExpression        (FieldDeclaration fieldDeclaration, PostfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     try {
      // page 361

      if (expression.primaryExpression instanceof JavaExpressionWithTarget) {
          JavaExpressionWithTarget primary = (JavaExpressionWithTarget) expression.primaryExpression;
        if (primary.dataType==IntType.theOne
        &&  expression.isTopLevelExpression()
        &&  primary.target.isLocalVariable()) {
            iincLocalVariable ((LocalVariable) primary.target,
                                expression.unaryToken==IncrementToken? 1: -1,
                                expression);
            expression.dataType = VoidType.theOne;
            return;
        }
      }
      int token = expression.unaryToken==IncrementToken
                ?  PlusToken
                : MinusToken;

      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, loadReferenceOnly/*arg1*/, arg2);
    //if      (!expression.isTopLevelExpression())
      emit     (expression.primaryExpression.dupReferenceInstruction());
      emit     (expression.primaryExpression.        loadInstructions (env, constantPoolOwner));
      if      (!expression.isTopLevelExpression())
      emit     (expression.primaryExpression.dup_xValueInstruction());  // the resulting value
      emitCode (expression.dataType.              const_1InstructionCode()     ,expression);
      emitCode (expression.dataType.               binaryInstructionCode(token),expression);
      if       (expression.dataType != expression.primaryExpression.dataType) // was promoted...
      emitCode (expression.dataType.convertToInstructionCode(expression.primaryExpression.dataType), expression);
      emit     (expression.primaryExpression.       storeInstructions (env, constantPoolOwner));
     } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
     } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                   expression.  sourceEndPosition);
     }
     if       (expression.isTopLevelExpression())
               expression.dataType = VoidType.theOne;
   }

   /*-------------------------------------------------------------------------------*/

   protected void processPrimitiveCastExpression  (FieldDeclaration fieldDeclaration, PrimitiveCastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
      emitPrimitiveCast (expression, expression.unaryExpression.dataType);
   }

   /*-------------------------------------------------------------------------------*/

   protected void emitPrimitiveCast (JavaExpression targetExpression, DataType fromType) {

      DataType promotedTargetType    = targetExpression.dataType.promoteUnary();
      DataType promotedFromType      = fromType                 .promoteUnary();

      if (promotedTargetType != promotedFromType)
         emitCode (promotedFromType.convertToInstructionCode (promotedTargetType), targetExpression);

      if (targetExpression.dataType != promotedTargetType) // it is only half way now... double->short etc.
         emitCode (promotedTargetType.convertToInstructionCode (targetExpression.dataType), targetExpression);
   }

   /*-------------------------------------------------------------------------------*/

   protected void processCastExpression           (FieldDeclaration fieldDeclaration, CastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
      if (expression.isNarrowing)
          emitCheckCast ((ClassType)expression.dataType, expression.unaryExpression);
   }

   /*-------------------------------------------------------------------------------*/

   protected void processTypeComparisonExpression (FieldDeclaration fieldDeclaration, TypeComparisonExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     processJavaExpression (fieldDeclaration, expression.relationalExpression, level + 1, retValue, arg1, arg2);
     try { 
       if (expression.relationalExpression.dataType.isSubtypeOf (env, expression.compareType))
            emitLoadTrue   (expression);
       else emitInstanceof (expression.compareType, expression.relationalExpression);
     } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
     } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.relationalExpression.nameStartPosition, 
                                                                 expression.relationalExpression.  nameEndPosition);
     }
   }

   /*-------------------------------------------------------------------------------*/

   // not needed: already caught & handled by processJavaExpression
   //
   // protected void processLiteralExpression        (FieldDeclaration fieldDeclaration, LiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   // }

   /*-------------------------------------------------------------------------------*/

   protected void processNameExpression (FieldDeclaration fieldDeclaration, NameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {

      if (expression.specialCode == SpecialCode.matchingTest
      ||  expression.specialCode == SpecialCode.isOutParameterOrAsOutParameter
      ||  expression.specialCode == SpecialCode.isMatchingParameterOrAsMatchingParameter) {

         // i!!  specialCode = 1    (!_n_.isForced(2) || p == old_p)
         // i!   specialCode = 2      _n_.isForced(2)
         // i?!  specialCode = 3      N.A.
         // i?   specialCode = 4      _n_.isOut(2)

         emitLoadNodeParameter (fieldDeclaration, expression, fieldDeclaration.isStatic());
         int index = ((BasicScriptDeclaration) fieldDeclaration).paramIndexOf (
                                    (NormalOrOldScriptParameter)expression.target);
         emitLoadInteger (index, expression); // here in the example: '2'
         Method m = env.mustResolveMethod (env.scripticVmNodeType,
                      expression.specialCode==SpecialCode.isOutParameterOrAsOutParameter? "isOut": "isForced",
                                           "(I)Z");
         Instruction invoke = m.invokeInstruction (env, INSTRUCTION_invokevirtual, constantPoolOwner, expression);
         emit (invoke);

         if (expression.specialCode == SpecialCode.matchingTest) {

            LabelInstruction trueLabel = new LabelInstruction("!!.2"   , expression);
            LabelInstruction  endLabel = new LabelInstruction("!!.end ", expression);
            emitIfTrue      (trueLabel, expression);
            emitLoadInteger (        1, expression);
            emitGoto        ( endLabel, expression);
            emit            (trueLabel);

            // "p == old_p" part:

            ScriptParameter p = (ScriptParameter) expression.target;
            emit (p.           loadReferenceInstructions (fieldDeclaration, env, constantPoolOwner, expression));
          //emit (p.                    loadInstructions (                  env, constantPoolOwner, expression));
            emit (p.oldVersion.loadReferenceInstructions (fieldDeclaration, env, constantPoolOwner, expression));
            Method cmpMethod = env.mustResolveMethod (p.dataType1.holderClass(env),
                                                      "hasEqualValue",
                                                      "(Ljava/lang/Object;)Z");
                                                    //"(L"+p.dataType1.wrapperClass(env).getNameWithSlashes()+";)Z");
            Instruction cmpInvoke = cmpMethod.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                                                 constantPoolOwner, expression);
            emit (cmpInvoke);
            emit (endLabel);

         }
      }
      else { // NORMAL, no special code etc.

        if (expression.target != null
        &&( expression.target ==    NodeDuration.theOne
        ||  expression.target == env.scripticVmNodePass
        ||  expression.target == env.scripticVmNodePriority
        ||  expression.target == env.scripticVmNodeSuccess)) {

            emitLoadNodeParameter (fieldDeclaration, expression, fieldDeclaration.isStatic());
        }
        else if (expression.   isTypeName()
             ||  expression. isMethodName()
             ||  expression.isPackageName() /*??? this one needed as of 970324; why? */) {
                 return;
        }
        else if (expression.target.isMemberVariable()
        &&      !expression.target.isStatic()) {
           emitLoadThisFor (expression);
        }
        else if (expression.target.isScriptParameter()
             ||  expression.target.   isOldParameter()
             ||  expression.target.isScriptLocalVariable()) {

          //_n_.   paramData(partnerIndex)[slot]   >>>  ValueHolder
          //_n_.oldParamData(partnerIndex)[slot]   >>>  Object|Integer|Character|...
          //_n_.localData[slot].value              >>>  ValueHolder
          LocalVariableOrParameter sl = (LocalVariableOrParameter) expression.target;
          emit (sl.loadReferenceInstructions (fieldDeclaration, env, constantPoolOwner, expression));
        }
        if (arg1==loadReferenceOnly) return;

        emit (expression.loadInstructions (env, constantPoolOwner));

        if (( expression.target.isScriptParameter()
           || expression.target.   isOldParameter()
           || expression.target.isScriptLocalVariable())
        &&   !expression.dataType.isPrimitive()
        &&    expression.dataType != env.javaLangObjectType) {
             emit (new Instruction (INSTRUCTION_checkcast,
                                    constantPoolOwner.resolveClass (expression.dataType),
                                    expression));
        }
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, expression);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                  expression.  sourceEndPosition);
    }
   }

   /*-------------------------------------------------------------------------------*/

   protected void processSpecialNameExpression    (FieldDeclaration fieldDeclaration, SpecialNameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (arg1==loadReferenceOnly) return;

    try {
      switch (expression.getToken()) {
      case  NullToken:
      case  ThisToken:
      case SuperToken:  emit (expression.loadInstructions (env, constantPoolOwner));
                        break;
      case ScriptToken: emitLoadNodeParameter (fieldDeclaration, expression, fieldDeclaration.isStatic());
                        break;
      }
    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);}
   }

   /*-------------------------------------------------------------------------------*/

   protected void processArrayInitializer         (FieldDeclaration fieldDeclaration, ArrayInitializer expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      // (this code has lots in common with processAllocationExpression)

      DataType accessType = ((ArrayType)expression.dataType).accessType;
      int arrayTypeNumber = accessType.getArrayTypeNumber();

      Instruction instruction = null;
      try {
        if (arrayTypeNumber >= 0) { // a primitive array...
            instruction = new Instruction (INSTRUCTION_newarray, arrayTypeNumber, expression);
        }
        else {
            instruction = new Instruction (INSTRUCTION_anewarray,
                                           accessType.classRef(constantPoolOwner),
                                           expression);
        }

        emitLoadInteger (expression.elementExpressions.size(), expression);
        emit            (instruction);

        // OK. The array is now on the stack.
        // Now load its elements as far as needed (recursively)

        for (int i = 0; i < expression.elementExpressions.size(); i++) {
            JavaExpression elt = expression.elementExpressions.get(i);
            if (!elt.isNull ()
            &&  !elt.isZero ()
            &&  !elt.isFalse()) {
                emit                  (expression.dupReferenceInstruction());
                emitLoadInteger       (i,                elt);
                processJavaExpression (fieldDeclaration, elt, level+1, retValue, arg1, arg2);
                emit                  (expression.storeInstructions (env, constantPoolOwner));
            }
        }
      } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);}
   }


   /*-------------------------------------------------------------------------------*/

   protected void processFieldAccessExpression    (FieldDeclaration fieldDeclaration, FieldAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      try {
        if (expression.target != null
        &&  expression.target.isOldParameter()) {
            emit (((OldParameter)expression.target).loadReferenceInstructions (
                                          fieldDeclaration, env, constantPoolOwner, expression));
            emit (expression.loadInstructions (env, constantPoolOwner));
        }
        else {
          if (expression.primaryExpression.isPackageName())
          return;

          //if (expression.isStaticMethodName()
          //&&  expression.primaryExpression.isThis ()
          //||  expression.primaryExpression.isSuper()) {}
          //else
          processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, null /*arg1*/, arg2);

          if (arg1!=loadReferenceOnly) {
             if (expression.target != null
             &&  expression.target.isStatic()
             && !expression.primaryExpression.isTypeName()) {

if (expression.target instanceof MemberVariable
&&  ((MemberVariable)expression.target).owner==null)
parserError (2, "MemberVariable.owner==null: "+((MemberVariable)expression.target).getDescription(),
expression.sourceStartPosition,
expression.sourceEndPosition
);

                 emitPop (expression.primaryExpression.dataType, expression.primaryExpression);
             }
             if (!expression.   isTypeName()
             &&  !expression. isMethodName()
             &&  !expression.isPackageName()) {
                 emit (expression.loadInstructions (env, constantPoolOwner));
             }
          }
        }
      } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                    expression.  sourceEndPosition);
      }
   }

   /*-------------------------------------------------------------------------------*/

   protected void processArrayAccessExpression    (FieldDeclaration fieldDeclaration, ArrayAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int size = expression.indexExpressions.size();
      processJavaExpression  (fieldDeclaration, expression.primaryExpression, level + 1, retValue, null /*arg1*/, arg2);
      for (int i=0; i<size; i++) {
        JavaExpression indexExpression = expression.indexExpressions.get(i);
        processJavaExpression (fieldDeclaration, indexExpression, level + 1, retValue, null /*arg1*/, arg2);

        if (i<size-1) emitCode (INSTRUCTION_aaload, indexExpression);
        else if (arg1!=loadReferenceOnly) {   // dereference entirely...
          int instructionCode = ((ArrayType)expression.primaryExpression.
                                       dataType).accessArray(i+1).arrayLoadInstructionCode();
          emitCode (instructionCode, indexExpression);
        }
      }
   }

   /*-------------------------------------------------------------------------------*/

   protected void processMethodCallExpression     (FieldDeclaration fieldDeclaration, MethodCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     checkMethodCallThrows (fieldDeclaration, expression);
     try {

  handleMethodAccessExpression:
      {
        JavaExpressionWithTarget accessExpression = expression.methodAccessExpression;
        if (accessExpression.languageConstructCode()==NameExpressionCode) {
            // just a method name. load 'this' if needed
            if (!expression.isStaticMode()) {
                emitLoadThisFor (expression);
            }
            break handleMethodAccessExpression;
        }
        if (accessExpression.languageConstructCode()==FieldAccessExpressionCode) {
            JavaExpression primary = ((FieldAccessExpression) accessExpression).primaryExpression;
            if (primary.isThis ()
            ||  primary.isSuper()) {
                if (!expression.isStaticMode()) {
                    emitLoadThis (expression);
                }
                break handleMethodAccessExpression;
            }
        }
        processJavaExpression (fieldDeclaration, accessExpression,
                               level + 1, retValue, arg1, arg2);
        if (expression.isStaticMode()
        &&  accessExpression.languageConstructCode()==FieldAccessExpressionCode) {
           FieldAccessExpression f = (FieldAccessExpression)accessExpression;
           if (!f.primaryExpression.isTypeName()) {
              emitPop (f.primaryExpression.dataType, f.primaryExpression);
           }
        }
      }
      // extra parameters in case of constructor calls in inner classes...
      if (classType        instanceof LocalOrNestedClassType
      &&  fieldDeclaration instanceof ConstructorDeclaration
      &&  ((ConstructorDeclaration)fieldDeclaration).otherConstructorInvocation == expression ) {
        ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration)fieldDeclaration;
        boolean hasSuperCall          = constructorDeclaration.otherConstructorInvocation != null
                                     && constructorDeclaration.otherConstructorInvocation.target.owner != classType;
        if (!hasSuperCall   //calls another this() constructor
        ||   classType.getSuperclass(env).parent() == classType.parent()) {

          // extraParameters: parent class + used locals etc.
          for (int i=0; i<constructorDeclaration.target.extraParameters.length; i++) {
            emit (constructorDeclaration.target.extraParameters[i].loadInstructions (env, classType, constructorDeclaration));
          }
        }
      }

      if (expression.parameterList != null)
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
      emit (expression.invokeInstruction (env, constantPoolOwner));

    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, expression);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                  expression.  sourceEndPosition);
    }
   }

   /*-------------------------------------------------------------------------------*/

   protected void processAllocationExpression     (FieldDeclaration fieldDeclaration, AllocationExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     try {
      Instruction instruction = null;

      if (expression.dataType.isArray()) {
          if (expression.arrayInitializer != null) {
             processArrayInitializer (fieldDeclaration, expression.arrayInitializer,
                                      level+1, retValue, arg1, arg2);

          }
          else {
            processJavaExpression (fieldDeclaration, expression.sizeExpressions.get(0), level + 1, retValue, arg1, arg2);

            if (expression.dataType.noOfArrayDimensions() == 1) {

              int arrayTypeNumber = expression.dataType.baseType().getArrayTypeNumber();
              if (arrayTypeNumber < 0) {

//System.out.println ("anewarray: "+expression.dataType.getNameWithDots()
//                   + "  "+expression.dataType.baseType().getNameForClassRef());

                instruction = new Instruction (INSTRUCTION_anewarray,
                                               expression.dataType.baseType().classRef(constantPoolOwner),
                                               expression);
              }
              else {
                instruction = new Instruction (INSTRUCTION_newarray, arrayTypeNumber, expression);
              }
            }
            else {
              instruction = new MultiarrayInstruction (expression.dataType.classRef(constantPoolOwner),
                                                       expression.sizeExpressions.size()-1,
                                                       expression);
            }
            emit (instruction);
          }
      }
      else { // object construction
        checkMethodCallThrows (fieldDeclaration, expression);
        emit     (expression.newInstruction (constantPoolOwner));
        emitCode (INSTRUCTION_dup, expression);

        if (expression.enclosingInstance != null)
        {
          processJavaExpression (fieldDeclaration, expression.enclosingInstance, level + 1, retValue, arg1, arg2);
          if (!((NestedClassType)expression.dataType).needsParentReference()) {
             emitCode (INSTRUCTION_pop, expression);
          }
        }

        if (expression.dataType instanceof LocalClassType) {
           LocalClassType lc = (LocalClassType) expression.dataType;

           if (expression.enclosingInstance == null
           &&  lc.enclosingInstance != null) {
              emitLoadThis (expression);
           }

           // and ... the other parameters
           for (LocalVariableOrParameter lvp: lc.usedLocalVariablesAndParameters.keySet()) {
               emit (lvp.loadInstructions (env, classType, expression));
           }
        }

        // 990531 toegevoegd:
        if (expression.dataType instanceof NestedClassType) {
           emitLoadThis (expression);
        }

        if (expression.parameterList != null) {
           processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
        }
        emit (expression.invokeInstruction (env, constantPoolOwner));

        if (expression.anonymousTypeDeclaration != null)
        {
            emitCode (INSTRUCTION_dup, expression); // for another constructor call

            if (expression.anonymousTypeDeclaration.target.needsParentReference()) {
                if  (expression.enclosingInstance != null
                &&(!(expression.dataType instanceof NestedClassType)
                 ||!((NestedClassType)expression.dataType).needsReferenceToEnclosingInstance())) {
                   processJavaExpression (fieldDeclaration, expression.enclosingInstance, level + 1, retValue, arg1, arg2);
                }
                else {
                   emitLoadThis (expression);
                }
            }
            else if  (expression.enclosingInstance != null) {
                   parserError (2, "No enclosing instance should be specified for target class",
                                 expression.sourceStartPosition,
                                 expression.sourceEndPosition);
            }
            emit (expression.anonymousTypeDeclaration.target
                 .defaultConstructor.target.invokeInstruction (env,
                                                               INSTRUCTION_invokespecial,
                                                               constantPoolOwner,
                                                               expression.anonymousTypeDeclaration));
        }
      }
    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                  expression.  sourceEndPosition);
    }
   }

   //-------------------------------------------------------------------------------

   protected void processClassLiteralExpression     (FieldDeclaration fieldDeclaration, ClassLiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      ClassType c = null;

      switch (expression.primitiveTypeToken) {
         default:                                        break;
         case    VoidToken: c = env.javaLangBooleanType; break;
         case BooleanToken: c = env.javaLangByteType;    break;
         case    CharToken: c = env.javaLangCharType;    break;
         case    ByteToken: c = env.javaLangShortType;   break;
         case   ShortToken: c = env.javaLangIntType;     break;
         case     IntToken: c = env.javaLangLongType;    break;
         case    LongToken: c = env.javaLangFloatType;   break;
         case   FloatToken: c = env.javaLangDoubleType;  break;
         case  DoubleToken: c = env.javaLangVoidType;    break;
      }
      try {
        if (c != null) {
          MemberVariable v = c.resolveMemberVariable (env, "TYPE");
          emit (v.loadInstructions(env, constantPoolOwner, expression));
        }
        else {
          String className   = expression.primaryExpression.dataType.getNameWithSlashes();
          Method m           = env.mustResolveMethod (env.javaLangClassType, "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
          Instruction invoke = m.invokeInstruction (env, INSTRUCTION_invokestatic, constantPoolOwner, expression);
          emitLoadString (className, expression.primaryExpression);
          emit (invoke);
        }
      } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
      } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, expression);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                    expression.  sourceEndPosition);
      }
   }

   protected void processQualifiedThisExpression (FieldDeclaration fieldDeclaration, QualifiedThisExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    try {

      emitLoadThis (expression);

      ClassType targetType = (ClassType) expression.target.dataType1;
      ClassType ct         = classType;

      while (!ct.isSubtypeOf (env, targetType)) {

          LocalOrNestedClassType lct = (LocalOrNestedClassType) ct;
          emit (lct.enclosingInstance.loadInstructions (env, constantPoolOwner, expression));
          ct = lct.parent();
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                  expression.  sourceEndPosition);
    }
   }

   //-------------------------------------------------------------------------------


   void emitScriptMethod (BasicScriptDeclaration decl) {
      boolean doTrace = false;

      codeAttribute  = new CodeAttribute(decl.scriptMethod,constantPoolOwner);
      statementStack = new ArrayList<JavaStatement>();
      decl.scriptMethod.nextVariableSlot = decl.isStatic()? 0: 1;
      if (doTrace) emitTraceString ("script: "+decl.name+" - 1", decl);

      try {
        if (decl.isMainScript ()) {
          Method mainscript = env.mustResolveMethod (env.scripticVmFromJavaType,
                                                      "mainscript",
                                                      "()Lscriptic/vm/Node;");
          emit (mainscript.invokeInstruction (env, INSTRUCTION_invokestatic, constantPoolOwner, decl));
        }
        else {
          emitLoadNodeParameter (decl, decl, decl.isStatic()); // parameter for call startScript()
          emitCheckCast (env.scripticVmCallerNodeInterfaceType, decl);
        }
        if (doTrace) emitTraceString ("script: "+decl.name+" - 2", decl);

        if (decl.isStatic()) {
            emitLoadNull(decl);
        }
        else {
          emitLoadThis (decl);
        }
        ScriptTemplateGetMethod stgm;
        if (decl instanceof ChannelDeclaration)
        {
        	stgm = ((ChannelDeclaration)decl).partnerTemplateGetMethod;
        }
        else
        {
        	stgm = decl.templateGetMethod;
        }
        emit (stgm.invokeInstruction (env, INSTRUCTION_invokestatic, constantPoolOwner, decl));
        if (doTrace) emitTraceString ("script: "+decl.name+" - 3", decl);

        // load an array with all parameters, wrapped as needed
        if (decl.parameterList != null
        && !decl.parameterList.parameters.isEmpty()) {

          int nrParameters       = decl.parameterList==null?    0: decl.parameterList.parameters.size();
          InstructionOwner owner = decl.parameterList==null? (InstructionOwner) decl
                                                           : (InstructionOwner) decl.parameterList;
          emitLoadInteger (nrParameters, owner);
          emit            (new Instruction (INSTRUCTION_anewarray,
                                             env.javaLangObjectType.classRef(constantPoolOwner),
                                             owner));
          if (decl.parameterList != null) {
            ArrayList<MethodParameterDeclaration> parameters = decl.parameterList.parameters;
            for (int i=0; i<parameters.size(); i++) {
              MethodParameterDeclaration p = parameters.get (i);
              emitCode         (INSTRUCTION_dup    , p);
              emitLoadInteger  (i                  , p);
              emitLoadAsObject (decl               , p);
              emitCode         (INSTRUCTION_aastore, p);
            }
          }
        }
        else {
          emitLoadNull (decl);
        }
        if (doTrace) emitTraceString ("script: "+decl.name+" - 4", decl);

        Method startScript = env.mustResolveMethod (env.scripticVmCallerNodeInterfaceType,
                                                    "startScript",
        											"(Ljava/lang/Object;Lscriptic/vm/NodeTemplate;[Ljava/lang/Object;)V");
        emit (startScript.invokeInstruction (env, INSTRUCTION_invokeinterface, constantPoolOwner, decl));
        if (doTrace) emitTraceString ("script: "+decl.name+" - 5", decl);
        emitCode (INSTRUCTION_return, decl);
      }
      catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, decl);}
      catch (IOException e) {parserError (2, e.getMessage());}

      codeAttribute. setVarSize (decl.scriptMethod.parameterSlots()+(decl.isStatic()? 0: 1));
      decl.scriptMethod.addAttribute (codeAttribute);
   }

   /* ****************
   static scriptic.vm.NodeTemplate get_AbpApplet__animateDA_template () {
      if (AbpApplet__animateDA_template == null)
         AbpApplet__animateDA_template = new scriptic.vm.NodeTemplate
            ("pakkage.clazz", "scriptName", "(signature)","codeMethodName",
              modifiers, nIndexes, nParameters,
              {  {{scriptToken, codeBits, sl, sp, el, ep}, {l, p, len}},
                 {{     ;token, codeBits, sl, sp, el, ep}, {l1,p1,l2,p2} }, // 3 operands...
                 {{   IntToken, codeBits, sl, sp, el, ep}, {l,  p, len} },
                 {{     <token, codeBits, sl, sp, el, ep}, {l,  p, len} },
                 {{     {token, codeBits, sl, sp, el, ep} },
                 {{     {token, codeBits, sl, sp, el, ep} },
                 {{ identToken, codeBits, sl, sp, el, ep}, {l,  p, len}, {exclamParams, questParams, ??adaptingIndexes??}},
                 {{    <<Token, codeBits, sl, sp, el, ep}, {l,  p, len}, {          0L,          0L}}
              }
            );
          //was: ("class AbpApplet script animateDA(3) = (int {}< {}; aCall(?,!); aSend<<()");
      return AbpApplet__animateDA_template;
   }

   The array with template information contains 1 up to 3 elements.
   [0]: {token, codeBits, sl, sp, el, ep}
         token        - operator/operand type
         codeBits     - 0x000 often (no code)
                        0x001 if there is a code fragment
                        0x011 if there is an extra fragment
                        0x111 3 bits possible for "for(..;..;..)"
         sl,sp,el,ep  - start/end line/position
   [1]: {l, p, len} or {l1,p1,..., ...}
         l, p, len    - line+position+length of central name
         l1,p1, ...   - line+position of n-ary operators (lenght==1)
   [2]: {exclamParams, questParams} for script calls
         exclamParams - 64 bit mask for actual parameters with exclamation mark
         questParams  - 64 bit mask for actual parameters with question mark

   ********************/
   void emitTemplateGetMethod (BasicScriptDeclaration owner, boolean doChannelPartner) {
    ScriptTemplateGetMethod m = doChannelPartner
    						? ((ChannelDeclaration)owner).partnerTemplateGetMethod
    						: owner.templateGetMethod;
    String codeMethodName = (m instanceof ScriptPartnerTemplateGetMethod)
    					  ? null
    					  : m.scriptDeclaration().getCodeMethodName();   
    codeAttribute  = new CodeAttribute(m,constantPoolOwner);
    statementStack = new ArrayList<JavaStatement>();
    List<ScriptTemplateGetMethod> relatedTemplateGetMethods = new ArrayList<ScriptTemplateGetMethod>();
    if (owner instanceof CommunicationDeclaration) {
    	for (CommunicationPartnerDeclaration partner: ((CommunicationDeclaration) owner).partners)
    	{
    		CommunicationPartnerDeclaration f = partner.firstOccurrence==null? partner: partner.firstOccurrence;
    		relatedTemplateGetMethods.add(f.templateGetMethod);
    	}
    }
    else if (owner instanceof CommunicationPartnerDeclaration) {
    	for (CommunicationDeclaration communication: ((CommunicationPartnerDeclaration) owner).communications)
    	{
    		relatedTemplateGetMethods.add(communication.templateGetMethod);
    	}
    }
    else if (owner instanceof ChannelDeclaration) {
    	ChannelDeclaration channelDeclaration = (ChannelDeclaration)owner;
  		relatedTemplateGetMethods.add(
  				doChannelPartner? channelDeclaration.templateGetMethod
  								: channelDeclaration.partnerTemplateGetMethod);
    }
    try {
     LabelInstruction label   = new LabelInstruction("new.template", owner);
     ScriptTemplateArrayGenerator scriptTemplateArrayGenerator
                              = new ScriptTemplateArrayGenerator (env, scanner, typeDeclaration, constantPoolOwner); 
     Object templateArray[][] =     scriptTemplateArrayGenerator.makeTemplateArrayFor(m);

     emit       (m.templateVariable.loadInstructions (env, constantPoolOwner, owner));
     emitIfNull (label              , owner);
     emit       (m.templateVariable.loadInstructions (env, constantPoolOwner, owner));
     emitCode   (INSTRUCTION_areturn, owner);
     emit       (label);

     // load parameters and call initializer
     emitLoadString      (classType.packageNameWithDots()                  , owner);
     emitLoadString      (classType.className                              , owner);
     emitLoadString      (m.scriptDeclaration().name                       , owner);
     emitLoadString      (m.scriptDeclaration().getParameterSignature(env) , owner);
     if (codeMethodName==null)
     {
    	 emitLoadNull(owner);
     }
     else
     {
    	 emitLoadString      (codeMethodName                                   , owner);
     }
     emitLoadInteger     (m.scriptDeclaration().modifiers.modifierFlags    , owner);
     emitLoadInteger     (m.scriptDeclaration().getFormalIndexCount()      , owner);
     emitLoadInteger     (m.scriptDeclaration().getParameters().size()
    		            - m.scriptDeclaration().getFormalIndexCount()      , owner);
     emitLoadNestedArray (templateArray                                    , owner, 2);

     Method init = env.mustResolveMethod (
                   env.scripticVmNodeTemplateType,
                   "makeNodeTemplate",
                   "(Ljava/lang/String;Ljava/lang/String;"
                   +"Ljava/lang/String;Ljava/lang/String;"
                   +"Ljava/lang/String;"
                   +"ISS[[Ljava/lang/Object;)Lscriptic/vm/NodeTemplate;");
     emit (init.invokeInstruction (env, INSTRUCTION_invokestatic, constantPoolOwner, owner));

     emit (m.templateVariable.storeInstructions (env, constantPoolOwner, owner));

     // call to setRelatedTemplates
     //  getstatic T.a_b_comm_template : scriptic.vm.NodeTemplate [79]
     //  iconst_2
     //  anewarray scriptic.vm.NodeTemplate [74]
	 //  dup
	 //  iconst_0
	 //  invokestatic T.a_template() : scriptic.vm.NodeTemplate [85]
	 //  aastore
	 //  dup
	 //  iconst_1
	 //  invokestatic T.b_template() : scriptic.vm.NodeTemplate [87]
	 //  aastore
	 //  invokevirtual scriptic.vm.NodeTemplate.setRelatedTemplates(scriptic.vm.NodeTemplate[]) : void [89]
     if (relatedTemplateGetMethods.size()>0)
     {
         emit (m.templateVariable.loadInstructions (env, constantPoolOwner, owner));
    	 emitLoadInteger(relatedTemplateGetMethods.size(), owner);
         emit (new Instruction (INSTRUCTION_anewarray,
                 env.scripticVmNodeTemplateType.classRef(constantPoolOwner),
                 owner));
         for (int i=0; i<relatedTemplateGetMethods.size(); i++)
         {
        	 ScriptTemplateGetMethod relatedTemplateGetMethod = relatedTemplateGetMethods.get(i);
        	 emitCode (INSTRUCTION_dup, owner);
        	 emitLoadInteger(i, owner);
             emit (relatedTemplateGetMethod.invokeInstruction (env, INSTRUCTION_invokestatic, constantPoolOwner, owner));
        	 emitCode (INSTRUCTION_aastore, owner);
         }
         Method setRelatedTemplates = env.mustResolveMethod (
                 env.scripticVmNodeTemplateType,
                 "setRelatedTemplates",
                 "([Lscriptic/vm/NodeTemplate;)V");
         emit (setRelatedTemplates.invokeInstruction (env, INSTRUCTION_invokevirtual, constantPoolOwner, owner));
     }

     emit (m.templateVariable.loadInstructions (env, constantPoolOwner, owner));
     emitCode (INSTRUCTION_areturn, owner);
     codeAttribute. setVarSize (0);
     m.addAttribute (codeAttribute);
   } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, owner);
   } catch (java.io.IOException e) {parserError (2, e.getMessage(), owner.sourceStartPosition, 
                                                                 owner.  sourceEndPosition);
   }
  }


  void startScriptBody (BasicScriptDeclaration decl) {
      if (decl.scriptExpression != null) { // else abstract...
          if (decl.isAbstract()
          ||  classType.isInterface())
              parserError (3, "INTERNAL ERROR: abstract method has statements", 
                              decl.sourceStartPosition,
                              decl.sourceEndPosition);

          ScriptCodeMethod codeMethod = decl.codeMethod;
          codeAttribute  = new CodeAttribute(codeMethod,classType);
          statementStack = new ArrayList<JavaStatement>();

          try {
            // prepare and emit the switch instruction
            codeMethod.loadIndexInstruction = codeMethod.indexParameter.loadInstructions (env, codeMethod.ownerClass(env), decl)[0];
                                                            // 1 element in array because it is just a parameter
            codeMethod.   switchInstruction = new TableswitchInstruction (decl);
            codeMethod.            endLabel = new LabelInstruction ("switch.end"  , decl);
            DefaultCaseTag defaultTag       = new DefaultCaseTag ();
                           defaultTag.label = codeMethod.endLabel;

            emit (codeMethod.loadIndexInstruction);
            emit (codeMethod.   switchInstruction);

          } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, decl);
          } catch (java.io.IOException e) {parserError (2, e.getMessage(), decl.sourceStartPosition, 
                                                                        decl.  sourceEndPosition);
          }
      }
  }

  void endScriptBody (BasicScriptDeclaration decl) {
    try {
      if (decl.scriptExpression != null) { // else abstract...

          ScriptCodeMethod codeMethod = decl.codeMethod;
          if (codeMethod.switchTargetLabels.size() < 2) {
              codeAttribute.removeInstruction (codeMethod.loadIndexInstruction);
              codeAttribute.removeInstruction (codeMethod.   switchInstruction);
          }
          else {
              LabelInstruction targets[] = new LabelInstruction [codeMethod.switchTargetLabels.size()];
              for (int i=0; i<targets.length; i++) {
                 targets[i] = codeMethod.switchTargetLabels.get(i);
              }
              codeMethod.switchInstruction.setContents (0, targets.length-1, codeMethod.endLabel, targets);
          }
          emit     (codeMethod.endLabel);
          emitCode (INSTRUCTION_return, decl);

          setCodeAttribute (decl.codeMethod);

          if (!env.doKeepMethodBodies) decl.scriptExpression = null;
      }
    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, decl);}
  }

   ClassType savedConstantPoolOwnerWhenDoingCodeFunction;

   protected void doCodeFragmentCaseTagPrefix (BasicScriptDeclaration decl) {
       savedConstantPoolOwnerWhenDoingCodeFunction = constantPoolOwner;
       constantPoolOwner = classType;

       LabelInstruction label = new LabelInstruction ("code "
                                                     + decl.codeMethod.switchTargetLabels.size());
       decl.codeMethod.switchTargetLabels.add (label);
       emit (label);
   }

   protected void doCodeFragmentCaseTagPostfix (BasicScriptDeclaration decl) {
      emitGoto (decl.codeMethod.endLabel, previousOwner);
      constantPoolOwner = savedConstantPoolOwnerWhenDoingCodeFunction;
   }

   //-------------------------------------------------------------------------------

   protected void processScriptDeclaration (ScriptDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    if (typeDeclaration.isInterface()
    ||  decl.isNative  ()   // illegal, but non-fatal
    ||  decl.isAbstract()) {
        return;
    }
    emitScriptMethod      (decl);
    emitTemplateGetMethod (decl, false);
 
    startScriptBody (decl);
    super.processScriptDeclaration (decl, level, retValue, arg1, arg2);
    endScriptBody (decl);

    // this must be the last thing because it may damage the expression tree a bit...
   }

   //-------------------------------------------------------------------------------

   protected void processCommunicationDeclaration (CommunicationDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    if (typeDeclaration.isInterface()
    ||  decl.isNative  ()   // illegal, but non-fatal
    ||  decl.isAbstract()) {
        return;
    }
    emitTemplateGetMethod (decl, false);

    processCommunicationPartners (decl, decl.partners, level + 1, retValue, arg1, arg2);

    startScriptBody (decl);
      if (decl.scriptExpression != null)
         processTopLevelScriptExpression (decl, decl.scriptExpression, level + 1, retValue, arg1, arg2);
    endScriptBody (decl);
   }

   protected void processCommunicationPartner   (CommunicationDeclaration decl,
                                                 CommunicationPartnerDeclaration partner, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if (typeDeclaration.isInterface()
      ||  decl.isNative  ()   // illegal, but non-fatal
      ||  decl.isAbstract()
      ||  partner.firstOccurrence!=null // as for 2nd a in: scripts a,b={} a,c={}
      ) {
          return;
      }
      emitScriptMethod            (partner);
      emitTemplateGetMethod       (partner, false);

      super.processCommunicationPartner (decl, partner, level, retValue, arg1, arg2);
   }

   //-------------------------------------------------------------------------------

   protected void processChannelDeclaration (ChannelDeclaration decl, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    classType.addMethod (decl.makeTarget (env));

    if (typeDeclaration.isInterface()
    ||  decl.isNative  ()   // illegal, but non-fatal
    ||  decl.isAbstract()) {
        return;
    }
    emitScriptMethod      (decl);
    emitTemplateGetMethod (decl, false);
    emitTemplateGetMethod (decl, true);
    startScriptBody (decl);
    super.processChannelDeclaration (decl, level, retValue, arg1, arg2);
    endScriptBody (decl);

    // this must be the last thing because it may damage the expression tree a bit...
   }

   /*-----------------------------------------------------------------*/

   protected void processNativeCodeFragment          (BasicScriptDeclaration scriptDeclaration, NativeCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processAnchor (scriptDeclaration, expression);
      if (expression.durationAssignment != null
      ||  expression.priorityAssignment != null) {
         doCodeFragmentCaseTagPrefix (scriptDeclaration);
         if (expression.durationAssignment != null)
            processJavaStatement (scriptDeclaration, expression.durationAssignment, level + 1, retValue, arg1, arg2);
         if (expression.priorityAssignment != null)
            processJavaStatement (scriptDeclaration, expression.priorityAssignment, level + 1, retValue, arg1, arg2);
         doCodeFragmentCaseTagPostfix (scriptDeclaration);
      }
      if (!expression.isEmpty()) {
        doCodeFragmentCaseTagPrefix (scriptDeclaration);
        processJavaStatements (scriptDeclaration, expression.statements, level + 1, retValue, arg1, arg2);
        doCodeFragmentCaseTagPostfix (scriptDeclaration);
      }
   }
   private void processAnchor(BasicScriptDeclaration scriptDeclaration, ScriptExpression expression)
   {
	   if (expression.anchorExpression != null) {
	       doCodeFragmentCaseTagPrefix (scriptDeclaration);
	       try {
	           emitLoadNodeParameter (scriptDeclaration, expression, scriptDeclaration.isStatic());
	           Method m = env.mustResolveMethod (env.scripticVmNodeType,
	                                                "setAnchor",
	                                                "(Ljava/lang/Object;)V");
	           Instruction invoke = m.invokeInstruction (env, INSTRUCTION_invokevirtual, constantPoolOwner, expression);
	           processJavaExpression (scriptDeclaration,
	                                  expression.anchorExpression, 0, null, null, null);
	           emit (invoke);
	         } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, scriptDeclaration);}
	       doCodeFragmentCaseTagPostfix (scriptDeclaration);
	    }
	}
   protected void processEventHandlingCodeFragment   (BasicScriptDeclaration scriptDeclaration, EventHandlingCodeFragment expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processNativeCodeFragment (scriptDeclaration, (NativeCodeFragment) expression, level, retValue, arg1, arg2);
   }

   void assignToSuccessField (BasicScriptDeclaration scriptDeclaration, JavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    doCodeFragmentCaseTagPrefix (scriptDeclaration);
    try {
      emitLoadNodeParameter (scriptDeclaration, expression, scriptDeclaration.isStatic());
      processJavaExpression (scriptDeclaration, expression, level + 1, retValue, arg1, arg2);
      //working code from the time when success was a boolean:
      //MemberVariable successField = env.scripticVmNodeType.resolveMemberVariable (env, "success");
      //emit     (successField.storeInstructions (env, constantPoolOwner, expression));
      
      Method setSuccess_method = 
    	  env.mustResolveMethod (env.scripticVmNodeType, "setSuccess", "(Z)Z");
      MethodCallExpression setSuccess_methodCallExpression = new MethodCallExpression();
      setSuccess_methodCallExpression.target = setSuccess_method;
      setSuccess_methodCallExpression.setVirtualMode();
      emit (setSuccess_methodCallExpression.invokeInstruction (env, constantPoolOwner));
      emitPop(BooleanType.theOne, expression);
      
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, scriptDeclaration);
    }
    doCodeFragmentCaseTagPostfix (scriptDeclaration);
   }

   protected void processIfScriptExpression      (BasicScriptDeclaration scriptDeclaration, IfScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processAnchor(scriptDeclaration, expression);
      if (!expression.condition.isConstant())
        assignToSuccessField (scriptDeclaration, expression.condition, level, retValue, arg1, arg2);
      processScriptExpression (scriptDeclaration, expression.ifTerm, level + 1, retValue, arg1, arg2);
      if (expression.elseTerm != null)
         processScriptExpression (scriptDeclaration, expression.elseTerm, level + 1, retValue, arg1, arg2);
   }

   protected void processWhileScriptExpression   (BasicScriptDeclaration scriptDeclaration, WhileScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processAnchor(scriptDeclaration, expression);
      if (!expression.condition.isConstant())
        assignToSuccessField (scriptDeclaration, expression.condition, level, retValue, arg1, arg2);
   }

   protected void processForScriptExpression    (BasicScriptDeclaration scriptDeclaration, ForScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processAnchor(scriptDeclaration, expression);
      if (expression.initExpression != null) {
         doCodeFragmentCaseTagPrefix (scriptDeclaration);
         processJavaExpression ((FieldDeclaration) scriptDeclaration,
                                expression.initExpression, 0, null, null, null);
         //int popInstructionCode = expression.initExpression.dataType.popInstructionCode();
         //if (popInstructionCode>=0) emitCode (popInstructionCode, expression.initExpression);
         doCodeFragmentCaseTagPostfix (scriptDeclaration);
      }
      if (expression.condition != null
      && !expression.condition.isConstant()) {
         assignToSuccessField (scriptDeclaration, expression.condition, level, retValue, arg1, arg2);
      }
      if (expression.loopExpression != null) {
         doCodeFragmentCaseTagPrefix (scriptDeclaration);
         processJavaExpression ((FieldDeclaration) scriptDeclaration,
                                expression.loopExpression, 0, null, null, null);
         //int popInstructionCode = expression.initExpression.dataType.popInstructionCode();
         //if (popInstructionCode>=0) emitCode (popInstructionCode, expression.initExpression);
         doCodeFragmentCaseTagPostfix (scriptDeclaration);
      }
   }

   protected void processSwitchScriptExpression  (BasicScriptDeclaration scriptDeclaration, SwitchScriptExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    processAnchor(scriptDeclaration, expression);
    doCodeFragmentCaseTagPrefix (scriptDeclaration);
    try {

      // generate code like:
      // _n_.pass = switch (exp) {case 1: case 12: 1; break; ...}
      emitLoadNodeParameter (scriptDeclaration, expression, scriptDeclaration.isStatic()); // for assignment to pass field...

      processJavaExpression (scriptDeclaration, expression.switchExpression, 0, null, null, null);

      LabelInstruction    endLabel        = new LabelInstruction ("switch.end", expression);
      LabelInstruction    defaultTagLabel = null;
      LookupswitchInstruction instruction = new LookupswitchInstruction (expression);
      ArrayList<LabelInstruction> targetVector = new ArrayList<LabelInstruction>();
      ArrayList<ConstantSmallOrNormalInteger> tagVector = new ArrayList<ConstantSmallOrNormalInteger>();

      emit (instruction);
      for (int i = 0; i < expression.caseTags.size(); i++) {
         CaseTagScriptExpression tagExp = expression.caseTags.get (i);

         // generate "label: load i; goto end"
         
         LabelInstruction tagLabel = new LabelInstruction ("switch.tag", expression);
         emit (tagLabel);
         emitLoadInteger (i+1, tagExp);
         emitGoto (endLabel, tagExp);

         for (JavaExpression tag: tagExp.tags) {

            if (   tag instanceof SpecialNameExpression
                && ((SpecialNameExpression) tag).getToken() == DefaultToken) {
               defaultTagLabel = tagLabel;
            } else {
               targetVector.add (tagLabel);
                  tagVector.add ((ConstantSmallOrNormalInteger)tag.constantValue);
            }
         }
      }
      if (defaultTagLabel==null) {
          defaultTagLabel = new LabelInstruction ("switch.default", expression);
          emit (defaultTagLabel);
          emitLoadInteger (0, expression);
      }
      int size = expression.caseTags.size();
      LabelInstruction targets[] = new LabelInstruction[size];
      int              tags   [] = new int             [size];
      for (int i=0; i<size; i++) {
           targets[i] = targetVector.get(i);
           tags   [i] =    tagVector.get(i).intValue();
      }
      instruction.setContents (defaultTagLabel, tags, targets);
      emit        (endLabel);

      // store in pass variable...
      emit (env.scripticVmNodePass.storeInstructions (env, constantPoolOwner, expression));

      doCodeFragmentCaseTagPostfix (scriptDeclaration);

      for (CaseTagScriptExpression caseTag: expression.caseTags) {
         processScriptExpression (scriptDeclaration, caseTag.caseExpression, level + 1, retValue, arg1, arg2);
      }
     } catch (ByteCodingException e) {env.handleByteCodingException (e, typeDeclaration, expression);
     } catch (IOException e) {parserError (2, e.getMessage());
     }
   }

   protected void processScriptCallExpression    (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
if (expression.target==null)
throw new RuntimeException ("expression.target==null in script call: "+expression.name);

    processAnchor(scriptDeclaration, expression);
    try {
      doCodeFragmentCaseTagPrefix (scriptDeclaration);

      // largely copied from processMethodCallExpression:

  handleScriptAccessExpression:
      {
        JavaExpressionWithTarget accessExpression = expression.scriptAccessExpression;
        if (accessExpression.languageConstructCode()==NameExpressionCode) {
            // just a script name. load 'this' if needed


            if (!expression.isStaticMode()) {
                emitLoadThisFor (expression);
            }
            break handleScriptAccessExpression;
        }
        if (accessExpression.languageConstructCode()==FieldAccessExpressionCode) {
            JavaExpression primary = ((FieldAccessExpression) accessExpression).primaryExpression;
            if (primary.isThis ()
            ||  primary.isSuper()) {
                if (!expression.isStaticMode()) {
                    emitLoadThis (expression);
                }
                break handleScriptAccessExpression;
            }
        }
        processJavaExpression (scriptDeclaration, accessExpression,
                               level + 1, retValue, arg1, arg2);
        if (expression.isStaticMode()
        &&  accessExpression.languageConstructCode()==FieldAccessExpressionCode) {
           FieldAccessExpression f = (FieldAccessExpression)accessExpression;
           if (!f.primaryExpression.isTypeName()) {
              emitPop (f.primaryExpression.dataType, f.primaryExpression);
           }
        }
      }
      emitLoadNodeParameter (scriptDeclaration, expression, scriptDeclaration.isStatic());

      ArrayList<ScriptCallParameter> outputAndAdaptingParameters = new ArrayList<ScriptCallParameter>();
      if (expression.parameterList != null) {
         processScriptCallParameterList (scriptDeclaration, expression, expression.parameterList, level + 1, retValue, outputAndAdaptingParameters, arg2);
      }
      emit (expression.invokeInstruction (env, constantPoolOwner));

      doCodeFragmentCaseTagPostfix (scriptDeclaration);

      for (ScriptCallParameter parameter: outputAndAdaptingParameters) {

         // Parameter transfer
         // s(str?)  >>>  str      = (String) ((ObjectHolder) _n_.calleeParams()[0]).value;
         // s(i ?!)  >>>  _n_.localData[3] = ((IntegerHolder) _n.calleeParams()[2]).value;

         doCodeFragmentCaseTagPrefix (scriptDeclaration);
         processJavaExpression (scriptDeclaration, parameter.expression, level + 1, retValue, loadReferenceOnly /*arg1*/, arg2);

         emitLoadNodeParameter (scriptDeclaration, expression, scriptDeclaration.isStatic());
         Method calleeParamsMethod = env.mustResolveMethod (env.scripticVmNodeType,
                                                   "calleeParams",
                                                   "()[Lscriptic/vm/ValueHolder;");
         emit (calleeParamsMethod.invokeInstruction (env, INSTRUCTION_invokevirtual,
                                                     constantPoolOwner, expression));
         emitLoadInteger (parameter.actualIndex, expression);
         emitCode (INSTRUCTION_aaload, expression);

         DataType dataType = parameter.dataType;
         ClassType holderClass = dataType.holderClass(env);
         emitCheckCast   (holderClass, expression);
         MemberVariable v = holderClass.resolveMemberVariable (env, "value");
         emit (v. loadInstructions (env, constantPoolOwner, expression));

         if (!dataType.isPrimitive()
         &&   dataType != env.javaLangObjectType) {
             emit (new Instruction (INSTRUCTION_checkcast,
                                    constantPoolOwner.resolveClass (dataType),
                                    expression));
         }
         emit (parameter.expression.storeInstructions (env, constantPoolOwner));

         doCodeFragmentCaseTagPostfix (scriptDeclaration);
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, expression);
    } catch (java.io.IOException e) {parserError (2, e.getMessage(), expression.sourceStartPosition, 
                                                                  expression.  sourceEndPosition);
    }
   }

   // arg1 - outputAndAdaptingParameters to be sampled
   protected void processScriptCallParameterList  (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, MethodCallParameterList parameterList, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (parameterList.parameterExpressions.isEmpty()) return;
      for (JavaExpression obj: parameterList.parameterExpressions) {
    	  ScriptCallParameter scp = (ScriptCallParameter) obj;
         processScriptCallParameter (scriptDeclaration, scriptCall, scp, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processScriptCallFormalIndex    (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, JavaExpression formalIndex, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (scriptDeclaration, formalIndex, 0, null, null, null);
   }

   // arg1 - outputAndAdaptingParameters to be sampled
   protected void processScriptCallParameter      (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression scriptCall, ScriptCallParameter parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      ArrayList<ScriptCallParameter> outputAndAdaptingParameters = (ArrayList<ScriptCallParameter>) arg1;

      processJavaExpression (scriptDeclaration, parameter.expression, 0, null, null, null);

      // To keep it consistent, only process the params if they were
      // resolved with no errors
      if (parameter.isAdapting) {
// ????????????? if (parameter.formalParameter != null) 
            outputAndAdaptingParameters.add (parameter);
      } else if (parameter.isOutput) {
         //if (parameter.targetDeclaration != null)             ?????????????????
            outputAndAdaptingParameters.add (parameter);
      }
   }

   protected void processScriptLocalVariable  (BasicScriptDeclaration scriptDeclaration, LocalScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      doCodeFragmentCaseTagPrefix (scriptDeclaration);

      //_n_.setLocalData(slot, new ValueHolder (){.value=initializer})
      ScriptLocalVariable sl = (ScriptLocalVariable) variableDeclaration.target;
      emitLoadNodeParameter (scriptDeclaration, variableDeclaration, scriptDeclaration.isStatic());
      emitLoadInteger (sl.slot, variableDeclaration);

      try {
        // push new ValueHolder
        // why are Objects prohibited instead of holders to Objects?
        // ? because otherwise processing here would become to complex?
        ClassType holderClass = variableDeclaration.dataType().holderClass(env);
        emit     (new Instruction (INSTRUCTION_new,
                                   holderClass.classRef(constantPoolOwner),
                                   variableDeclaration));
        emitCode (INSTRUCTION_dup, variableDeclaration);
        Method init = env.mustResolveMethod (holderClass, "<init>", "()V");
        emit (init.invokeInstruction (env, INSTRUCTION_invokespecial, constantPoolOwner, variableDeclaration));

        if (variableDeclaration.initializer != null) {
           emitCode (INSTRUCTION_dup, variableDeclaration.initializer);
           // push parameter for new ValueHolder
           processJavaExpression (scriptDeclaration, variableDeclaration.initializer, level, retValue, arg1, arg2);
           MemberVariable v = holderClass.resolveMemberVariable (env, "value");
           emit (v.storeInstructions (env, constantPoolOwner, variableDeclaration.initializer));
        }

        // call setLocalData
        Method m = env.mustResolveMethod (env.scripticVmNodeType,
                                          "setLocalData",
                                          "(ILscriptic/vm/ValueHolder;)V");
        emit (m.invokeInstruction (env, INSTRUCTION_invokevirtual, constantPoolOwner, variableDeclaration));
      } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, variableDeclaration);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), variableDeclaration.sourceStartPosition, 
                                                                    variableDeclaration.  sourceEndPosition);
      }
      doCodeFragmentCaseTagPostfix (scriptDeclaration);
   }

   protected void processPrivateScriptVariable  (BasicScriptDeclaration scriptDeclaration, PrivateScriptVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      BasicVariableDeclaration targetDeclaration 
               = variableDeclaration.targetDeclaration;

      doCodeFragmentCaseTagPrefix (scriptDeclaration);
      //_n_.setLocalData(slot, new ValueHolder (){.value = original.value})
      ScriptLocalVariable sl = (ScriptLocalVariable) variableDeclaration.target;
      emitLoadNodeParameter (scriptDeclaration, variableDeclaration, scriptDeclaration.isStatic());
      emitLoadInteger (sl.slot, variableDeclaration);

      try {

        // push new ValueHolder
        ClassType holderClass = variableDeclaration.dataType().holderClass(env);
        emit     (new Instruction (INSTRUCTION_new,
                                   holderClass.classRef(constantPoolOwner),
                                   variableDeclaration));
        emitCode (INSTRUCTION_dup, variableDeclaration);
        Method init = env.mustResolveMethod (holderClass, "<init>", "()V");
        emit (init.invokeInstruction (env, INSTRUCTION_invokespecial, constantPoolOwner, variableDeclaration));

        emitCode (INSTRUCTION_dup, variableDeclaration);

        // load _n.localData[targetDeclaration.slot]...
        emit (((LocalVariableOrParameter)targetDeclaration.targetField())
                .loadReferenceInstructions (scriptDeclaration, env, constantPoolOwner, variableDeclaration));
        MemberVariable v = holderClass.resolveMemberVariable (env, "value");
        emit (v. loadInstructions (env, constantPoolOwner, variableDeclaration));
        emit (v.storeInstructions (env, constantPoolOwner, variableDeclaration));

        // call setLocalData
        Method m = env.mustResolveMethod (env.scripticVmNodeType,
                                          "setLocalData",
                                          "(ILscriptic/vm/ValueHolder;)V");
        emit (m.invokeInstruction (env, INSTRUCTION_invokevirtual, constantPoolOwner, variableDeclaration));
      } catch (ByteCodingException      e) {env.handleByteCodingException (e, typeDeclaration, variableDeclaration);
      } catch (java.io.IOException e) {parserError (2, e.getMessage(), variableDeclaration.sourceStartPosition, 
                                                                    variableDeclaration.  sourceEndPosition);
      }
      doCodeFragmentCaseTagPostfix (scriptDeclaration);
   }
}

