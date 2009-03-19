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


import java.util.ArrayList;
import java.io.*;
import scriptic.tools.lowlevel.*;



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        JavaCodeGenerator5                        */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

/***** this is a temporary mixture of the old JavaCodeGenerator and ScripticCompilerPass
 * just for testing
 *
 */
class JavaCodeGenerator extends CodeGeneratorParseTreeEnumerator {


   protected ArrayList<JavaStatement> statementStack; // nothing to do with stack size...
   TypeDeclaration typeDeclaration;

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
        statementStack.add((JavaStatement)s);
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
   protected void processJumpingStatement (JumpingStatement jumpingStatement) {

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
                  }
                  else {
                      lab.isTargetOfBreak = true;
                  }
                  return;
              }
              break;

         case  SwitchStatementCode:
              if (jumpingStatement.languageConstructCode()==BreakStatementCode
              && !jumpingStatement.hasTargetLabel()) {
                  return;
              }
              if (jumpingStatement.languageConstructCode()==BreakStatementCode
              && !jumpingStatement.hasTargetLabel()) {
                  ((BreakableStatementInterface)s).setTargetOfBreak();
              }
              break;

         case     ForStatementCode:
         case   WhileStatementCode:
         case      DoStatementCode:
              if (jumpingStatement.languageConstructCode()==BreakStatementCode
              && !jumpingStatement.hasTargetLabel()) {
                  ((BreakableStatementInterface)s).setTargetOfBreak();
              }
              if (jumpingStatement instanceof BreakOrContinueStatement
              && !jumpingStatement.hasTargetLabel()) {
                  return;
              }
              break;
         }
     }
     switch (jumpingStatement.languageConstructCode()) {
     case    ReturnStatementCode: break;
     case     ThrowStatementCode: break;
     case     BreakStatementCode:
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

       DataType exceptionTypes[] = methodCall.target.exceptionTypes(env);
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
   protected void throwableIsThrown (FieldDeclaration  fieldDeclaration,
                                     DataType          throwable,
                                     LanguageConstruct thrower) {
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
               parserError (2,  "Exception " + throwable.getNameWithDots()
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
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (IOException   e) {parserError (2, e.getMessage(), thrower.sourceStartPosition, 
                                                          thrower.  sourceEndPosition);
    }
   }


   protected void processBaseDimensionDeclaration (BaseDimensionDeclaration baseDimensionDeclaration,
                                                   int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (PublicToken);
      outSpace  ();
      outToken  (IntToken);
      outSpace  ();
      outString (baseDimensionDeclaration.unit.target.name);
      outSpace  ();
      outToken  (AssignToken);
      outSpace  ();
      outInteger(1);
      outToken  (SemicolonToken);
      outLine   ();
   }

   /*-------------------------- Constructors -------------------------*/

   public JavaCodeGenerator (Scanner                  scanner,
                             PreprocessorOutputStream outputStream,
                             CompilerEnvironment env) {
      super (scanner, outputStream, env);
   }

   /*-----------------------------------------------------------------*/

   public void outStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock, boolean outBraces) {
      processStatementBlock (refinement, statementBlock, 0, null, null, outBraces? this: null);
   }

   public void outJavaExpression (FieldDeclaration fieldDeclaration, JavaExpression expression) {
      processJavaExpression (fieldDeclaration, expression, 0, null, null, null);
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                           CompilationUnit                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


   protected void processPackageStatement (PackageStatement packageStatement, ReturnValueHolder retValue, Object arg1, Object arg2) {
      packageStatement.outSource (outputStream);
      outLine ();
   }

   protected void processImportStatements (ArrayList<ImportStatement> importStatements, ReturnValueHolder retValue, Object arg1, Object arg2) {
      super.processImportStatements (importStatements, retValue, arg1, arg2);
      if (importStatements.size() > 0) {
         outLine ();
         outLine ();
      }
   }

   protected void processImportStatement (ImportStatement importStatement, ReturnValueHolder retValue, Object arg1, Object arg2) {
      importStatement.outSource (outputStream);
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                     Class and Interface stuff                   */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processTopLevelTypeDeclaration (TopLevelTypeDeclaration t, ReturnValueHolder retValue, Object arg1, Object arg2) {
      typeDeclaration = t; // ?????
      outHeaderSource (typeDeclaration);
      outLine ();
      outToken  (BraceOpenToken);
      outLine   ();
      indent    ();

      super.processTopLevelTypeDeclaration (t, retValue, arg1, arg2);

      unindent  ();
      outLine   ();
      outToken  (BraceCloseToken);
      outLine   ();
   }

   protected void processLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      typeDeclaration = t; // ?????
      if (!(t instanceof AnonymousTypeDeclaration)) // else we would get "{"...
         outHeaderSource (typeDeclaration);
      outLine ();
      outToken  (BraceOpenToken);
      outLine   ();
      indent    ();

      super.processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);

      unindent  ();
      outLine   ();
      outToken  (BraceCloseToken);
      outLine   ();
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                      Field declaration stuff                    */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processMultiVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    //outStatement (multiVariableDeclaration);

      outMultiVariableDeclaration (multiVariableDeclaration);
      outLine   ();
   }

   /* ------------------------------------------------------------------ */

   protected void processMethodDeclaration     (MethodDeclaration methodDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    //outHeaderSource (methodDeclaration);

	  ArrayList<JavaStatement> savedStatementStack = statementStack;
      statementStack = new ArrayList<JavaStatement>();
      try {
        methodDeclaration.outModifiers (outputStream);
        processDataTypeDeclaration (methodDeclaration.returnTypeDeclaration); 
        outSpace  ();
        methodDeclaration.outName (outputStream);
        outSpace  ();
        outParameterList (methodDeclaration.parameterList);

        if (methodDeclaration.throwsClause != null) {
            indent   (9);
            outLine  ();
            processThrowsClause (methodDeclaration,
                                methodDeclaration.throwsClause, level, retValue, arg1, arg2);
            unindent ();
        }

        if (   methodDeclaration.typeDeclaration.isInterface ()
            || methodDeclaration.isAbstract ()
            || methodDeclaration.isNative ()) {
            outToken  (SemicolonToken);
            outLine   ();
            return;
        }

        outSpace  ();

        //outSource         (methodDeclaration.statements);
        outStatementBlock (methodDeclaration,methodDeclaration.statements, true);
        outLine   ();

        if (methodDeclaration.statements.canCompleteNormally()) {
            if (!methodDeclaration.target.returnType.isVoid())
                parserError (2, "Method must return value", 
                            methodDeclaration.sourceStartPosition,
                            methodDeclaration.sourceEndPosition);
        }
      }
      finally {
        statementStack = savedStatementStack;
      }
   }

   /* ------------------------------------------------------------------ */

   protected void processConstructorDeclaration     (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

	  ArrayList<JavaStatement> savedStatementStack = statementStack;
      statementStack = new ArrayList<JavaStatement>();
      try {
        outHeaderSource   (constructorDeclaration);
        outSpace          ();
        outToken          (BraceOpenToken);
        outLine           ();
        indent    ();

        if (constructorDeclaration.otherConstructorInvocation != null) { // else Object ...
            outJavaExpression (constructorDeclaration,
                                constructorDeclaration.otherConstructorInvocation);
            outToken (SemicolonToken);
        }

        //outSource         (constructorDeclaration.statements);
        outStatementBlock (constructorDeclaration,constructorDeclaration.statements, false);
        unindent  ();
        outLine   ();
        outToken          (BraceCloseToken);
        outLine           ();
      }
      finally {
        statementStack = savedStatementStack;
      }
   }

   /* ------------------------------------------------------------------ */

   protected void processThrowsClause (MethodDeclaration method, ThrowsClause throwsClause, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      //for (DataTypeDeclaration exceptionType: throwsClause.exceptionTypeDeclarations) {
        outThrowsClause (throwsClause);
      //}
   }

   /* ------------------------------------------------------------------ */

   protected void processInitializerBlock     (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    //outSource       (initializerBlock);

	  ArrayList<JavaStatement> savedStatementStack = statementStack;
      statementStack = new ArrayList<JavaStatement>();
      try {

        if (initializerBlock.isStatic()) outToken  (StaticToken);
        outSpace  ();
        outStatementBlock (initializerBlock,initializerBlock.statements, true);
        outLine   ();
      }
      finally {
        statementStack = savedStatementStack;
      }
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                                                                 */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void outMultiVariableDeclaration (MultiVariableDeclaration declaration) {

      declaration.outModifiers (outputStream);
      processDataTypeDeclaration (declaration.dataTypeDeclaration); 
      if (declaration.variables.size () > 1) {
         indent   ();
         outLine  ();
      } else {
         outSpace ();
      }
      boolean                   firstTime = true;
      for (MemberOrLocalVariableDeclaration mvd: declaration.variables) {
    	  MemberVariableDeclaration obj = (MemberVariableDeclaration) mvd;
         if (firstTime)
            firstTime = false;
         else {
            outToken  (CommaToken);
            outLine   ();
         }

         outString (obj.getName ());
         int additionalArrayDimensions = obj.extraArrayDimensions;
         if (additionalArrayDimensions > 0) {
            outSpace  ();
            for (int i = 0; i < additionalArrayDimensions; i++) {
               outToken  (BracketOpenToken);
               outSpace  ();
               outToken  (BracketCloseToken);
            }
         }
         if (obj.initializer != null) {
            outSpace  ();
            outToken  (AssignToken);
            outSpace  ();
            outJavaExpression (null, obj.initializer);
         }
      }

      outToken (SemicolonToken);
      if (declaration.variables.size () > 1) {
         unindent  ();
      }
   }

   /*-----------------------------------------------------------------*/

   protected void outParameterList (ParameterList parameterList) {

      outToken (ParenthesisOpenToken);

      if (parameterList != null) {
         boolean                    firstTime = true;
         for (MethodParameterDeclaration param: parameterList.parameters) {
         
            if (firstTime)
               firstTime = false;
            else {
               outToken  (CommaToken);
               outSpace  ();
            }

            processDataTypeDeclaration (param.dataTypeDeclaration); 
            outSpace  ();
            param.outName (outputStream);
         }
      }

      outToken (ParenthesisCloseToken);
   }

   /*-----------------------------------------------------------------*/

   protected void outThrowsClause (ThrowsClause throwsClause) {

      if (throwsClause == null)
         return;

      outToken (ThrowsToken);
      outSpace ();

      boolean firstTime = true;
      for (DataTypeDeclaration exceptionType: throwsClause.exceptionTypeDeclarations) {
         if (firstTime)
            firstTime = false;
         else {
            outToken (CommaToken);
            outSpace ();
         }

         outString (exceptionType.getName());
      }
   }



   /*******************************************************************/
   /**                                                               **/
   /**                          STATEMENTS                           **/
   /**                                                               **/
   /*******************************************************************/

   protected void processDataTypeDeclaration (DataTypeDeclaration castTypeDeclaration) {
      outString (castTypeDeclaration.getName ());
      if (castTypeDeclaration.noOfArrayDimensions > 0) {
         outSpace  ();
         for (int i = 0; i < castTypeDeclaration.noOfArrayDimensions; i++) {
            outToken  (BracketOpenToken);
            outSpace  ();
            outToken  (BracketCloseToken);
         }
      }
   }

   protected void processJavaStatements (RefinementDeclaration refinement, ArrayList<JavaStatement> statements, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      boolean         firstTime = true;
      boolean previousCanCompleteNormally = true;

      // we have to remove empty statements,
      // otherwise we'll get unreachable messages on 'return;;' ...
      for (int i = statements.size()-1; i>=0; i--) {
          JavaStatement statement = statements.get (i);
         if (statement.languageConstructCode() == EmptyStatementCode)
             statements.remove(i);
      }

      for (JavaStatement statement: statements) {
         if (firstTime)
            firstTime = false;
         else
            outLine   ();
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

   protected void processStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (arg2 != null) {
        outToken  (BraceOpenToken);
        indent    ();
        if (!statementBlock.statements.isEmpty ())
           outLine   ();
      }
      processJavaStatements (refinement, statementBlock.statements, level + 1, retValue, arg1, arg2);
      if (arg2 != null) {
        unindent  ();
        outLine   ();
        outToken  (BraceCloseToken);
      }
      else {
        outLine   ();
      }
   }

   protected void processEmptyStatement              (RefinementDeclaration refinement, EmptyStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (SemicolonToken);
   }

   protected void processCommentStatement            (RefinementDeclaration refinement, CommentStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   }

   protected void processLabeledStatement (RefinementDeclaration refinement, LabeledStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      LabeledStatement conflict = findLabel (statement.name);
      if (conflict != null) {
         parserError (2, "Label same has with one defined higher in the statement hierarchy", 
                      statement.sourceStartPosition,
                      statement.sourceEndPosition);
      }
      outString (statement.getName ());
      outSpace  ();
      outToken  (ColonToken);
      outSpace  ();
      pushStatement(statement);
      processJavaStatement (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      popStatement(statement);
   }


   protected void processNestedStatement             (RefinementDeclaration refinement, NestedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (BraceOpenToken);
      indent    ();
      if (!statement.statements.isEmpty ())
         outLine   ();
      processJavaStatements (refinement, statement.statements, level + 1, retValue, arg1, arg2);
      unindent  ();
      outLine   ();
      outToken  (BraceCloseToken);
   }

   protected void processLocalVariableDeclarationStatement (RefinementDeclaration refinement, LocalVariableDeclarationStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLocalVariableDeclarationStatement (refinement, statement, level, retValue, arg1, arg2, true);
   }

   protected void processLocalVariableDeclarationStatement (RefinementDeclaration refinement, LocalVariableDeclarationStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2, boolean extraLineBreak) {

      processDataTypeDeclaration (statement.dataTypeDeclaration); 
      if (statement.variables.size () > 1 && extraLineBreak) {
         indent   ();
         outLine  ();
      } else {
         outSpace ();
      }

      boolean firstTime = true;
      for (LocalVariableDeclaration lvd: statement.variables) {

         if (firstTime)
            firstTime = false;
         else {
            outToken  (CommaToken);
            if (extraLineBreak)
               outLine   ();
            else
               outSpace  ();
         }

         outString (lvd.getName ());
         int additionalArrayDimensions = lvd.extraArrayDimensions;
         if (additionalArrayDimensions > 0) {
            outSpace  ();
            for (int i = 0; i < additionalArrayDimensions; i++) {
               outToken  (BracketOpenToken);
               outSpace  ();
               outToken  (BracketCloseToken);
            }
         }
         if (lvd.initializer != null) {
            outSpace  ();
            outToken  (AssignToken);
            outSpace  ();
            processJavaExpression (refinement, lvd.initializer, level + 1, retValue, arg1, arg2);
         }
      }

      outToken (SemicolonToken);
      if (statement.variables.size () > 1 && extraLineBreak) {
         unindent  ();
      }
   }


   protected void processCaseTag                     (RefinementDeclaration refinement, CaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (CaseToken);
      outSpace  ();
      processJavaExpression (refinement, statement.tagExpression, level + 1, retValue, arg1, arg2);
      outSpace  ();
      outToken  (ColonToken);
      outSpace  ();
   }

   protected void processDefaultCaseTag              (RefinementDeclaration refinement, DefaultCaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (DefaultToken);
      outSpace  ();
      outToken  (ColonToken);
      outSpace  ();
   }

   protected void processExpressionStatement         (RefinementDeclaration refinement, ExpressionStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (statement.omitSemicolonTerminator)
         outToken  (ParenthesisOpenToken);

      processJavaExpression (refinement, statement.expression, level + 1, retValue, arg1, arg2);

      if (statement.omitSemicolonTerminator)
         outToken  (ParenthesisCloseToken);
      else
         outToken  (SemicolonToken);
   }

   protected void processIfStatement                 (RefinementDeclaration refinement, IfStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (IfToken);
      outSpace  ();
      outToken  (ParenthesisOpenToken);
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      outToken  (ParenthesisCloseToken);
      outSpace  ();

      if (statement.trueStatement.languageConstructCode () != NestedStatementCode) {
         indent  ();
         outLine ();
      }
      processJavaStatement  (refinement, statement.trueStatement, level + 1, retValue, arg1, arg2);
      if (statement.trueStatement.languageConstructCode () != NestedStatementCode)
         unindent ();
      else
         outSpace  ();

      if (statement.falseStatement != null) {
         if (statement.trueStatement.languageConstructCode () != NestedStatementCode)
            outLine ();
         outToken (ElseToken);
         outSpace  ();

         if (statement.falseStatement.languageConstructCode () != NestedStatementCode) {
            indent  ();
            outLine ();
         }
         processJavaStatement  (refinement, statement.falseStatement, level + 1, retValue, arg1, arg2);
         if (statement.falseStatement.languageConstructCode () != NestedStatementCode)
            unindent ();
      }
   }

   protected void processSwitchStatement             (RefinementDeclaration refinement, SwitchStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (!statement.statements.isEmpty()) {
         JavaStatement firstStatement = statement.statements.get(0);
         if (!(firstStatement instanceof CaseOrDefaultTag)) {
               parserError (2, "Statement unreachable", 
                            firstStatement.sourceStartPosition,
                            firstStatement.sourceEndPosition);
         }
      }

      pushStatement(statement);
      outToken  (SwitchToken);
      outSpace  ();
      outToken  (ParenthesisOpenToken);
      processJavaExpression  (refinement, statement.switchExpression, level + 1, retValue, arg1, arg2);
      outToken  (ParenthesisCloseToken);
      outSpace  ();
      outToken  (BraceOpenToken);
      indent    ();

      indent (6);
      for (JavaStatement js: statement.statements) {
         int code = js.languageConstructCode ();
         if (code == CaseTagCode || code == DefaultCaseTagCode)
            unindent ();
         outLine ();
         processJavaStatement (refinement, js, level, retValue, arg1, arg2);
         if (code == CaseTagCode || code == DefaultCaseTagCode)
            indent (6);
      }
      unindent ();
      unindent ();
      outLine  ();
      outToken  (BraceCloseToken);

      popStatement(statement);
   }

   protected void processWhileStatement              (RefinementDeclaration refinement, WhileStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     if (statement.conditionExpression.isFalse()) {
         parserError (2, "Code unreachable", 
                      statement.statement.sourceStartPosition,
                      statement.statement.sourceEndPosition);
     }
      pushStatement(statement);

      outToken  (WhileToken);
      outSpace  ();
      outToken  (ParenthesisOpenToken);
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      outToken  (ParenthesisCloseToken);
      outSpace  ();

      if (statement.statement.languageConstructCode () != NestedStatementCode) {
         indent  ();
         outLine ();
      }
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      if (statement.statement.languageConstructCode () != NestedStatementCode)
         unindent ();

      popStatement(statement);
   }

   protected void processDoStatement                 (RefinementDeclaration refinement, DoStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      pushStatement(statement);

      outToken  (DoToken);
      outSpace  ();
      if (statement.statement.languageConstructCode () != NestedStatementCode) {
         indent  ();
         outLine ();
      }
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      if (statement.statement.languageConstructCode () != NestedStatementCode) {
         unindent ();
         outLine  ();
      } else
         outSpace ();

      outToken  (WhileToken);
      outSpace  ();
      outToken  (ParenthesisOpenToken);
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      outToken  (ParenthesisCloseToken);
      outToken  (SemicolonToken);

      popStatement(statement);
   }

   protected void processForStatement                (RefinementDeclaration refinement, ForStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (statement.conditionExpression != null
      &&  statement.conditionExpression.isFalse()) {
          parserError (2, "Code unreachable", 
                       statement.statement.sourceStartPosition,
                       statement.statement.sourceEndPosition);
      }

      pushStatement(statement);

      outToken  (ForToken);
      outSpace  ();
      outToken  (ParenthesisOpenToken);
      setIndent ();

      JavaStatement   js1 = null;
      boolean         firstTime = true;
      for (JavaStatement js: statement.initStatements) {
    	  js1 = js;
         if (firstTime)
            firstTime = false;
         else {
            outToken (CommaToken);
            outSpace ();
         }
         if (js.languageConstructCode () == ExpressionStatementCode) {
            /* Don't call processExpressionStatement() because it generates
               a semicolon */
            processJavaExpression (refinement, ((ExpressionStatement)js).expression, level + 1, retValue, arg1, arg2);
         } else {
            /* There's a single LocalVariableDeclarationStatement */
            processLocalVariableDeclarationStatement (refinement, (LocalVariableDeclarationStatement)js, level, retValue, arg1, arg2, false);
         }
      }

      if (statement.initStatements.size() == 0)
         outSpace ();

      if (   js1 == null
          || js1.languageConstructCode () == ExpressionStatementCode)
         outToken (SemicolonToken);

      if (statement.initStatements.size() > 0)
         outLine ();


      if (statement.conditionExpression == null) {
         if (statement.initStatements.size() == 0)
            outSpace ();
         outToken (SemicolonToken);
      } else {
         processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
         outToken (SemicolonToken);
         outLine  ();
      }

      firstTime = true;
      for (JavaStatement js: statement.loopStatements) {

         if (firstTime)
            firstTime = false;
         else {
            outToken (CommaToken);
            outSpace ();
         }
         /* Don't call processExpressionStatement() because it generates
            a semicolon */
         // A BETTER SOLUTION: Use the new "omitSemicolonTerminator" flag
         //                    recently added to class ExpressionStatement
         processJavaExpression (refinement, ((ExpressionStatement)js).expression, level + 1, retValue, arg1, arg2);
         /* Note that the loop statements are always ExpressionStatements
            according to the Java Spec and the JavaParser0 code */
      }

      if (statement.loopStatements.size() == 0) {
         if (statement.conditionExpression == null)
            outSpace ();
      }
      outToken (ParenthesisCloseToken);
      outSpace ();
      unindent ();

      if (statement.statement.languageConstructCode () != NestedStatementCode) {
         indent  ();
         outLine ();
      }
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      if (statement.statement.languageConstructCode () != NestedStatementCode)
         unindent ();

      popStatement(statement);
   }

   protected void processBreakStatement              (RefinementDeclaration refinement, BreakStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJumpingStatement (statement);
      outToken (BreakToken);
      if (statement.hasTargetLabel()) {
         outSpace  ();
         outString (statement.getName ());
      }
      outToken (SemicolonToken);
   }

   protected void processContinueStatement           (RefinementDeclaration refinement, ContinueStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJumpingStatement (statement);
      outToken (ContinueToken);
      if (statement.hasTargetLabel()) {
         outSpace  ();
         outString (statement.getName ());
      }
      outToken (SemicolonToken);
   }

   protected void processReturnStatement             (RefinementDeclaration refinement, ReturnStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken (ReturnToken);
      if (statement.returnExpression !=  null) {
         outSpace  ();
         processJavaExpression (refinement, statement.returnExpression, level + 1, retValue, arg1, arg2);
      }
      outToken (SemicolonToken);
      processJumpingStatement (statement);
   }

   protected void processThrowStatement              (RefinementDeclaration refinement, ThrowStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken (ThrowToken);
      outSpace  ();
      processJavaExpression (refinement, statement.throwExpression, level + 1, retValue, arg1, arg2);
      outToken (SemicolonToken);
      throwableIsThrown (refinement,
                         statement.throwExpression.dataType,
                         statement.throwExpression);
      processJumpingStatement (statement);
   }

   protected void processSynchronizedStatement       (RefinementDeclaration refinement, SynchronizedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (SynchronizedToken);
      outSpace  ();
      outToken  (ParenthesisOpenToken);
      processJavaExpression (refinement, statement.synchronizedExpression, level + 1, retValue, arg1, arg2);
      outToken  (ParenthesisCloseToken);
      outSpace  ();
      processNestedStatement (refinement, statement, level, retValue, arg1, arg2);
   }

   protected void processTryStatement                (RefinementDeclaration refinement, TryStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      statement. startLabel = new LabelInstruction("start try-catch-finally", statement);
      statement.   endLabel = new LabelInstruction("  end try-catch-finally", statement);
      statement.tryEndLabel = new LabelInstruction("  end try block",         statement);

      outToken  (TryToken);
      outSpace  ();
      if (statement.tryBlock.statements.isEmpty ()) {
        outToken  (BraceOpenToken);
        outToken  (BraceCloseToken);
      }
      else {
         outLine   ();
         processTryBlock (refinement, statement.tryBlock, level + 1, retValue, arg1, arg2);
      }
      outLine   ();

      for (JavaStatement js: statement.catches) {
          CatchBlock c = (CatchBlock) js;

         outSpace ();
         outToken (CatchToken);
         outSpace ();

         outToken (ParenthesisOpenToken);
         processCatchBlock (refinement, c, level + 1, retValue, arg1, arg2);
         outToken (ParenthesisCloseToken);
         outSpace ();

         outToken  (BraceOpenToken);
         indent    ();
         if (!c.statements.isEmpty ())
            outLine   ();
         processJavaStatements (refinement, c.statements, level + 1, retValue, arg1, arg2);
         unindent  ();
         outLine   ();
         outToken  (BraceCloseToken);
      }

      if (!statement.finalStatements.isEmpty ()) {
         outSpace ();
         outToken (FinallyToken);
         outSpace ();

         outToken  (BraceOpenToken);
         indent    ();
         outLine   ();
         processJavaStatements (refinement, statement.finalStatements, level + 1, retValue, arg1, arg2);
         unindent  ();
         outLine   ();
         outToken  (BraceCloseToken);
      }
   }

   protected void processTryBlock (RefinementDeclaration refinement, TryBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
       pushStatement(statement);
       processNestedStatement (refinement, statement, level, retValue, arg1, arg2);
       popStatement(statement);
   }


   protected void processCatchBlock (RefinementDeclaration refinement, CatchBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

    pushStatement(statement);
    //processLocalVariableDeclaration (refinement, statement, statement.catchVariable, level + 1, retValue, arg1, arg2);
    try {
      for (JavaStatement js: statement.tryStatement.catches) {
          CatchBlock c = (CatchBlock) js;
         if (c == statement) break;
         if (statement.catchVariable.dataType().isSubtypeOf (env, c.catchVariable.dataType())) {
            parserError (2, "Catch variable's type is subtype of earlier catch variable", 
                         statement.catchVariable.sourceStartPosition,
                         statement.catchVariable.  sourceEndPosition);
            statement.catchesAnything = true; // prevent the next message...
         }
      }
      if (!statement.catchesAnything
      &&  !statement.catchVariable.dataType().isSubtypeOf (env, env.javaLangRuntimeExceptionType)) {
          parserError (2, "Catch variable cannot possibly be thrown inside try part", 
                       statement.catchVariable.sourceStartPosition,
                       statement.catchVariable.  sourceEndPosition);
      }
    } catch (CompilerError e) {parserError (2, e.getMessage(), e.start, e.end);
    } catch (IOException   e) {parserError (2, e.getMessage(), statement.sourceStartPosition, 
                                                          statement.  sourceEndPosition);
    }

    processDataTypeDeclaration (statement.catchVariable.dataTypeDeclaration); 
    outSpace ();
    outString (statement.catchVariable.getName ());

    popStatement(statement);
   }


   /*******************************************************************/
   /**                                                               **/
   /**                         EXPRESSIONS                           **/
   /**                                                               **/
   /*******************************************************************/
 

   protected void processBinaryExpression         (FieldDeclaration fieldDeclaration, BinaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.leftExpression, level + 1, retValue, arg1, arg2);
      outSpace ();
      outToken (expression.operatorToken);
      outSpace ();
      processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processAssignmentExpression     (FieldDeclaration fieldDeclaration, AssignmentExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.leftExpression, level + 1, retValue, arg1, arg2);
      outSpace ();
      outToken (expression.operatorToken);
      outSpace ();
      processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processConditionalExpression    (FieldDeclaration fieldDeclaration, ConditionalExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.conditionExpression, level + 1, retValue, arg1, arg2);
      outSpace ();
      outToken (QuestionToken);
      outSpace ();
      processJavaExpression (fieldDeclaration, expression.trueExpression, level + 1, retValue, arg1, arg2);
      outSpace ();
      outToken (ColonToken);
      outSpace ();
      processJavaExpression (fieldDeclaration, expression.falseExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processUnaryExpression          (FieldDeclaration fieldDeclaration, UnaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken (expression.unaryToken);
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processPrimitiveCastExpression  (FieldDeclaration fieldDeclaration, PrimitiveCastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken (ParenthesisOpenToken);
      outToken (expression.castToken);
      outToken (ParenthesisCloseToken);
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processCastExpression           (FieldDeclaration fieldDeclaration, CastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (ParenthesisOpenToken);
      processDataTypeDeclaration (expression.castTypeDeclaration); 
      outToken  (ParenthesisCloseToken);
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processTypeComparisonExpression (FieldDeclaration fieldDeclaration, TypeComparisonExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.relationalExpression, level + 1, retValue, arg1, arg2);
      outSpace  ();
      outToken  (InstanceofToken);
      outSpace  ();
      processDataTypeDeclaration (expression.compareTypeDeclaration); 
   }

   protected void processPostfixExpression        (FieldDeclaration fieldDeclaration, PostfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      outToken (expression.unaryToken);
   }


   protected void outStringWithEscapes (String string) {
      int chunkStartPosition = 0, currentPosition = 0;
      char [ ] unicodeChars  = null;
      char [ ] octalChars    = null;
      int ch = 0;

      do {
         while (   currentPosition < string.length()
                && (ch = (int)string.charAt(currentPosition)) >= 32
                && ch <= 255
                && ch != (int) '\''
                && ch != (int) '\"'
                && ch != (int) '\\')
            currentPosition++;

         int chunkSize = currentPosition - chunkStartPosition;
         if (chunkSize > 0) {
            outString (string.substring (chunkStartPosition, currentPosition));
         }
         chunkStartPosition = currentPosition + 1;

         if (currentPosition >= string.length())
            break;

         if (ch > 255) {
            /* use Unicode escape */
            if (unicodeChars == null) {
               unicodeChars = new char [6];
               unicodeChars [0] = '\\';
               unicodeChars [1] = 'u';
            }
            unicodeChars [2] = Character.forDigit ((ch >> 12) & 0x000F, 16);
            unicodeChars [3] = Character.forDigit ((ch >>  8) & 0x000F, 16);
            unicodeChars [4] = Character.forDigit ((ch >>  4) & 0x000F, 16);
            unicodeChars [5] = Character.forDigit ((ch      ) & 0x000F, 16);
            outString (new String (unicodeChars));
         } else {
            /* use stock escape \b \t \n \f \r \" \' \\ or general octal */
            switch (ch) {
               case (int) '\b' : outString ("\\b"); break;
               case (int) '\t' : outString ("\\t"); break;
               case (int) '\n' : outString ("\\n"); break;
               case (int) '\f' : outString ("\\f"); break;
               case (int) '\r' : outString ("\\r"); break;
               case (int) '\"' : outString ("\\\""); break;
               case (int) '\'' : outString ("\\\'"); break;
               case (int) '\\' : outString ("\\\\"); break;
               default:
                        if (octalChars == null) {
                           octalChars = new char [4];
                           octalChars [0] = '\\';
                        }
                        octalChars [1] = Character.forDigit ((ch >> 6) & 0x0007, 8);
                        octalChars [2] = Character.forDigit ((ch >> 3) & 0x0007, 8);
                        octalChars [3] = Character.forDigit ((ch     ) & 0x0007, 8);
                        outString (new String (octalChars));
                        break;
            }
         }
         currentPosition++;
      } while (true);
   }

   protected void processLiteralExpression        (FieldDeclaration fieldDeclaration, LiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (expression.getLiteralToken() == StringLiteralToken) {
         outString ("\"");
         outStringWithEscapes (expression.getAsString());
         outString ("\"");
      } else if (expression.getLiteralToken() == CharacterLiteralToken) {
         outString ("\'");
         outStringWithEscapes (expression.getAsString());
         outString ("\'");
      } else {
         outString (expression.getAsString());
      }
   }

   protected void processNameExpression           (FieldDeclaration fieldDeclaration, NameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outString (expression.getName());
   }

   protected void processSpecialNameExpression    (FieldDeclaration fieldDeclaration, SpecialNameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken (expression.getToken());
   }

   protected void processNestedJavaExpression     (FieldDeclaration fieldDeclaration, NestedJavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (ParenthesisOpenToken);
      processJavaExpression (fieldDeclaration, expression.subExpression, level + 1, retValue, arg1, arg2);
      outToken  (ParenthesisCloseToken);
   }

   protected void processArrayInitializer         (FieldDeclaration fieldDeclaration, ArrayInitializer expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      outToken  (BraceOpenToken);
      outSpace  ();

      boolean isFirst = true;
      for (JavaExpression obj: expression.elementExpressions) {
    	 if (!isFirst)
    	 {
             outToken (CommaToken);
             outSpace ();
    	 }
    	 isFirst = false;
         processJavaExpression (fieldDeclaration, obj, level, retValue, arg1, arg2);
      }

      outToken  (BraceCloseToken);
   }

   protected void processFieldAccessExpression    (FieldDeclaration fieldDeclaration, FieldAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      outToken  (PeriodToken);
      outString (expression.getName());
   }

   protected void processArrayAccessExpression    (FieldDeclaration fieldDeclaration, ArrayAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression  (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);

      for (JavaExpression obj: expression.indexExpressions) {
         outToken (BracketOpenToken);
         processJavaExpression (fieldDeclaration, obj, level, retValue, arg1, arg2);
         outToken (BracketCloseToken);
      }
   }

   protected void processMethodCallExpression     (FieldDeclaration fieldDeclaration, MethodCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) { 
      checkMethodCallThrows (fieldDeclaration, expression);
      processJavaExpression (fieldDeclaration, expression.methodAccessExpression, level + 1, retValue, arg1, arg2);
      outSpace ();
      outToken (ParenthesisOpenToken);
      if (expression.parameterList != null)
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
      outToken (ParenthesisCloseToken);
   }

   protected void processMethodCallParameterList  (FieldDeclaration fieldDeclaration,
                                                   MethodOrConstructorCallExpression methodCall,
                                                   MethodCallParameterList parameterList,
                                                   int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	   boolean isFirst = true; 
	   for (JavaExpression obj: parameterList.parameterExpressions) {
		 if (!isFirst)
		 {
	            outToken (CommaToken);
		 }
		 isFirst = false;
         processJavaExpression (fieldDeclaration, obj, level, retValue, arg1, arg2);

      }
   }

   protected void processAllocationExpression     (FieldDeclaration fieldDeclaration, AllocationExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      if (expression.enclosingInstance != null) {
         processJavaExpression (fieldDeclaration, expression.enclosingInstance, level + 1, retValue, arg1, arg2);
         outToken (PeriodToken);
      }
      outToken (NewToken);
      outSpace ();
      outString (expression.dataTypeDeclaration.getName ());
      outSpace ();

      if (expression.sizeExpressions.size() > 0) {
         for (JavaExpression obj: expression.sizeExpressions) {
            if (obj == null) {
                continue;
            }
            outToken (BracketOpenToken);
            processJavaExpression (fieldDeclaration, obj, level, retValue, arg1, arg2);
            outToken (BracketCloseToken);
         }
         for (int i = 0; i < expression.extraBracketPairs; i++) {
            outToken (BracketOpenToken);
            outToken (BracketCloseToken);
         }
      }
      if (expression.parameterList          != null
      ||  expression.sizeExpressions.size() == 0   ) {
         outToken (ParenthesisOpenToken);

         if (expression.parameterList != null)
            processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);

         outToken (ParenthesisCloseToken);
      }
      if (expression.anonymousTypeDeclaration != null)
         processAnonymousTypeDeclaration (fieldDeclaration, expression.anonymousTypeDeclaration, level + 1, retValue, arg1, arg2);
      if (expression.arrayInitializer != null)
         processArrayInitializer (fieldDeclaration, expression.arrayInitializer, level + 1, retValue, arg1, arg2);
   }

   protected void processClassLiteralExpression     (FieldDeclaration fieldDeclaration, ClassLiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (expression.primaryExpression==null) {
        outToken (expression.primitiveTypeToken);
      }
      else {
        processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      }
      outToken (PeriodToken);
      outToken ( ClassToken);
   }
   protected void processQualifiedThisExpression (FieldDeclaration fieldDeclaration, QualifiedThisExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      outToken (PeriodToken);
      outToken ( ThisToken);
   }
   protected void processQualifiedSuperExpression (FieldDeclaration fieldDeclaration, QualifiedSuperExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      outToken (PeriodToken);
      outToken (SuperToken);
   }
}
