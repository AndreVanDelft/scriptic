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

 /* ***************************************

JavaParser behaves a bit like RefinementContext, with respect to the
context storage of classes that are local to blocks;
this seems simpler than introducing a special kind of context.
Each block local class gets a copy of this class context.
A nested class does not need such a context, for it
can access its parent.
Expressions etc in ScripticCompilerPass7 need such a context as well;
the context provisions are small and therefore copied.

Example:

  class C {
    void v() {
      class CV0{}
      class CV1{ void v1() {class CV11{} statement; } }
      class CV2{}
    }
  }

  CV1      .localTypesContext = CV0
  CV11     .localTypesContext = -
  statement.localTypesContext = CV11
  statement.context           = C; v; CV1; v1

  resolve a type in a statement =
    resolve in localTypesContext; if (result!=null) return result;
    resolve in           context; if (result!=null) return result;
    resolve in environment

  resolve a type in a context =
    if this is a classContext:
       if (it is the owner) return result;
       resolve in owner.nestedClasses    ; if (result!=null) return result;
       resolve in owner.localTypesContext; if (result!=null) return result;
    resolve in parent;
    return result;

Note: outer nested classes are also NOT known to the ClassesEnvironment
They should be looked up in encompassing classes.

Refinements with local types need carefully be reparsed:
  reparsing should not reparse the local types,
  but pick these up instead, from the sampled Vector method.localTypeDeclarations.
  The bodies of methods inside local types are not thrown away
Therefore each refinement has a Vector localTypes;
  these are filled during the initial parse
  and used during subsequent parses.

*******************************************/

package scriptic.tools;


import java.io.File;
import java.util.ArrayList;

