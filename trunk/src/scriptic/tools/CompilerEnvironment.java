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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import scriptic.tools.lowlevel.ByteCodingException;


/**************************************************

The compilation strategy is NOT straightforward. Objectives:

(1) high speed

(2) low memory requirements, induced by (1)

(3) automatic compilation of outdated source files that
    contain classes/interfaces that are
    * directly referenced by classes/interfaces being compiled
    * ancestors thereof

(4) if doDependencies option is given:
    automatic compilation of source files containing classes/interfaces
    that are used, directly or indirectly, by compiled sources,
    and that use classes from one or more compiled sources in turn

(5) deal with case-insensitivity of Windows and Macintosh file systems;
    no bugs like in javac and sj that yield error messages like:
    "file 'myClass.class' does not contain class 'myClass' as expected,
    but class 'MyClass' instead. Please remove the file"

(6) no bugs like in javac and sj that yield error messages like:
    "class 'MyClass' already defined in 'MyClass.java' (MyClass.java: 12)"

(7) flags errors like: a ".class" file says: source is in <sourceFile>
    but this <sourceFile> does not contain the class, and neither has
    the class been included in the source files set, nor can
    it be found in the sameName.java file

Some of these objectives are a bit conflicting.
After the initial parsing we want to drop all method bodies (2).
In later passes (constant and type resolution etc) we need used classes,
so it must be determined very fast which other files need recompilation
(3)(4). However, the names of some used classes may reside in
FieldAccessExpression, e.g., "Math.PI" and "java.lang.Math.PI".

Therefore, we'll attempt to load classes for each name-FieldAccessExpression as
well, except, if we're not too lazy, when the FieldAccessExpression starts with
a parameter, local variable or instance variable (inherited instance
variables not included; these are not yet known).

This strategy may load a few classes too much, in the unlikely case
that used variables in FieldAccessExpression have the same names as
packages or classes.

Also, names from allocationExpressions are interesting.

All these resolved classes will be cached in the CompilationUnit's hash tables,
so that resolution in a subsequent pass is fast (1).
There is also a global cache in the ClassesEnvironment.

(1) requires a fast strategy for handling the nameExpressions, which come with lots.
The ClassesEnvironment keeps track of:

*   knownPackageNames - gets package names of resolved classes
                        including partial package names
                        ("java", "sun", "scriptic")
* illegalPackageNames - names that are known NOT to be
                        leaders of package names ("i", "java.nonsense").

A FieldAccessExpression starting with an illegalPackageName can still
start with a class name from the default package, from package java.lang,
or from an imported class.

Import-on-demand statements are resolved in a global Hashtable 
packageName -> Vector(className|ClassType).

Names of "java.lang" classes are also stored separately.

All these considerations result in functions in CompilerEnvironment and in
ClassesEnvironment.

void     addKnownPackageName (String name) 
void   addIllegalPackageName (String name) 
boolean   isKnownPackageName (String name) 
boolean isIllegalPackageName (String name) 

DataType  resolveDataType       (CompilationUnit c, String name, Vector nameComponents,
                                 int primitiveTypeToken, int arrayDimensions)
ClassType resolveClassType      (CompilationUnit c, String name, Vector nameComponents)
ClassType resolveNameExpression (CompilationUnit c, NameExpression n)





DataType  resolveDataType       (CompilationUnit c, String name, Vector nameComponents,
                                 int primitiveTypeToken, int arrayDimensions)
ClassType resolveClassType      (CompilationUnit c, String name, Vector nameComponents)
ClassType resolveFieldAccessExpression (CompilationUnit c, FieldAccessExpression n)



Notations
---------
[... -> ... ] : HashMap
[Name->Class] : HashMap of the type ["Thread" -> "Thread"|ClassType]
[Name->SAME ] : HashMap with keys equal to values; for key lookup
[...]         : array or Vector



ClassesEnvironment sets
-----------------------
   classPathElements  : [C:\java\classes]
knownClassTypes       : ["java.util.Vector"-> ClassType               ]
packageJavaLangClasses: ["Thread"          -> "Thread" | ClassType    ]
packageContents       : ["java.lang"       -> [Name->Class]           ]
illegalPackageNames   : ["i"               -> "i"                     ,
                         "java.nonsense"   -> "java.nonsense"         ] // ??
  knownPackageNames   : ["java"            -> [""    ->[Name->Class]  ,
                                               "lang"->packageContents]
Hashtable classPackages       
Hashtable classPathPackages   


CompilerEnvironment sets
-----------------------
unprocessedSourceFiles      : [ FileName.java   ]
  processedSourceFiles      : [ FileName.java   ]
parsedCompilationUnits      : [ CompilationUnit ]
parsedClassTypes            : [ ClassType          -> SAME ]
loadedClassTypes            : [ ClassType          -> SAME ]
parsedTypeDeclarations      : [ qualifiedClassName -> TypeDeclaration ]
relevantClasses             : [ qualifiedClassName ]
notFoundClasses             : [ qualifiedClassName -> SAME ]
processedSourceFilesByPath  : [ FileName.java      -> sourceFile ]
expectedClassesBySourceFile : [ FileName.java      -> [qualifiedClassName -> classType]]
// 1 out of processedSourceFilesByPath, expectedClassesBySourceFile seems needed

CompilationUnit sets
--------------------
knownClassTypes       : ["System"          -> ClassType                          ,  // java.lang
                         "java.io.File "   -> ClassType                          ,  // fully qualified
                         "File"            -> ClassType                          ]  // import on demand
importedClasses       : ["Vector"          -> ClassType | import java.util.Vector]  // qualified import
relevantClasses       : [ NameExpression
                        | FieldAccessExpression
                        | DataTypeDeclaration
                        | SuperclassDeclaration
                        | InterfaceDeclaration  ]


Pseudo code for the compilation process
---------------------------------------
 PASS 1:
   while files in Vector unprocessedSourceFiles 

     1.1: parse each source file in unprocessedSourceFiles
         build parsed CompilationUnit, collecting relevantClasses,
                                                (= superclass + interfaces
                                                 + type names in field declarations and casts
                                                 + name expressions with '.' that don't start with known name)
         move source file from unprocessedSourceFiles to processedSourceFiles
         for TypeDeclarations in parsed CompilationUnit
               if exists a parsedTypeDeclaration with same name
                   error "Class ... has already been defined in file ..."
               else
                   if exists other ClassType with same name
                        the ClassType = that one; clear it
                   else the ClassType = new one
                   bind the ClassType to the typeDeclaration
                   add  the ClassType to the knownClassTypes
                   add  the TypeDeclaration to parsedTypeDeclarations
         if expectedClassesBySourceFile names more classes than the CompilationUnit
             error "class ... in class file expected to have source in file ...,
                  but not present there. Please remove"

     1.2: Load used classes, and recompile where needed
     for each parsed CompilationUnit
       for each relevant class in the CompilationUnit
         load the class and mark for recompilation if needed
     for each relevant class
         load the class and mark for recompilation if needed

     1.3: refresh dependent classes
     if doAllDependencies
        for each loaded ClassType
           if dependent on any class in parsed ClassType (see constantPool?)
               add to unprocessed source file set

  CHECK:
     error for loadedClassTypes claiming the same source file as existing processedSourceFiles

  PASS 3: for each parsed ClassType create field signatures
  PASS 4: for each parsed ClassType check dimensions
  PASS 5: for each parsed ClassType check constants

  for each parsed ClassType
    PASS 6: reparse method bodies
            check field definitions
              if not doAllDependencies: loading ClassTypes where applicable
    PASS 7: generate code, write to output


  load class and mark for recompilation if needed
  =     if name not in parsedTypeDeclarations
            attempt to load name from class file
            if not loaded
                if potential source file exists
                    if a source file with that name already processed
                        error ...
                    else if class name not in expectedClassesBySourceFile
                        add SourceFile to unprocessedSourceFiles
                        add class name to expectedClassesBySourceFile
                                   
                else if not a NameExpression
                   error ...
            else if SourceFile more recent && doRecompileDirtySources
                 if SourceFile in processedSourceFiles
                    error "class ... in class file expected to have source in file ...,
                           but not present there. Please remove"
                 else
                    add SourceFile to unprocessedSourceFiles
                    add class name to expectedClassesBySourceFile
            else
                if SourceFile in processedSourceFiles
                    warning "class ... in class file ... claim to originate from ... but it is not there
                add to loadedClassTypes
                add to  knownClassTypes
                add to  knownClassTypes of compilationUnit, if any
                add to loadedClassTypesWithUnfinishedSupertypes
                collect relevantClasses
                   (= superclass, interfaces,
                    + if doAllDependencies: type names in field declarations)


******************************************************************************/
   
