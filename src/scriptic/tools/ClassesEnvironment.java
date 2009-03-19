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

/**********************************************************************

 ClassesEnvironment
 ==================
 WARNING: ClassesEnvironment is a highly complicated class.
 Its design is closely related to CompilerEnvironment.

 Remarks
 -------
 ClassTypes have a 1-01 relationship to TypeDeclarations
 each TypeDeclaration necessarily has a ClassType, not vv.
 Java constructs such as variable declarations need a dataType, which may be
 a ClassType.
 When these constructs originate from a source file, the dataType will be
 determined based on the related dataTypeDeclaration.
 When originating from a class file, no dataTypeDeclaration is available,
 and the dataType is created directly
 Equivalent datatypes will be unique during execution. Primitive dataTypes
 and array types thereof are available through phrases as IntType.theOne
 and IntType.makeArray(3).


 Public Member Functions
 -----------------------

 // Try to resolve a fieldAccessExpression as a package.class.
 // The fieldAccessExpression MAY represent a package.class.
 // Anyway, it is a period-separated identifier list, so 
 // its primary.qualifiedName() could be a package name...
 public ClassType resolveClassForCompilationUnit(CompilationUnit       compilationUnit,
 FieldAccessExpression fieldAccessExpression)

 Protected Member Functions
 --------------------------

 protected boolean  isJavaLangClassName (String name)

 // resolve a class by its name (may have '/');
 // cache in the appropriate hash tables ...
 // Checks for slashes in the name;
 // if none, it looks at package statement, import etc.
 protected ClassType resolveClassNameForCompilationUnit (CompilationUnit compilationUnit,
 String          className)

 // resolve a class by its bare name without package; cache in the appropriate hash tables ...
 // Does a lot of work at the CompilationUnit, but it belongs in this class.
 protected ClassType resolveClassNameWithoutSlashes (CompilationUnit compilationUnit,
 String          className)

 // resolve given class in class path, given a package
 // e.g. for "import java.util.*; ... Vector.addElement" 
 protected ClassType resolveClass (PackageStatement packageStatement, String clazz)

 // resolve given class in class path, given a package
 // e.g. for "import java.util.*; ... Vector.addElement" 
 protected ClassType resolveClass (String pakkage, String clazz)

 // Finish the given ClassType's loading:
 // load superclass and interfaces,
 // or in subclass CompilerEnvironment:
 // mark the ClassType so that superclass and interfaces will be set later
 // To be called by resolveClass, after loading...
 protected void endOfLoading (ClassType c)

 // Register the given class name as a member of package "java.lang"
 protected void addJavaLangClassName (String className)

 // Attempt to resolve a class in package "java.lang"
 protected ClassType resolveJavaLangClass (String className)


 Some Calling Hierarchies
 ------------------------

 CompilerEnvironment .resolveClass (pakkage, clazz)
 ClassesEnvironment  .resolveClass (pakkage, clazz)
 classPathElements[i].resolveClass (pakkage, clazz);    ClassType.load



 *************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import scriptic.tools.lowlevel.ClassFileAttribute;
import scriptic.tools.lowlevel.ClassFileConstants;
import scriptic.tools.lowlevel.InnerClassEntry;
import scriptic.tools.lowlevel.InnerClassesAttribute;
import scriptic.tools.lowlevel.ByteCodingException;

class ClassesEnvironment implements scriptic.tokens.JavaTokens, ClassFileConstants,
		ParserErrorHandler {

	public String classPath;
	public String classPathWithLineBreaks;

	public ClassPathElement classPathElements[]; // "C:\java\classes"
	HashMap<String, ClassType> knownClassTypes = new HashMap<String, ClassType>(); // ["java.util.Vector"->ClassType]
	public HashMap<String, Object> packageJavaLangClasses = new HashMap<String, Object>(); // ["String"->
																							// "String"|ClassType]
	public HashMap<String, ClassPackage> classPackages = new HashMap<String, ClassPackage>();
	public HashSet<String> knownPackageNames = new HashSet<String>();
	public HashSet<String> illegalPackageNames = new HashSet<String>();

	public boolean doReadCodeAttributes = true;
	public boolean doParseAllSignatures = false;
	boolean initialized;
	public PrintWriter output = new PrintWriter(System.out);
	public boolean doTiming = true;

	public ParserErrorHandler parserErrorHandler;

	public void parserError(int severity, String message) {
		parserErrorHandler.parserError(severity, message);
	}

	public void parserError(int severity, String message,
			ScannerPosition position) {
		parserErrorHandler.parserError(severity, message, position);
	}

	public void parserError(int severity, String message, Scanner scanner,
			LanguageConstruct languageConstruct) {
		parserErrorHandler.parserError(severity, message, scanner,
				languageConstruct);
	}

	public void parserError(int severity, String message,
			TypeDeclaration typeDeclaration) {
		parserError(severity, message, typeDeclaration.compilationUnit.scanner,
				typeDeclaration);
	}

	public void parserError(int severity, String message,
			RefinementDeclaration refinement) {
		parserError(severity, message,
				refinement.typeDeclaration.compilationUnit.scanner,
				refinement.typeDeclaration);
	}

	public int parserErrorCount() {
		return parserErrorHandler.parserErrorCount();
	}

	final boolean doTrace = false;

	void trace(String s) {
		output.println(s);
	}

	/*
	 * void traceKnownClassTypes() { trace("------knownClassTypes-------"); for
	 * (Enumeration e = knownClassTypes.keys(); e.hasMoreElements(); ) { String
	 * str = e.nextElement().toString(); if (!str.startsWith("java.lang.") &&
	 * !str.startsWith("scriptic.vm.")) trace(str); } }
	 */
	/*---------------------------- Constructors --------------------------*/

	public ClassesEnvironment() {
		this(null);
	}

	public ClassesEnvironment(ParserErrorHandler parserErrorHandler) {
		this(parserErrorHandler, null);
	}

	public ClassesEnvironment(ParserErrorHandler parserErrorHandler,
			String classPath) {
		this.parserErrorHandler = parserErrorHandler;
		this.classPath = classPath;
	}

	/*---------------------------- Initialization -------------------------*/

	protected Method mustResolveMethod(ClassType c, String name,
			String signature) {

		Method result = c.findMethod(this, name, signature);
		if (result == null) {
			c.getMethodNamesSignatures(this);
			parserError(2, "Cannot resolve method " + name + signature
					+ " in class " + c.getDescription());
		}
		return result;
	}

	void initialize() {

		ArrayList<String> classPathNames = new ArrayList<String>();

		int startPos = 0, scanPos = 0;
		StringBuffer classPathBuffer = new StringBuffer();

		if (classPath == null)
			classPath = System.getProperty("java.class.path", ".");

		do {
			// Find the next occurrence of the class path item separator
			scanPos = classPath.indexOf(File.pathSeparator, startPos);
			if (scanPos < 0) {
				// Don't forget the last item
				if (startPos < classPath.length()) {
					classPathNames.add(classPath.substring(startPos));
				}
				break;
			}

			// store the class path item in classPathNames
			if (scanPos > startPos) {
				classPathNames.add(classPath.substring(startPos, scanPos));
			}
			scanPos += File.pathSeparator.length();
			startPos = scanPos;
		} while (true);

		// classPathNames now contain all class path names.
		// remove ".." as far as possible...
		for (int i = 0; i < classPathNames.size(); i++) {
			String name = classPathNames.get(i);

			for (startPos = 0;;) {

				scanPos = name.indexOf("..", startPos);
				startPos = scanPos + 1;
				if (scanPos < 1) {
					break;
				}
				char c = name.charAt(scanPos - 1);
				if (c != ':' && c != '.') {
					int j = 0;
					for (j = scanPos - 2; j >= 0; j--) {
						if (name.charAt(j) == File.separatorChar) {
							break;
						}
					}
					name = name.substring(0, j + 1)
							+ (name.length() <= scanPos + 2
									|| name.charAt(scanPos + 2) == File.separatorChar ? name
									.substring(scanPos + 3)
									: name.substring(scanPos + 2));
					classPathNames.set(i, name);
					startPos = 0;
				}
			}
		}

		// filter out doubles...
		for (int i = 1; i < classPathNames.size(); i++) {
			String name1 = classPathNames.get(i);
			for (int j = 0; j < i; j++) {
				String name2 = classPathNames.get(j);
				if (name1.equals(name2)) {
					classPathNames.remove(i--);
					break;
				}
			}
		}

		// Make classPathElements; load .zip directories
		classPathElements = new ClassPathElement[classPathNames.size()];
		int n = 0;
		for (int i = 0; i < classPathNames.size(); i++) {
			ClassPathElement element;
			String name = classPathNames.get(i);
			if (name.length() > 4
					&& (name.toLowerCase().endsWith(".zip") || name
							.toLowerCase().endsWith(".jar"))) {
				File file = new File(name);
				if (file.isFile() && file.canRead()) {
					element = new ZipClassPathElement(this, name, file);
					classPathElements[n++] = element;
					element.findPackageJavaLangClassNames();
				}
			} else {
				element = new ClassPathElement(this, name);
				classPathElements[n++] = element;
				element.findPackageJavaLangClassNames();
			}
			classPathBuffer.append(name)
					.append(LanguageConstruct.lineSeparator);
		}
		if (n != classPathElements.length) {
			ClassPathElement oldArray[] = classPathElements;
			classPathElements = new ClassPathElement[n];
			System.arraycopy(oldArray, 0, classPathElements, 0, n);
		}
		classPathWithLineBreaks = classPathBuffer.toString();
		initialized = true;
	}

	protected ClassType mustResolveJavaLangClass(String name, boolean loadAsWell) {
		ClassType result = resolveJavaLangClass(name, loadAsWell);
		if (result == null) {
			resolverError("Fatal error: could not load class java.lang." + name);
		}
		return result;
	}

	// ------------------------- error handling ----------------------//

	protected void parserError(int severity, String message,
			CompilationUnit compilationUnit, int startPosition, int endPosition) {
		parserErrorHandler.parserError(severity, message,
				compilationUnit == null ? null : new ScannerPosition(
						compilationUnit.scanner, startPosition, endPosition));
	}

	protected void resolverError(String message,
			CompilationUnit compilationUnit, int startPosition, int endPosition) {
		parserError(2, message, compilationUnit, startPosition, endPosition);
	}

	protected void resolverError(String message) {
		parserErrorHandler.parserError(2, message, null);
	}

	// ------------------------- type resolution functions
	// ----------------------//

	protected boolean isKnownPackageName(String name) { // unqualified name
		if (knownPackageNames.contains(name))
			return true;
		if (illegalPackageNames.contains(name))
			return false;
		for (int i = 0; i < classPathElements.length; i++) {
			if (classPathElements[i].isKnownPackageName(name)) {
				knownPackageNames.add(name);
				return true;
			}
		}
		illegalPackageNames.add(name);
		return false;
	}

	// ---------------------------------------------------------------------------------------//

	protected boolean isJavaLangClassName(String name) {
		return packageJavaLangClasses.containsKey(name);
	}

	// ---------------------------------------------------------------------------------------//

	/**
	 * Try to resolve a fieldAccessExpression as a package.class. The
	 * fieldAccessExpression MAY represent a package.class. Anyway, it is a
	 * period-separated identifier list, so its primary.qualifiedName() could be
	 * a package name...
	 * 
	 * This function will be called for ALL such fieldAccessExpressions in
	 * statements. Therefore, an extra cache of known and illegal package names
	 * is maintained. 'known' are all names of subdirectrories of the
	 * directories corresponding with class path elements, e.g., 'java'. It does
	 * not matter that 'java' itself is not actually used as a package...
	 */
	public ClassType resolveClassForCompilationUnit(
			CompilationUnit compilationUnit,
			FieldAccessExpression fieldAccessExpression, boolean loadAsWell) {

		String pakkage = fieldAccessExpression.primaryExpression
				.qualifiedName();

		// first use the fast package cache to see whether resolution is
		// possible:
		if (!isKnownPackageName(pakkage)) {
			return null;
		}
		// now resolve the old way:
		return resolveClass(pakkage, fieldAccessExpression.name, loadAsWell);
	}

	// ---------------------------------------------------------------------------------------//

	// ---------------------------------------------------------------------------------------//

	/**
	 * resolve the given datatype declaration.
	 */
	protected DataType resolveDataTypeDeclaration(
			CompilationUnit compilationUnit,
			DataTypeDeclaration dataTypeDeclaration) {
		if (dataTypeDeclaration.dataType != null) { // already done in pass 3
			return dataTypeDeclaration.dataType;
		}
		switch (dataTypeDeclaration.primitiveTypeToken) {
		case ByteToken:
		case CharToken:
		case DoubleToken:
		case FloatToken:
		case IntToken:
		case LongToken:
		case ShortToken:
		case BooleanToken:
		case VoidToken:
			return dataTypeDeclaration.dataType = PrimitiveType.forToken(
					dataTypeDeclaration.primitiveTypeToken).makeArray(
					dataTypeDeclaration.noOfArrayDimensions);
		}
		ClassType result = null;
		if (dataTypeDeclaration.nameComponents == null) {
			result = resolveClassNameWithoutSlashes(compilationUnit,
					dataTypeDeclaration.name, true);
		} else {
			String name = dataTypeDeclaration.nameComponents.get(0);
			result = resolveClassNameWithoutSlashes(compilationUnit, name, true);

			for (int i = 1;; i++) {

				if (result != null) {
					// the start is there. Now the rest should also come...
					for (; i < dataTypeDeclaration.nameComponents.size(); i++) {
						name = dataTypeDeclaration.nameComponents.get(i);

						// // @#$@#$ nestedClassesByName WAS not filled from
						// class files !!! ERROR
						ClassType nestedClass = (ClassType) result.nestedClassesByName
								.get(name);
						if (nestedClass == null) {
							parserError(2, "Class or interface "
									+ result.nameWithDots
									+ " does not contain type " + name,
									compilationUnit,
									dataTypeDeclaration.nameStartPosition,
									dataTypeDeclaration.nameEndPosition);
							return null;
						}
						result = nestedClass;
					}
					break;
				}
				if (i >= dataTypeDeclaration.nameComponents.size()) {
					break;
				}
				name += '.' + dataTypeDeclaration.nameComponents.get(i);
				result = resolveClassNameWithDots(name, true);
			}
		}
		if (result == null) {
			parserError(3, "Class " + dataTypeDeclaration.name
					+ " cannot be resolved", compilationUnit,
					dataTypeDeclaration.nameStartPosition,
					dataTypeDeclaration.nameEndPosition);
			result = new UnresolvedClassOrInterfaceType();
		}
		return dataTypeDeclaration.dataType = result
				.makeArray(dataTypeDeclaration.noOfArrayDimensions);
	}

	// ---------------------------------------------------------------------------------------//

	/**
	 * resolve a class by its name (may have '/'); cache in the appropriate hash
	 * tables ... Checks for slashes in the name; if none, it looks at package
	 * statement, import etc. DEACTIVATED **********************************
	 * protected ClassType resolveClassNameForCompilationUnit (CompilationUnit
	 * compilationUnit, String className, boolean loadAsWell ) { //if
	 * (className.equals("IntHolder"))
	 * output.println("resolveClassNameForCompilationUnit(IntHolder): "
	 * +compilationUnit); if (className.indexOf('/')<0) return
	 * resolveClassNameWithoutSlashes (compilationUnit, className, loadAsWell);
	 * else return resolveClassNameWithDots (className.replace('/','.'),
	 * loadAsWell); }
	 *******************************/
	// ---------------------------------------------------------------------------------------//

	// ---------------------------------------------------------------------------------------//
	/**
	 * resolve a statically imported member variable by its bare name
	 */
	
	
	/**
	 * resolve a class by its bare name without package; cache in the
	 * appropriate hash tables ... Does a lot of work at the CompilationUnit,
	 * but it belongs in this class.
	 */
	protected ClassType resolveClassNameWithoutSlashes(
			CompilationUnit compilationUnit, String className,
			boolean loadAsWell) {

		ClassType result = (ClassType) compilationUnit.knownClassTypes
				.get(className);
		if (result != null) {

			if (!result.hasBeenLoaded() && !result.hasBeenCompiled()
					&& loadAsWell) {
				load(result, result.packageNameWithSlashes, result
						.packageNameWithDots(), result.className);
			}
			return result;
		}
		try { // find it; finally, store in cache...

			ImportStatement is = (ImportStatement) compilationUnit.importedClasses
					.get(className);
			if (is != null) {
				result = resolveClass(is.getPackagePart(), className,
						loadAsWell);
				if (result == null) {
					parserError(2, "Could not load class " + is.name,
							compilationUnit, is.nameStartPosition,
							is.nameEndPosition);
				}
				return result;
			}
			result = resolveClass(compilationUnit.packageStatement, className,
					loadAsWell);

			if (result != null)
				return result;

			result = resolveJavaLangClass(className, loadAsWell);

			if (result != null)
				return result;

			if (compilationUnit.hasImportStatements()) { // now try the wild
															// cards...
				for (ImportStatement importStatement : compilationUnit.importStatements) {
					if (!importStatement.importOnDemand)
						continue;
					if (importStatement.importStatic)
						continue;
					PackageStatement p = importStatement.getPackagePart();
					if (p.getName().equals("java.lang")) {
						continue;
					}
					ClassType nextResult = resolveClass(p, className,
							loadAsWell);
					if (nextResult != null) {
						if (result != null) {
							parserError(
									2,
									"Ambiguous import on demand for class "
											+ className,
									compilationUnit,
									compilationUnit.packageStatement.nameStartPosition,
									compilationUnit.packageStatement.nameEndPosition);
						}
						result = nextResult;
					}
				} // end of imports loop
			}
		} finally {
			if (result != null)
				compilationUnit.knownClassTypes.put(className, result);
		}
		return result;
	}

	// ---------------------------------------------------------------------------------------//

	/*
	 * resolve given class in class path, given a package e.g. for
	 * "import java.util.*; ... Vector.addElement"
	 */
	protected ClassType resolveClass(PackageStatement packageStatement,
			String clazz, boolean loadAsWell) {
		return resolveClass(packageStatement == null ? "" : packageStatement
				.getName(), clazz, loadAsWell);
	}

	// ---------------------------------------------------------------------------------------//

	/*
	 * resolve given class in class path, given a package e.g. for
	 * "import java.util.*; ... Vector.addElement"
	 */
	protected ClassType resolveClass(String pakkage, String clazz,
			boolean loadAsWell) {
		return resolveClass(pakkage, clazz, clazz, loadAsWell);
	}

	/*
	 * resolve given class, possibly an inner class, in class path, given a
	 * package e.g. for "import java.util.*; ... Vector.addElement"
	 */
	protected ClassType resolveClass(String pakkage, String clazz,
			String innerName, boolean loadAsWell) {
		// traceKnownClassTypes();
		StringBuffer fullNameBuffer = new StringBuffer();
		if (pakkage.length() != 0)
			fullNameBuffer.append(pakkage).append('.');
		fullNameBuffer.append(clazz);

		ClassType result = knownClassTypes.get(fullNameBuffer.toString());
		String pakkageWithSlashes = pakkage.replace('.', '/');

		if (result == null) {

			for (int i = 0; i < classPathElements.length; i++) {

				ClassFile classFile = classPathElements[i].findClassFile(
						pakkage, clazz);
				if (classFile == null)
					continue;
				result = new ClassType(pakkage, clazz, innerName, classFile);
				classPathElements[i].addToClassPathPackage(pakkage, clazz,
						result);

				knownClassTypes.put(fullNameBuffer.toString(), result);
				if (doTrace) {
					trace("resolveClass: " + fullNameBuffer + " >> " + result
							+ " .. " + result.getNameWithDots() + " // "
							+ result.getNameWithSlashes());
				}
				if (loadAsWell) {

					load(result, pakkageWithSlashes, pakkage, clazz);

					ClassFileAttribute attr;
					if (result.attributesContainer != null
							&& (attr = result.attributesContainer
									.getInnerClasses()) != null) {
						InnerClassesAttribute ia = (InnerClassesAttribute) attr;
						for (int j = 0; i < ia.entries.size(); i++) {
							InnerClassEntry e = ia.entries.get(j);
							try {
								if (e.outerClass.getName(result).equals(
										result.className)) {
									ClassType c = resolveClass(pakkage,
											e.innerClass.getName(result),
											e.innerName.getName(result),
											loadAsWell);
									if (c != null) {
										result.nestedClassesByName.put(
												c.className, c);
									}
								}
							} catch (java.io.IOException exc) {
								parserError(2, exc.toString());
							} catch (ByteCodingException exc) {
								parserError(3, exc.toString());
							}
						}
					}
				}
				break;
			}
		} else if (!result.hasBeenLoaded() && !result.hasBeenCompiled()
				&& loadAsWell) {

			if (result.classFile == null) {

				for (int i = 0; i < classPathElements.length; i++) { // almost
																		// the
																		// same
																		// loop
																		// as
																		// above...

					result.classFile = classPathElements[i].findClassFile(
							pakkage, clazz);
					if (result.classFile != null) {
						break;
					}
				}
				if (result.classFile == null) {
					return null;
				}
			}
			load(result, pakkageWithSlashes, pakkage, clazz);
		}
		return result;
	}

	// ---------------------------------------------------------------------------------------//

	/**
	 * load the ClassType from its class file. May be overloaded to include
	 * timing
	 */
	protected ClassType load(ClassType c, String pakkageWithSlashes,
			String pakkage, String clazz) {
		if (c.classFile == null) {
			new Exception("load - classFile==null: " + c.getPresentation())
					.printStackTrace();
		}
		try {
			c.load(this); // LOADED here !!!. supertypes not yet done...

			if (c.packageNameWithSlashes.length() != 0
					&& !c.packageNameWithSlashes.equals(pakkageWithSlashes)) {

				if (c.classFile instanceof ZipClassFile
						|| c.classFile instanceof UncompressedZipClassFile) {
					resolverError(c.classFile.getPath()
							+ " unexpectedly contains class "
							+ c.packageNameWithDots() + "." + c.className);
				} else {
					output.println("Warning: " + c.classFile.getPath()
							+ " does not contain class "
							+ (pakkage.length() == 0 ? "" : pakkage + '.')
							+ clazz + " as expected, but " + c.nameWithDots
							+ " instead");
				}
				return null;
			} else {

				endOfLoading(c); // resolve superclass and interfaces as well
									// (no immediate loading)
				// or in subclass CompilerEnvironment:
				// mark result so that superclass and interfaces will be set
				// later
			}
		} catch (RuntimeException e) {
			output.println(c.getDescription(this));
			throw (e);
		} catch (IOException e) {
			resolverError(e + "; when loading class " + c.nameWithDots
					+ " from " + c.classFile.getPath());
			e.printStackTrace();
		} catch (CompilerError e) {
			resolverError("Error loading class " + c.nameWithDots + " from "
					+ c.classFile.getPath() + ": " + e.getMessage());
		} catch (ByteCodingException e) {
			resolverError("Error loading class " + c.nameWithDots + " from "
					+ c.classFile.getPath() + ": " + e.getMessage());
		}
		return c;
	}

	// ---------------------------------------------------------------------------------------//

	/**
	 * Finish the given ClassType's loading: load superclass and interfaces, or
	 * in subclass CompilerEnvironment: mark the ClassType so that superclass
	 * and interfaces will be set later To be called by resolveClass, after
	 * loading...
	 */
	protected void endOfLoading(ClassType c) {
		/**** c.resolveUsedClasses (this, false); ****/
	}

	// ---------------------------------------------------------------------------------------//

	ClassType resolveClassNameWithSlashes(String pakkageclazz,
			boolean loadAsWell) {
		// NOTE: check for '$' !!!!!!
		return resolveClassNameWithDots(pakkageclazz.replace('/', '.'),
				loadAsWell);
	}

	/*
	 * resolve a class in class path, given its fully qualified name e.g. for
	 * "import java.util.*; ... Vector.addElement"
	 */
	ClassType resolveClassNameWithDots(String pakkageclazz, boolean loadAsWell) {
		String pakkage = "";
		String clazz = pakkageclazz;
		int i = pakkageclazz.lastIndexOf('.');
		if (i >= 0) {
			pakkage = pakkageclazz.substring(0, i);
			clazz = pakkageclazz.substring(i + 1);
			if (pakkage.equals("java.lang"))
				return resolveJavaLangClass(clazz, loadAsWell);
		}
		return resolveClass(pakkage, clazz, loadAsWell);
	}

	// ---------------------------------------------------------------------------------------//

	/**
	 * Register the given class name as a member of package "java.lang"
	 */
	protected void addJavaLangClassName(String className) {
		packageJavaLangClasses.put(className, className);
	}

	// ---------------------------------------------------------------------------------------//

	/**
	 * Attempt to resolve a class in package "java.lang"
	 */
	protected ClassType resolveJavaLangClass(String className,
			boolean loadAsWell) {
		Object obj = packageJavaLangClasses.get(className);
		if (obj == null)
			return null;
		ClassType result = null;
		if (obj instanceof String) {
			result = resolveClass("java.lang", className, loadAsWell);
			if (result != null)
				packageJavaLangClasses.put(className, result);
			else
				System.out.println("packageJavaLangClasses not found: "
						+ className);
		} else { // obj instanceof ClassType
			result = (ClassType) obj;
			if (!loadAsWell || result.hasBeenLoaded())
				return result;
			result = resolveClass("java.lang", className, loadAsWell);
			// if (result == null) error?
		}
		return result;
	}

	/*----------------------------- Display ------------------------------*/

	public static int[] getConstructSortOrder(ArrayList<ClassType> cc) {
		int[] indexes = new int[cc.size()];
		for (int index = 0; index < indexes.length; index++)
			indexes[index] = index;

		if (cc.size() <= 1)
			return indexes;

		/* Simple, indexed bubble sort */
		boolean sorted = false;
		int sortBoundary = indexes.length - 1;

		/* Main sort loop, sweep toward the end of the array */
		while (!sorted && sortBoundary > 0) {
			sorted = true;
			for (int index = 0; index < sortBoundary; index++) {
				ClassType c1 = cc.get(indexes[index]);
				ClassType c2 = cc.get(indexes[index + 1]);
				if (c1.className.compareTo(c2.className) > 0) {
					sorted = false;
					int temp = indexes[index];
					indexes[index] = indexes[index + 1];
					indexes[index + 1] = temp;
				}
			}
			sortBoundary--;
		}

		return indexes;
	}

	/*
	 * Get constructs in sorted order -- EXAMPLE public String
	 * getSortedPartnerNames (ArrayList<ClassType> cc) { StringBuffer names =
	 * new StringBuffer (); int [ ] indexes = getConstructSortOrder (cc);
	 * 
	 * for (int index = 0; index < indexes.length; index++) { ClassType c =
	 * cc.get (indexes[index]);
	 * 
	 * // ...process construct... // if (index > 0) // names.append (','); //
	 * names.append (c.getName()); } return names.toString (); }
	 */
	void gcIfNeeded() {
		Runtime r = Runtime.getRuntime();
		long freeMem = r.freeMemory();

		if (freeMem > 512 * 1024)
			return;
		// long t = timerLocalStart();
		r.gc();
		// timerLocalStop (GCMsg, ""+freeMem, t);
	}

	// ---------------------------------- timing
	// -----------------------------------/

	static final int InitializedMsg = 0;
	static final int ParsedMsg = 1;
	static final int Inspected0Msg = 2;
	static final int InspectedMsg = 3;
	static final int CheckedMsg = 4;
	static final int ResolvedMsg = 5;
	static final int GeneratedCodeMsg = 6;
	static final int GeneratedJavaCodeMsg = 7;
	static final int WrittenMsg = 8;
	static final int LoadedMsg = 9;
	static final int OptimizeMsg = 10;
	static final int WriteCstPoolMsg = 11;
	static final int WriteOtherMsg = 12;
	static final int ReparseMsg = 13;
	static final int GCMsg = 14;

	static final String categories[] = { "Initialized", "Parsed", "Inspected0",
			"Inspected", "Checked", "Resolved", "Generated code",
			"Generated Java code", "Written", "  Loaded", "  Optimized",
			"  Wrote CstPool", "  Wrote Other", "  Reparse", "  GC" };

	long startMilliseconds;
	long lastMilliseconds;
	long cumulativeTimes[] = new long[categories.length];

	void timerMsg(String msg, String name, long millis) {
		String timeStr = String.valueOf(millis);
		StringBuffer buf = new StringBuffer();
		// buf.append ("[");
		for (int i = timeStr.length(); i < 6; i++)
			buf.append(' ');
		buf.append(timeStr);
		// buf.append ("] ");

		/*****
		 * Runtime r = Runtime.getRuntime(); long freeMem = r.freeMemory();
		 * String s = ""+freeMem; for (int i=s.length(); i<9; i++) buf.append
		 * (' '); buf.append (s);
		 * 
		 * long totalMem = r.totalMemory(); s = ""+totalMem; for (int
		 * i=s.length(); i<9; i++) buf.append (' '); buf.append (s);
		 ****/
		buf.append(" ");

		buf.append(msg);
		if (name.length() > 0) {
			for (int i = msg.length(); i < 20; i++)
				buf.append(' ');
			buf.append(name);
		}
		output.println(buf);
		output.flush();
	}

	void timerStart() {
		startMilliseconds = lastMilliseconds = System.currentTimeMillis();
		cumulativeTimes = new long[categories.length];
	}

	void timerStop(String msg) {
		long totalTime = System.currentTimeMillis() - startMilliseconds;
		timerMsg(msg, "", totalTime);
		/*
		 * for (int i=0; i<cumulativeTimes.length; i++) { if
		 * (cumulativeTimes[i]>0) { double percents =
		 * ((1000cumulativeTimes[i])/totalTime)/10.0; String percentString = "";
		 * if (percents<10) percentString += " "; percentString += percents;
		 * timerMsg (categories[i], percentString, cumulativeTimes[i]); } if
		 * (i==WrittenMsg) output.println("--------------------------"); }
		 */
	}

	void timer(int categoryIndex, String name) {
		if (!doTiming)
			return;
		long currentMillis = System.currentTimeMillis();
		long millis = currentMillis - lastMilliseconds;
		timerMsg(categories[categoryIndex], name, millis);
		cumulativeTimes[categoryIndex] += millis;
		lastMilliseconds = currentMillis;
	}

	long timerLocalStart() {
		return System.currentTimeMillis();
	}

	void timerLocalStop(int categoryIndex, String name, long startTime) {
		if (!doTiming)
			return;
		long currentMillis = System.currentTimeMillis();
		long millis = currentMillis - startTime;
		switch (categoryIndex) {
		case LoadedMsg:
		case GCMsg:
			timerMsg(categories[categoryIndex], name, millis);
		}
		cumulativeTimes[categoryIndex] += millis;
	}
}