public class JavaParser extends Parser 
   implements scriptic.tokens.JavaTokens, ModifierFlags, JavaParseTreeCodes {

   // do or don't keep parsed bodies of methods (and scripts)
   // after the initial parsing
   // if removed, the body may be recaptured using
   //
   // reparseMethodBody (MethodDeclaration method) (likewise for scripts)
   boolean keepFieldBodies = true;
   boolean seenDeprecated;
   JavaExpression seenRelativeDimension;

           TypeDeclaration  currentTypeDeclaration; // only for nested & local types
   TopLevelTypeDeclaration topLevelTypeDeclaration; // will collect nested & local types

   ArrayList<LocalTypeDeclaration> localTypeDeclarationsContext = new ArrayList<LocalTypeDeclaration>();

   ArrayList<LocalTypeDeclaration> localTypeDeclarationsInRefinement = new ArrayList<LocalTypeDeclaration>();// 
          // to be parsed only once; not to be reparsed...see...

   File sourceFile;
   CompilerEnvironment env;

   /* Constructors */
   // public JavaParser (Scanner scanner, File sourceFile) {
   //    super (scanner);
   //    this.sourceFile = sourceFile;
   // }
   
   public JavaParser (Scanner scanner, File sourceFile, CompilerEnvironment env) {
      super (scanner, env);
      this.sourceFile = sourceFile;
      this.env        = env;
   }
   public JavaParser (Scanner scanner, CompilerEnvironment env) {
      super (scanner, env);
      this.env          = env;
   }

   /* Main entry point */
   public Object parse (String compilationUnitName) {
      scanner.reset ();
      scanner.next  ();
      return parseCompilationUnit (compilationUnitName);
   }


   /*******************************************************************/
   /**                                                               **/
   /**                       COMPILATION UNIT                        **/
   /**                                                               **/
   /*******************************************************************/

   protected CompilationUnit parseCompilationUnit (String compilationUnitName) {

      CompilationUnit compilationUnit        = new CompilationUnit ();
      compilationUnit.scanner                = scanner;
      compilationUnit.sourceStartPosition    = 0;
      compilationUnit.name                   = compilationUnitName;
      compilationUnit.sourceFile             = sourceFile;

      if (!parsePackageStatement (compilationUnit)) return null;
      if (!parseImportStatements (compilationUnit)) return null;
      if (!parseTypeDeclarations (compilationUnit)) return null;

      compilationUnit.sourceEndPosition   = scanner.tokenEndPosition;
      return compilationUnit;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parsePackageStatement (CompilationUnit compilationUnit) {
      if (scanner.token != PackageToken) return true;
      PackageStatement packageStatement    = new PackageStatement ();
      packageStatement.sourceStartPosition = scanner.tokenStartPosition;

      scanner.next ();
      if (!parseCompoundName (packageStatement)) return false;
      packageStatement.nameStartPosition = packageStatement.sourceStartPosition;
      packageStatement.sourceEndPosition = packageStatement.nameEndPosition;
      compilationUnit.packageStatement   = packageStatement;

      int t[] = {PeriodToken, SemicolonToken};
      if (!expectTokens (t)) return false;  /* for proper error message */
      scanner.next ();
      return true;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseImportStatements (CompilationUnit compilationUnit) {
      while (scanner.token == ImportToken) {
         ImportStatement importStatement     = new ImportStatement ();
         importStatement.sourceStartPosition = scanner.tokenStartPosition;

         scanner.next ();
         if (scanner.token == StaticToken) {
        	 importStatement.importStatic = true;
             scanner.next ();
         }
         if (!parseImportName (importStatement)) return false;
         importStatement.nameStartPosition = importStatement.sourceStartPosition;
         importStatement.sourceEndPosition = importStatement.nameEndPosition;
         compilationUnit.addImportStatement (env, importStatement);

         String  importPackageName = importStatement.packagePart.getNameWithDots();
         //     if(importPackageName.equals ("java.lang")) parserError (2, "Cannot import package 'java.lang'");
         //else 
              if(compilationUnit.hasPackageStatement ()
              && importPackageName.equals (compilationUnit.packageStatement.getNameWithDots()))
                     parserError (2, "Cannot import this package");

         if (!skipToken (SemicolonToken)) return false;
      }
      return true;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*              Type (= class or interface) Declaration            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseTypeDeclarations (CompilationUnit compilationUnit) {
      ScannerPosition position;

      ModifierList modifierList;
      do {
         position     = scanner.getPosition ();
         modifierList = new ModifierList ();

         if (!parseModifiers (modifierList)) return false;
         if (   scanner.token != ClassToken
             && scanner.token != InterfaceToken) break;

          currentTypeDeclaration =
         topLevelTypeDeclaration = new TopLevelTypeDeclaration ();

         // withAllNestedAndLocalClasses should partially be ordered by declaration level !
         topLevelTypeDeclaration.withAllNestedAndLocalClasses.add (topLevelTypeDeclaration);

         if (scanner.token == InterfaceToken) {
             modifierList.modifiers |= InterfaceFlag;
         } 
         currentTypeDeclaration.sourceStartPosition = modifierList.sourceStartPosition;
         currentTypeDeclaration.nameStartPosition   = scanner.tokenStartPosition; /* include "class" / "interface" */
         currentTypeDeclaration.setModifiers (modifierList.modifiers);
         if (!modifierList.checkModifiers (currentTypeDeclaration, this)) return false;

         scanner.next ();
         if (scanner.token != IdentifierToken) {
            parserError (2, "Identifier ("
                         + currentTypeDeclaration.getPresentationName ()
                         + " name) expected");
            return false;
         }
         currentTypeDeclaration.nameEndPosition     = scanner.tokenEndPosition;
         currentTypeDeclaration.headerEndPosition   = scanner.tokenEndPosition;
         currentTypeDeclaration.sourceEndPosition   = scanner.tokenEndPosition;
         currentTypeDeclaration.name                = (String)scanner.tokenValue;
         currentTypeDeclaration.compilationUnit     = compilationUnit;

         boolean isFirstTypeDeclaration = true;
         for (TypeDeclarationOrComment t: compilationUnit.typeDeclarationsAndComments) {
           if (t instanceof TopLevelTypeDeclaration) {
             isFirstTypeDeclaration = true;
             if (currentTypeDeclaration.name.equals(t.name)) {
                parserError (2,  "Attempt for multiple class or interface definitions named "
                            + currentTypeDeclaration.name);
                return false;
             }
           }
         }
         for (ImportStatement is: compilationUnit.importStatements) {
           if (currentTypeDeclaration.name.equals(is.name)
           //&&  !is.importStatic   don't test this
           ) {
              parserError (2,  "Attempt to define class or interface named "
                          + currentTypeDeclaration.name
                          + " while this name also appears in import list");
              return false;
           }
         }
         if (isFirstTypeDeclaration
         &&  currentTypeDeclaration.isPublic()) {
           if (compilationUnit.sourceFile != null) {
              String sourceFileName = compilationUnit.sourceFile.getName();
              int i = sourceFileName.lastIndexOf (File.separatorChar);
              String sourceFileNamePart = i<0
                                 ? sourceFileName
                                 : sourceFileName.substring (i+1);
              if (!sourceFileNamePart.equals (currentTypeDeclaration.name+".java")
              &&  !sourceFileNamePart.equals (currentTypeDeclaration.name+".s.java")
              &&  !sourceFileNamePart.equals (currentTypeDeclaration.name+".sawa")) {
                parserError (2,  "Attempt to define public class or interface "
                        + currentTypeDeclaration.name
                        + " in file that is not named "+currentTypeDeclaration.name+"(.java,.s.java,.sawa) "
                        + " (" + sourceFileNamePart + ")");
                   return false;
              }
           }
         }
         else if (currentTypeDeclaration.isPublic()) {
            parserError (2,  "Attempt to define public class or interface "
                        + currentTypeDeclaration.name
                        + " not as first one in file");
            return false;
         }
         scanner.next ();
         if (!parseExtendsClause    (currentTypeDeclaration))  return false;
         if (!parseImplementsClause (currentTypeDeclaration))  return false;
         if (!parseDeclarationBody  (currentTypeDeclaration))  return false;

         compilationUnit.typeDeclarationsAndComments.add (currentTypeDeclaration);

         while (scanner.token == SemicolonToken)  scanner.next();

      } while (true);

      if (   scanner.token != EofToken
          || modifierList.flags.size() > 0) {
         position.tokenEndPosition = position.tokenStartPosition;
         parserError (2, "Class or interface declaration expected",
                      position);
         return false;
      }
      return true;
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*         Nested Type (= class or interface) Declaration          */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseNestedTypeDeclaration (ModifierList modifierList) {
      LocalOrNestedTypeDeclaration result = parseLocalOrNestedTypeDeclaration (new NestedTypeDeclaration (),
                                                                               modifierList);
      addToFieldDeclarations:
      if (result != null) {

         for (FieldDeclaration f: currentTypeDeclaration.fieldDeclarations) {
            if ((f instanceof NestedTypeDeclaration)
            &&   result.name.equals (f.name)) {
                parserError (2, "A nested type named "+f.name+" already exists in the encompassing type declaration",
                             result.nameStartPosition,
                             result.  nameEndPosition);
                break addToFieldDeclarations;
            }
         }
          currentTypeDeclaration.fieldDeclarations.add (result);
      }
      return result != null;
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*          Local Type (= class or interface) Declaration          */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected LocalTypeDeclaration parseLocalTypeDeclaration (ModifierList modifierList) {

      LocalTypeDeclaration result = null;

      result = (LocalTypeDeclaration) parseLocalOrNestedTypeDeclaration (new LocalTypeDeclaration (),
                                                                                              modifierList);
      if (result != null) {
          // result.localTypesContext = (Vector) localTypeDeclarationsContext.clone();
          localTypeDeclarationsInRefinement.add (result);
      }
      return result;
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*   Local or Nested Type (= class or interface) Declaration       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected LocalOrNestedTypeDeclaration parseLocalOrNestedTypeDeclaration (LocalOrNestedTypeDeclaration result,
                                                                             ModifierList modifierList) {
     topLevelTypeDeclaration.withAllNestedAndLocalClasses.add (result);
     result.setParentTypeDeclaration (currentTypeDeclaration);
     currentTypeDeclaration               = result;
     ArrayList<LocalTypeDeclaration> savedLocalTypeDeclarations = localTypeDeclarationsContext;
     localTypeDeclarationsContext         = new ArrayList<LocalTypeDeclaration>();

     if (scanner.token == InterfaceToken) {
         modifierList.modifiers |= InterfaceFlag;
     }

     try {
       result.sourceStartPosition = modifierList.sourceStartPosition;
       result.nameStartPosition   = scanner.tokenStartPosition; /* include "class" / "interface" */
       result.setModifiers (modifierList.modifiers);
       if (!modifierList.checkModifiers (result, this)) return null;

       scanner.next ();
       if (scanner.token != IdentifierToken) {
          parserError (2,  "Identifier ("
                      + result.getPresentationName ()
                      + " name) expected");
          return null;
       }
       result.nameEndPosition       = scanner.tokenEndPosition;
       result.headerEndPosition     = scanner.tokenEndPosition;
       result.sourceEndPosition     = scanner.tokenEndPosition;
       result.name                  = (String)scanner.tokenValue;
       result.compilationUnit       = currentTypeDeclaration.compilationUnit;

       for (FieldDeclaration f: result.getParentTypeDeclaration().fieldDeclarations) {
            if (f.languageConstructCode() == LocalTypeDeclarationCode) {
              if (result.name.equals(f.name)) {
                 parserError (2,  "Attempt for multiple local class or interface definitions named "
                             + result.name);
                 return null;
              }
            }
       }
       scanner.next ();
       if (!parseExtendsClause    (result))  return null;
       if (!parseImplementsClause (result))  return null;
       if (!parseDeclarationBody  (result))  return null;

       return result;
     }
     finally {
         localTypeDeclarationsContext = savedLocalTypeDeclarations;
         currentTypeDeclaration       = result.getParentTypeDeclaration();
     }
   }

   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*      Anonymous Type (= class or interface) Declaration          */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected AnonymousTypeDeclaration parseAnonymousTypeDeclaration() {

     AnonymousTypeDeclaration result      = new AnonymousTypeDeclaration ();
     topLevelTypeDeclaration.withAllNestedAndLocalClasses.add (result);
     result.setParentTypeDeclaration (currentTypeDeclaration);
     currentTypeDeclaration               = result;
     ArrayList<LocalTypeDeclaration> savedLocalTypeDeclarations = localTypeDeclarationsContext;
     localTypeDeclarationsContext         = new ArrayList<LocalTypeDeclaration>();

     try {
       result.sourceStartPosition   = scanner.tokenStartPosition;
       result.nameStartPosition     = scanner.tokenStartPosition;
       result.nameEndPosition       = scanner.tokenEndPosition;
       result.headerEndPosition     = scanner.tokenEndPosition;
       result.compilationUnit       = currentTypeDeclaration.compilationUnit;
       result.setModifiers          ( (short)0 );

       if (!parseDeclarationBody  (result))  return null;
       result.sourceEndPosition     = scanner.tokenEndPosition;
     }
     finally {
       localTypeDeclarationsContext = savedLocalTypeDeclarations;
       currentTypeDeclaration       = result.getParentTypeDeclaration();
     }
     localTypeDeclarationsInRefinement.add (result);
     return result;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseExtendsClause (TypeDeclaration typeDeclaration) {
      if (scanner.token != ExtendsToken) return true;
      scanner.next ();

      do {
         SuperTypeDeclaration superclass  = typeDeclaration.isClass ()
                                          ?  (SuperTypeDeclaration) new SuperclassDeclaration()
                                          :  (SuperTypeDeclaration) new ImplementsDeclaration();
         superclass.sourceStartPosition   = scanner.tokenStartPosition;
         if (!parseCompoundName (superclass)) return false;
         typeDeclaration.headerEndPosition = superclass.nameEndPosition;
         superclass.sourceEndPosition      = superclass.nameEndPosition;
         if (typeDeclaration.isClass ()) {
             typeDeclaration.superclass = (SuperclassDeclaration) superclass;
         }
         else {
             typeDeclaration.interfaces.add ((ImplementsDeclaration) superclass);
         }
         if (scanner.token != CommaToken) break;

         ScannerPosition savePos = scanner.getPosition ();
         scanner.next ();
         if (typeDeclaration.isClass ()) {
            if (scanner.token == IdentifierToken) {
               parserError (2, "A class cannot extend multiple superclasses");
               return false;
            }
            scanner.setPosition (savePos);
            return true; /* Let caller print a better message */
         }
      } while (true);

      if (typeDeclaration.isClass ()) {
         int c [] = {ImplementsToken, BraceOpenToken};
         if (!expectTokens (c)) return false;
      } else {
         if (scanner.token == ImplementsToken) return true; /* Let caller print a better message */
         int m [] = {CommaToken, BraceOpenToken};
         if (!expectTokens (m)) return false;
      }

      return true;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseImplementsClause (TypeDeclaration typeDeclaration) {
      if (scanner.token != ImplementsToken) return true;
      if (typeDeclaration.isInterface ()) {
         parserError (2, "An interface cannot have an \"implements\" clause");
         return false;
      }
      scanner.next ();

      do {
         ImplementsDeclaration implement = new ImplementsDeclaration ();
         implement.sourceStartPosition   = scanner.tokenStartPosition;
         if (!parseCompoundName (implement)) return false;
         typeDeclaration.headerEndPosition = implement.nameEndPosition;
         implement.sourceEndPosition       = implement.nameEndPosition;
         typeDeclaration.interfaces.add(implement);

         if (scanner.token != CommaToken) break;

         scanner.next ();
      } while (true);
      
      return true;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseDeclarationBody (TypeDeclaration typeDeclaration) {

      if (!skipToken (BraceOpenToken)) return false;
      while (   scanner.token != BraceCloseToken
             && scanner.token != EofToken) {

         if (   scanner.token == SemicolonToken) {
            skipToken(SemicolonToken);
            continue;
         }

         seenDeprecated = ((JavaScanner) scanner).seenDeprecated;
         ModifierList modifierList = new ModifierList ();
         if (!parseModifiers (modifierList)) return false;
         if (typeDeclaration.isInterface()) modifierList.modifiers |= PublicFlag;

         if (!parseFieldDeclaration (typeDeclaration, modifierList)) return false;
      }

      typeDeclaration.sourceEndPosition = scanner.tokenEndPosition;
      skipToken (BraceCloseToken); // Non-fatal parser error
      return true;
   }



   /*******************************************************************/
   /**                                                               **/
   /**           FIELD (= variable and method) DECLARATIONS          **/
   /**                                                               **/
   /*******************************************************************/

   protected boolean parseFieldDeclaration (TypeDeclaration typeDeclaration,
                                            ModifierList modifierList) {

      if (!typeDeclaration.isTopLevelType()
      && (modifierList.modifiers & StaticFlag) != 0) {
          parserError (2, "Inner types cannot contain static fields");
      }

      /* Local type? */
      if (scanner.token                       ==     ClassToken   
        ||scanner.token                       == InterfaceToken) {
         if (seenDeprecated) {
             parserError (2, "Nested types cannot be deprecated");
         }
         return parseNestedTypeDeclaration (modifierList);
      }


      /* Dimension? */
      if (scanner.token                       == DimensionToken) {
         if (seenDeprecated) {
             parserError (2, "Dimensions cannot be deprecated");
         }
         if (modifierList.modifiers != 0
         && (modifierList.modifiers != PublicFlag
           ||typeDeclaration.isClass())) {
            parserError (2, "Dimensions cannot have modifiers");
         }
         return parseDimensionDeclaration (typeDeclaration, modifierList);
      }

      /* Static initializer? */
      if (scanner.token                       == BraceOpenToken) {
         if (seenDeprecated) {
             parserError (2, "Initializer blocks cannot be deprecated");
         }
         return parseInitializerBlock (typeDeclaration, modifierList);
      }
      /* Parse type */
      seenRelativeDimension = null;
      DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
      if (!parseDataType (dataTypeDeclaration, 
                          "Field declaration or \"}\" expected",
                          false,
                          false,
                          true)) return false;

      return parseRestOfFieldDeclaration (typeDeclaration, modifierList, dataTypeDeclaration);
   }

   protected boolean parseRestOfFieldDeclaration (TypeDeclaration typeDeclaration,
                                                  ModifierList modifierList,
                                                  DataTypeDeclaration dataTypeDeclaration) {

      /* Constructor declaration? */
      if (scanner.token == ParenthesisOpenToken)
         return parseConstructorDeclaration (typeDeclaration, modifierList, dataTypeDeclaration);

      /* Parse any array brackets */
      while (scanner.token == BracketOpenToken) {
         if (!nextToken (BracketCloseToken)) return false;
         dataTypeDeclaration.noOfArrayDimensions++;
         dataTypeDeclaration.sourceEndPosition = scanner.tokenEndPosition;
         scanner.next ();
      }

      /* Variable or method name */
      if (scanner.token != IdentifierToken) {
         if (dataTypeDeclaration.isVoidType())
              parserError (2, "Identifier (method name) expected");
         else parserError (2, "Identifier (variable or method name) expected");
         return false;
      }

      /* Create variable declaration; 
         convert to method declaration if that's what it turns out to be */
      MemberVariableDeclaration variable = new MemberVariableDeclaration ();
      variable.sourceStartPosition = modifierList.sourceStartPosition;
      variable.nameStartPosition   = scanner.tokenStartPosition;
      variable.nameEndPosition     = scanner.tokenEndPosition;
      variable.sourceEndPosition   = scanner.tokenEndPosition;
      variable.name                = (String)scanner.tokenValue;
      variable.dataTypeDeclaration = dataTypeDeclaration;
      variable.declarationIndex    = typeDeclaration.fieldDeclarations.size();
      scanner.next ();

      /* If type is "void", it must be a method */
      if (dataTypeDeclaration.isVoidType()) {
         if (!expectToken (ParenthesisOpenToken)) return false;
      }

      /* Check extra array dimensions ("int [] i [] ...") */
      /* NOTE: According to the syntax rule in the Java Spec section 6.4.2,
               array brackets can appear in THREE places in a method declaration
               ("int [] foo [] (int i, int j) [] { ... }"). It also does not
               state explicitly that there may be no array brackets with the "void"
               return type. */
      while (scanner.token == BracketOpenToken) {
         if (!nextToken (BracketCloseToken)) return false;
         variable.extraArrayDimensions++;
         variable.sourceEndPosition = scanner.tokenEndPosition;
         scanner.next ();
      }

      /* Method declaration? */
      if (scanner.token == ParenthesisOpenToken) {
         MethodDeclaration method = new MethodDeclaration ();
         method.sourceStartPosition = variable.sourceStartPosition;
         method.sourceEndPosition   = variable.sourceEndPosition;
         method.nameStartPosition   = variable.nameStartPosition;
         method.nameEndPosition     = variable.nameEndPosition;
         method.name                = variable.name;
         dataTypeDeclaration.noOfArrayDimensions += variable.extraArrayDimensions;
         method.returnTypeDeclaration = dataTypeDeclaration;
         method.isDeprecated         = seenDeprecated;
         return parseMethodDeclaration (typeDeclaration,
                                        modifierList, 
                                        method);
      }

      /* Variable declaration! */
      variable.isDeprecated = seenDeprecated;
      if (   scanner.token == SemicolonToken
          || scanner.token == CommaToken
          || scanner.token == AssignToken) {
         return parseVariableDeclaration (typeDeclaration, 
                                          modifierList, 
                                          dataTypeDeclaration, 
                                          variable);
      }
      parserError (2, "\";\" or rest of variable/method declaration expected");
      return false;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                       Static Initializer                        */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseInitializerBlock (TypeDeclaration typeDeclaration,
                                             ModifierList modifierList) {

    try {
      if (typeDeclaration.isInterface()) {
         parserError (2, "Cannot declare a static or instance initializer in an interface");
         return false;
      }
      return true;
    }
    finally {
      if ((modifierList.modifiers|StaticFlag) != StaticFlag) {
         parserError (2, "The only allowed modifier for a static or instance initializer is 'static'");
      }
      InitializerBlock initializerBlock = new InitializerBlock ();
      initializerBlock.sourceStartPosition = modifierList.sourceStartPosition;
      initializerBlock.sourceEndPosition   = scanner.tokenEndPosition;
      initializerBlock.nameStartPosition   = scanner.tokenStartPosition;
      initializerBlock.nameEndPosition     = scanner.tokenEndPosition;
      initializerBlock.declarationIndex    = typeDeclaration.fieldDeclarations.size();
      initializerBlock.setModifiers        ( modifierList.modifiers);

      StatementBlock statements = parseStatementBlock ();
      if (statements == null) return false;
      initializerBlock.statements = statements;
      initializerBlock.sourceEndPosition = statements.sourceEndPosition;

      initializerBlock.typeDeclaration   = typeDeclaration;
      typeDeclaration.fieldDeclarations.add (initializerBlock);
    }
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                    Constructor Declaration                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseConstructorDeclaration (TypeDeclaration typeDeclaration,
                                                  ModifierList modifierList,
                                                  DataTypeDeclaration dataTypeDeclaration) {

      if (!(dataTypeDeclaration.name.equals (typeDeclaration.name))) {
         parserError (2, "Identifier (method name) expected");
         return false;
      }
      if (typeDeclaration.isInterface()) {
         parserError (2, "Cannot declare a constructor in an interface");
         return false;
      }

      ConstructorDeclaration constructor = new ConstructorDeclaration ();
      constructor.sourceStartPosition    = modifierList.sourceStartPosition;
      constructor.sourceEndPosition      = dataTypeDeclaration.nameEndPosition;
      constructor.nameStartPosition      = dataTypeDeclaration.nameStartPosition;
      constructor.nameEndPosition        = dataTypeDeclaration.nameEndPosition;
      constructor.name                   = dataTypeDeclaration.name;
      constructor.returnTypeDeclaration  = dataTypeDeclaration;
      constructor.isDeprecated          = seenDeprecated;

      return parseMethodDeclaration (typeDeclaration,
                                      modifierList,
                                      constructor);
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        Method Declaration                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseMethodDeclaration (TypeDeclaration typeDeclaration,
                                             ModifierList modifierList,
                                             MethodDeclaration method) {
     /* scanner.token == ParenthesisOpenToken */

     method.setModifiers    ( modifierList.modifiers);
     method.typeDeclaration = typeDeclaration;
     if (!modifierList.checkModifiers (method, this)) return false;

     ParameterList parameterList = parseParameterList ("method", method);
     if (parameterList == null) return false;
     parameterList.nameStartPosition   = method.nameStartPosition;
     parameterList.nameEndPosition     = method.nameEndPosition;
     parameterList.name                = method.name;
     method.parameterList              = parameterList;
     method.headerEndPosition          = parameterList.sourceEndPosition;
     method.declarationIndex           = typeDeclaration.fieldDeclarations.size();
     typeDeclaration.fieldDeclarations.add (method);

     if (   method.isConstructorDeclaration ()
         && scanner.token == BracketOpenToken) {
        parserError (2, "Cannot return an array from a constructor");
        return false;
     }

     while (scanner.token == BracketOpenToken) {
        if (!nextToken (BracketCloseToken)) return false;
        method.returnTypeDeclaration.noOfArrayDimensions++;
        method.headerEndPosition = scanner.tokenEndPosition;
        scanner.next ();
     }

     ArrayList<LocalTypeDeclaration> savedLocalTypeDeclarationsInRefinement = localTypeDeclarationsInRefinement; // may be nested
     localTypeDeclarationsInRefinement = method.localTypeDeclarations;

     try {
       return parseRestOfMethodDeclaration (typeDeclaration, method);
     }
     finally {
       localTypeDeclarationsInRefinement = savedLocalTypeDeclarationsInRefinement;
     }
   }

   /* Throws clause & statement block */
   protected boolean parseRestOfMethodDeclaration (TypeDeclaration typeDeclaration,
                                                   MethodDeclaration method) {

      if (scanner.token == ThrowsToken) {
         ThrowsClause throwsClause        = new ThrowsClause ();
         throwsClause.sourceStartPosition = scanner.tokenStartPosition;
         throwsClause.sourceEndPosition   = scanner.tokenEndPosition;
         throwsClause.nameStartPosition   = scanner.tokenStartPosition;
         throwsClause.nameEndPosition     = scanner.tokenEndPosition;
         throwsClause.name                = Scanner.tokenRepresentation (scanner.token);
         method.throwsClause              = throwsClause;
         scanner.next ();

         do {
            if (scanner.token != IdentifierToken) {
               parserError (2, "Identifier (exception name) expected");
               return false;
            }
            DataTypeDeclaration dataTypeDeclaration = new ThrownTypeDeclaration ();
            dataTypeDeclaration.sourceStartPosition = scanner.tokenStartPosition;
            dataTypeDeclaration.sourceEndPosition   = scanner.tokenEndPosition;
            dataTypeDeclaration.nameStartPosition   = scanner.tokenStartPosition;
            dataTypeDeclaration.nameEndPosition     = scanner.tokenEndPosition;
            dataTypeDeclaration.primitiveTypeToken  = scanner.token;
            dataTypeDeclaration.name                = Scanner.tokenRepresentation (scanner.token);

            if (!parseCompoundName (dataTypeDeclaration)) return false;
            dataTypeDeclaration.sourceEndPosition       = dataTypeDeclaration.nameEndPosition;
            throwsClause.sourceEndPosition   = dataTypeDeclaration.nameEndPosition;
            method.headerEndPosition         = dataTypeDeclaration.nameEndPosition;
            throwsClause.exceptionTypeDeclarations.add (dataTypeDeclaration);

            if (scanner.token != CommaToken) break;
            scanner.next ();
         } while (true);
      }

      if (   typeDeclaration.isInterface ()
          || (method.modifiers.modifierFlags & (AbstractFlag | NativeFlag)) > 0) {
         if (!skipToken (SemicolonToken)) return false;
      } else {
         method.bodyStartPosition  = scanner.getPosition();
       //StatementBlock statements = parseUnparsedStatementBlock ();
         StatementBlock statements = parseStatementBlock ();
         if (statements == null) return false;
         if (keepFieldBodies
         || !method.localTypeDeclarations.isEmpty() ) method.statements = statements;
         //else System.gc();
      }
      method.sourceEndPosition   = scanner.previousTokenEndPosition;
      return true;
   }

   /*-----------------------------------------------------------------*/

   /* Reparse statement block  DEACTIVATED
*********************************************
   protected boolean reparseMethodBody (MethodDeclaration method) {
      if (method.statements != null
      ||  method.isAbstract() 
      ||  method.typeDeclaration.isInterface() ) {
          return true;
      }
      if (method.bodyStartPosition == null) {

          if (!method.isConstructorDeclaration())
          {
              new Exception("Internal error when reparsing: "+method.getPresentation()).printStackTrace();
          }
          else  ;    // would presumably be inserted default constructor...
          return true;
      }
      compilationUnit = method.typeDeclaration.compilationUnit;

      Vector savedLocalTypeDeclarationsInRefinement = localTypeDeclarationsInRefinement; // may be nested
      localTypeDeclarationsInRefinement             = method.localTypeDeclarations;

      scanner.setPosition (method.bodyStartPosition);
      //StatementBlock statements = parseUnparsedStatementBlock ();
      long startTime              = env.timerLocalStart();
      StatementBlock statements   = parseStatementBlock ();
      env.timerLocalStop(env.ReparseMsg, method.name, startTime);

      localTypeDeclarationsInRefinement = savedLocalTypeDeclarationsInRefinement;

      if (statements == null) {
          return false;
      }
      method.statements = statements;
      return true;
   }
*****************************************/
   /*-----------------------------------------------------------------*/

   protected ParameterList parseParameterList (String constructName, RefinementDeclaration refinementConstruct) {

      if (!expectToken (ParenthesisOpenToken)) return null;

      ParameterList parameterList       = new ParameterList ();
      parameterList.sourceStartPosition = scanner.tokenStartPosition;
      parameterList.sourceEndPosition   = scanner.tokenEndPosition;
      parameterList.nameStartPosition   = scanner.tokenStartPosition;
      parameterList.nameEndPosition     = scanner.tokenEndPosition;
      parameterList.owner               = refinementConstruct;

      return parseParameterList (constructName, parameterList, ParenthesisCloseToken);
   }

   protected ParameterList parseParameterList (String constructName,
                                               ParameterList parameterList,
                                               int closingToken) {

      scanner.next();

      if (scanner.token != closingToken) {
         do {
            boolean isFinal = false;
            if (scanner.token == FinalToken) {
                isFinal = true;
                scanner.next();
            }
            DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
            if (!parseDataType (dataTypeDeclaration, 
                                "Type (" + constructName
                                         + " parameter) expected",
                                true,
                                true,
                                true)) return null;

            if (dataTypeDeclaration.isVoidType()) {
               parserError (2, "A " + constructName + " parameter "
                            + "cannot be \"void\"",
                            dataTypeDeclaration.nameStartPosition,
                            dataTypeDeclaration.nameEndPosition);
               return null;
            }

            if (scanner.token != IdentifierToken) {
               parserError (2, "Identifier (parameter name) expected");
               return null;
            }
            MethodParameterDeclaration parameter = new MethodParameterDeclaration ();
            parameter.sourceStartPosition = dataTypeDeclaration.sourceStartPosition;
            parameter.sourceEndPosition   = scanner.tokenEndPosition;
            parameter.nameStartPosition   = scanner.tokenStartPosition;
            parameter.nameEndPosition     = scanner.tokenEndPosition;
            parameter.name                = (String)scanner.tokenValue;
            parameter.dataTypeDeclaration            = dataTypeDeclaration;
            parameter.declarationIndex    = parameterList.parameters.size();
            parameter.owner               = parameterList.owner;
            parameter.isFinal             = isFinal;

            while (scanner.next() == BracketOpenToken) {
               if (!nextToken (BracketCloseToken)) return null;
               parameter.dataTypeDeclaration.noOfArrayDimensions++;
               parameter.sourceEndPosition = scanner.tokenEndPosition;
            }

            parameterList.parameters.add (parameter);
            if (scanner.token == closingToken)
               break;

            int ct [ ] = {CommaToken, closingToken};
            if (!expectTokens (ct)) return null;
            skipToken (CommaToken);
         } while (true);
      }

      parameterList.sourceEndPosition = scanner.tokenEndPosition;
      skipToken (closingToken);
      return parameterList;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                       Variable Declaration                      */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected boolean parseVariableDeclaration (TypeDeclaration typeDeclaration,
                                               ModifierList modifierList,
                                               DataTypeDeclaration dataTypeDeclaration,
                                               MemberVariableDeclaration firstVariable) {

      if (seenRelativeDimension != null) {
               parserError (2, "A dimension declaration for a variable may not include a 'dimension(...)' construct",
                            seenRelativeDimension.sourceStartPosition,
                            seenRelativeDimension.sourceEndPosition);
      }
      firstVariable.sourceStartPosition = firstVariable.nameStartPosition;

      MultiVariableDeclaration declaration = new MultiVariableDeclaration ();
      declaration.sourceStartPosition = modifierList.sourceStartPosition;
      declaration.nameStartPosition   = dataTypeDeclaration.nameStartPosition;
      declaration.nameEndPosition     = dataTypeDeclaration.nameEndPosition;
      declaration.dataTypeDeclaration = dataTypeDeclaration;
      declaration.setModifiers        ( modifierList.modifiers);
      declaration.isDeprecated       = firstVariable.isDeprecated;

      if (!modifierList.checkModifiers (declaration, this)) return false;

      MemberVariableDeclaration variable = firstVariable;
      do {
         /* Variable initializer? */
         if (scanner.token == AssignToken) {
            scanner.next ();

            JavaExpression initializer;
            if (scanner.token == BraceOpenToken)
               initializer = parseArrayInitializer ();
            else
               initializer = parseJavaExpression ();

            if (initializer == null) return false;
            variable.initializer = initializer;
            variable.sourceEndPosition = initializer.sourceEndPosition;
         }
         else if ((modifierList.modifiers & ModifierFlags.FinalFlag) != 0) {
         // OK since JDK 1.1:
         //      parserError (2, "Final variable requires initializer",
         //                   variable.nameStartPosition,
         //                   variable.nameEndPosition);
         //      return false;
         }
         variable.owner            = declaration;
         variable.declarationIndex = typeDeclaration.fieldDeclarations.size();
         declaration.variables.add (variable);

         /* Another declaration? */
         if (scanner.token != CommaToken) break;

         if (!nextToken (IdentifierToken)) return false;

         variable = new MemberVariableDeclaration ();
         variable.sourceStartPosition = scanner.tokenStartPosition;
         variable.nameStartPosition   = scanner.tokenStartPosition;
         variable.nameEndPosition     = scanner.tokenEndPosition;
         variable.sourceEndPosition   = scanner.tokenEndPosition;
         variable.name                = (String)scanner.tokenValue;
         variable.dataTypeDeclaration = dataTypeDeclaration;
         variable.isDeprecated       = declaration.isDeprecated;

         while (scanner.next () == BracketOpenToken) {
            if (!nextToken (BracketCloseToken)) return false;
            variable.extraArrayDimensions++;
            variable.sourceEndPosition = scanner.tokenEndPosition;
         }
      } while (true);

      if (variable.initializer != null) {
         int vt [] = {CommaToken, SemicolonToken};
         if (!expectTokens (vt)) return false;
      } else {
         if (   scanner.token != SemicolonToken
             && scanner.token != CommaToken
             && scanner.token != AssignToken) {
            parserError (2, "\";\" or rest of variable declaration expected");
            return false;
         }
      }

      declaration.sourceEndPosition = variable.sourceEndPosition;
      declaration.typeDeclaration   = typeDeclaration;
      typeDeclaration.fieldDeclarations.add (declaration);
      return skipToken (SemicolonToken);
   }



   /*******************************************************************/
   /**                                                               **/
   /**                          STATEMENTS                           **/
   /**                                                               **/
   /*******************************************************************/

   /*********** NOT USED **********
   protected StatementBlock parseUnparsedStatementBlock () {
      StatementBlock statements = new StatementBlock ();
      statements.sourceStartPosition = scanner.tokenStartPosition;
      statements.sourceEndPosition   = scanner.tokenEndPosition;
      statements.nameStartPosition   = scanner.tokenStartPosition;
      statements.nameEndPosition     = scanner.tokenEndPosition;

      if (!expectToken (BraceOpenToken)) return null;
      int braceLevel = 1;
      do {
         int token = scanner.next ();
         
              if (token == EofToken)         break;
         else if (token == BraceOpenToken)   braceLevel++;
         else if (token == BraceCloseToken) {braceLevel--; if (braceLevel < 1) break;} 
      } while (true);

      statements.sourceEndPosition   = scanner.tokenEndPosition;
      scanner.next ();
      return statements;
   }
   ***************************/

   /*-----------------------------------------------------------------*/

protected NestedStatement parseNestedStatement (NestedStatement nestedStatement) {

      if (!skipToken (BraceOpenToken)) return null;
      enterStatementLexicalLevel ();

      // push the context and continue with a clone
      ArrayList<LocalTypeDeclaration> savedLocalTypeDeclarationsContext = localTypeDeclarationsContext;
      localTypeDeclarationsContext = new ArrayList<LocalTypeDeclaration>(localTypeDeclarationsContext);

      while (   scanner.token != BraceCloseToken
             && scanner.token != EofToken) {
         JavaStatement statement = parseJavaStatement ();
         if (statement == null) return null;
         nestedStatement.statements.add (statement);
      }
      // pop the old context
      localTypeDeclarationsContext = savedLocalTypeDeclarationsContext;

      exitStatementLexicalLevel ();
      nestedStatement.sourceEndPosition = scanner.tokenEndPosition;
      skipToken (BraceCloseToken); // Non-fatal parser error
      return nestedStatement;
   }

   /*-----------------------------------------------------------------*/

   protected StatementBlock parseStatementBlock () {
      StatementBlock statementBlock      = new StatementBlock();
      statementBlock.sourceStartPosition = scanner.tokenStartPosition;
      statementBlock.nameStartPosition   = scanner.tokenStartPosition;
      statementBlock.nameEndPosition     = scanner.tokenEndPosition;
      return (StatementBlock) parseNestedStatement (statementBlock);
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseJavaStatement () {
      return parseJavaStatement (true);
   }

   protected JavaStatement parseJavaStatement (boolean doCheckExpressionStatement) {

      boolean isFinal = false;
      if (scanner.token == FinalToken) {
           isFinal = true;
           scanner.next();
           switch (scanner.token) {
           case IdentifierToken:
           case BooleanToken:
           case CharToken:
           case ByteToken:
           case ShortToken:
           case IntToken:
           case LongToken:
           case FloatToken:
           case DoubleToken:  break; // these are OK
           default:
                   parserError (2, "A local variable is expected after 'final'");
           }
      }
      switch (scanner.token) {

         case ClassToken:
         case InterfaceToken:
             {
               LocalTypeDeclaration localTypeDeclaration = 
                               parseLocalTypeDeclaration (new ModifierList());

              addToLocalTypeDeclarationsContext:
               if (localTypeDeclaration != null) {
                   for (LocalTypeDeclaration lt: localTypeDeclarationsContext) {
                      if (lt.name.equals (localTypeDeclaration.name)) {
                          parserError (2, "A local type with that name already exists in the current refinement",
                                        localTypeDeclaration.nameStartPosition,
                                        localTypeDeclaration.  nameEndPosition
                                       );
                          break addToLocalTypeDeclarationsContext;
                      }
                   }
                   localTypeDeclarationsContext.add (localTypeDeclaration);
                   localTypeDeclaration.localTypesContext = localTypeDeclarationsContext;
               }
               return new LocalTypeDeclarationStatement (localTypeDeclaration);
             }

         case IdentifierToken:
               return parseIdentifierLedStatement (doCheckExpressionStatement,
                                                   true, true, isFinal);

         case BooleanToken:
         case CharToken:
         case ByteToken:
         case ShortToken:
         case IntToken:
         case LongToken:
         case FloatToken:
         case DoubleToken:
            {
               ScannerPosition position = scanner.getPosition();
               DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
               if (!parseDataType (dataTypeDeclaration, 
                                   "Type (local variable declaration) expected",
                                   true,
                                   false,
                                   true)) return null;
               if (!isFinal
               &&  scanner.token == PeriodToken) {  // class literal: "int.class"
                   scanner.setPosition(position);
                   return parseExpressionStatement (doCheckExpressionStatement, true);
               }
               return parseLocalVariableDeclarationStatement (dataTypeDeclaration, isFinal);
            }
         case IncrementToken:
         case DecrementToken:
         case NewToken:
         case ThisToken:
         case SuperToken:
         case NullToken:
         case ParenthesisOpenToken:
               return parseExpressionStatement (doCheckExpressionStatement,
                                                true);

         case SemicolonToken:    return parseEmptyStatement ();
         case BraceOpenToken:    return parseNestedStatement ();
         case IfToken:           return parseIfStatement ();
         case SwitchToken:       return parseSwitchStatement ();
         case WhileToken:        return parseWhileStatement ();
         case DoToken:           return parseDoStatement ();
         case ForToken:          return parseForStatement ();
         case BreakToken:        return parseBreakStatement ();
         case ContinueToken:     return parseContinueStatement ();
         case ReturnToken:       return parseReturnStatement ();
         case ThrowToken:        return parseThrowStatement ();
         case SynchronizedToken: return parseSynchronizedStatement ();
         case TryToken:          return parseTryStatement ();
      }
      parserError (2, "Statement expected");
      return null;
   }

   /*-----------------------------------------------------------------*/

   protected boolean skipStatementTerminator () {
      /* Verify that the ";" is present following statements that 
         require it. Skip it and answer true if OK, false if an error
         occurs.
         This method is provided as a hook for the Scriptic parser. */
      
      return skipToken (SemicolonToken);
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected boolean testStatementTerminator () {
      /* Check if the current token is a statement terminator.
         This method is used by statements that have an optional tail,
         like the "return" statement.
         This method is provided as a hook for the Scriptic parser. */
      return    scanner.token == SemicolonToken
             || scanner.token == EofToken;
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected void enterStatementLexicalLevel () {
      /* Enter a new lexical level for statements, i.e.,
         enter a "{...}" construct.
         This method is provided as a hook for the Scriptic parser. */
   }

/* Currently called from --

      parseStatementBlock ()
      parseNestedStatement ()
      parseSwitchStatement ()
      parseTryStatement ()
         try
         catches
         finally

   Currently NOT called from parseForStatement () */

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected void exitStatementLexicalLevel () {
      /* Exit from a lexical level for statements, i.e.,
         exit a "{...}" construct.
         This method is provided as a hook for the Scriptic parser. */
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseEmptyStatement () {

      EmptyStatement emptyStatement      = new EmptyStatement ();
      emptyStatement.sourceStartPosition = scanner.tokenStartPosition;
      emptyStatement.sourceEndPosition   = scanner.tokenEndPosition;
      emptyStatement.nameStartPosition   = scanner.tokenStartPosition;
      emptyStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (SemicolonToken)) return null;
      return emptyStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseIdentifierLedStatement (boolean doCheckExpressionStatement,
                                                        boolean allowLabeledStatement,
                                                        boolean requireSemicolon,
                                                        boolean startedWithFinalToken) {

      /* Any of the following:

            LocalVariableDeclarationStatement :== TypeSpecifier VariableDeclarators
            LabeledStatement                  :== Identifier ':' Statement
            ExpressionStatement

      */

      ScannerPosition identifierPosition = scanner.getPosition ();

      /* Try a LabeledStatement, if allowed in this context */
      if (!startedWithFinalToken
      &&   allowLabeledStatement) {
          if (scanner.next () == ColonToken) {
             scanner.setPosition (identifierPosition);
             return parseLabeledStatement ();
          }
          scanner.setPosition (identifierPosition);
      }

      /* Try a QualifiedThisExpression, if allowed in this context */
      if (!startedWithFinalToken) {
          if  (scanner.next () == PeriodToken) {
            if(scanner.next () ==   ThisToken) {
               scanner.setPosition (identifierPosition);
               return parseExpressionStatement (doCheckExpressionStatement,
                                                requireSemicolon);
            }
          }
          scanner.setPosition (identifierPosition);
      }

      /* Try to parse as a LocalVariableDeclarationStatement */
      DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
      if (!parseDataType (dataTypeDeclaration, 
                          "Type (local variable declaration) expected",
                          false,
                          false,
                          false)) return null;

      /* Parse any array brackets */
      boolean seenEmptyBracketPairs = false;
      while (scanner.token == BracketOpenToken) {
         if (scanner.next() != BracketCloseToken) {
            if (!seenEmptyBracketPairs) {
               /* Well, looks like an array access expression */
               scanner.setPosition (identifierPosition);
               return parseExpressionStatement (doCheckExpressionStatement,
                                                requireSemicolon);
            }
            expectToken (BracketCloseToken);
            return null;
         }
         seenEmptyBracketPairs = true;
         dataTypeDeclaration.noOfArrayDimensions++;
         dataTypeDeclaration.sourceEndPosition = scanner.tokenEndPosition;
         scanner.next ();
      }

      if (   scanner.token == IdentifierToken
          || seenEmptyBracketPairs) {
         return parseLocalVariableDeclarationStatement (dataTypeDeclaration, startedWithFinalToken);
      }
      if (startedWithFinalToken) {
           parserError (2, "A local variable is expected after 'final'");
      }

      /* Only one possibility left... */
      scanner.setPosition (identifierPosition);
      return parseExpressionStatement (doCheckExpressionStatement,
                                       requireSemicolon);
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseLabeledStatement () {
      LabeledStatement labeledStatement    = new LabeledStatement ();
      labeledStatement.sourceStartPosition = scanner.tokenStartPosition;
      labeledStatement.nameStartPosition   = scanner.tokenStartPosition;
      labeledStatement.nameEndPosition     = scanner.tokenEndPosition;
      labeledStatement.name                = (String)scanner.tokenValue;

      if (scanner.token != IdentifierToken) {
         parserError (2, "Identifier (statement label) expected");
         return null;
      }
      scanner.next ();
      labeledStatement.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipToken (ColonToken)) return null;
      labeledStatement.statement = parseJavaStatement();
      return labeledStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseExpressionStatement (boolean doCheckExpressionStatement,
                                                     boolean requireSemicolon) {

      ExpressionStatement expressionStatement = new ExpressionStatement ();
      expressionStatement.sourceStartPosition = scanner.tokenStartPosition;

      JavaExpression expression = parseJavaExpression ();
      if (expression == null) return null;

      expressionStatement.nameStartPosition   = expression.nameStartPosition;
      expressionStatement.nameEndPosition     = expression.nameEndPosition;
      expressionStatement.sourceEndPosition   = expression.sourceEndPosition;
      expressionStatement.name                = expression.name;
      expressionStatement.expression          = expression;

      return parseRestOfExpressionStatement (expressionStatement,
                                             doCheckExpressionStatement,
                                             requireSemicolon);
   }


   /* Parse rest of expression statement. This
      is provided as a hook for the Scriptic parser. */

   protected JavaStatement parseRestOfExpressionStatement 
                                             (ExpressionStatement expressionStatement,
                                              boolean doCheckExpressionStatement,
                                              boolean requireSemicolon) {

      if (doCheckExpressionStatement)
         checkExpressionStatement (expressionStatement);

      if (requireSemicolon) {
         expressionStatement.sourceEndPosition = scanner.tokenEndPosition;
         if (!skipStatementTerminator ()) return null;
      }
      return expressionStatement;
   }

   /* -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   */

   protected boolean checkExpressionStatement (ExpressionStatement expressionStatement) {

      JavaExpression expression = expressionStatement.expression;

      /* Check restrictions on ExpressionStatement, according to
         Java Spec section 8.6 */
      int code = expression.languageConstructCode ();
      int operator;
      if (expressionStatement.specialCode != SpecialCode.doubleSuccessTest // ??
    	  && code != AssignmentExpressionCode
          && code !=      UnaryExpressionCode
          && code !=    PostfixExpressionCode
          && code != MethodCallExpressionCode
          && code != AllocationExpressionCode) {

         parserError (2, "Invalid expression statement", 
                      expression.sourceStartPosition,
                      expression.sourceEndPosition);
         //Make this non-fatal
         //return false;
      }
      if (   code == UnaryExpressionCode
          && (  (operator = ((UnaryExpression)expression).unaryToken)
                          != IncrementToken
              && operator != DecrementToken)) {

         parserError (2, "Invalid expression statement", 
                      expression.sourceStartPosition,
                      expression.sourceEndPosition);
         //Make this non-fatal
         //return false;
      }
      return true;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseLocalVariableDeclarationStatement (DataTypeDeclaration dataTypeDeclaration,
                                                                   boolean startedWithFinalToken) {

      LocalVariableDeclarationStatement statement = new LocalVariableDeclarationStatement ();
      statement.sourceStartPosition = dataTypeDeclaration.sourceStartPosition;
      statement.nameStartPosition   = dataTypeDeclaration.nameStartPosition;
      statement.nameEndPosition     = dataTypeDeclaration.nameEndPosition;
      statement.dataTypeDeclaration = dataTypeDeclaration;

      LocalVariableDeclaration variable;
      do {
         if (scanner.token != IdentifierToken) {
            parserError (2, "Identifier (local variable name) expected");
            return null;
         }

         variable                     = new LocalVariableDeclaration ();
         variable.sourceStartPosition = scanner.tokenStartPosition;
         variable.sourceEndPosition   = scanner.tokenEndPosition;
         variable.nameStartPosition   = scanner.tokenStartPosition;
         variable.nameEndPosition     = scanner.tokenEndPosition;
         variable.name                = (String)scanner.tokenValue;
         variable.dataTypeDeclaration = dataTypeDeclaration;
         variable.isFinal             = startedWithFinalToken;

         while (scanner.next () == BracketOpenToken) {
            if (!nextToken (BracketCloseToken)) return null;
            variable.extraArrayDimensions++;
            variable.sourceEndPosition = scanner.tokenEndPosition;
         }

         /* Variable initializer? */
         if (scanner.token == AssignToken) {
            scanner.next ();

            JavaExpression initializer;
            if (scanner.token == BraceOpenToken)
               initializer = parseArrayInitializer ();
            else
               initializer = parseJavaExpression ();

            if (initializer == null) return null;
            variable.initializer = initializer;
            variable.sourceEndPosition = initializer.sourceEndPosition;
         }
         statement.variables.add (variable);

         /* Another declaration? */
         if (scanner.token != CommaToken) break;
         scanner.next ();
      } while (true);

    //Nice error message... nice try.
    //But incompatible with the Scriptic hook, skipStatementTerminator ().
    //
    //if (variable.initializer != null) {
    //   int vt [] = {CommaToken, SemicolonToken};
    //   if (!expectTokens (vt)) return null;
    //} else {
    //   if (   scanner.token != SemicolonToken
    //       && scanner.token != CommaToken
    //       && scanner.token != AssignToken) {
    //      parserError (2, "\";\" or rest of variable declaration expected");
    //      return null;
    //   }
    //}

      statement.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipStatementTerminator ()) return null;
      return statement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseNestedStatement () {
      NestedStatement nestedStatement      = new NestedStatement();
      nestedStatement.sourceStartPosition = scanner.tokenStartPosition;
      nestedStatement.nameStartPosition   = scanner.tokenStartPosition;
      nestedStatement.nameEndPosition     = scanner.tokenEndPosition;
      return parseNestedStatement (nestedStatement);
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseIfStatement () {
      IfStatement ifStatement         = new IfStatement ();
      ifStatement.sourceStartPosition = scanner.tokenStartPosition;
      ifStatement.nameStartPosition   = scanner.tokenStartPosition;
      ifStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (IfToken)) return null;
      if (!skipToken (ParenthesisOpenToken)) return null;

      JavaExpression conditionExpression = parseJavaExpression ();
      if (conditionExpression == null) return null;
      if (!skipToken (ParenthesisCloseToken)) return null;

      JavaStatement trueStatement = parseJavaStatement ();
      if (trueStatement == null) return null;
      ifStatement.sourceEndPosition   = trueStatement.sourceEndPosition;

      JavaStatement falseStatement = null;
      if (scanner.token == ElseToken) {
         scanner.next ();
         falseStatement = parseJavaStatement ();
         if (falseStatement == null) return null;
         ifStatement.sourceEndPosition   = falseStatement.sourceEndPosition;
      }

      ifStatement.conditionExpression   = conditionExpression;
      ifStatement.trueStatement         = trueStatement;
      ifStatement.falseStatement        = falseStatement;
      return ifStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseSwitchStatement () {
      SwitchStatement switchStatement     = new SwitchStatement ();
      switchStatement.sourceStartPosition = scanner.tokenStartPosition;
      switchStatement.nameStartPosition   = scanner.tokenStartPosition;
      switchStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (SwitchToken)) return null;
      if (!skipToken (ParenthesisOpenToken)) return null;

      JavaExpression switchExpression = parseJavaExpression ();
      if (switchExpression == null) return null;
      if (!skipToken (ParenthesisCloseToken)) return null;
      switchStatement.switchExpression    = switchExpression;

      if (!skipToken (BraceOpenToken)) return null;
      enterStatementLexicalLevel ();
      boolean seenDefaultCase = false;
      while (   scanner.token != BraceCloseToken
             && scanner.token != EofToken) {

         /* The Java Spec allows (or rather, does not disallow) the presence
            of one or more statements AFTER the opening brace but BEFORE
            the first "case" or "default" label. Experimentation with javac
            has revealed that such statements are simply ignored without
            warning: they will never be reached during execution, not even
            in such degenerate cases as a switch statement without
            "case" or "default" labels AT ALL, like the following - -
            
               switch (1) { System.out.println ("This is never printed") }
         */

         switch (scanner.token) {

            case CaseToken:
                  CaseTag caseTag             = new CaseTag ();
                  caseTag.sourceStartPosition = scanner.tokenStartPosition;
                  caseTag.nameStartPosition   = scanner.tokenStartPosition;
                  caseTag.nameEndPosition     = scanner.tokenEndPosition;
                  if (!skipToken (CaseToken)) return null;

                  JavaExpression tagExpression = parseJavaExpression ();
                  if (tagExpression == null) return null;

                  caseTag.tagExpression        = tagExpression;
                  caseTag.sourceEndPosition    = scanner.tokenEndPosition;
                  if (!skipToken (ColonToken)) return null;
                  switchStatement.statements.add (caseTag);
                  break;

            case DefaultToken:
                  if (seenDefaultCase) {
                     parserError (2, "Duplicate \"default\" case");
                     // Non-fatal parser error
                  }
                  seenDefaultCase = true;

                  DefaultCaseTag defaultCaseTag      = new DefaultCaseTag ();
                  defaultCaseTag.sourceStartPosition = scanner.tokenStartPosition;
                  defaultCaseTag.nameStartPosition   = scanner.tokenStartPosition;
                  defaultCaseTag.nameEndPosition     = scanner.tokenEndPosition;
                  if (!skipToken (DefaultToken)) return null;

                  defaultCaseTag.sourceEndPosition   = scanner.tokenEndPosition;
                  if (!skipToken (ColonToken)) return null;
                  switchStatement.statements.add (defaultCaseTag);
                  break;

            default:
                  JavaStatement statement = parseJavaStatement ();
                  if (statement == null) return null;
                  switchStatement.statements.add (statement);
         }
      }

      exitStatementLexicalLevel ();
      switchStatement.sourceEndPosition   = scanner.tokenEndPosition;
      skipToken (BraceCloseToken); // Non-fatal parser error
      return switchStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseWhileStatement () {
      WhileStatement whileStatement      = new WhileStatement ();
      whileStatement.sourceStartPosition = scanner.tokenStartPosition;
      whileStatement.nameStartPosition   = scanner.tokenStartPosition;
      whileStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (WhileToken)) return null;
      if (!skipToken (ParenthesisOpenToken)) return null;

      JavaExpression conditionExpression = parseJavaExpression ();
      if (conditionExpression == null) return null;
      if (!skipToken (ParenthesisCloseToken)) return null;

      JavaStatement statement = parseJavaStatement ();
      if (statement == null) return null;
      whileStatement.sourceEndPosition   = statement.sourceEndPosition;

      whileStatement.conditionExpression   = conditionExpression;
      whileStatement.statement             = statement;
      return whileStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseDoStatement () {
      DoStatement doStatement         = new DoStatement ();
      doStatement.sourceStartPosition = scanner.tokenStartPosition;
      doStatement.nameStartPosition   = scanner.tokenStartPosition;
      doStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (DoToken)) return null;

      JavaStatement statement = parseJavaStatement ();
      if (statement == null) return null;

      if (!skipToken (WhileToken)) return null;
      if (!skipToken (ParenthesisOpenToken)) return null;

      JavaExpression conditionExpression = parseJavaExpression ();
      if (conditionExpression == null) return null;
      if (!skipToken (ParenthesisCloseToken)) return null;

      doStatement.sourceEndPosition     = scanner.tokenEndPosition;
      doStatement.conditionExpression   = conditionExpression;
      doStatement.statement             = statement;
      if (!skipStatementTerminator ()) return null;
      return doStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseForStatement () {
      ForStatement forStatement        = new ForStatement ();
      forStatement.sourceStartPosition = scanner.tokenStartPosition;
      forStatement.nameStartPosition   = scanner.tokenStartPosition;
      forStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (ForToken)) return null;
      if (!skipToken (ParenthesisOpenToken)) return null;

      if (scanner.token == SemicolonToken) {
         scanner.next ();
      } else {
         JavaStatement initStatement;
         boolean isFinal = false;
         if (scanner.token == FinalToken) {
            isFinal = true;
            scanner.next();
            switch (scanner.token) {
            case IdentifierToken:
            case BooleanToken:
            case CharToken:
            case ByteToken:
            case ShortToken:
            case IntToken:
            case LongToken:
            case FloatToken:
            case DoubleToken:  break; // these are OK
            default:
                    parserError (2, "A local variable is expected after 'final'");
            }
         }

         /* Code copied from the beginning of parseStatement (). Sorry. */
         switch (scanner.token) {
            case IdentifierToken:
                  initStatement = parseIdentifierLedStatement (true, false, false, isFinal);
                  break;

            case BooleanToken:
            case CharToken:
            case ByteToken:
            case ShortToken:
            case IntToken:
            case LongToken:
            case FloatToken:
            case DoubleToken:
               {
                  ScannerPosition position = scanner.getPosition();
                  DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
                  if (!parseDataType (dataTypeDeclaration, 
                                      "Type (loop variable declaration) expected",
                                      true,
                                      false,
                                      true)) return null;
                  if (!isFinal
                  &&  scanner.token == PeriodToken) {  // class literal: "int.class"
                      scanner.setPosition(position);
                      initStatement = parseExpressionStatement (true, true);
                  }
                  else {
                    initStatement = parseLocalVariableDeclarationStatement (dataTypeDeclaration, isFinal);
                  }
                  break;
               }
            case IncrementToken:
            case DecrementToken:
            case NewToken:
            case ThisToken:
            case SuperToken:
            case NullToken:
                  initStatement = parseExpressionStatement (true, false);
                  break;

            default:
                  parserError (2, "Expression statement expected");
                  return null;
         }

         if (initStatement == null) return null;
         forStatement.initStatements.add (initStatement);

         /* Init expression of a for loop is either a single
            LocalVariableDeclarationStatement or any number of 
            ExpressionStatements separated by commas. */
         if (initStatement.languageConstructCode () == ExpressionStatementCode) {
            while (scanner.token == CommaToken) {
               scanner.next ();
               initStatement = parseExpressionStatement (true, false);
               if (initStatement == null) return null;
               forStatement.initStatements.add (initStatement);
            }
          //int it [] = {CommaToken, SemicolonToken};
          //if (!expectTokens (ct)) return null;
            if (!skipToken (SemicolonToken)) return null;
         }
      }

      JavaExpression conditionExpression = null;
      if (scanner.token == SemicolonToken) {
         scanner.next ();
      } else {
         conditionExpression = parseJavaExpression ();
         if (conditionExpression == null) return null;
         if (!skipToken (SemicolonToken)) return null;
      }

      if (scanner.token == ParenthesisCloseToken) {
         scanner.next ();
      } else {
         do {
            JavaStatement loopStatement = parseExpressionStatement (true, false);
            if (loopStatement == null) return null;
            forStatement.loopStatements.add (loopStatement);
            if (scanner.token != CommaToken) break;
            scanner.next ();
         } while (true);
         if (!skipToken (ParenthesisCloseToken)) return null;
      }

      JavaStatement statement = parseJavaStatement ();
      if (statement == null) return null;
      forStatement.sourceEndPosition  = statement.sourceEndPosition;

      forStatement.conditionExpression     = conditionExpression;
      forStatement.statement          = statement;
      return forStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseBreakStatement () {
      BreakStatement breakStatement      = new BreakStatement ();
      breakStatement.sourceStartPosition = scanner.tokenStartPosition;
      breakStatement.nameStartPosition   = scanner.tokenStartPosition;
      breakStatement.nameEndPosition     = scanner.tokenEndPosition;

      if (!skipToken (BreakToken)) return null;
      if (scanner.token == IdentifierToken) {
         breakStatement.nameStartPosition   = scanner.tokenStartPosition;
         breakStatement.nameEndPosition     = scanner.tokenEndPosition;
         breakStatement.name                = (String)scanner.tokenValue;
         breakStatement.hasTargetLabel(true);
         scanner.next ();
      }

      breakStatement.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipStatementTerminator ()) return null;
      return breakStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseContinueStatement () {
      ContinueStatement continueStatement   = new ContinueStatement ();
      continueStatement.sourceStartPosition = scanner.tokenStartPosition;
      continueStatement.nameStartPosition   = scanner.tokenStartPosition;
      continueStatement.nameEndPosition     = scanner.tokenEndPosition;

      if (!skipToken (ContinueToken)) return null;
      if (scanner.token == IdentifierToken) {
         continueStatement.nameStartPosition = scanner.tokenStartPosition;
         continueStatement.nameEndPosition   = scanner.tokenEndPosition;
         continueStatement.name              = (String)scanner.tokenValue;
         continueStatement.hasTargetLabel(true);
         scanner.next ();
      }

      continueStatement.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipStatementTerminator ()) return null;
      return continueStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseReturnStatement () {
      ReturnStatement returnStatement     = new ReturnStatement ();
      returnStatement.sourceStartPosition = scanner.tokenStartPosition;
      returnStatement.nameStartPosition   = scanner.tokenStartPosition;
      returnStatement.nameEndPosition     = scanner.tokenEndPosition;

      if (!skipToken (ReturnToken)) return null;
      if (!testStatementTerminator ()) {
         JavaExpression returnExpression = parseJavaExpression ();
         if (returnExpression == null) return null;
         returnStatement.returnExpression = returnExpression;
      }

      returnStatement.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipStatementTerminator ()) return null;
      return returnStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseThrowStatement () {
      ThrowStatement throwStatement      = new ThrowStatement ();
      throwStatement.sourceStartPosition = scanner.tokenStartPosition;
      throwStatement.nameStartPosition   = scanner.tokenStartPosition;
      throwStatement.nameEndPosition     = scanner.tokenEndPosition;

      if (!skipToken (ThrowToken)) return null;
      JavaExpression throwExpression = parseJavaExpression ();
      if (throwExpression == null) return null;

      throwStatement.throwExpression     = throwExpression;
      throwStatement.sourceEndPosition   = scanner.tokenEndPosition;
      if (!skipStatementTerminator ()) return null;
      return throwStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseSynchronizedStatement () {
      SynchronizedStatement synchronizedStatement = new SynchronizedStatement ();
      synchronizedStatement.sourceStartPosition = scanner.tokenStartPosition;
      synchronizedStatement.nameStartPosition   = scanner.tokenStartPosition;
      synchronizedStatement.nameEndPosition     = scanner.tokenEndPosition;

      if (!skipToken (SynchronizedToken)) return null;
      if (!skipToken (ParenthesisOpenToken)) return null;

      synchronizedStatement.synchronizedExpression = parseJavaExpression ();
      if (synchronizedStatement.synchronizedExpression == null) return null;
      if (!skipToken (ParenthesisCloseToken)) return null;

      return parseNestedStatement (synchronizedStatement);

      // if we want to set the sourceEndPosition to '}':
      //
      // synchronizedStatement = parseNestedStatement (synchronizedStatement);
      // if (synchronizedStatement == null) return null;
      // synchronizedStatement.sourceEndPosition   = statement.sourceEndPosition; 
      // return synchronizedStatement;
   }

   /*-----------------------------------------------------------------*/

   protected JavaStatement parseTryStatement () {
      TryStatement tryStatement        = new TryStatement ();
      tryStatement.sourceStartPosition = scanner.tokenStartPosition;
      tryStatement.nameStartPosition   = scanner.tokenStartPosition;
      tryStatement.nameEndPosition     = scanner.tokenEndPosition;
      if (!skipToken (TryToken)) return null;

      enterStatementLexicalLevel ();

      tryStatement.tryBlock                     = new TryBlock ();
      tryStatement.tryBlock.tryStatement        = tryStatement;
      tryStatement.tryBlock.sourceStartPosition = scanner.tokenStartPosition;
      tryStatement.tryBlock.nameStartPosition   = scanner.tokenStartPosition;
      tryStatement.tryBlock.nameEndPosition     = scanner.tokenEndPosition;

      if (parseNestedStatement (tryStatement.tryBlock)==null) return null;
      
      boolean seenAnyCatches = false;
      int seenCatches = 0; // JIT misinterprets seenAnyCatches, so here's another variable....
      while (scanner.token == CatchToken) {
        seenCatches++;
         boolean seenFinal = false;
         CatchBlock catchBlock          = new CatchBlock ();
         catchBlock.tryStatement        = tryStatement;
         catchBlock.sourceStartPosition = scanner.tokenStartPosition;
         seenAnyCatches = true;
         scanner.next ();

         if (!skipToken (ParenthesisOpenToken)) return null;
         if (scanner.token == FinalToken) {
            seenFinal = true;
            scanner.next ();
         }
         if (scanner.token != IdentifierToken) {
            parserError (2, "Identifier (exception class or interface name) expected");
            return null;
         }
         DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
         if (!parseDataType (dataTypeDeclaration, 
                             "Exception class or interface name expected",
                             false,
                             false,
                             false)) return null;

         catchBlock.nameStartPosition   = dataTypeDeclaration.nameStartPosition;
         catchBlock.nameEndPosition     = dataTypeDeclaration.nameEndPosition;
         catchBlock.name                = dataTypeDeclaration.name;

         if (scanner.token != IdentifierToken) {
            parserError (2, "Identifier (exception variable name) expected");
            return null;
         }
         LocalVariableDeclaration catchVariable = new LocalVariableDeclaration ();
         catchVariable.sourceStartPosition = scanner.tokenStartPosition;
         catchVariable.sourceEndPosition   = scanner.tokenEndPosition;
         catchVariable.nameStartPosition   = scanner.tokenStartPosition;
         catchVariable.nameEndPosition     = scanner.tokenEndPosition;
         catchVariable.name                = (String)scanner.tokenValue;
         catchVariable.dataTypeDeclaration = dataTypeDeclaration;
         catchBlock.catchVariable          = catchVariable;
         if (seenFinal) catchVariable.isFinal = true;

         scanner.next ();
         if (!skipToken (ParenthesisCloseToken)) return null;

         if (parseNestedStatement (catchBlock)==null) return null;

         tryStatement.sourceEndPosition  = scanner.previousTokenEndPosition;
         catchBlock  .sourceEndPosition  = scanner.previousTokenEndPosition;

         tryStatement.catches.add (catchBlock);
      }

      boolean seenFinally = false;
      if (scanner.token == FinallyToken) {
         seenFinally = true;
         scanner.next ();
         if (!skipToken (BraceOpenToken)) return null;

         while (   scanner.token != BraceCloseToken
                && scanner.token != EofToken) {
            JavaStatement finalStatement = parseJavaStatement ();
            if (finalStatement == null) return null;
            tryStatement.finalStatements.add (finalStatement);
         }
         tryStatement.sourceEndPosition  = scanner.tokenEndPosition;
         skipToken (BraceCloseToken); // Non-fatal parser error
      }

      exitStatementLexicalLevel ();

      if (   !seenAnyCatches
          &&  seenCatches == 0
          && !seenFinally) {
         parserError (2, "\"catch\" or \"finally\" expected");
         return null;
      }
      return tryStatement;
   }



   /*******************************************************************/
   /**                                                               **/
   /**                         EXPRESSIONS                           **/
   /**                                                               **/
   /*******************************************************************/
 
   protected JavaExpression parseJavaExpression () {
      return parseAssignmentExpression ();
   }

   /*-----------------------------------------------------------------*/

   /* Parse an assignment. Note that assignment operators
      have a different associativity than the other binary operators! */

   protected JavaExpression parseAssignmentExpression () {

      ScannerPosition leftPosition = scanner.getPosition ();
      JavaExpression leftExpression = parseConditionalExpression ();
      if (leftExpression == null) return null;

      /* The range 'FirstAssignmentToken' to 'LastAssignmentToken'
         includes any bits denoting the user-defined operators.
         Don't use the 'scanner.token & BasicOperatorMask' stuff. */
      if (   scanner.token < FirstAssignmentToken
          || scanner.token > LastAssignmentToken)
         return leftExpression;

      return parseRestOfAssignmentExpression (leftExpression, leftPosition);
   }
      
   protected JavaExpression parseRestOfAssignmentExpression (JavaExpression leftExpression,
                                                             ScannerPosition leftPosition) {

      // this test is in general impossible here, since more information is required...
      //if (!leftExpression.canBeAssigned()) {
      //   parserError (2, "Invalid left hand side of assignment", 
      //                leftExpression.sourceStartPosition, leftExpression.sourceEndPosition);
         //Non-fatal parser error
         //return null;
      //}

      AssignmentExpression expression = new AssignmentExpression ();
      expression.sourceStartPosition = leftExpression.sourceStartPosition;
      expression.nameStartPosition   = scanner.tokenStartPosition;
      expression.nameEndPosition     = scanner.tokenEndPosition;
      expression.name                = Scanner.tokenRepresentation (scanner.token);
      expression.operatorToken       = scanner.token;
      scanner.next ();

      JavaExpression rightExpression = parseAssignmentExpression ();
      if (rightExpression == null) return null;

      expression.leftExpression      = leftExpression;
      expression.rightExpression     = rightExpression;
      expression.sourceEndPosition   = rightExpression.sourceEndPosition;
      return expression;
   }

   /*-----------------------------------------------------------------*/

   /* Parse the ? : operator. Note that it has a different associativity
      than the non-assignment binary operators! */

   protected JavaExpression parseConditionalExpression () {
   
      JavaExpression conditionExpression = parseConditionalOrExpression (null);
      if (conditionExpression == null) return null;
      if (scanner.token != QuestionToken) 
    	  return conditionExpression;

      return parseRestOfConditionalExpression (conditionExpression);
   }

   /* This method is provided as a hook for the ScripticParser0 */

   protected JavaExpression parseRestOfConditionalExpression (JavaExpression conditionExpression) {

      ConditionalExpression expression = new ConditionalExpression ();
      expression.sourceStartPosition = conditionExpression.sourceStartPosition;
      expression.nameStartPosition   = scanner.tokenStartPosition;
      expression.nameEndPosition     = scanner.tokenEndPosition;
      expression.name                = "? :";
      expression.conditionExpression = conditionExpression;
      scanner.next ();

      JavaExpression trueExpression = parseJavaExpression ();
      if (trueExpression == null) return null;
      if (!skipToken (ColonToken)) return null;

      JavaExpression falseExpression = parseConditionalExpression ();
      if (falseExpression == null) return null;

      expression.trueExpression      = trueExpression;
      expression.falseExpression     = falseExpression;
      expression.sourceEndPosition   = falseExpression.sourceEndPosition;
      return expression;
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseConditionalOrExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseConditionalAndExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseConditionalAndExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (scanner.token != BooleanOrToken)
         return expression;
      else
         return parseConditionalOrExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseConditionalAndExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseInclusiveOrExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseInclusiveOrExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (scanner.token != BooleanAndToken)
         return expression;
      else
         return parseConditionalAndExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseInclusiveOrExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseExclusiveOrExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseExclusiveOrExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (scanner.token != VerticalBarToken)
         return expression;
      else
         return parseInclusiveOrExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseExclusiveOrExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseAndExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseAndExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (scanner.token != CaretToken)
         return expression;
      else
         return parseExclusiveOrExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseAndExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseEqualityExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseEqualityExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (scanner.token != AmpersandToken)
         return expression;
      else
         return parseAndExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseEqualityExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseRelationalExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseRelationalExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (   scanner.token != EqualsToken
          && scanner.token != NotEqualToken)
         return expression;
      else
         return parseEqualityExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseRelationalExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseShiftExpression (null);
      else {
         /* Additional terms */
         if (scanner.token == InstanceofToken) {

            /* The syntax of the "instanceof" operator is slightly different */
            scanner.next ();

            /* Parse type expression */
            DataTypeDeclaration compareTypeDeclaration = new DataTypeDeclaration ();
            if (!parseDataType (compareTypeDeclaration, 
                                "Type (for instanceof) expected",
                                true,
                                true,
                                true)) return null;

            /* dataTypeDeclaration may not be a primitive type */
            /* To be checked? (Seems not essential for our current purposes) */

            TypeComparisonExpression typeComparisonExpression = new TypeComparisonExpression ();
            typeComparisonExpression.sourceStartPosition = leftExpression.sourceStartPosition;
            typeComparisonExpression.sourceEndPosition   = compareTypeDeclaration.sourceEndPosition;
            typeComparisonExpression.nameStartPosition   = compareTypeDeclaration.nameStartPosition;
            typeComparisonExpression.nameEndPosition     = compareTypeDeclaration.nameEndPosition;
            typeComparisonExpression.name                = compareTypeDeclaration.name;
            typeComparisonExpression.compareTypeDeclaration = compareTypeDeclaration;
            typeComparisonExpression.relationalExpression = leftExpression;
            expression = typeComparisonExpression;
         } else {

            /* "Ordinary" relational operator */
            ScannerPosition operatorPosition = scanner.getPosition ();
            scanner.next ();
            JavaExpression rightExpression = parseShiftExpression (null);
            if (rightExpression == null) return null;
            expression = constructBinaryExpression (leftExpression,
                                                    operatorPosition,
                                                    rightExpression); 
         }
      }
      if (expression == null) return null;

      if (   scanner.token != GreaterThanToken
          && scanner.token != LessThanToken
          && scanner.token != GreaterOrEqualToken
          && scanner.token != LessOrEqualToken
          && scanner.token != InstanceofToken)
         return expression;
      else
         return parseRelationalExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseShiftExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseAdditiveExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseAdditiveExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (   scanner.token != LeftShiftToken
          && scanner.token != RightShiftToken
          && scanner.token != UnsignedRightShiftToken)
         return expression;
      else
         return parseShiftExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseAdditiveExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseMultiplicativeExpression (null);
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseMultiplicativeExpression (null);
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (   scanner.token != PlusToken
          && scanner.token != MinusToken)
         return expression;
      else
         return parseAdditiveExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parseMultiplicativeExpression (JavaExpression leftExpression) {
      JavaExpression expression;

      if (leftExpression == null)
         /* First term */
         expression = parseUnaryExpression ();
      else {
         /* Additional terms */
         ScannerPosition operatorPosition = scanner.getPosition ();
         scanner.next ();
         JavaExpression rightExpression = parseUnaryExpression ();
         if (rightExpression == null) return null;
         expression = constructBinaryExpression (leftExpression,
                                                 operatorPosition,
                                                 rightExpression); 
      }
      if (expression == null) return null;

      if (   scanner.token != AsteriskToken
          && scanner.token != SlashToken
          && scanner.token != PercentToken)
         return expression;
      else
         return parseMultiplicativeExpression (expression);
   }

   /*-----------------------------------------------------------------*/

   /* Construct a binary expression. */

   protected JavaExpression constructBinaryExpression (JavaExpression leftExpression,
                                                   ScannerPosition operatorPosition,
                                                   JavaExpression rightExpression) {

      BinaryExpression expression    = new BinaryExpression ();
      expression.sourceStartPosition = leftExpression.sourceStartPosition;
      expression.sourceEndPosition   = rightExpression.sourceEndPosition;
      expression.nameStartPosition   = operatorPosition.tokenStartPosition;
      expression.nameEndPosition     = operatorPosition.tokenEndPosition;
      expression.name                = Scanner.tokenRepresentation (operatorPosition.token);

      expression.operatorToken       = operatorPosition.token;
      expression.leftExpression      = leftExpression;
      expression.rightExpression     = rightExpression;
      return expression;
   }


   /*******************************************************************/
   /**                                                               **/
   /**                      UNARY EXPRESSION                         **/
   /**                                                               **/
   /*******************************************************************/

   protected JavaExpression parseUnaryExpression () {

      if (   scanner.token == TildeToken
          || scanner.token == ExclamationToken
          || scanner.token == IncrementToken
          || scanner.token == DecrementToken
          || scanner.token == PlusToken
          || scanner.token == MinusToken) {

         UnaryExpression expression     = new UnaryExpression ();
         expression.sourceStartPosition = scanner.tokenStartPosition;
         expression.sourceEndPosition   = scanner.tokenEndPosition;
         expression.nameStartPosition   = scanner.tokenStartPosition;
         expression.nameEndPosition     = scanner.tokenEndPosition;
         expression.name                = Scanner.tokenRepresentation (scanner.token);
         expression.unaryToken          = scanner.token;

         scanner.next ();
         JavaExpression primaryExpression;
         if (   scanner.token == IncrementToken
             || scanner.token == DecrementToken)
            primaryExpression = parsePrimaryExpression ();
         else
            primaryExpression = parseUnaryExpression ();

         if (primaryExpression == null) return null;
         expression.primaryExpression = primaryExpression;
         expression.sourceEndPosition = primaryExpression.sourceEndPosition;
         return expression;
      }

      /* Remaining cases: PostfixExpression, CastExpression */

      if (scanner.token != ParenthesisOpenToken) {
         /* Can't be a cast expression */
         return parsePostfixExpression ();
      }
         
      ScannerPosition parenthesisPosition = scanner.getPosition ();
      scanner.next ();

      ScannerPosition typeKeywordPosition = scanner.getPosition ();
      boolean hasTypeKeyword = scanner.token == BooleanToken
                            || scanner.token == CharToken
                            || scanner.token == ByteToken
                            || scanner.token == ShortToken
                            || scanner.token == IntToken
                            || scanner.token == LongToken
                            || scanner.token == FloatToken
                            || scanner.token == DoubleToken;

      /* First check for a cast to a primitive type */
      if (hasTypeKeyword) {

         /* Tentative primitive cast. Could still turn out to be a general
            cast, e.g. "(char []) c" */

         PrimitiveCastExpression castExpression = new PrimitiveCastExpression ();
         castExpression.sourceStartPosition = parenthesisPosition.tokenStartPosition;
         castExpression.nameStartPosition   = scanner.tokenStartPosition;
         castExpression.nameEndPosition     = scanner.tokenEndPosition;
         castExpression.name                = Scanner.tokenRepresentation (scanner.token);
         castExpression.castToken           = scanner.token;
         scanner.next ();
         
         if (scanner.token == ParenthesisCloseToken) {
         
            /* Primitive cast, for sure */
            scanner.next ();

            JavaExpression unaryExpression = parseUnaryExpression ();
            if (unaryExpression == null) return null;
            castExpression.unaryExpression   = unaryExpression;
            castExpression.sourceEndPosition = unaryExpression.sourceEndPosition;
            return castExpression;
         }

         /* It was not a primitive cast. Try something else. */

         scanner.setPosition (typeKeywordPosition);
      }

      /* Check for a general type cast */
      if (hasTypeKeyword || scanner.token == IdentifierToken) {

         /* Parse type expression */
         DataTypeDeclaration castTypeDeclaration = new DataTypeDeclaration ();
         if (!parseDataType (castTypeDeclaration, 
                             "Type (cast expression) expected",
                             false,
                             false,
                             true)) return null;

         /* Check for empty bracket sets "[ ]". If there are brackets
            but there's something in between, it's not a cast. */

         if (scanner.token == BracketOpenToken) {
            scanner.next ();
            if (scanner.token != BracketCloseToken) {

               /* There was something between the brackets. 
                  I say this looks more like an array access expression. */
               scanner.setPosition (parenthesisPosition);
               return parsePostfixExpression ();
            }
            castTypeDeclaration.noOfArrayDimensions++;
            castTypeDeclaration.sourceEndPosition = scanner.tokenEndPosition;
            scanner.next ();
         }            

         /* Parse the rest of the empty bracket pairs. These must be empty. */
         while (scanner.token == BracketOpenToken) {
            if (!nextToken (BracketCloseToken)) return null;
            castTypeDeclaration.noOfArrayDimensions++;
            castTypeDeclaration.sourceEndPosition = scanner.tokenEndPosition;
            scanner.next ();
         }

         CastExpression castExpression      = new CastExpression ();
         castExpression.sourceStartPosition = parenthesisPosition.tokenStartPosition;
         castExpression.nameStartPosition   = castTypeDeclaration.nameStartPosition;
         castExpression.nameEndPosition     = castTypeDeclaration.nameEndPosition;
         castExpression.name                = castTypeDeclaration.name;
         castExpression.castTypeDeclaration = castTypeDeclaration;

         if (hasTypeKeyword 
         ||  castTypeDeclaration.isArray()) {
            /* Require proper cast syntax */
            if (!expectToken (ParenthesisCloseToken)) return null;
         }

         if (scanner.token == ParenthesisCloseToken) {
            scanner.next ();

            /* Check all legal prefixes of an UnaryExpressionNotPlusMinus.
               If the next token is such a prefix, assume it's a cast,
               e.g. "(a)expr". Otherwise, assume it's a NestedExpression, 
               e.g. "(a).b or (a) + b". */

              if ( scanner.token == TildeToken
                || scanner.token == ExclamationToken

                /* See PrimaryExpression */
                || scanner.token == NewToken
                || (   scanner.token >= FirstLiteralToken
                    && scanner.token <= LastLiteralToken)
                || scanner.token == ParenthesisOpenToken
                || scanner.token == ThisToken
                || scanner.token == SuperToken
                || scanner.token == NullToken
                || scanner.token == IdentifierToken) {

               JavaExpression unaryExpression = parseUnaryExpression ();
               if (unaryExpression == null) return null;
               castExpression.unaryExpression   = unaryExpression;
               castExpression.sourceEndPosition = unaryExpression.sourceEndPosition;

               return castExpression;

            } else {
               if (hasTypeKeyword
               ||  castTypeDeclaration.isArray()) {
                  /* Should definitely have been a cast, (type)expr */
                  if (   scanner.token == PlusToken
                      || scanner.token == MinusToken
                      || scanner.token == IncrementToken
                      || scanner.token == DecrementToken) {
                     parserError (2,   "Cast \"("
                                  + castTypeDeclaration.getPresentation() 
                                  + ")\" cannot be followed by a \""
                                  + Scanner.tokenRepresentation (scanner.token)
                                  + "\"");
                  } else {
                     parserError (2,   "Expression (cast to \""
                                  + castTypeDeclaration.getPresentation() 
                                  + "\") expected");
                  }
                  return null;
               }
            }
         }
      }

      /* Check for a dimension cast */
      if (scanner.token == DimensionToken) {
          DimensionCastExpression dimensionCastExpression = new DimensionCastExpression ();
          dimensionCastExpression.sourceStartPosition     = scanner.tokenStartPosition;
          dimensionCastExpression.nameStartPosition       = scanner.tokenStartPosition;
          dimensionCastExpression.nameEndPosition         = scanner.tokenEndPosition;
          dimensionCastExpression.compoundDimensionDeclaration = new CompoundDimensionDeclaration();
          dimensionCastExpression.compoundDimensionDeclaration.sourceStartPosition = scanner.tokenStartPosition;
          dimensionCastExpression.compoundDimensionDeclaration.nameStartPosition   = scanner.tokenStartPosition;
          dimensionCastExpression.compoundDimensionDeclaration.  nameEndPosition   = scanner.tokenEndPosition;
          scanner.next();
          if (scanner.token == ParenthesisOpenToken) {
              // (dimension (expr)) is shorthand for (dimension dimension(expr))
              scanner.next();
              JavaExpression javaExpression = parseJavaExpression();
              DimensionReference dimensionReference  = new RelativeDimensionReference(javaExpression);
              dimensionReference.sourceStartPosition = javaExpression.sourceStartPosition;
              dimensionReference.sourceEndPosition   = javaExpression.sourceEndPosition;
              dimensionReference.nameStartPosition   = javaExpression.nameStartPosition;
              dimensionReference.nameEndPosition     = javaExpression.nameEndPosition;
              dimensionCastExpression.compoundDimensionDeclaration.addNormal (dimensionReference);
              if (!expectToken (ParenthesisCloseToken)) {
                  return null;
              }
              scanner.next();
          }
          else {
            if (!parseCompoundDimensionExpression (dimensionCastExpression.compoundDimensionDeclaration)) return null;
          }
          if (!expectToken (ParenthesisCloseToken)) {
              return null;
          }
          scanner.next();
          dimensionCastExpression.unaryExpression = parseUnaryExpression ();
          if (dimensionCastExpression.unaryExpression == null) return null;
          dimensionCastExpression.sourceEndPosition = dimensionCastExpression.unaryExpression.sourceEndPosition;

          return dimensionCastExpression;
      }

      /* Not a recognizable cast expression. */
      scanner.setPosition (parenthesisPosition);
      return parsePostfixExpression ();
   }

   /*-----------------------------------------------------------------*/

   protected JavaExpression parsePostfixExpression () {
      JavaExpression primaryExpression = parsePrimaryExpression ();

      if (   scanner.token != IncrementToken
          && scanner.token != DecrementToken)
         return primaryExpression;

      PostfixExpression expression   = new PostfixExpression ();
      expression.sourceStartPosition = primaryExpression.sourceStartPosition;
      expression.sourceEndPosition   = scanner.tokenEndPosition;
      expression.nameStartPosition   = scanner.tokenStartPosition;
      expression.nameEndPosition     = scanner.tokenEndPosition;
      expression.name                = Scanner.tokenRepresentation (scanner.token);
      expression.unaryToken          = scanner.token;
      expression.primaryExpression   = primaryExpression;

      scanner.next ();
      return expression;
   }


   /*******************************************************************/
   /**                                                               **/
   /**                      PRIMARY EXPRESSION                       **/
   /**                                                               **/
   /*******************************************************************/

   protected JavaExpression parsePrimaryExpression () {

      switch (scanner.token) {
        case NewToken: return parse_Field_Access (parseAllocationExpression (null));

        case BooleanToken:
        case CharToken:
        case ByteToken:
        case ShortToken:
        case IntToken:
        case LongToken:
        case FloatToken:
        case DoubleToken:
             {                 // "int.class" etc
                int primitiveToken = scanner.token;
                scanner.next();
                if (scanner.token == PeriodToken) {
                  scanner.next();
                  if (expectToken (ClassToken)) {
                     scanner.next();
                     return parse_Field_Access (new ClassLiteralExpression (primitiveToken));
                  }
                  return null;
                }
                else {
                  expectToken (PeriodToken);
                  return null;
                }
             }
        case   IntegerLiteralToken:
        case      LongLiteralToken:
        case     FloatLiteralToken:
        case    DoubleLiteralToken:
        case    StringLiteralToken:
        case CharacterLiteralToken:
        case   BooleanLiteralToken:
        case      ByteLiteralToken:
        case     ShortLiteralToken:
          {  LiteralExpression literalExpression = LiteralExpression.makeNew (env, scanner.token, scanner.tokenValue);
             literalExpression.sourceStartPosition = scanner.tokenStartPosition;
             literalExpression.sourceEndPosition   = scanner.tokenEndPosition;
             literalExpression.nameStartPosition   = scanner.tokenStartPosition;
             literalExpression.nameEndPosition     = scanner.tokenEndPosition;
             literalExpression.name                = Scanner.tokenRepresentation (scanner.token);

             scanner.next ();
             return parse_FieldArray_Access (literalExpression);
          }


        case ParenthesisOpenToken:     /* "(" Expression ")" */
          {
             NestedJavaExpression nestedExpression = new NestedJavaExpression ();
             nestedExpression.sourceStartPosition = scanner.tokenStartPosition;
             nestedExpression.sourceEndPosition   = scanner.tokenEndPosition;
             nestedExpression.nameStartPosition   = scanner.tokenStartPosition;
             nestedExpression.nameEndPosition     = scanner.tokenEndPosition;
             nestedExpression.name                = Scanner.tokenRepresentation (scanner.token);

             scanner.next ();
             JavaExpression subExpression = parseJavaExpression ();
             if (subExpression == null) return null;
             nestedExpression.subExpression = subExpression;
             nestedExpression.sourceEndPosition = scanner.tokenEndPosition;
             if (!skipToken (ParenthesisCloseToken)) return null;
             return parse_FieldArray_Access (nestedExpression);
          }

        case  ThisToken:
        case SuperToken:
        case  NullToken:
          {
             SpecialNameExpression specialNameExpression = new SpecialNameExpression (scanner.token);
             specialNameExpression.sourceStartPosition = scanner.tokenStartPosition;
             specialNameExpression.sourceEndPosition   = scanner.tokenEndPosition;
             specialNameExpression.nameStartPosition   = scanner.tokenStartPosition;
             specialNameExpression.nameEndPosition     = scanner.tokenEndPosition;
             specialNameExpression.name                = Scanner.tokenRepresentation (scanner.token);
    
             scanner.next ();
             return parse_FieldArrayMethod_Access (specialNameExpression);
             /* OK, I understand that "this()" and "super()" are
                constructor calls... but shouldn't we raise a complaint
                about "null()"?????
             */
          }

        case IdentifierToken:     /* Qualified name */
          {
             NameExpression nameExpression      = new NameExpression ();
             nameExpression.sourceStartPosition = scanner.tokenStartPosition;
             nameExpression.sourceEndPosition   = scanner.tokenEndPosition;
             nameExpression.nameStartPosition   = scanner.tokenStartPosition;
             nameExpression.nameEndPosition     = scanner.tokenEndPosition;
           //nameExpression.name                = scanner.tokenRepresentation (scanner.token);
             nameExpression.name                = (String) scanner.tokenValue;

             scanner.next (); // instead of parseCompoundName...
             //if (!parseCompoundName (nameExpression)) return null; obsolete...
             //nameExpression.sourceEndPosition = nameExpression.nameEndPosition;
             JavaExpression result = parse_NameFieldArrayMethod_Access (nameExpression);
             return result;
          }

        case ErrorToken: parserError (2, (String) scanner.tokenValue); return null;
      }
      parserError (2, "Expression expected");
      return null;
   }

   /*-----------------------------------------------------------------*/

   /* This method is provided as a hook for the Scriptic parser */

   protected JavaExpression parse_NameFieldArrayMethod_Access (NameExpression nameExpression) {
      return parse_FieldArrayMethod_Access (nameExpression);
   }

   /*-----------------------------------------------------------------*/

   /* Check if the primary expression is followed by a "(", "[" or ".".
      If so, it's a method call, array access, or field access, respectively.
      See the Java Syntax Specification, chapter 9, for where this is legal. */

   protected JavaExpression parse_FieldArrayMethod_Access (JavaExpressionWithTarget primaryExpression) {
      if (primaryExpression == null) return null;
      if (scanner.token == ParenthesisOpenToken)
              return parseMethodCall (primaryExpression);
      return parse_FieldArray_Access (primaryExpression);
   }

   /*-----------------------------------------------------------------*/

   /* Check if the primary expression is followed by a "[" or ".".
      If so, it's an array access or field access, respectively.
      See the Java Syntax Specification, chapter 9, for where this is legal. */

   protected JavaExpression parse_FieldArray_Access (JavaExpression primaryExpression) {
      if (primaryExpression == null) return null;
      if (scanner.token == BracketOpenToken)
         return parseArrayAccess (primaryExpression);
      return  parse_Field_Access (primaryExpression);
   }

   /*-----------------------------------------------------------------*/

   /* Check if the primary expression is followed by a ".".
      If so, it's a field access (which can in turn be a method access).
      See the Java Syntax Specification, chapter 9, for where this is legal. */

   protected JavaExpression parse_Field_Access (JavaExpression primaryExpression) {
      if (primaryExpression == null) return null;
      if (scanner.token == PeriodToken)
         return parseFieldAccess (primaryExpression);
      return primaryExpression;
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                          FieldAccess                            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   /* FieldAccess == PrimaryExpression "." Identifier */

   protected JavaExpression parseFieldAccess (JavaExpression primaryExpression) {
      if (primaryExpression == null) return null;
      if (scanner.token != PeriodToken) return primaryExpression;

      /* Period must be followed by an identifier OR "class" */
      ScannerPosition periodPosition = scanner.getPosition ();
      scanner.next();
      switch (scanner.token) {
        case NewToken:
             return parse_Field_Access (parseAllocationExpression (primaryExpression));

        case ClassToken:
             scanner.next();
             return parse_Field_Access (new ClassLiteralExpression (primaryExpression));

        case ThisToken:  // "EnclosingClass.this"
             scanner.next();
             return parse_Field_Access (new QualifiedThisExpression (primaryExpression));

        case SuperToken:  // "enclosingInstance.super (...)"
             scanner.next();
             return parseMethodCall (new QualifiedSuperExpression (primaryExpression));

        case IdentifierToken:
             FieldAccessExpression expression = new FieldAccessExpression ();
             expression.sourceStartPosition = primaryExpression.sourceStartPosition;
             expression.sourceEndPosition   = scanner.tokenEndPosition;
             expression.nameStartPosition   = scanner.tokenStartPosition;
             expression.nameEndPosition     = scanner.tokenEndPosition;
             expression.name                = (String)scanner.tokenValue;

             expression.primaryExpression   = primaryExpression;

             if (primaryExpression.isQualifiedName()) {
                 expression.qualifiedName = primaryExpression.qualifiedName()
                                          + '.' + expression.name;
             }
             scanner.next ();
             return parse_FieldArrayMethod_Access (expression);

        default: 
             scanner.setPosition (periodPosition);
             return primaryExpression;
      }

   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                          ArrayAccess                            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   /* ArrayAccess == ComplexPrimary "[" Expression "]" ... */

   protected JavaExpression parseArrayAccess (JavaExpression primaryExpression) {
      if (primaryExpression == null) return null;
      if (scanner.token != BracketOpenToken) return primaryExpression;

      ArrayAccessExpression arrayAccess  = new ArrayAccessExpression ();
      arrayAccess.sourceStartPosition    = primaryExpression.sourceStartPosition;
      arrayAccess.sourceEndPosition      = primaryExpression.sourceEndPosition;
      arrayAccess.nameStartPosition      = primaryExpression.nameStartPosition;
      arrayAccess.nameEndPosition        = primaryExpression.nameEndPosition;
      arrayAccess.name                   = primaryExpression.name;
      arrayAccess.primaryExpression      = primaryExpression;

      while (scanner.token == BracketOpenToken) {
         scanner.next ();
         JavaExpression indexExpression = parseJavaExpression ();
         if (indexExpression == null) return null;
         
         arrayAccess.indexExpressions.add(indexExpression);
         arrayAccess.sourceEndPosition = scanner.tokenEndPosition;
         if (!skipToken (BracketCloseToken)) return null;
      }

      return parse_Field_Access (arrayAccess);
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                           MethodCall                            */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   /* MethodCall == MethodAccess "(" ArgumentList ")" */

   protected JavaExpression parseMethodCall (JavaExpressionWithTarget primaryExpression) {

      /* primaryExpression must be a valid MethodAccess
         (a (Special)NameExpression or a FieldAccessExpression) */

      if (primaryExpression == null) return null;
      if (scanner.token != ParenthesisOpenToken) return primaryExpression;

      MethodCallExpression methodCall   = new MethodCallExpression ();
      methodCall.sourceStartPosition    = primaryExpression.sourceStartPosition;
      methodCall.sourceEndPosition      = primaryExpression.sourceEndPosition;
      methodCall.nameStartPosition      = primaryExpression.nameStartPosition;
      methodCall.nameEndPosition        = primaryExpression.nameEndPosition;
      methodCall.name                   = primaryExpression.name;
      methodCall.methodAccessExpression = primaryExpression;

      MethodCallParameterList parameterList = new MethodCallParameterList ();
      parameterList.sourceStartPosition = scanner.tokenStartPosition;
      parameterList.sourceEndPosition   = scanner.tokenEndPosition;
      parameterList.nameStartPosition   = methodCall.nameStartPosition;
      parameterList.nameEndPosition     = methodCall.nameEndPosition;
      parameterList.name                = methodCall.name;
      methodCall.parameterList          = parameterList;

      scanner.next ();
      if (scanner.token != ParenthesisCloseToken) {
         do {
            JavaExpression argument = parseJavaExpression ();
            if (argument == null) return null;
            parameterList.parameterExpressions.add (argument);

            if (scanner.token == ParenthesisCloseToken) break;
            int ct [] = {CommaToken, ParenthesisCloseToken};
            if (!expectTokens (ct)) return null;
            skipToken (CommaToken);
         } while (true);
      }

      parameterList.sourceEndPosition = scanner.tokenEndPosition;
      methodCall.sourceEndPosition    = scanner.tokenEndPosition;
      skipToken (ParenthesisCloseToken);
      return parse_FieldArray_Access (methodCall);
   }


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                     Allocation Expression                       */
   /*                                                                 */
   /*-----------------------------------------------------------------*/

   protected JavaExpression parseAllocationExpression (JavaExpression enclosingInstance) {
      if (scanner.token != NewToken) return null;

      AllocationExpression expression = new AllocationExpression ();
      expression.sourceStartPosition  = scanner.tokenStartPosition;
      scanner.next ();
      expression.sourceEndPosition    = scanner.tokenEndPosition;
      expression.nameStartPosition    = scanner.tokenStartPosition;
      expression.nameEndPosition      = scanner.tokenEndPosition;
      expression.enclosingInstance    = enclosingInstance;

      DataTypeDeclaration dataTypeDeclaration = new DataTypeDeclaration ();
      expression.dataTypeDeclaration  = dataTypeDeclaration;
      ScannerPosition typePosition    = scanner.getPosition ();
      if (!parseDataType (dataTypeDeclaration,
                          "Type expected",
                          false,
                          false,
                          true)) return null;

      expression.name                 = dataTypeDeclaration.name;
      expression.sourceEndPosition    = dataTypeDeclaration.sourceEndPosition;

      if (dataTypeDeclaration.isVoidType()) {
         parserError (2, "Cannot allocate void type", typePosition);
         return null;
      }

      if (enclosingInstance != null) {
         if (dataTypeDeclaration.isPrimitiveType()) {
            parserError (2, "Cannot use specify instance when allocating a primitive type");
            return null;
         }
      }

      if (scanner.token == ParenthesisOpenToken) {
         if (dataTypeDeclaration.isPrimitiveType()) {
            parserError (2, "Cannot use \"()\" when allocating type \"" + dataTypeDeclaration.name + "\"");
            return null;
         }

         MethodCallParameterList parameterList = new MethodCallParameterList ();
         parameterList.sourceStartPosition = scanner.tokenStartPosition;
         parameterList.sourceEndPosition   = scanner.tokenEndPosition;
         parameterList.nameStartPosition   = expression.nameStartPosition;
         parameterList.nameEndPosition     = expression.nameEndPosition;
         parameterList.name                = expression.name;
         expression.parameterList          = parameterList;

         scanner.next ();
         if (scanner.token != ParenthesisCloseToken) {
            do {
               JavaExpression constructorArgument = parseJavaExpression ();
               if (constructorArgument == null) return null;
               parameterList.parameterExpressions.add (constructorArgument);

               if (scanner.token == ParenthesisCloseToken) break;
               int ct [] = {CommaToken, ParenthesisCloseToken};
               if (!expectTokens (ct)) return null;
               skipToken (CommaToken);
            } while (true);
         }
         parameterList.sourceEndPosition = scanner.tokenEndPosition;
         expression.sourceEndPosition    = scanner.tokenEndPosition;
         skipToken (ParenthesisCloseToken);

         if (scanner.token == BraceOpenToken) {
             expression.anonymousTypeDeclaration = parseAnonymousTypeDeclaration();
         }
         return expression;
      }

      if (scanner.token == BracketOpenToken) {
        if (enclosingInstance != null) {
           parserError (2, "\"(\" expected (required for allocation with explicit enclosing instance)");
           return null;
        }
      }
      else {
         if (dataTypeDeclaration.isPrimitiveType()) {
            parserError (2, "\"[\" expected (if you want an array of type \"" + dataTypeDeclaration.name + "\")");
            return null;
         } else {
            int bt [] = {ParenthesisOpenToken, BracketOpenToken};
            expectTokens (bt);
         }
      }

      boolean seenEmptyBrackets = false;
      boolean hasSizeExpression =  true;
      while (scanner.token == BracketOpenToken) {
         scanner.next ();
         if (scanner.token == BracketCloseToken) {
            if (dataTypeDeclaration.noOfArrayDimensions < 1) {
               hasSizeExpression = false;
            }
            seenEmptyBrackets = true;
            expression.extraBracketPairs++;
            expression.sizeExpressions.add (null);
         } else {
            if (seenEmptyBrackets) {
               expectToken (BracketCloseToken);
               return null;
            }
            JavaExpression sizeExpression = parseJavaExpression ();
            if (sizeExpression == null) return null;
            expression.sizeExpressions.add (sizeExpression);
         }
         expression.sourceEndPosition = scanner.tokenEndPosition;
         if (!skipToken (BracketCloseToken)) return null;
         dataTypeDeclaration.noOfArrayDimensions++;
      }

      if (scanner.token == BraceOpenToken) {
          expression.arrayInitializer = parseArrayInitializer();
          if (hasSizeExpression) {
               parserError (2, "Array initializer not allowed if array size was specified");
          }
      }
      else if (!hasSizeExpression) {
               parserError (2, "Must specify at least one array size for the \"new\" operator"
                           + " or an array initializer");
      }
      return expression;
   }


   /*******************************************************************/
   /**                                                               **/
   /**                  LITTLE UTILITY ROUTINES                      **/
   /**                                                               **/
   /*******************************************************************/

   protected ArrayInitializer parseArrayInitializer () {

      /* Assumes this is called with the scanner on the opening brace */

      ArrayInitializer initializer = new ArrayInitializer ();
      initializer.sourceStartPosition  = scanner.tokenStartPosition;
      initializer.sourceEndPosition    = scanner.tokenEndPosition;
      initializer.nameStartPosition    = scanner.tokenStartPosition;
      initializer.nameEndPosition      = scanner.tokenEndPosition;
      if (!skipToken (BraceOpenToken)) return null;

      if (scanner.token != BraceCloseToken) {
         do {
            JavaExpression elementExpression;
            if (scanner.token == BraceOpenToken)
               elementExpression = parseArrayInitializer ();
            else
               elementExpression = parseJavaExpression ();

            if (elementExpression == null) return null;
            initializer.elementExpressions.add (elementExpression);

            int ct [] = {CommaToken, BraceCloseToken};
            if (!expectTokens (ct)) return null;
            if (scanner.token == CommaToken) scanner.next ();
            if (scanner.token == BraceCloseToken) break;
         }  while (true);
      }

      initializer.sourceEndPosition = scanner.tokenEndPosition;
      if (!skipToken (BraceCloseToken)) return null;
      return initializer;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseModifiers (ModifierList modifierList) {
      modifierList.sourceStartPosition = scanner.tokenStartPosition;

      int flag;
      do {
         flag = 0;
         switch (scanner.token) {
            case PublicToken:         flag = PublicFlag;         break;
            case ProtectedToken:      flag = ProtectedFlag;      break;
            case PrivateToken:        flag = PrivateFlag;        break;
            case StaticToken:         flag = StaticFlag;         break;
            case AbstractToken:       flag = AbstractFlag;       break;
            case FinalToken:          flag = FinalFlag;          break;
            case TransientToken:      flag = TransientFlag;      break;
            case VolatileToken:       flag = VolatileFlag;       break;
            case NativeToken:         flag = NativeFlag;         break;
            case SynchronizedToken:   flag = SynchronizedFlag;   break;
            default: return true; 
         }
         if ((modifierList.modifiers & flag) > 0) {
            /* Non-fatal error */
            parserError (2, (new StringBuffer ())
                                 .append ("duplicate occurrence of ")
                                 .append (scanner.quotedTokenRepresentation (scanner.token))
                                 .toString ());
         } else {
            modifierList.modifiers |= flag;
            modifierList.flags.add     (flag);
            modifierList.tokens.add    (scanner.token);
            modifierList.positions.add (scanner.getPosition ());
         }
         scanner.next ();
      } while (true);
   }

   /*-----------------------------------------------------------------*/

   /* Parse a compound identifier, consisting of one or more 
      plain identifiers separated by periods. Store the compound identifier
      as the name of the given language construct. */

   protected boolean parseCompoundName (LanguageConstructWithNameComponents languageConstruct) {
      if (!expectToken (IdentifierToken)) return false;
      languageConstruct.sourceStartPosition = 
      languageConstruct.  nameStartPosition = scanner.tokenStartPosition;

      StringBuffer compoundName = new StringBuffer ();
      do {
         compoundName.append (scanner.tokenValue);
         languageConstruct.addNameComponent ( (String) scanner.tokenValue);
         languageConstruct.sourceEndPosition = 
         languageConstruct.  nameEndPosition = scanner.tokenEndPosition;
         if (scanner.next () != PeriodToken) break;
         if (!nextToken (IdentifierToken)) return false;
         compoundName.append ('.');
      } while (true);

      languageConstruct.name = compoundName.toString ();
      return true;
   }

   /*-----------------------------------------------------------------*/

   /* Parse an import name, which is a compound identifier whose last
      element may be an asterisk (as long as it's not the only element).
      Store the compound identifier as the name of the given 
      import statement. If there is an asterisk, set the onDemand flag. */

   protected boolean parseImportName (ImportStatement importStatement) {
      if (!expectToken (IdentifierToken)) return false;
      importStatement.packagePart.nameStartPosition = importStatement.sourceStartPosition;
      
      Object       currentName = scanner.tokenValue;
      StringBuffer packageName = new StringBuffer();

      for (int i=0; ;i++) {

         importStatement.nameStartPosition = importStatement.sourceStartPosition;
         if (scanner.next () != PeriodToken) break;
         scanner.next ();

         if (i>0) packageName.append ('.');
         packageName.append (currentName);
         importStatement.packagePart.addNameComponent ((String)currentName);
         importStatement.packagePart.nameEndPosition = scanner.tokenEndPosition;

         int c[] = {IdentifierToken, AsteriskToken};
         if (!expectTokens (c)) return false;
         
         if (scanner.token == AsteriskToken) {
            currentName = "*";
            importStatement.importOnDemand = true;
            scanner.next ();
            break;
         }
         currentName = scanner.tokenValue;
      }

      if (!importStatement.importOnDemand) {
             int t[] = {PeriodToken, SemicolonToken};
             if (!expectTokens (t)           ) return false;     
      } else if (!expectToken(SemicolonToken)) return false;
      
      importStatement.nameEndPosition  = scanner.tokenEndPosition;
      importStatement.name             = (String)currentName;
      importStatement.packagePart.name = packageName.toString();

      if (importStatement.importStatic)
      {
    	 // eat the class name away from the package part  
    	  if (packageName.length()==0)
    	  {
              parserError (2, "\"ïmport static\" requires class name");
    	  }
    	  else
    	  {
    		  importStatement.classNameForStaticImport = importStatement.packagePart.popLastNameComponent();
    	  }
      }
      return true;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseDataType (DataTypeDeclaration dataTypeDeclaration, 
                                    String errorMessage,
                                    boolean includeArrayBrackets,
                                    boolean allowFreeDimension,
                                    boolean allowRelativeDimension) {
      dataTypeDeclaration.sourceStartPosition = scanner.tokenStartPosition;
      dataTypeDeclaration.nameStartPosition   = scanner.tokenStartPosition;
      dataTypeDeclaration.sourceEndPosition   = scanner.tokenEndPosition;
      dataTypeDeclaration.nameEndPosition     = scanner.tokenEndPosition;
      dataTypeDeclaration.primitiveTypeToken  = scanner.token;
      dataTypeDeclaration.name                = Scanner.tokenRepresentation (scanner.token);

      if (dataTypeDeclaration.isVoidType()) {
         scanner.next ();
         return true;
      }

      if (dataTypeDeclaration.isPrimitiveType()) {
         scanner.next ();
         switch (dataTypeDeclaration.primitiveTypeToken) {
         case   ByteToken:
         case  ShortToken:
         case    IntToken:
         case   LongToken:
         case  FloatToken:
         case DoubleToken:  // PARSE THE DIMENSION ...

          {
           CompoundDimensionDeclaration compoundDimensionDeclaration = null;
           for (int i=0; ;i++) {
             boolean isInverted = false;
             if (scanner.token==AsteriskToken) {
             }
             else if (scanner.token==SlashToken) {
                isInverted = true;
             }
             else {
               break;
             }
             DimensionReference dimensionReference = null;
             scanner.next();
             if (scanner.token==DimensionToken) {
                 scanner.next();
                 if (i==0
                 &&  scanner.token!=ParenthesisOpenToken) {
                     dataTypeDeclaration.dimensionDeclaration                     = new FreeDimensionDeclaration();
                     dataTypeDeclaration.dimensionDeclaration.sourceStartPosition = scanner.tokenStartPosition;
                     dataTypeDeclaration.dimensionDeclaration.  sourceEndPosition = scanner.tokenEndPosition;
                     if (!allowFreeDimension) {
                        parserError (2, "Free dimension is not allowed here");
                     }
                     if (isInverted) {
                        parserError (2, "Free dimensions should be preceded by '*'");
                     }
                     break;
                 }
                 if (i==0) {
                   compoundDimensionDeclaration = new CompoundDimensionDeclaration();
                   dataTypeDeclaration.dimensionDeclaration = compoundDimensionDeclaration;
                   compoundDimensionDeclaration.sourceStartPosition = scanner.tokenStartPosition;
                 }
                 if (!expectToken (ParenthesisOpenToken)) return false;
                 scanner.next();

/*********************
                 if (!expectToken (IdentifierToken)) return false;
                 NameExpression nameExpression      = new NameExpression ();
                 nameExpression.sourceStartPosition = scanner.tokenStartPosition;
                 nameExpression.sourceEndPosition   = scanner.tokenEndPosition;
                 nameExpression.nameStartPosition   = scanner.tokenStartPosition;
                 nameExpression.nameEndPosition     = scanner.tokenEndPosition;
                 nameExpression.name                = (String) scanner.tokenValue;
                 dimensionReference = new RelativeDimensionReference(nameExpression);
                 scanner.next();
****************/
                 JavaExpression javaExpression = parseJavaExpression();
                 dimensionReference = new RelativeDimensionReference(javaExpression);
                 dimensionReference.sourceStartPosition = javaExpression.sourceStartPosition;
                 dimensionReference.sourceEndPosition   = javaExpression.sourceEndPosition;
                 dimensionReference.nameStartPosition   = javaExpression.nameStartPosition;
                 dimensionReference.nameEndPosition     = javaExpression.nameEndPosition;

                 if (!allowRelativeDimension) {
                        parserError (2, "Relative dimension is not allowed here");
                 }
                 else {
                   seenRelativeDimension = javaExpression;
                 }
                 if (!expectToken (ParenthesisCloseToken)) return false;
                 scanner.next();
             }
             else if (scanner.token!=IdentifierToken) {
                int t[] = {IdentifierToken, DimensionToken};
                expectTokens(t);
                return false;
             }
             else {
                if (i==0) {
                   compoundDimensionDeclaration = new CompoundDimensionDeclaration();
                   dataTypeDeclaration.dimensionDeclaration = compoundDimensionDeclaration;
                   compoundDimensionDeclaration.sourceStartPosition = scanner.tokenStartPosition;
                }
                dimensionReference = new DimensionReference();
                if (!parseCompoundName (dimensionReference)) return false;
             }
             compoundDimensionDeclaration.sourceEndPosition = scanner.tokenEndPosition;
             if (isInverted) {
                compoundDimensionDeclaration.addInverted (dimensionReference);
             }
             else {
                compoundDimensionDeclaration.addNormal   (dimensionReference);
             }
           }
          }
        }
      } else {
         if (scanner.token != IdentifierToken) {
            parserError (2, errorMessage);
            return false;
         }
         if (!parseCompoundName (dataTypeDeclaration)) return false;
         dataTypeDeclaration.sourceEndPosition = dataTypeDeclaration.nameEndPosition;
      }

      if (includeArrayBrackets) {
         while (scanner.token == BracketOpenToken) {
            if (!nextToken (BracketCloseToken)) return false;
            dataTypeDeclaration.noOfArrayDimensions++;
            dataTypeDeclaration.sourceEndPosition = scanner.tokenEndPosition;
            scanner.next ();
         }
      }
      return true;
   }

   /*-----------------------------------------------------------------*/

   protected boolean parseDimensionDeclaration (TypeDeclaration typeDeclaration,
                                                ModifierList    modifierList) {

      int sourceStartPosition = scanner.tokenStartPosition;

      scanner.next (); // eat DimensionToken
      if (!expectToken(IdentifierToken)) return false;
      String name = (String) scanner.tokenValue;
      int nameStartPosition = scanner.tokenStartPosition;
      int   nameEndPosition = scanner.tokenEndPosition;

      scanner.next (); // eat IdentifierToken
      int t[] = {ParenthesisOpenToken, AssignToken};
      if (!expectTokens(t)) return false;

      if (scanner.token == ParenthesisOpenToken) {
         scanner.next (); // eat UnitToken
         if (!expectToken(IdentifierToken)) return false;

              BaseUnitDeclaration      baseUnitDeclaration = new BaseUnitDeclaration();
              baseUnitDeclaration.sourceStartPosition      = scanner.tokenStartPosition;
              baseUnitDeclaration.sourceEndPosition        = scanner.tokenEndPosition;
              baseUnitDeclaration.nameStartPosition        = scanner.tokenStartPosition;
              baseUnitDeclaration.  nameEndPosition        = scanner.tokenEndPosition;
              baseUnitDeclaration.name                     = (String) scanner.tokenValue;
         BaseDimensionDeclaration baseDimensionDeclaration = new BaseDimensionDeclaration();
         baseDimensionDeclaration.sourceStartPosition      = sourceStartPosition;
         baseDimensionDeclaration.sourceEndPosition        = scanner.tokenEndPosition;
         baseDimensionDeclaration.nameStartPosition        = nameStartPosition;
         baseDimensionDeclaration.  nameEndPosition        =   nameEndPosition;
         baseDimensionDeclaration.name                     = name;
         baseDimensionDeclaration.unit                     = baseUnitDeclaration;
         baseDimensionDeclaration.typeDeclaration          = typeDeclaration;
         baseDimensionDeclaration.setModifiers        ( modifierList.modifiers);
         typeDeclaration.fieldDeclarations.add (baseDimensionDeclaration);
         scanner.next ();
         if (!expectToken(ParenthesisCloseToken)) return false;
         scanner.next ();
         if (!modifierList.checkModifiers (baseDimensionDeclaration, this)) return false;
         baseDimensionDeclaration.setModifiers ( modifierList.modifiers);
      }
      else {  // "dimension d = expr "

         CompoundDimensionDeclaration compoundDimensionDeclaration = new CompoundDimensionDeclaration();
         compoundDimensionDeclaration.sourceStartPosition      = sourceStartPosition;
         compoundDimensionDeclaration.nameStartPosition        = nameStartPosition;
         compoundDimensionDeclaration.  nameEndPosition        =   nameEndPosition;
         compoundDimensionDeclaration.name                     = name;
         compoundDimensionDeclaration.typeDeclaration          = typeDeclaration;
         compoundDimensionDeclaration.setModifiers (modifierList.modifiers);

         scanner.next (); // eat AssignToken
         if (!parseCompoundDimensionExpression (compoundDimensionDeclaration)) return false;

         typeDeclaration.fieldDeclarations.add (compoundDimensionDeclaration);
         if (!modifierList.checkModifiers (compoundDimensionDeclaration, this)) return false;
         compoundDimensionDeclaration.setModifiers ( modifierList.modifiers);
      }

      if (!expectToken(SemicolonToken)) return false;
      return true;
   }


     boolean parseCompoundDimensionExpression (CompoundDimensionDeclaration compoundDimensionDeclaration) {
         int tt[] = {IntegerLiteralToken, IdentifierToken};
         if (!expectTokens (tt)) return false;
         boolean isInverted = false;
         if (scanner.token==IntegerLiteralToken) {
            if (((Integer)scanner.tokenValue).intValue() != 1) {
               parserError (2, "Only number '1' is allowed as numeric start for dimension definition");
            }
            scanner.next();
            if (!expectToken (SlashToken)) return false;
            isInverted = true;
            scanner.next();
            if (!expectToken (IdentifierToken)) return false;
         }
         for (;;) {
           DimensionReference dimensionReference = new DimensionReference();
           if (!parseCompoundName (dimensionReference)) return false;
           compoundDimensionDeclaration.sourceEndPosition = scanner.tokenEndPosition;
           if (isInverted) {
              compoundDimensionDeclaration.addInverted (dimensionReference);
           }
           else {
              compoundDimensionDeclaration.addNormal   (dimensionReference);
           }
           if (scanner.token==AsteriskToken) {
              isInverted = false;
           }
           else if (scanner.token==SlashToken) {
              isInverted = true;
           }
           else {
             break;
           }
           scanner.next();
         }
         return true;
     }
}