class CompilerEnvironment extends ClassesEnvironment
                       implements JavaParseTreeCodes {

   boolean needScripticVmNodeType  = false;
   boolean doTrace                 = false;
   boolean doVerbose               =  true;
   boolean doRecompileDirtySources =  true;
   boolean doRecompileDependents   = false;
   boolean doGenerateJavaCode      = false;
   boolean doUseOperators          = false;
   boolean doKeepMethodBodies      = false;
   boolean doPrintClassinfo        = false;
   
   public String outputDirectoryName;

   HashSet<String> notFoundClasses;
   HashSet<ClassType> loadedClassTypes;
   HashMap<String, HashMap<String, ClassType>> expectedClassesBySourceFile;
   HashMap<String, Object> processedSourceFilesByPath;
   ArrayList<String>    relevantClasses;
   ArrayList<Object>   processedSourceFiles;
   ArrayList<Object> unprocessedSourceFiles;
   ArrayList<ClassType> loadedClassTypesWithUnfinishedSupertypes;
   ArrayList<CompilationUnit>  parsedCompilationUnits;

   public ClassType        javaLangObjectType;
   public ClassType        javaLangSystemType;
   public ClassType        javaLangStringType;
   public ClassType        javaLangStringBufferType;
   public ClassType        javaLangThrowableType;
   public ClassType        javaLangRuntimeExceptionType;
   public ClassType        javaLangExceptionType;
   public ClassType        javaLangCloneableType;
   public ClassType        javaLangClassType;
   public ClassType        javaLangBooleanType;
   public ClassType        javaLangByteType;
   public ClassType        javaLangCharType;
   public ClassType        javaLangShortType;
   public ClassType        javaLangIntType;
   public ClassType        javaLangLongType;
   public ClassType        javaLangFloatType;
   public ClassType        javaLangDoubleType;
   public ClassType        javaLangVoidType;
   public ClassType        javaIoPrintStreamType;
   public ClassType        scripticVmNodeType;
   public ClassType        scripticVmNodeTemplateType;
   public ClassType        scripticVmFromJavaType;
   public ClassType        scripticVmCallerNodeInterfaceType;
   public ClassType        scripticVmBooleanHolderType;
   public ClassType        scripticVmByteHolderType;
   public ClassType        scripticVmCharHolderType;
   public ClassType        scripticVmShortHolderType;
   public ClassType        scripticVmIntHolderType;
   public ClassType        scripticVmLongHolderType;
   public ClassType        scripticVmFloatHolderType;
   public ClassType        scripticVmDoubleHolderType;
   public ClassType        scripticVmObjectHolderType;
   public ClassType        scripticVmCodeInvokerSynchronousType;
   public ClassType        scripticVmCodeInvokerAsynchronousType;
   public ClassType        scripticVmCodeInvokerThreadedType;

   public MemberVariable   scripticVmNodePass; 
   public MemberVariable   scripticVmNodePriority;
   public MemberVariable   scripticVmNodeSuccess;

          boolean          canAcceptNewSourcesForCompilation;

   public CompilerEnvironment (ParserErrorHandler parserErrorHandler) {
                        super (parserErrorHandler);
   }
   public CompilerEnvironment (ParserErrorHandler parserErrorHandler, String classPath) {
                        super (parserErrorHandler, classPath);
   }

   /*---------------------------- Initialization -------------------------*/

   protected void initialize() {
      super.initialize();
      timer (InitializedMsg, "");
      javaLangObjectType           = mustResolveJavaLangClass ("Object"          , true);
      javaLangSystemType           = mustResolveJavaLangClass ("System"          ,false);
      javaLangStringType           = mustResolveJavaLangClass ("String"          ,false);
      javaLangStringBufferType     = mustResolveJavaLangClass ("StringBuffer"    ,false);
      javaLangThrowableType        = mustResolveJavaLangClass ("Throwable"       ,false);
      javaLangRuntimeExceptionType = mustResolveJavaLangClass ("RuntimeException",false);
      javaLangExceptionType        = mustResolveJavaLangClass ("Exception"       ,false);
      javaLangCloneableType        = mustResolveJavaLangClass ("Cloneable"       ,false);
      javaLangClassType            = mustResolveJavaLangClass ("Class"           ,false);
      javaLangBooleanType          = mustResolveJavaLangClass ("Boolean"         ,false);
      javaLangByteType             = mustResolveJavaLangClass ("Byte"            ,false);
      javaLangCharType             = mustResolveJavaLangClass ("Character"       ,false);
      javaLangShortType            = mustResolveJavaLangClass ("Short"           ,false);
      javaLangIntType              = mustResolveJavaLangClass ("Integer"         ,false);
      javaLangLongType             = mustResolveJavaLangClass ("Long"            ,false);
      javaLangFloatType            = mustResolveJavaLangClass ("Float"           ,false);
      javaLangDoubleType           = mustResolveJavaLangClass ("Double"          ,false);
      javaLangVoidType             = mustResolveJavaLangClass ("Void"            ,false);
      javaIoPrintStreamType        = super.resolveClass ("java.io", "PrintStream",false);
   }
   protected void initializeScripticTypes()
    throws CompilerError, IOException {
      scripticVmNodeType                  = mustResolveScripticVmClass ("Node"                 );
      scripticVmNodeTemplateType          = mustResolveScripticVmClass ("NodeTemplate"         );
      scripticVmFromJavaType              = mustResolveScripticVmClass ( "FromJava"            );
      scripticVmCallerNodeInterfaceType   = mustResolveScripticVmClass ("CallerNodeInterface"  );

      scripticVmBooleanHolderType         = mustResolveScripticVmClass ("BooleanHolder"        );
      scripticVmByteHolderType            = mustResolveScripticVmClass (   "ByteHolder"        );
      scripticVmCharHolderType            = mustResolveScripticVmClass (   "CharHolder"        );
      scripticVmShortHolderType           = mustResolveScripticVmClass (  "ShortHolder"        );
      scripticVmIntHolderType             = mustResolveScripticVmClass (    "IntHolder"        );
      scripticVmLongHolderType            = mustResolveScripticVmClass (   "LongHolder"        );
      scripticVmFloatHolderType           = mustResolveScripticVmClass (  "FloatHolder"        );
      scripticVmDoubleHolderType          = mustResolveScripticVmClass ( "DoubleHolder"        );
      scripticVmObjectHolderType          = mustResolveScripticVmClass ( "ObjectHolder"        );
      scripticVmCodeInvokerSynchronousType = mustResolveScripticVmClass ( "CodeInvokerSynchronous" );
      scripticVmCodeInvokerAsynchronousType= mustResolveScripticVmClass ( "CodeInvokerAsynchronous");
      scripticVmCodeInvokerThreadedType    = mustResolveScripticVmClass ( "CodeInvokerThreaded"    );
      scripticVmNodePass                  = scripticVmNodeType.resolveMemberVariable (this, "pass"    );
      scripticVmNodePriority              = scripticVmNodeType.resolveMemberVariable (this, "priority");
      scripticVmNodeSuccess               = scripticVmNodeType.resolveMemberVariable (this, "success" );
   }
   protected ClassType mustResolveScripticVmClass (String name) {
        ClassType result = super.resolveClass ("scriptic.vm", name, true);
        if (result==null) {
            resolverError ("Fatal error: could not load class scriptic.vm."+name);
        }
        return result;
   }


   void reset() {}

   public Method getJavaLangStringBufferInit() {
      return mustResolveMethod (javaLangStringBufferType, "<init>", "()V");
   }
   public Method getJavaLangStringBufferToString() {
      return mustResolveMethod (javaLangStringBufferType, "toString", "()Ljava/lang/String;");
   }
   public Method getJavaLangStringBufferAppend(DataType d) {
     String simpleSignature = d==javaLangStringType
                            ? "Ljava/lang/String;"
                            : d.getSimpleSignature();
      return mustResolveMethod (javaLangStringBufferType, "append",
                                "("+simpleSignature+")Ljava/lang/StringBuffer;");
   }
   /* compile; return the parsedCompilationUnits */
   public ArrayList<CompilationUnit> compile (ArrayList<File> sourceFiles) {

       HashMap<String, TypeDeclaration> parsedTypeDeclarations = new HashMap<String, TypeDeclaration>();
       HashMap<String, ClassType>            parsedClassTypes = new HashMap<String, ClassType>();
                      parsedCompilationUnits = new ArrayList<CompilationUnit>();
                             relevantClasses = new ArrayList<String>();
                        processedSourceFiles = new ArrayList<Object>();
    loadedClassTypesWithUnfinishedSupertypes = new ArrayList<ClassType>();
                      unprocessedSourceFiles = new ArrayList<Object>(sourceFiles);
                             notFoundClasses = new HashSet<String>();
                            loadedClassTypes = new HashSet<ClassType>();
                 expectedClassesBySourceFile = new HashMap<String, HashMap<String, ClassType>>();
                  processedSourceFilesByPath = new HashMap<String, Object>();
                             knownClassTypes = new HashMap<String, ClassType>();

    timerStart ();
    if (!initialized) {initialize();}

    try { // for finalize with timerStop()...

       canAcceptNewSourcesForCompilation = true;

       // PASS 1
       while (!unprocessedSourceFiles.isEmpty()
          &&   parserErrorCount() == 0 ) {

    	   ArrayList<CompilationUnit> parsedCompilationUnitsThisRound = new ArrayList<CompilationUnit>();

          // 1.1: parse each unprocessedSourceFile
          for (Object source: unprocessedSourceFiles) {

        	  HashSet<ClassType> parsedClassTypesForThisSource = new HashSet<ClassType>();
              CompilationUnit c = null;

              String sourceName = source instanceof File
                                ? ((File) source).getPath()
                                : "source string";
              try {
               c = parse (source, sourceName);
               gcIfNeeded();
               if (c != null) {

                 parsedCompilationUnits         .add (c);
                 parsedCompilationUnitsThisRound.add (c);

                 for (TypeDeclarationOrComment tc: c.typeDeclarationsAndComments) {

                    if (tc.languageConstructCode() != TopLevelTypeDeclarationCode) {
                        continue;
                    }
                    TopLevelTypeDeclaration t = (TopLevelTypeDeclaration) tc;
 
                    ClassType conflicting = parsedClassTypes.get (t.fullNameWithDots()); 

                    if (conflicting != null) {
                       parserError (2,  "Type "+ t.fullNameWithDots()
                                   + " has already been defined in file "
                                   + conflicting.getSourceFilePath(),
                                     conflicting.sourceDeclaration.compilationUnit.scanner,
                                     t
                                   );
                    }
                    else {
                       for (TypeDeclaration tOrLocalType: t.withAllNestedAndLocalClasses) {
                          String fullNameWithDots  = tOrLocalType.fullNameWithDots();
                          ClassType ct = null;
                          if (!tOrLocalType.isInsideMethod()) { // not somewhere in method
                            parsedTypeDeclarations.put (fullNameWithDots, tOrLocalType);
                            ct = (ClassType) knownClassTypes.get (fullNameWithDots);
                          }
                          if (ct == null) {

/*****
System.out.println("makeNewClassType: "+tOrLocalType.name
+"  parent: "+(tOrLocalType.getParentTypeDeclaration()==null? "null"
              :tOrLocalType.getParentTypeDeclaration().name+
           ":"+tOrLocalType.getParentTypeDeclaration().target)
);
****/
                              ct = tOrLocalType.makeNewClassType ();
                              if (!tOrLocalType.isInsideMethod()) {
//trace("knownClassTypes.put("+fullNameWithDots+")");
                                knownClassTypes.put(fullNameWithDots, ct);
                              }
                          }
                           tOrLocalType.target = ct;
                          ct.initializeForSource (tOrLocalType, c, source instanceof File? (File) source: null);

                          if (!tOrLocalType.isInsideMethod()) {
                             c.knownClassTypes.put(tOrLocalType.name, ct); // it should know its own unqualified name
                             parsedClassTypesForThisSource.add(ct);
                             parsedClassTypes             .put(fullNameWithDots, ct);
                          }

                          needScripticVmNodeType |= tOrLocalType.hasScripts;
                       }
                    }
                  }
                  // PASS 2:
                  ScripticCompilerPass2 scripticCompilerPass2 = new ScripticCompilerPass2(c.scanner,this);
                  scripticCompilerPass2.resolve(c);
                  //timer (InspectedMsg, typeDecl.getNameWithDots());
                } // end if (c != null)
              }
              catch (IOException e) {
                    parserError (2, e.toString() + " when parsing " + sourceName);
              }

              // move source file from unprocessedSourceFiles to processedSourceFiles
              // the 'from' part is automatic, since unprocessedSourceFiles is recreated each iteration
              processedSourceFiles.add (source);
              processedSourceFilesByPath.put (sourceName, source);
//System.out.println ("processedSourceFilesByPath.put: "+sourceName);
              HashMap<String, ClassType> expectedClassesInThisSourceFile = 
                        expectedClassesBySourceFile.get (sourceName);
              if (expectedClassesInThisSourceFile != null) {
                  for (ClassType ct: expectedClassesInThisSourceFile.values()) {
                      if (!parsedClassTypesForThisSource.contains(ct)) {
                          parserError (2,  "Class "                                + ct.nameWithDots
                                      + (ct.classFile != null
                                      ? " found in class file "                 + ct.classFile.getPath()
                                      : "")
                                      + " was expected to have source in file " + sourceName
                                      + " but it was not present there. Recompile the proper source file");
                      }
                  }
              }
              timer (ParsedMsg, sourceName);
          }
          if (parserErrorCount() > 0) return parsedCompilationUnits;

          unprocessedSourceFiles = new ArrayList<Object>(); // empty it...

          // 1.2: Load used classes, and recompile where needed

          if (needScripticVmNodeType
          &&  scripticVmNodeType == null) {
              try {
                  initializeScripticTypes();
              }
              catch (CompilerError e) {parserError (2, e.toString());}
              catch (  IOException e) {parserError (2, e.toString());}
          }
          for (CompilationUnit c: parsedCompilationUnitsThisRound) {
              for (Object relevantClass: c.relevantClasses.values()) {
                  loadAndMarkForRecompilationIfNeeded (c, relevantClass);
              }
          }
          do {
            while (!relevantClasses.isEmpty()) {
                ArrayList<String> workingCopy = relevantClasses;
                relevantClasses = new ArrayList<String>();
                for (String relevantClass: workingCopy) {
                    loadAndMarkForRecompilationIfNeeded(relevantClass);
                }
            }
            ArrayList<ClassType> workingCopy = loadedClassTypesWithUnfinishedSupertypes;
            loadedClassTypesWithUnfinishedSupertypes = new ArrayList<ClassType>();
            for (ClassType c: workingCopy) {
                //c.resolveUsedClasses (this, doRecompileDependents);
                c.getSuperclass (this);
                c.getInterfaces (this);
            }
          } while (!loadedClassTypesWithUnfinishedSupertypes.isEmpty());
    
/********************** this may be hard to complete ******************
          // 1.3: refresh dependent classes
          if (doAllDependencies) {
             for (Enumeration el = loadedClassTypes.elements(); el.hasMoreElements(); ) {
                ClassType ct = (ClassType) el.nextElement();

                // is this hard to check now? how about the superclass line of parameter types?
                // or is this OK now since these are in relevantClasses ?
                if (ct.depends on any class in parsedClassTypes (see constantPool?)) {
                 
                   File file = new File (ct.getSourceFilePath());
                   unprocessedSourceFiles.addElement (fileName);
                }
             }
          }
************************************************************/
       }
       // end PASS 2

       canAcceptNewSourcesForCompilation = false;

       // CHECK:    (drop this or the one given 30 lines earlier...)
       //   error for loadedClassTypes claiming the same source file as processedSourceFiles
       for (ClassType ct: loadedClassTypes) {
           String sourceFilePath = ct.getSourceFilePath();
           if (ct.hasBeenLoaded()
           &&  sourceFilePath != null
           &&  processedSourceFilesByPath.get (sourceFilePath) != null) {
               parserError (2,  "Class "                                + ct.nameWithDots
                           + " found in class file "                 +(ct.classFile==null?"??????????":ct.classFile.getPath())
                           + " was expected to have source in file " + sourceFilePath
                           + " but it was not present there. Recompile the proper source file");
           }
       }

       if (parserErrorCount() > 0) return parsedCompilationUnits;

       // PASS 3: build signatures; 

       for (CompilationUnit c: parsedCompilationUnits) {

         c.scanner.getCharBuffer();

         for (LanguageConstruct lc: c.typeDeclarationsAndComments) {

           if (lc.languageConstructCode() != TopLevelTypeDeclarationCode) continue;

           TopLevelTypeDeclaration typeDecl = (TopLevelTypeDeclaration) lc;
           // currentFile = typeDecl.compilationUnit.sourceFile; ??
           ScripticCompilerPass3 scripticCompilerPass3 = new ScripticCompilerPass3(typeDecl.compilationUnit.scanner,this);
           scripticCompilerPass3.resolve(typeDecl);
           timer (InspectedMsg, typeDecl.getNameWithDots());
         }
         c.scanner.dropCharBuffer(); // in case there have been error messages...
       }
       gcIfNeeded();


       /* PASS 4: handle dimension declarations
        * Iterates until declared compound dimensions reduce all to base dimensions, or error
        * reducingCompoundDimensions - Vector with compound dimensions that are not yet reduced
        *   at start of first iteration: empty.
        *   Example:
        *   dimension A = B;
        *   dimension B = C;
        *   dimension C unit c;
        *   shrinking sequence: {A,B} >> {A} >> {}
        * 
        *   Will not shrink with this silly source, which should be an error
        *   dimension A = B;
        *   dimension B = A;
        */

       ArrayList<DimensionDeclaration> oldReducingCompoundDimensions = null;

       for (int i=0 ; ; i++) {

         if (parserErrorCount() > 0) return parsedCompilationUnits;
         ArrayList<DimensionDeclaration> reducingCompoundDimensions = new ArrayList<DimensionDeclaration>();

         for (CompilationUnit c: parsedCompilationUnits) {

           c.scanner.getCharBuffer();

           for (LanguageConstruct lc: c.typeDeclarationsAndComments) {

        	 if (lc.languageConstructCode() != TopLevelTypeDeclarationCode) continue;

             TopLevelTypeDeclaration typeDecl = (TopLevelTypeDeclaration) lc;
             ScripticCompilerPass4 scripticCompilerPass4 = new ScripticCompilerPass4(typeDecl.compilationUnit.scanner,this);
             scripticCompilerPass4.resolve(typeDecl, oldReducingCompoundDimensions, reducingCompoundDimensions);
             //timer (CheckedMsg, typeDecl.getNameWithDots());
           }
           c.scanner.dropCharBuffer(); // in case there have been error messages...
         }
         if (reducingCompoundDimensions.size()==0) { // ? not oldReducingCompoundDimensions !!
             break;
         }
         if (parserErrorCount() == 0
         &&  oldReducingCompoundDimensions != null
         &&  oldReducingCompoundDimensions.size()
         <=     reducingCompoundDimensions.size()) { // #$%@#$ no shrinking!

             DimensionDeclaration v = (DimensionDeclaration)
                                              reducingCompoundDimensions.get(0);
             parserError (2, "Attempt for circular dimension definition",
                          new ScannerPosition (v.typeDeclaration.compilationUnit.scanner,
                                               v.nameStartPosition, v.nameEndPosition));
             break;
         }
         oldReducingCompoundDimensions = reducingCompoundDimensions;
      }
      gcIfNeeded();

      if (parserErrorCount() > 0) return parsedCompilationUnits;

       // PASS 5: determine variable dimensions 

       for (CompilationUnit c: parsedCompilationUnits) {

         c.scanner.getCharBuffer();

         for (LanguageConstruct lc: c.typeDeclarationsAndComments) {

           if (lc.languageConstructCode() != TopLevelTypeDeclarationCode) continue;

           TopLevelTypeDeclaration typeDecl = (TopLevelTypeDeclaration) lc;
           // currentFile = typeDecl.compilationUnit.sourceFile; ??
           ScripticCompilerPass5 scripticCompilerPass5 = new ScripticCompilerPass5(typeDecl.compilationUnit.scanner,this);
           scripticCompilerPass5.resolve(typeDecl);
           //timer (InspectedMsg, typeDecl.getNameWithDots());
         }
         c.scanner.dropCharBuffer(); // in case there have been error messages...
       }
       gcIfNeeded();

      if (parserErrorCount() > 0) return parsedCompilationUnits;

       /* PASS 6: check constantness of member variables
        * Iterates until final variables reduce all to constants, or error
        * potentialConstantMembers - Vector with member declarations that may be constant
        *   at start of first iteration: empty.
        *   Example:
        *   class A {static final int iA = B.iB;}
        *   class B {static final int iB = C.iC;}
        *   class C {static final int iC = 1;}
        *   shrinking sequence: {A.iA,B.iB} >> {A.iA} >> {}
        * 
        *   Will not shrink with this silly source, which should be an error
        * class A {static final int iA = B.iB+1;}
        * class B {static final int iB = A.iA+1;}
        */

      ArrayList<MemberVariableDeclaration> oldPotentialConstantMembers = null;

       for (int i=0 ; ; i++) {

         if (parserErrorCount() > 0) return parsedCompilationUnits;
         ArrayList<MemberVariableDeclaration> potentialConstantMembers = new ArrayList<MemberVariableDeclaration>();

         for (CompilationUnit c: parsedCompilationUnits) {

           c.scanner.getCharBuffer();

           for (LanguageConstruct lc: c.typeDeclarationsAndComments) {

        	 if (lc.languageConstructCode() != TopLevelTypeDeclarationCode) continue;

             TopLevelTypeDeclaration typeDecl = (TopLevelTypeDeclaration) lc;
             ScripticCompilerPass6 scripticCompilerPass6 = new ScripticCompilerPass6(typeDecl.compilationUnit.scanner,this);
             scripticCompilerPass6.resolve(typeDecl, oldPotentialConstantMembers, potentialConstantMembers);
             timer (CheckedMsg, typeDecl.getNameWithDots());
           }
           c.scanner.dropCharBuffer(); // in case there have been error messages...
         }
         if (potentialConstantMembers.size()==0) {
             break;
         }
         if (oldPotentialConstantMembers != null
         &&  oldPotentialConstantMembers.size()
         <=     potentialConstantMembers.size()) { // #$%@#$ no shrinking!

             MemberVariableDeclaration v = potentialConstantMembers.get(0);
             parserError (2, "Attempt for circular variable initialization",
                          new ScannerPosition (v.owner.typeDeclaration.compilationUnit.scanner,
                                               v.nameStartPosition, v.nameEndPosition));
             break;
         }
         oldPotentialConstantMembers = potentialConstantMembers;
      }
      gcIfNeeded();

      if (parserErrorCount() > 0) return parsedCompilationUnits;

      /* pass 7, 8: in 1 group so that methods are reparsed and the information is kept
       * until the end of the passes.
       *
       * pass 7:
       * //Reparse method bodies
       * Various checks on inheritance
       * Checks expression types and constantness;
       *         not for final variable initializers and not for static fields
       *
       * pass 8:
       * Flow analysis and code generation
       * write class file
       * dispose method bodies
       *
       * we'll pass through the compilation units,
       * since these have to reload their scanners for reparsing method bodies.
       * we can call scanner.dropCharBuffer at the end of the pass.
       * This may save up to 30% RAM usage
       */

      for (CompilationUnit c: parsedCompilationUnits) {

    	  c.scanner.getCharBuffer();
         try {

           FileOutputStream fileStream = null;
           String          outFileName = null;

           if (doGenerateJavaCode) {

             String  sourceName = c.sourceFile.getPath();

             if (sourceName.length() > 5
             &&  sourceName.endsWith (".sawa")) {
                outFileName = sourceName.substring (0, sourceName.length() - 5)+".java";
             }
             else if (sourceName.length() > 7
                  &&  sourceName.endsWith (".s.java")) {
                outFileName = sourceName.substring (0, sourceName.length() - 7)+".java";
             }
             else if (sourceName.length() > 5
                  &&  sourceName.endsWith (".java")) {
               outFileName = sourceName.substring (0, sourceName.length() - 5)+".out.java";
             }
             else {
                parserError (2, "Internal error: unexpected source file name "+sourceName);
                return parsedCompilationUnits;
             }
             fileStream = new FileOutputStream(outFileName);
           }
           boolean errorOccurred = false;
           for (LanguageConstruct lc: c.typeDeclarationsAndComments) {

        	 if (lc.languageConstructCode() != TopLevelTypeDeclarationCode) continue;

             TopLevelTypeDeclaration typeDecl = (TopLevelTypeDeclaration) lc;
             ClassType ct                     = typeDecl.target;
             int savedNoOfErrors              = parserErrorCount();

             try { // so that after continue, garbage will be removed...

               // PASS 6: check field definitions
               //         if not doAllDependencies: loading ClassTypes where applicable

               ScripticCompilerPass7 scripticCompilerPass7 = new ScripticCompilerPass7(typeDecl.compilationUnit.scanner,this);
               scripticCompilerPass7.resolve(typeDecl);
               timer (ResolvedMsg, typeDecl.getNameWithDots());

               if (parserErrorCount() > savedNoOfErrors) {
                  errorOccurred = true;
                  continue;
               }
               // PASS 7: generate code, write to output

               if (!doGenerateJavaCode) {
                 ScripticCompilerPass8 scripticCompilerPass8 = new ScripticCompilerPass8(typeDecl.compilationUnit.scanner,this);
                 scripticCompilerPass8.resolve(typeDecl);
                 timer (GeneratedCodeMsg, typeDecl.getNameWithDots());

                 if (parserErrorCount() > savedNoOfErrors) continue;

                 try {
                   ct.write(this, true);
                 } catch (IOException e) {e.  printStackTrace();
                 } catch (ByteCodingException    e) {handleByteCodingException(e,typeDecl,typeDecl);
                 }
               }
             }
             finally {
               if (doPrintClassinfo) {
                  output.println (ct.getMemberDescription (this, true /*doDissassemble*/));
               }
               if (!doKeepMethodBodies
               &&  !doGenerateJavaCode) {
                  ct.freeCompiledMembers ();  // free the memory occupied by type declaration, code etc.
                  gcIfNeeded();
               }
             }
           }
           if (doGenerateJavaCode
           &&  !errorOccurred) {
                    ByteArrayOutputStream   byteStream = new    ByteArrayOutputStream (4096);
                 PreprocessorOutputStream outputStream = new PreprocessorOutputStream (byteStream);

                 int savedNoOfErrors = parserErrorCount();
                 ScripticPreprocessor scripticPreprocessor
                                = new ScripticPreprocessor (c.scanner, outputStream, this);
                 scripticPreprocessor.resolve(c);
                 timer (GeneratedJavaCodeMsg, c.getName());

                 if (parserErrorCount() > savedNoOfErrors) continue;

                 outputStream.flush ();
                 byteStream.writeTo (fileStream);
                 timer (WrittenMsg, outFileName);
                 fileStream.close();
                 gcIfNeeded();
           }
           c.scanner.dropCharBuffer();

         } catch (IOException e) {e.  printStackTrace();}

                                     // catch (Throwable thr) {thr.printStackTrace();}
      }
      return parsedCompilationUnits;
    }
    finally {
       timerStop ("done"+LanguageConstruct.lineSeparator);
       if (parserErrorCount() > 0) {
           output.println(parserErrorCount()+(parserErrorCount()==1? " error": " errors"));
       }
       output.flush();
    }
   }

   //---------------------------------------------------------------------------------------//

   CompilationUnit parse (Object source, String sourceName) throws IOException {
     // build parse tree, collecting relevantClasses,
     //                               (= superclass + interfaces
     //                                + type names in field declarations and casts
     //                                + primaries of field expressions that are also fields or names,
     //                                  and that preferably don't start with any known variable name)

     ScripticScanner scanner = new ScripticScanner ();
     scanner.setErrorHandler   (this);
     ScripticParser parser;
     if (source instanceof File) {
       scanner.setFile     ((File)source);
       scanner.processUnicodeEscapes ();
       parser = new ScripticParser (scanner, (File) source, this);
     }
     else {
       scanner.setString  ((String)source, sourceName);
       scanner.processUnicodeEscapes ();
       parser = new ScripticParser (scanner, this);
     }
     parser.keepFieldBodies = true; // doKeepMethodBodies; // if false, field bodies will be reparsed when needed
     CompilationUnit result = (CompilationUnit) parser.parse (sourceName);
     scanner.dropCharBuffer(); // saves a lot of space...only effective if source instanceof File
     return result;
   }

   //---------------------------------------------------------------------------------------//

   void handleByteCodingException (ByteCodingException e, TypeDeclaration typeDecl, LanguageConstruct languageConstruct) {
        parserError (3, "INTERNAL ERROR: "+e.toString(),
                     typeDecl.compilationUnit.scanner,
                     languageConstruct);
        e.printStackTrace();
   }

   //---------------------------------------------------------------------------------------//

   void loadAndMarkForRecompilationIfNeeded (Object relevantClass) {
        loadAndMarkForRecompilationIfNeeded (null, relevantClass);
   }

   //---------------------------------------------------------------------------------------//

   /** attempt to load a class file denoted by relevantClass, if not yet known
    * if the sourceFile is newer, put into unparsedSourceFiles...
    * NAME IS BIT CONFUSING...
    */
   void loadAndMarkForRecompilationIfNeeded (CompilationUnit c, Object relevantClass) {

      String name;
             if (relevantClass instanceof JavaExpression) {
                                 name = ((JavaExpression) relevantClass).qualifiedName();
      } else if (relevantClass instanceof DataTypeDeclaration) {
                                 name = ((DataTypeDeclaration) relevantClass).name;
      } else if (relevantClass instanceof String) {
                                  name = (String) relevantClass;
      }
      else return; // what to do here??? what other possibilities?

      if (knownClassTypes.get(name) != null) return;

      ClassType ct = null;

      if (name.indexOf('.') < 0
      &&  c != null) {
          ct = (ClassType) c.knownClassTypes.get(name);
          if (ct == null) {
              if (relevantClass instanceof NameExpression
              ||  relevantClass instanceof String) {
                     ct = resolveClassNameWithoutSlashes (c, name, true);
              }
              else if (relevantClass instanceof DataTypeDeclaration) {
                     ct = (ClassType) resolveDataTypeDeclaration (c, (DataTypeDeclaration) relevantClass).baseType();
                     // cast is OK. We know that !baseTypeIsPrimitive, ensured in rCompilationUnit.addPossibleRelevantClass
              }
              else if (relevantClass instanceof FieldAccessExpression) {
                     ct = resolveClassForCompilationUnit (c, (FieldAccessExpression) relevantClass, true);
              }
              else return; // what to do here??? what other possibilities?

              if (ct != null) {
                  c.knownClassTypes.put(name, ct);
              }
          }
          else return; // ???????????
      }
      if (ct == null) { // when loading for a compilation unit, load all used classes...
          ct = resolveClassNameWithSlashes (name, doRecompileDependents || c != null);
      }
      if (ct == null) {

          if (relevantClass instanceof DataTypeDeclaration) {
               if (!notFoundClasses.contains (name)) {
                   parserError (2,  "Class " + name + " could not be found");
                   notFoundClasses.add (name);
               }
          }
      }
   }

   //---------------------------------------------------------------------------------------//

   /** the central class resolving function.
    *  Works as in superclass, but also checks what the source file can do...
    */
   public ClassType resolveClass (String pakkage, String clazz, boolean loadAsWell)
   {
      ClassType result = super.resolveClass (pakkage, clazz, loadAsWell);

      if (result == null) {

         for (int i=0; i<classPathElements.length; i++) {

           String sourceFileName = classPathElements[i].findSourceFileName (pakkage, clazz);
           if (sourceFileName==null) {
/*             
if (clazz.equals("IntHolder")) {
 Hashtable sourceFileNamesInPackage = classPathElements[i].getFileNamesInPackage (pakkage, ".java");
 if (sourceFileNamesInPackage!=null) {
  System.out.println(classPathElements[i].name+":");
  System.out.println("---------------------------");
  for (Enumeration e=sourceFileNamesInPackage.keys(); e.hasMoreElements();) {
   Object obj = e.nextElement();
   System.out.println(obj +" >> " + sourceFileNamesInPackage.get(obj));
  }
 }
}
*/
             continue;
           }
           if (processedSourceFilesByPath.get (sourceFileName) != null) {
               parserError (2,  "Class " + packageDotClassName (pakkage, clazz)
                           + " expected to have source in file " + sourceFileName
                           + " but it is not present there");
               return null;
           }
           HashMap<String, ClassType> h = expectedClassesBySourceFile.get (sourceFileName);
           if (h==null) {
               h = new HashMap<String, ClassType>();
               expectedClassesBySourceFile.put (sourceFileName, h);
               unprocessedSourceFiles.add (new File(sourceFileName));
               if (!canAcceptNewSourcesForCompilation) {
                 parserError (2, "File "                         + sourceFileName
                             + " is expected to contain class "
                             + packageDotClassName (pakkage, clazz)
                             + " but it should be compiled by an explicit command first");
                 return null;
               }
//System.out.println("unprocessedSourceFiles.addElement 1: "+sourceFileName);
           }
           String qualifiedClassName = packageDotClassName (pakkage, clazz);
           result = (ClassType) h.get (qualifiedClassName);
           if (result==null) {
               result = new ClassType (qualifiedClassName, clazz);
               result.initializeTables ();
                       h              .put(qualifiedClassName, result);
                       knownClassTypes.put(qualifiedClassName, result);

               if (doTrace) System.out.println("Expecting: " + result.nameWithDots
                                              + "("          + qualifiedClassName   + ")");
           }
           return result;
         }
         return null;
      }
      if (!loadAsWell)              return result;
      if (result.classFile == null) return result;  // otherwise it is being compiled...

      boolean dontAddToLoadedClassTypes = false; // true in case there is a dirty source file...
 
      if (!result.sourceFileChecked) {

        File sourceFile = result.sourceFile();

        if  (sourceFile != null) {

          String sourceFilePath = sourceFile.getPath();
//System.out.print ("sourceFilePath: "+sourceFilePath+" >> ");
               if (sourceFilePath.regionMatches (0,"./"   ,0,2)) sourceFilePath=sourceFilePath.substring (2);
          else if (sourceFilePath.regionMatches (0,".\\"  ,0,3)) sourceFilePath=sourceFilePath.substring (2);
          else if (sourceFilePath.regionMatches (0,".\\\\",0,2)) sourceFilePath=sourceFilePath.substring (3);
//System.out.println(sourceFilePath);

          if (      sourceFile.lastModified()
           >  result.classFile.lastModified()) {

             if (doTrace) System.out.println("dirty: " + sourceFilePath
                                              + "(" + result.classFile.getPath()   + ")");

             if (canAcceptNewSourcesForCompilation
             &&  doRecompileDirtySources) { 
               if (processedSourceFilesByPath.get (sourceFilePath) != null) {
                 parserError (2,  "Class "                         + result.nameWithDots
                             + " in class file "                   + result. classFile  .getPath()
                             + " expected to have source in file " + sourceFilePath
                             + " but it is not present there");
               }
               else {
            	 HashMap<String, ClassType> h = expectedClassesBySourceFile.get (sourceFilePath);
                 if (h==null) {
                     h = new HashMap<String, ClassType>();
                     expectedClassesBySourceFile.put (sourceFilePath, h);
                     unprocessedSourceFiles.add (new File(sourceFilePath));
//System.out.println("unprocessedSourceFiles.addElement 2: "+sourceFilePath);

                 }
                 if (h.get (result.nameWithDots) == null)
                     h.put (result.nameWithDots, result);
               }
               dontAddToLoadedClassTypes = true;
             }
             else {
              parserError (1,  "Warning: class "            + result.nameWithDots
                          + " in class file "            + result. classFile  .getPath()
                          + " seems to have a more recent source in file " + sourceFilePath
                          + " but we cannot compile it now.");
             }
          }

          else if (processedSourceFilesByPath.get(sourceFilePath) != null) {
              parserError (1, "Warning: class "            + result.nameWithDots
                          + " in class file "            + result. classFile  .getPath()
                          + " claims to originate from " + sourceFilePath
                          + " but it is not there");
          }
        }
      }
      knownClassTypes.put (result.nameWithDots, result);  // also for the compilation unit??? already done!

      if (!dontAddToLoadedClassTypes) {
        loadedClassTypes.add (result);
        relevantClasses.add (result.superclassName);
        if (result.interfaceNames != null) {
            for (int i=0; i<result.interfaceNames.length; i++)
                relevantClasses.add (result.interfaceNames[i]);
        }
      }

      /********************** this may be hard to complete ******************
      if (doAllDependencies) {
             for (result.constantpool.classRefs) {
                  relevantClasses.add (classRef);
             }
      }
      ***************************************/

      return result;
  }

   //---------------------------------------------------------------------------------------//

   /** load the ClassType from its class file.
    */
   protected ClassType load (ClassType c,
                             String pakkageWithSlashes,
                             String pakkage,
                             String clazz) {

     //long  t = timerLocalStart();
     ClassType result = super.load(c, pakkageWithSlashes, pakkage, clazz);
     //timerLocalStop (LoadedMsg, result==null? "????": result.nameWithDots, t);
     return result;
   }

   //---------------------------------------------------------------------------------------//

   /** Finish the given ClassType's loading:
    *     mark it so that superclass and interfaces will be set later
    *  To be called by ClassesEnvironment.resolveClass, after loading...
    */
   protected void endOfLoading (ClassType c) {
      loadedClassTypesWithUnfinishedSupertypes.add (c);
   }
   String packageDotClassName( String pakkage, String clazz ) {
     return pakkage.length()==0? clazz: pakkage + "." + clazz;
   }
}
