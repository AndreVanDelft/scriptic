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


public class JavaParseTreeEnumerator extends Parser
         implements JavaParseTreeCodes {

   /* This class "extends Parser" in order to inherit the parserError()
      set of methods. If a subclass calls any of them, the "scanner"
      and "errorHandler" fields must be set!
      Subclasses that use parserError() should, therefore, ONLY define
      constructors that set the scanner and errorHandler; they should
      NOT define a constructor without arguments and NOT rely on the
      default constructor. */

   /* -------------------------- Constructors ---------------------------*/

   CompilerEnvironment env;
   TypeDeclaration    typeDeclaration; // set in processTopLevelTypeDeclaration
   ClassType          classType;       // set in processTopLevelTypeDeclaration

   public JavaParseTreeEnumerator (Scanner scanner) {
      super (scanner);
   }
   
   public JavaParseTreeEnumerator (Scanner scanner, ParserErrorHandler errorHandler) {
      super (scanner, errorHandler);
   }

   public JavaParseTreeEnumerator (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
      this.env = env;
   }

   // not nice to put this here, but otherwise a new class would be required:
   Dimension findTargetDimension (DimensionReference dr) throws CompilerError, IOException {

      String dimensionName = dr.nameComponents.get (dr.nameComponents.size()-1);
      ClassType ct = classType;
      if (dr.nameComponents.size() > 1) {
         String className = dr.nameComponents.get (dr.nameComponents.size()-2);
         if (dr.nameComponents.size()==2) {
            ct = env.resolveClassNameWithoutSlashes (typeDeclaration.compilationUnit, className, true /*loadAsWell*/);
         }
         else {
            String pakkage = dr.nameComponents.get (0);
            for (int i=1; i<dr.nameComponents.size()-2; i++) {
               pakkage += '.'+ dr.nameComponents.get (i);
            }
            ct = env.resolveClass (pakkage, className, true /*loadAsWell*/);
         }
      }
      return ct.resolveDimension (env, dimensionName);
   }


   /* Constructor without arguments; can be used when parserError()
      isn't called */
   public JavaParseTreeEnumerator () {
      super (null, null);
   }

   /* --------------------- Default main entry points ------------------ */

   public Object processCompilationUnit (CompilationUnit compilationUnit) {
      ReturnValueHolder retValue = new ReturnValueHolder ();
      processCompilationUnit (compilationUnit, retValue, null, null);
      return retValue.value;
   }

   public Object processFieldDeclaration (FieldDeclaration fieldDeclaration) {
      ReturnValueHolder retValue = new ReturnValueHolder ();
      processFieldDeclaration (fieldDeclaration, 0, retValue, null, null);
      return retValue.value;
   }

   /* ------------------------------------------------------------------ */

   public Object processStatementBlock (StatementBlock statementBlock) {
      return processStatementBlock (null, statementBlock);
   }

   public Object processStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock) {
      ReturnValueHolder retValue = new ReturnValueHolder ();
      processStatementBlock (refinement, statementBlock, 0, retValue, null, null);
      return retValue.value;
   }

   /* ------------------------------------------------------------------ */

   public Object processJavaExpression (JavaExpression expression) {
      return processJavaExpression (null, expression);
   }

   public Object processJavaExpression (FieldDeclaration fieldDeclaration, JavaExpression expression) {
      ReturnValueHolder retValue = new ReturnValueHolder ();
      processJavaExpression (fieldDeclaration, expression, 0, retValue, null, null);
      return retValue.value;
   }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                          LanguageConstruct                        */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processLanguageConstruct (LanguageConstruct languageConstruct, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                           CompilationUnit                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processCompilationUnit (CompilationUnit compilationUnit, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (compilationUnit, 0, retValue, arg1, arg2);

      if (compilationUnit.packageStatement != null)
         processPackageStatement (compilationUnit.packageStatement, retValue, arg1, arg2);
      processImportStatements    (compilationUnit.importStatements, retValue, arg1, arg2);
      processTypeDeclarationsAndComments(compilationUnit.typeDeclarationsAndComments, retValue, arg1, arg2);
   }

   protected void processPackageStatement (PackageStatement packageStatement, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (packageStatement, 1, retValue, arg1, arg2);
   }

   protected void processImportStatements (ArrayList<ImportStatement> importStatements, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (ImportStatement is: importStatements) {
         processImportStatement (is, retValue, arg1, arg2);
      }
   }

   protected void processImportStatement (ImportStatement importStatement, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (importStatement, 1, retValue, arg1, arg2);
   }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Type (= class or interface) Declaration            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processTypeDeclarationsAndComments (ArrayList<TypeDeclarationOrComment> typeDeclarationsAndComments, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (TypeDeclarationOrComment t: typeDeclarationsAndComments) {
         processTypeDeclarationOrComment (t, retValue, arg1, arg2);
      }
   }

   protected void processTypeDeclarationOrComment      (TypeDeclarationOrComment typeDeclarationOrComment, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (typeDeclarationOrComment.languageConstructCode ()) {
         case TopLevelTypeDeclarationCode :
                  processTopLevelTypeDeclaration     ((TopLevelTypeDeclaration)typeDeclarationOrComment, retValue, arg1, arg2);
                  break;
         case CommentTypeDeclarationCode :
                  processCommentTypeDeclaration ((CommentTypeDeclaration)typeDeclarationOrComment, retValue, arg1, arg2);
                  break;
         default:
              throw new RuntimeException ("Unknown kind of TypeDeclaration");
      }
   }

   protected void processTopLevelTypeDeclaration (TopLevelTypeDeclaration t, ReturnValueHolder retValue, Object arg1, Object arg2) {
      typeDeclaration = t;
      classType       = t.target;
      processLanguageConstruct      (t, 1, retValue, arg1, arg2);

      if (t.superclass != null)
      processSuperclassDeclaration  (t, t.superclass, retValue, arg1, arg2);
      processImplementsDeclarations (t, t.interfaces, retValue, arg1, arg2);
      processFieldDeclarations      (t.fieldDeclarations, 2, retValue, arg1, arg2);
      endOfProcessTopLevelTypeDeclaration();
      typeDeclaration = null;
      classType       = null;;
   }

   protected void processLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      TypeDeclaration savedTypeDeclaration = typeDeclaration;
      typeDeclaration = t;
      classType       = t.target;
      processLanguageConstruct      (t, level, retValue, arg1, arg2);
      if (!(t instanceof AnonymousTypeDeclaration)) {
        if (t.superclass != null)
        processSuperclassDeclaration  (t, t.superclass, retValue, arg1, arg2);
        processImplementsDeclarations (t, t.interfaces, retValue, arg1, arg2);
      }
      processFieldDeclarations      (t.fieldDeclarations, level+1, retValue, arg1, arg2);
      endOfProcessLocalOrNestedTypeDeclaration();
      typeDeclaration = savedTypeDeclaration;
      if (typeDeclaration!=null) { // could be null, in case of pass 7 calling pass 3....
         classType       = typeDeclaration.target;
      }
   }
   void endOfProcessTopLevelTypeDeclaration     () {}
   void endOfProcessLocalOrNestedTypeDeclaration() {}

   protected void processNestedTypeDeclaration (NestedTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
   }

   protected void processLocalTypeDeclaration (RefinementDeclaration refinement, LocalTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
   }

   protected void processAnonymousTypeDeclaration (FieldDeclaration field, AnonymousTypeDeclaration t, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLocalOrNestedTypeDeclaration (t, level, retValue, arg1, arg2);
   }

   protected void processCommentTypeDeclaration (CommentTypeDeclaration commentTypeDeclaration, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /* Ignore the comment, unless this method is specifically overridden */
   }

   protected void processSuperclassDeclaration (TypeDeclaration t, SuperclassDeclaration superclass, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (superclass, 2, retValue, arg1, arg2);
   }

   protected void processImplementsDeclarations (TypeDeclaration t, ArrayList<ImplementsDeclaration> interfaceDeclarations, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (ImplementsDeclaration id: interfaceDeclarations) {
         processImplementsDeclaration (t, id, retValue, arg1, arg2);
      }
   }

   protected void processImplementsDeclaration (TypeDeclaration t, ImplementsDeclaration interfaceDeclaration, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (interfaceDeclaration, 2, retValue, arg1, arg2);
   }



   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        Field Declaration                        */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected void processFieldDeclarations (ArrayList<FieldDeclaration> fieldDeclarations, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (FieldDeclaration f: fieldDeclarations) {
         processFieldDeclaration (f, level, retValue, arg1, arg2);
      }
   }

   protected void processFieldDeclaration (FieldDeclaration fieldDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (fieldDeclaration.languageConstructCode ()) {
         case NestedTypeDeclarationCode :
                  processNestedTypeDeclaration    ((NestedTypeDeclaration) fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case MultiVariableDeclarationCode :
                  processMultiVariableDeclaration ((MultiVariableDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case MethodDeclarationCode :
                  processMethodDeclaration        ((MethodDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case ConstructorDeclarationCode :
                  processConstructorDeclaration   ((ConstructorDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case InitializerBlockCode :
                  processInitializerBlock        ((InitializerBlock)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case CommentFieldDeclarationCode :
                  processCommentFieldDeclaration  ((CommentFieldDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case BaseDimensionDeclarationCode :
                  processBaseDimensionDeclaration  ((BaseDimensionDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         case CompoundDimensionDeclarationCode :
                  processCompoundDimensionDeclaration  ((CompoundDimensionDeclaration)fieldDeclaration, level, retValue, arg1, arg2);
                  break;
         default:
                  throw new RuntimeException ("Unknown kind of FieldDeclaration");
      }
   }

   protected void processMultiVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct    (multiVariableDeclaration, level, retValue, arg1, arg2);

      for (MemberOrLocalVariableDeclaration mvd: multiVariableDeclaration.variables) {
    	 MemberVariableDeclaration obj = (MemberVariableDeclaration) mvd;
         processVariableDeclaration (multiVariableDeclaration, obj, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processMethodDeclaration     (MethodDeclaration methodDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (methodDeclaration, level, retValue, arg1, arg2);

      if (methodDeclaration.parameterList != null)
         processParameterList  ((RefinementDeclaration)methodDeclaration, methodDeclaration.parameterList, level + 1, retValue, arg1, arg2);
      if (methodDeclaration.throwsClause != null)
         processThrowsClause   (methodDeclaration, methodDeclaration.throwsClause, level + 1, retValue, arg1, arg2);
      if (methodDeclaration.statements != null)
         processStatementBlock (methodDeclaration, methodDeclaration.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processConstructorDeclaration     (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (constructorDeclaration, level, retValue, arg1, arg2);

      if (constructorDeclaration.parameterList != null)
         processParameterList  ((RefinementDeclaration)constructorDeclaration, constructorDeclaration.parameterList, level + 1, retValue, arg1, arg2);
      if (constructorDeclaration.throwsClause != null)
         processThrowsClause   (constructorDeclaration, constructorDeclaration.throwsClause, level + 1, retValue, arg1, arg2);
      if (constructorDeclaration.otherConstructorInvocation != null)
         processMethodCallExpression (constructorDeclaration,
                                      constructorDeclaration.otherConstructorInvocation, level + 1, retValue, arg1, arg2);
      if (constructorDeclaration.statements != null)
         processStatementBlock (constructorDeclaration, constructorDeclaration.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processInitializerBlock     (InitializerBlock initializerBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (initializerBlock, level, retValue, arg1, arg2);
      if (initializerBlock.statements != null)
         processStatementBlock (initializerBlock, initializerBlock.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processVariableDeclaration  (MultiVariableDeclaration multiVariableDeclaration, MemberVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (variableDeclaration, level, retValue, arg1, arg2);
      if (variableDeclaration.initializer != null)
         processJavaExpression (multiVariableDeclaration, variableDeclaration.initializer, level + 1, retValue, arg1, arg2);
   }

   protected void processParameterList  (RefinementDeclaration declaration, ParameterList parameterList, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (parameterList.parameters.isEmpty()) return;
      processLanguageConstruct     (parameterList, level, retValue, arg1, arg2);

      for (MethodParameterDeclaration mpd: parameterList.parameters) {
         processParameterDeclaration (declaration, mpd, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processParameterDeclaration  (RefinementDeclaration declaration, MethodParameterDeclaration parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (parameter, level, retValue, arg1, arg2);
   }

   protected void processThrowsClause (MethodDeclaration methodDeclaration, ThrowsClause throwsClause, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (throwsClause, level, retValue, arg1, arg2);
   }

   protected void processCommentFieldDeclaration (CommentFieldDeclaration commentFieldDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /* Ignore the comment, unless this method is specifically overridden */
   }

   protected void processBaseDimensionDeclaration (BaseDimensionDeclaration baseDimensionDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   }
   protected void processCompoundDimensionDeclaration (CompoundDimensionDeclaration compoundDimensionDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
   }




   /*******************************************************************/
   /**                                                               **/
   /**                          STATEMENTS                           **/
   /**                                                               **/
   /*******************************************************************/

   protected void processStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statementBlock, level, retValue, arg1, arg2);
      processJavaStatements (refinement, statementBlock.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processJavaStatements (RefinementDeclaration refinement, ArrayList<JavaStatement> statements, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (JavaStatement js: statements) {
         processJavaStatement (refinement, js, level, retValue, arg1, arg2);
      }
   }

   protected void processJavaStatement  (RefinementDeclaration refinement, JavaStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (statement.languageConstructCode ()) {
         case LocalTypeDeclarationStatementCode :
                  processLocalTypeDeclarationStatement(refinement, (LocalTypeDeclarationStatement)statement, level, retValue, arg1, arg2);
                  break;
         case EmptyStatementCode :
                  processEmptyStatement              (refinement, (EmptyStatement)statement, level, retValue, arg1, arg2);
                  break;
         case CommentStatementCode :
                  processCommentStatement            (refinement, (CommentStatement)statement, level, retValue, arg1, arg2);
                  break;
         case NestedStatementCode :
                  processNestedStatement             (refinement, (NestedStatement)statement, level, retValue, arg1, arg2);
                  break;
         case LocalVariableDeclarationStatementCode :
                  processLocalVariableDeclarationStatement (refinement, (LocalVariableDeclarationStatement)statement, level, retValue, arg1, arg2);
                  break;
         case LabeledStatementCode :
                  processLabeledStatement            (refinement, (LabeledStatement)statement, level, retValue, arg1, arg2);
                  break;
         case CaseTagCode :
                  processCaseTag                     (refinement, (CaseTag)statement, level, retValue, arg1, arg2);
                  break;
         case DefaultCaseTagCode :
                  processDefaultCaseTag              (refinement, (DefaultCaseTag)statement, level, retValue, arg1, arg2);
                  break;
         case ExpressionStatementCode :
                  processExpressionStatement         (refinement, (ExpressionStatement)statement, level, retValue, arg1, arg2);
                  break;
         case IfStatementCode :
                  processIfStatement                 (refinement, (IfStatement)statement, level, retValue, arg1, arg2);
                  break;
         case SwitchStatementCode :
                  processSwitchStatement             (refinement, (SwitchStatement)statement, level, retValue, arg1, arg2);
                  break;
         case WhileStatementCode :
                  processWhileStatement              (refinement, (WhileStatement)statement, level, retValue, arg1, arg2);
                  break;
         case DoStatementCode :
                  processDoStatement                 (refinement, (DoStatement)statement, level, retValue, arg1, arg2);
                  break;
         case ForStatementCode :
                  processForStatement                (refinement, (ForStatement)statement, level, retValue, arg1, arg2);
                  break;
         case BreakStatementCode :
                  processBreakStatement              (refinement, (BreakStatement)statement, level, retValue, arg1, arg2);
                  break;
         case ContinueStatementCode :
                  processContinueStatement           (refinement, (ContinueStatement)statement, level, retValue, arg1, arg2);
                  break;
         case ReturnStatementCode :
                  processReturnStatement             (refinement, (ReturnStatement)statement, level, retValue, arg1, arg2);
                  break;
         case ThrowStatementCode :
                  processThrowStatement              (refinement, (ThrowStatement)statement, level, retValue, arg1, arg2);
                  break;
         case SynchronizedStatementCode :
                  processSynchronizedStatement       (refinement, (SynchronizedStatement)statement, level, retValue, arg1, arg2);
                  break;
         case TryStatementCode :
                  processTryStatement                (refinement, (TryStatement)statement, level, retValue, arg1, arg2);
                  break;
         case TryBlockCode :
                  processTryBlock                    (refinement, (TryBlock)statement, level, retValue, arg1, arg2);
                  break;
         case CatchBlockCode :
                  processCatchBlock                  (refinement, (CatchBlock)statement, level, retValue, arg1, arg2);
                  break;
         default:
                  throw new RuntimeException ("Unknown kind of JavaStatement");
      }
   }

   protected void processLocalTypeDeclarationStatement (RefinementDeclaration refinement, LocalTypeDeclarationStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processLocalTypeDeclaration (refinement, statement.localTypeDeclaration, level, retValue, arg1, arg2);
   }

   protected void processEmptyStatement              (RefinementDeclaration refinement, EmptyStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
   }

   protected void processCommentStatement            (RefinementDeclaration refinement, CommentStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /* Default is to completely skip/ignore the CommentStatement.
         To do anything with it, this method must be explicitly overridden. */
      processJavaStatements (refinement, statement.affectedStatements, level, retValue, arg1, arg2);
   }

   protected void processNestedStatement             (RefinementDeclaration refinement, NestedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaStatements (refinement, statement.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processLocalVariableDeclarationStatement (RefinementDeclaration refinement, LocalVariableDeclarationStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);

      for (LocalVariableDeclaration lvd: statement.variables) {
         processLocalVariableDeclaration (refinement, statement, lvd, level + 1, retValue, arg1, arg2);
      }
   }

   protected void processLocalVariableDeclaration  (RefinementDeclaration refinement, JavaStatement statement, LocalVariableDeclaration variableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (variableDeclaration, level, retValue, arg1, arg2);
      if (variableDeclaration.initializer != null)
         processJavaExpression (refinement, variableDeclaration.initializer, level + 1, retValue, arg1, arg2);
   }

   protected void processLabeledStatement          (RefinementDeclaration refinement, LabeledStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaStatement (refinement, statement.statement, level + 1, retValue, arg1, arg2);
   }

   protected void processCaseTag                     (RefinementDeclaration refinement, CaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.tagExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processDefaultCaseTag              (RefinementDeclaration refinement, DefaultCaseTag statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
   }

   protected void processExpressionStatement         (RefinementDeclaration refinement, ExpressionStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.expression, level + 1, retValue, arg1, arg2);
   }

   protected void processIfStatement                 (RefinementDeclaration refinement, IfStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);

      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      processJavaStatement  (refinement, statement.trueStatement, level + 1, retValue, arg1, arg2);
      if (statement.falseStatement != null)
         processJavaStatement  (refinement, statement.falseStatement, level + 1, retValue, arg1, arg2);
   }

   protected void processSwitchStatement             (RefinementDeclaration refinement, SwitchStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaExpression  (refinement, statement.switchExpression, level + 1, retValue, arg1, arg2);
      processJavaStatements  (refinement, statement.statements, level + 1, retValue, arg1, arg2);
   }

   protected void processWhileStatement              (RefinementDeclaration refinement, WhileStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
   }

   protected void processDoStatement                 (RefinementDeclaration refinement, DoStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processForStatement                (RefinementDeclaration refinement, ForStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);

      processJavaStatements (refinement, statement.initStatements, level + 1, retValue, arg1, arg2);
      if (statement.conditionExpression != null)
         processJavaExpression (refinement, statement.conditionExpression, level + 1, retValue, arg1, arg2);
      processJavaStatements (refinement, statement.loopStatements, level + 1, retValue, arg1, arg2);
      processJavaStatement  (refinement, statement.statement, level + 1, retValue, arg1, arg2);
   }

   protected void processBreakStatement              (RefinementDeclaration refinement, BreakStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
   }

   protected void processContinueStatement           (RefinementDeclaration refinement, ContinueStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
   }

   protected void processReturnStatement             (RefinementDeclaration refinement, ReturnStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      if (statement.returnExpression !=  null)
         processJavaExpression (refinement, statement.returnExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processThrowStatement              (RefinementDeclaration refinement, ThrowStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.throwExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processSynchronizedStatement       (RefinementDeclaration refinement, SynchronizedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);
      processJavaExpression (refinement, statement.synchronizedExpression, level + 1, retValue, arg1, arg2);
      processNestedStatement(refinement, statement, level, retValue, arg1, arg2);
   }

   protected void processTryStatement                (RefinementDeclaration refinement, TryStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);

      processTryBlock        (refinement, statement.tryBlock       , level + 1, retValue, arg1, arg2);
      processJavaStatements  (refinement, statement.catches        , level + 1, retValue, arg1, arg2);
      processJavaStatements  (refinement, statement.finalStatements, level + 1, retValue, arg1, arg2);
   }

   protected void processTryBlock                    (RefinementDeclaration refinement, TryBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
       processNestedStatement (refinement, statement, level, retValue, arg1, arg2);
   }

   protected void processCatchBlock                  (RefinementDeclaration refinement, CatchBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (statement, level, retValue, arg1, arg2);

      processLocalVariableDeclaration (refinement, statement, statement.catchVariable, level + 1, retValue, arg1, arg2);
      processJavaStatements           (refinement, statement.statements, level + 1, retValue, arg1, arg2);
   }


   /*******************************************************************/
   /**                                                               **/
   /**                         EXPRESSIONS                           **/
   /**                                                               **/
   /*******************************************************************/

   protected void processJavaExpression (FieldDeclaration fieldDeclaration, JavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      switch (expression.languageConstructCode ()) {
         case LayoutExpressionCode :
                  processLayoutExpression         (fieldDeclaration, (LayoutExpression)expression, level, retValue, arg1, arg2);
                  break;
         case BinaryExpressionCode :
                  processBinaryExpression         (fieldDeclaration, (BinaryExpression)expression, level, retValue, arg1, arg2);
                  break;
         case AssignmentExpressionCode :
                  processAssignmentExpression     (fieldDeclaration, (AssignmentExpression)expression, level, retValue, arg1, arg2);
                  break;
         case ConditionalExpressionCode :
                  processConditionalExpression    (fieldDeclaration, (ConditionalExpression)expression, level, retValue, arg1, arg2);
                  break;
         case UnaryExpressionCode :
                  processUnaryExpression          (fieldDeclaration, (UnaryExpression)expression, level, retValue, arg1, arg2);
                  break;
         case PrimitiveCastExpressionCode :
                  processPrimitiveCastExpression  (fieldDeclaration, (PrimitiveCastExpression)expression, level, retValue, arg1, arg2);
                  break;
         case CastExpressionCode :
                  processCastExpression           (fieldDeclaration, (CastExpression)expression, level, retValue, arg1, arg2);
                  break;
         case TypeComparisonExpressionCode :
                  processTypeComparisonExpression (fieldDeclaration, (TypeComparisonExpression)expression, level, retValue, arg1, arg2);
                  break;
         case PostfixExpressionCode :
                  processPostfixExpression        (fieldDeclaration, (PostfixExpression)expression, level, retValue, arg1, arg2);
                  break;
         case LiteralExpressionCode :
                  processLiteralExpression        (fieldDeclaration, (LiteralExpression)expression, level, retValue, arg1, arg2);
                  break;
         case NameExpressionCode :
                  processNameExpression           (fieldDeclaration, (NameExpression)expression, level, retValue, arg1, arg2);
                  break;
         case SpecialNameExpressionCode :
                  processSpecialNameExpression    (fieldDeclaration, (SpecialNameExpression)expression, level, retValue, arg1, arg2);
                  break;
         case NestedJavaExpressionCode :
                  processNestedJavaExpression     (fieldDeclaration, (NestedJavaExpression)expression, level, retValue, arg1, arg2);
                  break;
         case ArrayInitializerCode :
                  processArrayInitializer         (fieldDeclaration, (ArrayInitializer)expression, level, retValue, arg1, arg2);
                  break;
         case FieldAccessExpressionCode :
                  processFieldAccessExpression    (fieldDeclaration, (FieldAccessExpression)expression, level, retValue, arg1, arg2);
                  break;
         case ArrayAccessExpressionCode :
                  processArrayAccessExpression    (fieldDeclaration, (ArrayAccessExpression)expression, level, retValue, arg1, arg2);
                  break;
         case MethodCallExpressionCode :
                  processMethodCallExpression     (fieldDeclaration, (MethodCallExpression)expression, level, retValue, arg1, arg2);
                  break;
         case AllocationExpressionCode :
                  processAllocationExpression     (fieldDeclaration, (AllocationExpression)expression, level, retValue, arg1, arg2);
                  break;
         case ClassLiteralExpressionCode :
                  processClassLiteralExpression   (fieldDeclaration, (ClassLiteralExpression)expression, level, retValue, arg1, arg2);
                  break;

         case QualifiedThisExpressionCode :
                  processQualifiedThisExpression   (fieldDeclaration, (QualifiedThisExpression)expression, level, retValue, arg1, arg2);
                  break;

         case QualifiedSuperExpressionCode :
                  processQualifiedSuperExpression   (fieldDeclaration, (QualifiedSuperExpression)expression, level, retValue, arg1, arg2);
                  break;

         case DimensionCastExpressionCode :
                  processDimensionCastExpression   (fieldDeclaration, (DimensionCastExpression)expression, level, retValue, arg1, arg2);
                  break;

         default:
                  throw new RuntimeException ("Unknown kind of JavaExpression: "+expression.languageConstructCode ());
      }
   }

   protected void processJavaExpressions (FieldDeclaration fieldDeclaration, ArrayList<JavaExpression> expressions, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (JavaExpression obj: expressions) {
         processJavaExpression (fieldDeclaration, obj, level, retValue, arg1, arg2);
      }
   }

   protected void processLayoutExpression          (FieldDeclaration fieldDeclaration, LayoutExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      /* Default is to completely skip/ignore the LayoutExpression.
         To do anything with it, this method must be explicitly overridden. */
      processJavaExpression (fieldDeclaration, expression.realExpression, level, retValue, arg1, arg2);
   }

   protected void processBinaryExpression         (FieldDeclaration fieldDeclaration, BinaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.leftExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processAssignmentExpression     (FieldDeclaration fieldDeclaration, AssignmentExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.leftExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.rightExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processConditionalExpression    (FieldDeclaration fieldDeclaration, ConditionalExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.conditionExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.trueExpression, level + 1, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.falseExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processUnaryExpression          (FieldDeclaration fieldDeclaration, UnaryExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processPrimitiveCastExpression  (FieldDeclaration fieldDeclaration, PrimitiveCastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processCastExpression           (FieldDeclaration fieldDeclaration, CastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processDimensionCastExpression  (FieldDeclaration fieldDeclaration, DimensionCastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression   (fieldDeclaration, expression.   unaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processTypeComparisonExpression (FieldDeclaration fieldDeclaration, TypeComparisonExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.relationalExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processPostfixExpression        (FieldDeclaration fieldDeclaration, PostfixExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processLiteralExpression        (FieldDeclaration fieldDeclaration, LiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
   }

   protected void processNameExpression           (FieldDeclaration fieldDeclaration, NameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
   }

   protected void processSpecialNameExpression    (FieldDeclaration fieldDeclaration, SpecialNameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
   }

   protected void processNestedJavaExpression     (FieldDeclaration fieldDeclaration, NestedJavaExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.subExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processArrayInitializer         (FieldDeclaration fieldDeclaration, ArrayInitializer expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpressions (fieldDeclaration, expression.elementExpressions, level + 1, retValue, arg1, arg2);
   }

   protected void processFieldAccessExpression    (FieldDeclaration fieldDeclaration, FieldAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processArrayAccessExpression    (FieldDeclaration fieldDeclaration, ArrayAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression  (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
      processJavaExpressions (fieldDeclaration, expression.indexExpressions, level + 1, retValue, arg1, arg2);
   }

   protected void processMethodCallExpression     (FieldDeclaration fieldDeclaration, MethodCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) { 
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.methodAccessExpression, level + 1, retValue, arg1, arg2);
      if (expression.parameterList != null)
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
   }

   protected void processMethodCallParameterList  (FieldDeclaration fieldDeclaration,
                                                   MethodOrConstructorCallExpression methodCall,
                                                   MethodCallParameterList parameterList,
                                                   int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (parameterList.parameterExpressions.isEmpty()) return;
      processLanguageConstruct     (parameterList, level, retValue, arg1, arg2);
      processJavaExpressions       (fieldDeclaration, parameterList.parameterExpressions, level + 1, retValue, arg1, arg2);
   }

   protected void processAllocationExpression     (FieldDeclaration fieldDeclaration, AllocationExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      if (expression.enclosingInstance != null)
         processJavaExpression (fieldDeclaration, expression.enclosingInstance, level + 1, retValue, arg1, arg2);
      for (JavaExpression se: expression.sizeExpressions) {
        if (se != null) {
            processJavaExpression (fieldDeclaration, se, level+1, retValue, arg1, arg2);
        }
      }
      if (expression.parameterList != null)
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
      if (expression.anonymousTypeDeclaration != null)
         processAnonymousTypeDeclaration (fieldDeclaration, expression.anonymousTypeDeclaration, level + 1, retValue, arg1, arg2);
      if (expression.arrayInitializer != null)
         processArrayInitializer (fieldDeclaration, expression.arrayInitializer, level + 1, retValue, arg1, arg2);
   }

   protected void processClassLiteralExpression     (FieldDeclaration fieldDeclaration, ClassLiteralExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      if (expression.primaryExpression != null)
        processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }
   protected void processQualifiedThisExpression (FieldDeclaration fieldDeclaration, QualifiedThisExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }
   protected void processQualifiedSuperExpression (FieldDeclaration fieldDeclaration, QualifiedSuperExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processLanguageConstruct (expression, level, retValue, arg1, arg2);
      processJavaExpression (fieldDeclaration, expression.primaryExpression, level + 1, retValue, arg1, arg2);
   }
}



