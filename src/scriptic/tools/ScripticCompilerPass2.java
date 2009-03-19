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
import java.util.List;

/**
 * 
Pass 1: Parsing
        build parse tree
        answer CompilationUnit
CompilerEnv: find/create ClassTypes
Pass 2: Build classes tree, for later use (nestedClassesByName)
        Lookup names:
        - lookup in localTypeDeclarationsContext
        - compare with enclosing class
        - compare with parents etc, and direct childs thereof
        CompilationUnit.addPossibleRelevantClass-------------

        Drop method bodies;
Pass 3: create field signatures
Pass 4: resolve dimensions
Pass 5: determine variable dimensions
Pass 6: resolve constants
Pass 7: resolve expressions
Pass 8: generate code
 */
public class ScripticCompilerPass2 extends ScripticParseTreeEnumerator
               implements scriptic.tokens.ScripticTokens {

   ArrayList<ClassType> localTypeDeclarationsInRefinement = new ArrayList<ClassType>();// 

   public ScripticCompilerPass2 (Scanner scanner,
                                 CompilerEnvironment env) {
      super (scanner, env);
   }

   CompilationUnit compilationUnit;

   /* Main entry point */
   public boolean resolve (CompilationUnit c) {
      compilationUnit = c;
      processCompilationUnit(c, null, null, null);
      return true;
   }

   private void endOfProcessTypeDeclaration () {
      // check for inner class cycles
      if (typeDeclaration.name != null) {
        if (classType==null)
            new Exception ("pass 2, endOfProcessTypeDeclaration: classType==null").printStackTrace();
        else
        for (ClassType parent = classType.parent(); parent!=null; parent=parent.parent()) {
          if (parent.className.equals (typeDeclaration.name)) {
              env.parserError (2, classType.nameWithDots + " attempts embedding in class with same name", typeDeclaration);
              return;
          }
        }
      }
   }
   void endOfProcessTopLevelTypeDeclaration     () {endOfProcessTypeDeclaration();}
   void endOfProcessLocalOrNestedTypeDeclaration() {
      if (classType.sourceDeclaration.languageConstructCode() == LocalTypeDeclarationCode) {
          localTypeDeclarationsInRefinement.add (classType);
      }
      classType.setNeedForParentReference(); //do it allways
      endOfProcessTypeDeclaration();
   }

   // see whether the given name is a local or nested class in the current scope
   ClassType lookupLocalOrNestedClass (String name, LanguageConstruct languageConstruct) {

      for (ClassType c: localTypeDeclarationsInRefinement){
         if (c.className.equals (name)) {
               return c;
         }
      } 
      for (ClassType c = classType; c!=null; c=c.parent()) {
            if (c.className.equals (name)) {
                  return c;
            }
            ClassType d = (ClassType) c.nestedClassesByName.get (name);
            if (d != null) {
               try {
                  if (!d.isAccessibleFor (env, classType)) {
                     parserError (2, "Class or interface not accessible",
                                     languageConstruct.sourceStartPosition,
                                     languageConstruct.sourceEndPosition);
                  }
                  return d;
               } catch (CompilerError       e) {parserError (2, e.getMessage(), e.start, e.end);
               } catch (java.io.IOException e) {parserError (2, e.getMessage(), languageConstruct.sourceStartPosition, 
                                                                                languageConstruct.sourceEndPosition);
               }
            }
      }
      return null;
   }

   // see whether the given name is a local or nested class in the current scope
   ClassType lookupLocalOrNestedClass (DataTypeDeclaration d) {
      if (d.baseTypeIsPrimitive()) return null;
      ClassType result = lookupLocalOrNestedClass (d.nameComponents.get(0), d);
      if (result != null) {
         if (d.nameComponents.size() > 1) {
             parserError (2, "Access expression in local or nested class not (yet) supported",
                              d.sourceStartPosition,
                              d.sourceEndPosition);
         }
         else {
            d.dataType = result.makeArray (d.noOfArrayDimensions);
         }
      }
      else {
         compilationUnit.addPossibleRelevantClass(d);
      }
      return result;
   }


   protected void processSuperclassDeclaration (TypeDeclaration t, SuperclassDeclaration superclass, ReturnValueHolder retValue, Object arg1, Object arg2) {
      lookupLocalOrNestedClass (superclass);
   }

   protected void processImplementsDeclaration (TypeDeclaration t, ImplementsDeclaration implement, ReturnValueHolder retValue, Object arg1, Object arg2) {
      lookupLocalOrNestedClass (implement);
   }



   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected void processMultiVariableDeclaration (MultiVariableDeclaration multiVariableDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      lookupLocalOrNestedClass (multiVariableDeclaration.dataTypeDeclaration);
      super.processMultiVariableDeclaration (multiVariableDeclaration, level, retValue, arg1, arg2);
   }
   protected void processLocalVariableDeclarationStatement (RefinementDeclaration refinement, LocalVariableDeclarationStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      lookupLocalOrNestedClass (statement.dataTypeDeclaration);
      super.processLocalVariableDeclarationStatement (refinement, statement, level, retValue, arg1, arg2);
   }

   protected void processMethodDeclaration (MethodDeclaration method, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      ArrayList<ClassType> savedLocalTypeDeclarationsInRefinement = localTypeDeclarationsInRefinement;
      localTypeDeclarationsInRefinement = new ArrayList<ClassType>();
      lookupLocalOrNestedClass (method.returnTypeDeclaration);
      super.processMethodDeclaration (method, level, retValue, arg1, arg2);
      localTypeDeclarationsInRefinement = savedLocalTypeDeclarationsInRefinement;
   }

   protected void processThrowsClause (MethodDeclaration methodDeclaration, ThrowsClause throwsClause, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      for (DataTypeDeclaration exceptionType: throwsClause.exceptionTypeDeclarations) {
         lookupLocalOrNestedClass (exceptionType);
      }
   }

   protected void processConstructorDeclaration (ConstructorDeclaration constructorDeclaration, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
	   ArrayList<ClassType> savedLocalTypeDeclarationsInRefinement = localTypeDeclarationsInRefinement;
      localTypeDeclarationsInRefinement = new ArrayList<ClassType>();
      super.processConstructorDeclaration (constructorDeclaration, level, retValue, arg1, arg2);
      localTypeDeclarationsInRefinement = savedLocalTypeDeclarationsInRefinement;
   }

   protected void processParameterDeclaration  (RefinementDeclaration declaration, MethodParameterDeclaration parameter, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      lookupLocalOrNestedClass (parameter.dataTypeDeclaration);
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

   private void setSize(List<? extends Object> list, int size)
   {
	   while (list.size()>size)
	   {
		   list.remove(list.size()-1);
	   }
   }
   protected void processStatementBlock (RefinementDeclaration refinement, StatementBlock statementBlock, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int size = localTypeDeclarationsInRefinement.size();
      processJavaStatements (refinement, statementBlock.statements, level + 1, retValue, arg1, arg2);
      setSize(localTypeDeclarationsInRefinement, size);
   }

   protected void processNestedStatement             (RefinementDeclaration refinement, NestedStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      int size = localTypeDeclarationsInRefinement.size();
      processJavaStatements (refinement, statement.statements, level + 1, retValue, arg1, arg2);
      setSize(localTypeDeclarationsInRefinement, size);
   }

   protected void processTryStatement                (RefinementDeclaration refinement, TryStatement statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      processTryBlock        (refinement, statement.tryBlock, level    , retValue, arg1, arg2);
      processJavaStatements  (refinement, statement.catches , level + 1, retValue, arg1, arg2);
      if (!statement.finalStatements.isEmpty ()) {
         int size = localTypeDeclarationsInRefinement.size();
         processJavaStatements  (refinement, statement.finalStatements, level + 1, retValue, arg1, arg2);
         setSize(localTypeDeclarationsInRefinement, size);
      }
   }

   protected void processCatchBlock                  (RefinementDeclaration refinement, CatchBlock statement, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
    lookupLocalOrNestedClass (statement.catchVariable.dataTypeDeclaration);
    int size = localTypeDeclarationsInRefinement.size();
    processJavaStatements (refinement, statement.statements, level + 1, retValue, arg1, arg2);
    setSize(localTypeDeclarationsInRefinement, size);
   }

   /* ------------------------------------------------------------------ */


   protected void processCastExpression           (FieldDeclaration fieldDeclaration, CastExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
     lookupLocalOrNestedClass (expression.castTypeDeclaration);
     processJavaExpression (fieldDeclaration, expression.unaryExpression, level + 1, retValue, arg1, arg2);
   }

   protected void processTypeComparisonExpression (FieldDeclaration fieldDeclaration, TypeComparisonExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      lookupLocalOrNestedClass (expression.compareTypeDeclaration);
      processJavaExpression (fieldDeclaration, expression.relationalExpression, level + 1, retValue, arg1, arg2);
   }


   protected void processNameExpression (FieldDeclaration fieldDeclaration, NameExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (expression.    isMethodName()) return;
      if (expression.isExpressionName()) return;
      // see whether this name is the name of the current class, or
      //  of a parent thereof etc, or of another inner class name in scope

      ClassType c = lookupLocalOrNestedClass (expression.name, expression);

      if (c != null) {
               expression.dataType = c;
               expression.setTypeName();
      }
   }

   protected void processFieldAccessExpression    (FieldDeclaration fieldDeclaration, FieldAccessExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {
      if (expression.primaryExpression.dataType==NullType.theOne) {
          parserError (2, "Invalid expression statement",
                        expression.sourceStartPosition,
                        expression.sourceEndPosition);
          return;
      }
      JavaExpression primaryExpression = expression.primaryExpression;
      primaryExpression.setAmbiguousName();
      processJavaExpression (fieldDeclaration, primaryExpression, level + 1, retValue, arg1, arg2);

      // if primaryExpression is a class name, see whether expression is also a (inner) class name
      if (primaryExpression.isTypeName()) {
         expression.dataType = (ClassType) ((ClassType) primaryExpression.dataType).nestedClassesByName.get (expression.name);
         // test accessibility ...
      }

  }

   protected void processMethodCallExpression     (FieldDeclaration fieldDeclaration, MethodCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) { 
//System.out.println("processMethodCallExpression 1: "+expression.getDescription()+"\n----------------------------------\n");

      //boolean foundError = false;
      if (expression.parameterList != null) {
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
         //foundError = expression.parameterList.hasError();
      }

      switch (expression.methodAccessExpression.languageConstructCode()) {

      case QualifiedSuperExpressionCode:
          ((JavaExpressionWithTarget) expression.methodAccessExpression).setMethodName();
             // only here and in few other places is default ExpressionName overruled 
             // $6.5.1 Syntactic Classification of a Name According to Context
          processJavaExpression (fieldDeclaration, expression.methodAccessExpression, level + 1, retValue, arg1, arg2);
          case SpecialNameExpressionCode:
          int token = ((SpecialNameExpression) expression.methodAccessExpression).getToken();
          if (token == NullToken) {
              parserError (2, "Invalid expression statement",
                            expression.sourceStartPosition,
                            expression.sourceEndPosition);
              return;
          }

          if (!fieldDeclaration.isConstructorDeclaration()) {
                  parserError (2, "Constructor invoked from non-constructor",
                                expression.sourceStartPosition,
                                expression.sourceEndPosition);
                  return;
          }
      }
      if (expression.methodAccessExpression.isJavaExpressionWithTarget()) {
          ((JavaExpressionWithTarget) expression.methodAccessExpression).setMethodName();
             // only here and in processFieldAccessExpression
             //           and in processScriptCallExpression
             // is default ExpressionName overruled 
             // $6.5.1 Syntactic Classification of a Name According to Context

          processJavaExpression (fieldDeclaration, expression.methodAccessExpression, level + 1, retValue, arg1, arg2);
//System.out.println("processMethodCallExpression 2: "+expression.getDescription()
//+" foundError: "+foundError+"\n----------------------------------\n");
      }
      else {
              parserError (3, "Probably internal compiler error, please report...Invalid expression statement",
                            expression.sourceStartPosition,
                            expression.sourceEndPosition);
      }
   }


   protected void processAllocationExpression     (FieldDeclaration fieldDeclaration, AllocationExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      lookupLocalOrNestedClass (expression.dataTypeDeclaration);

      if (expression.enclosingInstance != null) {
         processJavaExpression (fieldDeclaration, expression.enclosingInstance, level + 1, retValue, arg1, arg2);
      }
      for (JavaExpression se: expression.sizeExpressions) {
        if (se != null) {
            processJavaExpression (fieldDeclaration, se, level+1, retValue, arg1, arg2);
        }
      }

      //boolean foundError = false;
      if (expression.parameterList != null) {
         processMethodCallParameterList (fieldDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
         // foundError = expression.parameterList.hasError();
      }

      if (expression.anonymousTypeDeclaration != null) {
         processAnonymousTypeDeclaration (fieldDeclaration, expression.anonymousTypeDeclaration, level + 1, retValue, arg1, arg2);
      }
      if (expression.arrayInitializer != null) {
         processArrayInitializer (fieldDeclaration, expression.arrayInitializer, level + 1, retValue, arg1, arg2);
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

        boolean foundStatic = false;
        for (ClassType c = classType; c.parent()!=null; c=c.parent()) {
            if (c.modifiers.isStatic()) {foundStatic = true;}
            if (c.parent().className.equals (primaryExpression.name)) {
                  //((LocalOrNestedClassType) classType).setNeedForParentReference ();
                  if (foundStatic) {
                     parserError (2, "Cannot refer to enclosing instance from static context",
                                  primaryExpression.sourceStartPosition, 
                                  primaryExpression.sourceEndPosition);
                  }
                  break searchAncestor;
            }
        }
        parserError (2, "Should be name of enclosing type",
                      primaryExpression.sourceStartPosition, 
                      primaryExpression.sourceEndPosition);
      }
  }

  protected void processScriptCallExpression (BasicScriptDeclaration scriptDeclaration, ScriptCallExpression expression, int level, ReturnValueHolder retValue, Object arg1, Object arg2) {

      //boolean foundError = false;

      if (expression.parameterList != null) {
         processScriptCallParameterList (scriptDeclaration, expression, expression.parameterList, level + 1, retValue, arg1, arg2);
         // foundError |= expression.parameterList.hasError();
      }

      ((JavaExpressionWithTarget) expression.scriptAccessExpression).setMethodName();
             // only here and in processFieldAccessExpression 
             //           and in processMethodCallExpression
             //  is default ExpressionName overruled
             // $6.5.1 Syntactic Classification of a Name According to Context
      processJavaExpression (scriptDeclaration, expression.scriptAccessExpression, level + 1, retValue, arg1, arg2);

//if (foundError)
//throw new RuntimeException ("processScriptCallExpression, foundError: "
//+expression.name);

   }
}
