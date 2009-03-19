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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import scriptic.tools.lowlevel.*;


   /*-----------------------------------------------------------------*/
   /*                                                                 */
   /*                        ClassType                                */
   /*                                                                 */
   /*-----------------------------------------------------------------*/


public class ClassType extends NonArrayReferenceType
                    implements ClassEnvironment, AttributeOwner {

   // some variables have a temporary suffix '1',
   // meaning that, in case of a loaded class file,
   // their values should be set before first use, with a get... method
   ClassType superclass1;
   ClassType interfaces1[];
   HashMap<String, ClassType> nestedClassesByName;
   LocalOrNestedClassType innerClasses[] = new LocalOrNestedClassType[0];
   String    superclassName;  // filled after loading; later to be resolved into superclass
   String    interfaceNames[]; // filled after loading; later to be resolved into interfaces

   public String    className;
   public String  packageNameWithSlashes = "";
   public String  packageNameWithDots() {return packageNameWithSlashes.replace('/', '.');}

   public String nameWithDots;
   public String nameWithSlashes;
   public String getPresentationName () {return nameWithDots;}
   private String signature;

   public MemberVariable   variables[]; //may be filled by ScripticCompilerPass4
   public Method           methods  []; //may be filled by ScripticCompilerPass4
   public int              fieldCount;

   public  boolean         sourceFileChecked; // when loaded: whether source file has been determined
   private boolean         hasError;
   public  boolean         hasError() {return hasError;}
   public  TypeDeclaration sourceDeclaration;
   public  Modifiers       modifiers = new Modifiers();;
   public  Modifiers       modifiers() {return modifiers;}

   public int getToken() {return nameWithDots.equals("java.lang.String")? StringLiteralToken: 0;}
   
   public boolean isResolved () {return !hasError;}
                             // && (hasBeenCompiled() || hasBeenLoaded());}  does not work...

   public  ConstructorDeclaration defaultConstructor;
   public  int accessMethodIndex = 1; // sequence number, for access methods "access$1" etc.
   AttributesContainer attributesContainer;
   // constant pool stuff...


   /* Hash tables to accumulate per-class method and variable declaration info */
   protected HashMap<String, MemberVariable> variablesByName;          // null, needed for get-method
   protected HashMap<String, HashMap<String, Object>> methodNamesSignatures1; // null, needed for get-method

   HashMap<String      , ConstantPoolUnicode>                  constantPoolLookup_Utf8;
   HashMap<Integer     , ConstantPoolInteger>                  constantPoolLookup_Integer;
   HashMap<Float       , ConstantPoolFloat>                    constantPoolLookup_Float;
   HashMap<Long        , ConstantPoolLong>                     constantPoolLookup_Long;
   HashMap<Double      , ConstantPoolDouble>                   constantPoolLookup_Double;
   HashMap<String      , ConstantPoolClass>                    constantPoolLookup_Class;
   HashMap<String      , ConstantPoolString>                   constantPoolLookup_String;
   HashMap<StringBuffer, ConstantPoolFieldReference>           constantPoolLookup_Fieldref;
   HashMap<StringBuffer, ConstantPoolMethodReference>          constantPoolLookup_Methodref;
   HashMap<StringBuffer, ConstantPoolInterfaceMethodReference> constantPoolLookup_InterfaceMethodref;
   HashMap<StringBuffer, ConstantPoolNameAndType>              constantPoolLookup_NameAndType;

   private ConstantPoolItem constantPool1        [];
   //private int              constantPoolPositions[];
   int   constantPoolSize  = 64;
   int   constantPoolCount =  1; // should stay <64k.
      // constantPoolCount should be used rather than constantPool1.lenght,
      // since constantPool1 grows like a Vector, in case of generating from a source file.

   DataInputStream constantPoolStream;
   public ConstantPoolItem getConstantPoolItem (int index) throws ByteCodingException, IOException {

       if (index < 0
       ||  index >= constantPool1.length) {
           throw new RuntimeException ("getConstantPoolItem: array index "+index
                                      +" out of range (max "+(constantPool1.length-1)+")");
       }
       ConstantPoolItem result = constantPool1[index];
       return result;
   }

           File      sourceFile;
   public  ClassFile classFile;

   private int       origin; // compiled or loaded or ...?
   public void       setCompiled    () {       origin =  1;}
   public void       setLoaded      () {       origin =  2;}
   public boolean    hasBeenCompiled() {return origin == 1;}
   public boolean    hasBeenLoaded  () {return origin == 2;}
   public boolean isInDefaultPackage() {return packageNameWithSlashes==null;}

   int localClassSequenceNumber; // #local classes, not the nested ones
   public ClassPackage     classPackage;
   public ClassPathPackage classPathPackage;

   public String  getNameForClassRef() {return nameWithSlashes;}
   public ClassType parent()           {return null;}
   public int nestingLevel()           {return 0;}
   public MemberVariable targetForEnclosingInstance (CompilerEnvironment env, ClassType enclosingClass)
          throws CompilerError, IOException {return null;}
   public boolean needsParentReference() {return false;}
   public void    setNeedForParentReference() {}

   static int instances;
   static void incInstances() {
      instances++;
      //if (instances%100==0) System.out.println("ClassFile instances: "+instances);
   }

   public void setSourceFile (File sourceFile) {this.sourceFile = sourceFile;}
   public File sourceFile() {
     if (!sourceFileChecked) {
          sourceFileChecked = true;
       if (hasBeenLoaded()
       &&  attributesContainer != null
       &&  attributesContainer.getSourceFile() != null) {
           String name = attributesContainer.getSourceFile().sourceFileName;
           if (name.indexOf (File.separator) < 1) {

               // copy directory part from class file...
               int index = classFile.getPath().lastIndexOf(File.separator);
               if (index >= 0) {
                   name = classFile.getPath().substring (0, index+1) + name;
               }
           }
           sourceFile = new File (name);
           if (!sourceFile.isFile()
           ||  !sourceFile.canRead())
                sourceFile = null;
       }
     }
     return sourceFile;
   }

   public String getSourceFilePath() {
       if (hasBeenLoaded()) {
          if (attributesContainer==null) return null;
          SourceFileAttribute sa = attributesContainer.getSourceFile();
          if (sa==null) return null;
          String result = sa.sourceFileName;
          if  (classFile != null
          &&   result.lastIndexOf (File.separatorChar) < 0) { // copy classFile's directory path

             String cfp = classFile.getPath();
             int index  = cfp.lastIndexOf (File.separatorChar); 
             if (index >= 0) {
                 cfp = cfp.substring (0, index+1);
                 result = cfp + result;
             }
          }
          return result;
       }
       if (sourceFile()==null) return null;
       return sourceFile.getPath();
   }

   public String getClassFileDirectoryName(CompilerEnvironment env) {
      String result = "";
      if (env.outputDirectoryName==null)
      {
	      if  (sourceFile != null) {
	      
	         result = sourceFile.getPath();
	         int index  = result.lastIndexOf (File.separatorChar);
	         if (index >= 0) {
	             result = result.substring (0, index+1);
	         }
	         else {
	             result = "";
	         }
	      }
      }
      else
      {
    	  result = env.outputDirectoryName 
    	         + File.separatorChar;
    	  if (packageNameWithSlashes.length()>0)
    	  {
    		  result += packageNameWithSlashes.replace('/', File.separatorChar)
    		         +  File.separatorChar;
    	  }
      }
      return result;
   }
   public String getClassFilePath(CompilerEnvironment env) {
      if  ( classFile != null) return classFile.getPath();
      // File sourceFile  = sourceDeclaration.compilationUnit.sourceFile;

      String result = getClassFileDirectoryName(env)
                    + nameForFile() +  ".class";
      return result;
   }
   String nameForFile () {return className;}

   ClassType(String fullNameWithDots, String clazz) {
     incInstances();
     initializeTables();
     className       = clazz;
     nameWithDots    = fullNameWithDots;
     nameWithSlashes = fullNameWithDots.replace('.','/');
     packageNameWithSlashes = "";
     if (nameWithDots.length()==0)
     {
        new Exception("Class "+clazz+" has empty fullNameWithDots").printStackTrace();
     }
     int i = fullNameWithDots.lastIndexOf('.');
     if (i>0) packageNameWithSlashes = fullNameWithDots.substring(0, i);
   }

   ClassType(String pakkage, String clazz, String innerName, ClassFile classFile) {
     incInstances();
     initializeTables();
     this.classFile = classFile;
     this.packageNameWithSlashes = pakkage.replace('.','/');
     this.className   = clazz;
     StringBuffer s = new StringBuffer();
     if (packageNameWithSlashes.length()>0) {
         s.append(packageNameWithSlashes).append('/');
     }
     s.append(innerName);
     nameWithSlashes = s.toString();
     nameWithDots    = nameWithSlashes.replace('/','.');
   }

   public String getName           () {return nameWithDots;}
   public String getNameWithDots   () {return nameWithDots;}
   public String getNameWithSlashes() {return nameWithSlashes;}

   public void addMethod (Method m) {
       Method old[] = methods;
       methods = new Method[old.length+1];
       System.arraycopy (old, 0, methods, 0, old.length);
       methods[methods.length-1] = m;
   }
   public void addMemberVariable (MemberVariable v) {
       MemberVariable old[] = variables;
       variables = new MemberVariable[old.length+1];
       System.arraycopy (old, 0, variables, 0, old.length);
       variables[variables.length-1] = v;
       if (variablesByName==null) variablesByName = new HashMap<String, MemberVariable>();
       variablesByName.put (v.name, v);
   }

   public void addInterface (ClassType it) {
       if (interfaces1 != null) {
         ClassType old[] = interfaces1;
         interfaces1 = new ClassType[old.length+1];
         System.arraycopy (old, 0, interfaces1, 0, old.length);
       }
       else interfaces1 = new ClassType[1];
       interfaces1[interfaces1.length-1] = it;
   }
   public void addInnerClass (LocalOrNestedClassType lc) {
       if (innerClasses != null) {
         LocalOrNestedClassType old[] = innerClasses;
         innerClasses = new LocalOrNestedClassType[old.length+1];
         System.arraycopy (old, 0, innerClasses, 0, old.length);
       }
       else innerClasses = new LocalOrNestedClassType[1];
       innerClasses[innerClasses.length-1] = lc;
   }

   /* dimension stuff */
   Dimension dimensions[];
   void addDimension(Dimension d) {
       if (dimensions != null) {
         Dimension old[] = dimensions;
         dimensions = new Dimension[old.length+1];
         System.arraycopy (old, 0, dimensions, 0, old.length);
       }
       else dimensions = new Dimension[1];
       dimensions[dimensions.length-1] = d;
   }

   public Dimension resolveDimensionHere (String name) {
      if (dimensions != null) {
         for (int i=0; i<dimensions.length; i++) {
            if (name.equals (dimensions[i].name)) {
               return dimensions[i];
            }
         }
      }
      return null;  
   }

   /** resolve the given dimension name, and check accessability from the given class */
   public Dimension resolveDimension (CompilerEnvironment env, String name)
                                   throws CompilerError, IOException {
      if (!hasBeenCompiled()
      &&  !hasBeenLoaded  ()) {
          env.load (this, packageNameWithSlashes, packageNameWithDots(), className);
      }
                                    
      Dimension result        = resolveDimensionHere (name);
      if (result             == null
      && !isInterface()
      &&  getSuperclass(env) != null) {
          result = superclass1.resolveDimension (env, name);
      }
      if (result     == null)
      for (int i = 0; i<getInterfaces(env).length; i++) {
          Dimension d = interfaces1[i].resolveDimension (env, name);
          if      (d      == null) continue;
          if      (result == null) result = d;
          else if (result != d) {
                   throw new CompilerError ("Dimension '"+name+"' ambiguously inherited from interfaces "
                                           +        result.owner.nameWithDots
                                           +" and "+  d   .owner.nameWithDots);
          }
      }
      return result;
   }
   void getDimensionsFrom (DimensionsAttribute dimensionsAttribute) {
      dimensions = new Dimension[dimensionsAttribute.dimensionNames.length];
//System.out.println(nameWithSlashes+".getDimensionsFrom # "+dimensions.length);
      for (int i = 0; i<dimensions.length; i++) {
         String signatureOrUnit = dimensionsAttribute.signaturesAndUnits[i];
         if (Character.isDigit (signatureOrUnit.charAt(0))) {
            dimensions[i] = new CompoundDimension (this, dimensionsAttribute.dimensionModifiers[i],
                                                         dimensionsAttribute.dimensionNames    [i],
                                                         dimensionsAttribute.signaturesAndUnits[i]);
         }
         else {
            dimensions[i] = new BaseDimension (this, dimensionsAttribute.dimensionModifiers[i],
                                                     dimensionsAttribute.dimensionNames[i],
                                                     dimensionsAttribute.signaturesAndUnits[i]);
         }
//System.out.println(nameWithSlashes+".getDimensionsFrom: "+dimensions[i].getPresentation());
      }
   }
   void addDimensionsAttribute() {
     if (dimensions==null) return;
     int    dimensionModifiers[] = new int    [dimensions.length];
     String dimensionNames    [] = new String [dimensions.length];
     String signaturesAndUnits[] = new String [dimensions.length];
     for (int i=0; i<dimensions.length; i++) {
        Dimension d = dimensions[i];
        dimensionModifiers[i] = dimensions[i].modifierFlags;
        dimensionNames[i]     = dimensions[i].name;
        if (d instanceof BaseDimension)
             signaturesAndUnits[i] = ((    BaseDimension)d).unit.name;
        else signaturesAndUnits[i] = ((CompoundDimension)d).signature;
     }
     addAttribute (new DimensionsAttribute(dimensionModifiers, dimensionNames, signaturesAndUnits)); 
   }

   public void addAttribute (ClassFileAttribute attribute) {
     if (attributesContainer==null)
         attributesContainer = new AttributesContainer(this, this);
     attributesContainer.add (attribute);
   }
   public void addInnerClassEntry (ClassType c) {
     InnerClassesAttribute attr;
     if (attributesContainer==null
     || (attr = attributesContainer.getInnerClasses()) == null) {
         addAttribute (attr = new InnerClassesAttribute());
     }
     attr.add (new InnerClassEntry(resolveClass  (c.nameWithDots),
                                   resolveClass  (c.parent().nameWithDots),
                                   resolveUnicode(c.className),
                                   (short) 0));
   }

   public int line (int position) {
     Scanner scanner = sourceDeclaration.compilationUnit.scanner;
     return scanner.lineAtPosition (position);
   }
   public int positionOnLine (int line, int position) {
     Scanner scanner = sourceDeclaration.compilationUnit.scanner;
     return scanner.positionOnLine (line, position);
   }

   public String  getSignature() {
     if (signature==null) {
       StringBuffer s = new StringBuffer();
       s.append('L');
       s.append(getNameWithSlashes());
       s.append(';');
       signature = s.toString();
     }
     return signature;
   }

   protected void setNameWithSlashes (String s) {
      nameWithSlashes = s;
      nameWithDots    = s.replace('/','.');
      int index       = s.lastIndexOf('/');
      className       = index<0? s : s.substring (index+1);
      packageNameWithSlashes     = index<0? "": s.substring (0,index);
   }

   protected ClassType getSuperclass (ClassesEnvironment env) {
     if (superclass1==null) {
        if (   !hasBeenCompiled()) {
           if (!hasBeenLoaded  ()) {
              env.load (this, packageNameWithSlashes, packageNameWithDots(), className);
           }
           if (superclassName != null) { // else Object
               superclass1 = getFromNameWithSlashes (env, superclassName, this, false);
           }
        }
     }
     return superclass1;
   }
   protected ClassType[] getInterfaces (ClassesEnvironment env) {
     if (interfaces1==null) {
        if (!hasBeenCompiled()
        &&  !hasBeenLoaded  ()) {
            env.load (this, packageNameWithSlashes, packageNameWithDots(), className);
        }
        int n = interfaceNames==null? 0: interfaceNames.length;
        interfaces1 = new ClassType [n];
        for (int i=0; i<interfaces1.length; i++) {
           interfaces1[i] = getFromNameWithSlashes (env, interfaceNames[i], this);
        }
     }
     return interfaces1;
   }
   final HashMap<String, MemberVariable> getVariablesByName (ClassesEnvironment env) {
     if (variablesByName==null) {
        if (!hasBeenCompiled()
        &&  !hasBeenLoaded  ()) {
            env.load (this, packageNameWithSlashes, packageNameWithDots(), className);
        }
        // env.load does it all...
     }
     return variablesByName;
   }

   HashMap<String, HashMap<String, Object>> getMethodNamesSignatures (ClassesEnvironment env) {
      if (methodNamesSignatures1 == null) {
          if (!hasBeenCompiled()
          &&  !hasBeenLoaded   ()) {
              env.load (this, packageNameWithSlashes, packageNameWithDots(), className);
          }
          methodNamesSignatures1 = new HashMap<String, HashMap<String, Object>>();

          //System.out.println(className+".getMethodNamesSignatures: "+methods.length);
          // maybe this part only after loading...??
          for (int i = 0; i < methods.length; i++) {
        	  HashMap<String, Object> signatures = methodNamesSignatures1.get(methods[i].name);
             if (signatures == null) {
                signatures = new HashMap<String, Object>();
                methodNamesSignatures1.put (methods[i].name, signatures);
             }
             signatures.put (methods[i].signature, methods[i]);
          }
      }
      return methodNamesSignatures1;
   }


   /** resolve superclass, interfaces and signatures of the given class type
    *  If the given ClassesEnvironment is null, or !loadAsWell
    *    no attempt is made to load a class type from file
**************************
   protected final void resolveUsedClasses (ClassesEnvironment env,
                                            boolean   loadAsWell ) {
//System.out.println ("resolveUsedClasses: "+className);
       if (env != null) {
         if (superclassName != null) {
             superclass = getFromNameWithSlashes (env, superclassName, this, loadAsWell);
//System.out.println ("resolve superclass: "+superclassName);
         }
         if (interfaceNames != null) {
             interfaces = new ClassType [interfaceNames.length];
             for (int i=0; i<interfaces.length; i++) {
               interfaces[i] = getFromNameWithSlashes (env, interfaceNames[i], this, loadAsWell);
//System.out.println ("resolve interface: "+interfaceNames[i]);
             }
         }
       }
//System.out.println ("resolveUsedClasses: "+className+" #variables: "+variables.length);
       for (int i=0; i<variables.length; i++) {
         MemberVariable v = variables[i];
         v.dataType       = DataType.getFromSignature (env, v.signature, this, loadAsWell);
         v.resolveConstantValue();
       }
       for (int i=0; i<methods.length; i++) {
         Method m = methods[i];
         m.parseSignature (env, loadAsWell);
       }
   }
*************/



   public boolean isClassOrInterface() {return  true;}
   public boolean isClass           () {return !modifiers.isInterface();}
   public boolean isInterface       () {return  modifiers.isInterface();}
   public boolean isPublic          () {return  modifiers.isPublic   ();}
   public boolean isFinal           () {return  modifiers.isFinal    ();}
   public boolean isStatic          () {return  modifiers.isStatic   ();}
   public boolean isAbstract        () {return  modifiers.isAbstract ();}

   public boolean isSubtypeOf (CompilerEnvironment env, DataType d) throws IOException, CompilerError {
     if (this == d                     ) return  true;
     if (null == d                     ) return false;
     if (d    ==env.javaLangObjectType ) return  true; // now test for presence in knownSuperTypes?
     if (!d.isClassOrInterface()       ) return false;
 //  if (!isResolved()                 ) return false; // ?? already handled ??
     if (getSuperclass(env) == null    ) return false; // so this is javaLangObject...
     if (superclass1.isSubtypeOf(env,d)) return  true;
                                                       // we could test here for d.isInterface(), 
     for (int i=0; i<getInterfaces(env).length; i++)   //      but that would require loading...
     if (interfaces1[i].isSubtypeOf(env,d)) return true;
     return false;
   }

   /** Casting compatibility - $5.5
    *  @return null: OK, else reason why not OK
    */
   public String whyCannotBeCastedTo(CompilerEnvironment env, DataType d) throws IOException, CompilerError {
     if (!d.isReference()) return "only one reference type involved";
     if (isClass()) {
       if (d.isInterface()) {
         if (!modifiers.isFinal()) return null;
         return isSubtypeOf (env, d)? null: "final class is not compatible with interface it does not inherit from";
       }
       if (d.noOfArrayDimensions() > 0) return this==env.javaLangObjectType
                                             ? null: "array is only compatible with class java.lang.Object";
       return isSubtypeOf (env, d)
         || d.isSubtypeOf (env, this)? null: "no inheritance relation between classes";
     }
     else { // isInterface()
       if (d.isClass()) {
         ClassType c = (ClassType) d;
         if (!c.isFinal()) return null;
         return c.isSubtypeOf (env, this)? null: "final class is not compatible with interface it does not inherit from";
       }
       if (d.isInterface()) {
         ClassType c = (ClassType) d;
         Method r = findMethodWithSameSignatureButDifferentReturnTypesAsIn(env, c);
         return r==null? null
              : ("refinement "+r.name+ " occurs with same signature but also with different return types");
       }
       return "interface cannot be casted to array";
     }
   }


  public void initializeTables() {
      superclass1            = null;
      interfaces1            = null;
      methodNamesSignatures1 = null; // must before reading .class file
      variablesByName          = null; // must before reading .class file
      sourceFileChecked      = false;
      nestedClassesByName                   = new HashMap<String, ClassType>();
      constantPoolLookup_Utf8               = new HashMap<String      , ConstantPoolUnicode>                 ();
      constantPoolLookup_Integer            = new HashMap<Integer     , ConstantPoolInteger>                 ();
      constantPoolLookup_Float              = new HashMap<Float       , ConstantPoolFloat>                   ();
      constantPoolLookup_Long               = new HashMap<Long        , ConstantPoolLong>                    ();
      constantPoolLookup_Double             = new HashMap<Double      , ConstantPoolDouble>                  ();
      constantPoolLookup_Class              = new HashMap<String      , ConstantPoolClass>                   ();
      constantPoolLookup_String             = new HashMap<String      , ConstantPoolString>                  ();
      constantPoolLookup_Fieldref           = new HashMap<StringBuffer, ConstantPoolFieldReference>          ();
      constantPoolLookup_Methodref          = new HashMap<StringBuffer, ConstantPoolMethodReference>         ();
      constantPoolLookup_InterfaceMethodref = new HashMap<StringBuffer, ConstantPoolInterfaceMethodReference>();
      constantPoolLookup_NameAndType        = new HashMap<StringBuffer, ConstantPoolNameAndType>             ();
  }                                          
  void initializeForSource (CompilationUnit c) {
      initializeTables();
      variablesByName         = new HashMap<String, MemberVariable>();
      constantPoolSize        = 5;
      constantPool1           = new ConstantPoolItem [constantPoolSize];
      constantPoolCount       = 1;
      variables               = new MemberVariable[0];
      methods                 = new Method        [0];
      dimensions              = null;
      attributesContainer     = null;
      packageNameWithSlashes  = c.getPackageNameWithSlashes();
      classFile               = null; // maybe it existed; then it gets overwritten...
      setCompiled();
      modifiers.modifierFlags|= SuperFlag;
  }
  void initializeForSource (TypeDeclaration t, CompilationUnit c, File sourceFile) {
      initializeForSource (c);
      sourceDeclaration       = t;
      modifiers.modifierFlags = t.modifiers.modifierFlags;
      modifiers.modifierFlags|= SuperFlag;
      setSourceFile           ( sourceFile); // c.sourceFile ?????????
      sourceFileChecked       = true;
  }


  void growConstantPool(int i) {
    constantPoolCount += i;
    while (constantPoolCount >= constantPoolSize) {
           ConstantPoolItem oldConstantPool[] = constantPool1;
           constantPoolSize *= 2;
           constantPool1 = new ConstantPoolItem [constantPoolSize];
           System.arraycopy (oldConstantPool, 0 , constantPool1, 0, oldConstantPool.length);
    }
  }
  void addItemToConstantPool (ConstantPoolItem item) {
    item.slot = constantPoolCount;
    growConstantPool (item.noOfSlots ());

if (constantPool1==null) System.out.println("constantPool1==null: "+nameWithDots);
if (item         ==null) System.out.println("item         ==null: "+nameWithDots);
    constantPool1 [item.slot] = item;
  }

  /**
   * resolve ConstantPoolItems; these are "uniquefied".
   * Ie, if you add a ConstantPoolItem whose
   * contents already exist in the ConstantPool, only one entry is finally
   * written out when the ConstantPoolItem is written.
   */

  // clazz is a DataType; namely: could be an array type!
  public ConstantPoolClass resolveClass(DataType clazz) {
      return resolveClass (clazz.getNameForClassRef());
  }

  public ConstantPoolClass resolveClass(String s)
  {
    ConstantPoolClass item = (ConstantPoolClass) constantPoolLookup_Class.get(s);
    if (item==null) {
      item = new ConstantPoolClass (resolveUnicode(s));
      constantPoolLookup_Class.put (s, item);
      addItemToConstantPool (item);
    }
    return item;
  }


  public ConstantPoolFieldReference resolveFieldReference(String clazz, String name, String sig)
  {
    StringBuffer sb = new StringBuffer();
    sb.append (clazz).append(' ').append (name).append(' ').append (sig);
    ConstantPoolFieldReference item = (ConstantPoolFieldReference)constantPoolLookup_Fieldref.get(sb);
    if (item==null) {
      item = new ConstantPoolFieldReference (resolveClass      (clazz),
                                             resolveNameAndType(name, sig));
      constantPoolLookup_Fieldref.put (sb, item);
      addItemToConstantPool (item);
    }
    return item;
  }

  public ConstantPoolMethodReference resolveMethodReference(String clazz, String name, String sig)
  {
    StringBuffer sb = new StringBuffer();
    sb.append (clazz).append(' ').append (name).append(' ').append (sig);
    ConstantPoolMethodReference item = (ConstantPoolMethodReference)constantPoolLookup_Methodref.get(sb);
    if (item==null) {
      item = new ConstantPoolMethodReference(resolveClass      (clazz),
                                             resolveNameAndType(name, sig));
      constantPoolLookup_Methodref.put (sb, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolInterfaceMethodReference resolveInterfaceMethodReference(String clazz, String name, String sig)
  {
    StringBuffer sb = new StringBuffer();
    sb.append (clazz).append(' ').append (name).append(' ').append (sig);
    ConstantPoolInterfaceMethodReference item = (ConstantPoolInterfaceMethodReference)
                                                 constantPoolLookup_InterfaceMethodref.get(sb);
    if (item==null) {
      item = new ConstantPoolInterfaceMethodReference(resolveClass      (clazz),
                                                      resolveNameAndType(name, sig));
      constantPoolLookup_InterfaceMethodref.put (sb, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolNameAndType resolveNameAndType(String name, String sig)
  {
    StringBuffer sb = new StringBuffer();
    sb.append (name).append(' ').append (sig);
    ConstantPoolNameAndType item = (ConstantPoolNameAndType)
                                    constantPoolLookup_NameAndType.get(sb);
    if (item==null) {
      item = new ConstantPoolNameAndType (resolveUnicode(name),
                                          resolveUnicode(sig));
      constantPoolLookup_NameAndType.put (sb, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolString resolveString(String name)
  {
    ConstantPoolString item = (ConstantPoolString)
                                    constantPoolLookup_String.get(name);
    if (item==null) {
      item = new ConstantPoolString(resolveUnicode(name));
      constantPoolLookup_String.put (name, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolInteger resolveInteger(int value)
  {
	  Integer key = new Integer(value);
    ConstantPoolInteger item = (ConstantPoolInteger) constantPoolLookup_Integer.get(key);
    if (item==null) {
      item = new ConstantPoolInteger (value);
      constantPoolLookup_Integer.put (key, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolFloat resolveFloat(float value)
  {
    Float key = new Float(value);
    ConstantPoolFloat item = (ConstantPoolFloat) constantPoolLookup_Float.get(key);
    if (item==null) {
      item = new ConstantPoolFloat (value);
      constantPoolLookup_Float.put (key, item);
      addItemToConstantPool(item);
    }
    return item;
  }


  public ConstantPoolLong resolveLong(long value)
  {
	Long key = new Long(value);
    ConstantPoolLong item = (ConstantPoolLong)constantPoolLookup_Long.get(key);
    if (item==null) {
      item = new ConstantPoolLong (value);
      constantPoolLookup_Long.put (key, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolDouble resolveDouble(double value)
  {
	Double key = new Double(value);
    ConstantPoolDouble item = (ConstantPoolDouble)constantPoolLookup_Double.get(key);
    if (item==null) {
      item = new ConstantPoolDouble (value);
      constantPoolLookup_Double.put (key, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  public ConstantPoolUnicode resolveUnicode (String value)
  {
    ConstantPoolUnicode item = (ConstantPoolUnicode) constantPoolLookup_Utf8.get(value);
    if (item==null) {
      item = new ConstantPoolUnicode (value);
      constantPoolLookup_Utf8.put (value, item);
      addItemToConstantPool(item);
    }
    return item;
  }

  /**
   * Write the contents of the class that comes after the constant pool.
   *
   * @param out DataOutputStream on which the contents are written.
   */
  public void writeRemainingAfterConstantPool (CompilerEnvironment env,
                                               DataOutputStream out,
                                               boolean disposeMethodBodies)
    throws IOException, ByteCodingException {

    // Class hierarchy/access
    out.writeShort(modifiers.modifierFlags&0xffff);
    out.writeShort(resolveClass(this       ).slot);
if (superclass1==null)
System.out.println("superclass1==null at: "+className);
    out.writeShort(resolveClass(superclass1).slot); // don't try this with java.lang.Object...
    out.writeShort(interfaces1.length);
    for (int i=0;i<interfaces1.length; i++)
    {
        out.writeShort(resolveClass(interfaces1[i]).slot);
    }
    // variables
    out.writeShort(variables.length);
    for (int i=0; i<variables.length; i++)
    {
        variables[i].write(env, this, out);
        // if (disposeMethodBodies) variables[i].source = null; ????????
    }

    // methods
    out.writeShort(methods.length);
    for (int i=0; i<methods.length; i++)
    {
        //long startTime = env.timerLocalStart();
        methods[i].finishCodeForWriting();
        //env.timerLocalStop(env.OptimizeMsg, methods[i].getPresentation(), startTime);

        methods[i].write(env, this, out);
        // if (disposeMethodBodies) methods[i].source = null; ?????????
    }
    if (attributesContainer == null)
         out.writeInt(0);
    else attributesContainer.write(this, out); 
  }

  /**
   * Write the contents of the class.
   *
   * @param env CompilerEnvironment
   */
  public void write(CompilerEnvironment env, boolean disposeMethodBodies)
    throws IOException, ByteCodingException
  {
    String classFileDirectoryName = getClassFileDirectoryName(env);
    File classFileDirectory = new File(classFileDirectoryName);
    if (!classFileDirectory.exists())
    {
      classFileDirectory.mkdirs();
    }
    String classFileName = getClassFilePath(env);
    PlainClassFile plainClassFile = new PlainClassFile (classFileName);
    classFile = plainClassFile;
    write(env, new DataOutputStream (new FileOutputStream(plainClassFile.file)), true);
    env.timer (ClassesEnvironment.WrittenMsg, classFileName);

    for (int i=0; i<innerClasses.length; i++) {
        innerClasses[i].write (env, disposeMethodBodies);
    }
  }

  /**
   * Write the contents of the class.
   *
   * @param env CompilerEnvironment
   * @param out DataOutputStream on which the contents are written.
   */
  public void write(CompilerEnvironment env, DataOutputStream out, boolean disposeMethodBodies)
    throws IOException, ByteCodingException
  {
    addDimensionsAttribute();

    // first write the variables and methods onto a temporary stream;
    // that resolves the constant pool...only then can we write this constant pool
    ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();
    ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
         DataOutputStream stream1 = new      DataOutputStream(buffer1);
         DataOutputStream stream2 = new      DataOutputStream(buffer2);

    long startTime = env.timerLocalStart();
    writeRemainingAfterConstantPool (env, stream2, disposeMethodBodies);
    env.timerLocalStop(ClassesEnvironment.WriteOtherMsg, className, startTime);

    //headers
    stream1.writeInt  (JAVA_MAGIC);
    stream1.writeShort(JAVA_MINOR_VERSION);
    stream1.writeShort(JAVA_VERSION);

    // constantPool
    startTime = env.timerLocalStart();
    stream1.writeShort(constantPoolCount);
    for (int i=1;  i < constantPoolCount; i+=constantPool1[i].noOfSlots())
    {
        constantPool1[i].writeToStream(stream1);
    }
    env.timerLocalStop(ClassesEnvironment.WriteCstPoolMsg, className, startTime);

    out.write(buffer1.toByteArray());
    out.write(buffer2.toByteArray());
    out.flush();
    out.close();
  }

  // free the memory occupied by type declaration, code etc.
  void freeCompiledMembers () {
    for (int i=0; i<methods.length; i++)
    {
        methods[i].freeCompiledMembers();
    }
    sourceDeclaration.freeCompiledMembers();
    sourceDeclaration                     = null;
    superclassName                        = null;
    interfaceNames                        = null;
    if (classFile!=null) classFile.freeMembers();
    variables                             = null; // the array 'methods' is still needed...
    constantPoolLookup_Utf8               = null;
    constantPoolLookup_Integer            = null;
    constantPoolLookup_Float              = null;
    constantPoolLookup_Long               = null;
    constantPoolLookup_Double             = null;
    constantPoolLookup_Class              = null;
    constantPoolLookup_String             = null;
    constantPoolLookup_Fieldref           = null;
    constantPoolLookup_Methodref          = null;
    constantPoolLookup_InterfaceMethodref = null;
    constantPoolLookup_NameAndType        = null;
    constantPool1                         = null;

    //System.gc();
  }

   /** check whether the given abstract refinement construct is implemented in this
    *  check all superclasses if the refinement is in an interface;
    *  else stop checking the superclass chain as soon as the type declaration
    *  of the refinement is reached.
    *  Note: does not check for staticness
    */
   public Method resolveAbstractMethodImplementation (CompilerEnvironment env, Method toBeResolved)
                                               throws IOException, CompilerError {

	 HashMap<String, Object> signatures = getMethodNamesSignatures (env).get(toBeResolved.name);

     if (signatures != null) {
         Method r = (Method) signatures.get(toBeResolved.getSignature(env));
         if (r != null) {
             if (!r.isAbstract()) return r;
             else                 return null;
         }
     }
     if (getSuperclass(env) != null
     &&  superclass1 != toBeResolved.owner) {
         return superclass1.resolveAbstractMethodImplementation (env, toBeResolved);
     }
     return null;
   }

   public boolean isAccessibleFor (CompilerEnvironment env, ClassType t) throws IOException, CompilerError {
        if (!hasBeenCompiled()
        &&  !hasBeenLoaded  ()) {
            env.load (this, packageNameWithSlashes, packageNameWithDots(), className);
        }
       return modifiers.isPublic()
           || packageNameWithSlashes.equals(t.packageNameWithSlashes);
   }

   public ArrayList<Method> findApplicableConstructors (CompilerEnvironment                 env,
                                             ClassType                  callerClass,
                                             MethodOrConstructorCallExpression    m,
                                             boolean                isForAllocation)
                                      throws IOException, ByteCodingException {
	   ArrayList<Method> result = new ArrayList<Method>();
       HashMap<String, Object> signatures = getMethodNamesSignatures (env).get ("<init>");
       if (signatures != null) {
           for (Object obj: signatures.values()) {
        	   Method rf = (Method) obj;
               // determine accessibility (modifiers: public, none, protected, or private)
               if (!rf.isAccessibleFor (env, callerClass, isForAllocation)) continue;

               // determine applicability
               if (!rf.isApplicableFor (env, m)) continue;

               result.add(rf);
           }
       }
       return result;
   }


   /** find a method by name and signature.
    *  E.g. String.concat(), generated for '+'.
    *  Requires that getMethodNamesSignatures (env) has been called earlier.
    */
   public Method findMethod (ClassesEnvironment env, String name, String signature) {

	   HashMap<String, Object> signatures = getMethodNamesSignatures(env).get (name);
       if (signatures == null) return null;

       for (Object obj: signatures.values()) {
    	   Method m = (Method) obj;
           if (m.signature.equals(signature)) return m;
       }
       return null;
   }

   /** $15.11.2.1 Find Methods that are Applicable and Accessible
    * @return Hashtable of Methods
    */
   public HashMap<String, Object> findApplicableMethods (CompilerEnvironment env,
                                           ClassType callerClass,
                                           RefinementCallExpression c)
                                    throws IOException, ByteCodingException {
       return findApplicableMethodsWithHiding (env, new HashMap<String, Object>(), callerClass, c);
   }

   /** $15.11.2.1 Find Methods that are Applicable and Accessible,
    *  but apply hiding: when one is about to be found with a signature
    *  that already in the 'result' set,
    *  then don't add it to the result set
    *  This hiding occurs when
    *  - this is a class and a subclass already contains an appropriate refinement, or
    *  - this is an interface and an appropriate refinement has already been
    *    found in the class or interface that was taken in the first place,
    *    or in its superclasses or some other superinterface
    *  The class chain is searched first
    */
   private HashMap<String, Object> findApplicableMethodsWithHiding (CompilerEnvironment env,
		   											  HashMap<String, Object> result,
                                                      ClassType callerClass,
                                                      RefinementCallExpression call)
             throws ByteCodingException, IOException {

	   HashMap<String, Object> signatures = getMethodNamesSignatures(env).get (call.getName());

       if (signatures != null) {
           for (Object obj: signatures.values()) {
        	   Method rf = (Method) obj;

//System.out.println("findApplicableMethodsWithHiding in "+nameWithDots+": "+rf.getPresentation());

               // hide when needed
               if (result.containsKey (rf.getParameterSignature(env))) continue;

//System.out.println("findApplicableMethodsWithHiding 2");

               // determine accessibility (modifiers: public, none, protected, or private)
               if (!rf.isAccessibleFor (env, callerClass, false/*isForAllocation*/)) continue;

//System.out.println("findApplicableMethodsWithHiding 3");

               // determine applicability
               if (!rf.isApplicableFor (env, call)) continue;

//System.out.println("foundApplicableMethodsWithHiding in "+nameWithDots+": "+rf.getPresentation());

               result.put (rf.getParameterSignature(env), rf);
           }
       }
       if (getSuperclass(env) != null) {
           superclass1.findApplicableMethodsWithHiding (env, result, callerClass, call);
       }
       for (int i = 0; i<getInterfaces(env).length; i++) {
           interfaces1[i].findApplicableMethodsWithHiding (env, result, callerClass, call);
       }
       return result;
   }

   /** resolve the given variable name, and check accessability from the given class */
   public MemberVariable resolveMemberVariable (CompilerEnvironment env, String name)
                                                     throws CompilerError, IOException {

      MemberVariable result = (MemberVariable) getVariablesByName(env).get(name);

      if (result             == null
      &&  getSuperclass(env) != null) {
          result = superclass1.resolveMemberVariable (env, name);
      }
      if (result     == null)
      for (int i = 0; i<getInterfaces(env).length; i++) {
          MemberVariable v = interfaces1[i].resolveMemberVariable (env, name);
          if      (v      == null) continue;
          if      (result == null) result = v;
          else if (result != v) {
                   throw new CompilerError ("Field '"+name+"' ambiguously inherited from interfaces "
                                           +        result.owner.nameWithDots
                                           +" and "+  v   .owner.nameWithDots);
          }
      }
      return result;
   }

   public String getDescription (ClassesEnvironment env) {
       return     getShortDescription()
            + getConstantsDescription()
            + getAttributeDescription()
            + getVariablesDescription()
            +   getMethodsDescription(env)
            ;
   }

   public String getShortDescription () {
      StringBuffer result = new StringBuffer ();
      result
         .append (modifiers.getModifierString())
         .append (getPresentation ());

      if (superclassName       == null
      &&  getSuperclass(null) != null)                   // Beware...
          superclassName = superclass1.nameWithSlashes;

      if (superclassName != null)
         result
            .append (" extends ")
            .append (superclassName.replace('/','.'));
      if (hasError) result.append (" ERROR");
      if (sourceDeclaration!=null) result.append (" source: "+sourceDeclaration.name);
      if (hasBeenLoaded  ()) result.append (" loaded ");
      if (hasBeenCompiled()) result.append (" compiled ");
      if (!isResolved()) result.append (" NOT resolved ");
      result.append (lineSeparator);

      if (interfaceNames == null) {
        interfaceNames   = new String [getInterfaces(null).length];  // Beware...
        for (int i = 0; i < interfaceNames.length; i++) {
           interfaceNames[i] = interfaces1[i].nameWithSlashes;
        }
      }
      for (int i = 0; i < interfaceNames.length; i++) {
         if (i==0) result.append ("      implements ");
         else      result.append (", ");
         result.append (interfaceNames[i].replace('/','.'));
      }
      return result.toString ();
   }


   public String getAttributeDescription () {
       return attributesContainer==null? "": attributesContainer.getDescription();}


   public String getVariablesDescription () {
      if (variablesByName==null) return "";
      StringBuffer result = new StringBuffer ();
      int i = 0;
      for (MemberVariable mv: variablesByName.values()) {
         if (i++==0) result.append (lineSeparator)
                           .append (lineSeparator)
                           .append ("Variables").append (lineSeparator)
                           .append ("==============");
         result.append (lineSeparator);
         result.append (mv.getPresentation());
      }
      return result.toString ();
   }

   public String getMethodsDescription (ClassesEnvironment env) { // methodNamesSignatures should exist!!

      if (methodNamesSignatures1==null)
          return lineSeparator+lineSeparator+"??? Method Names-Signatures NOT set"+lineSeparator;

      StringBuffer result = new StringBuffer ();
      if (methods.length > 0) {
          result
               .append (lineSeparator)
               .append (lineSeparator)
               .append ("Methods")
               .append (lineSeparator)
               .append ("===========================");
      }
      for (int i=0; i<methods.length; i++) {
             if (env!=null) methods[i].parseSignatureIfNeeded (env);
             result.append (lineSeparator)
                   .append (methods[i].getPresentation());
      }
      if (!methodNamesSignatures1.isEmpty()) {
          result
               .append (lineSeparator)
               .append (lineSeparator)
               .append ("Method Names-Signatures")
               .append (lineSeparator)
               .append ("===========================");
          for (String n: methodNamesSignatures1.keySet()) {
             result.append (lineSeparator)
                   .append (n)
                   .append (':');
             for (String sig: methodNamesSignatures1.get(n).keySet()) {
                result.append(' ').append(sig);
             }
          }
      }
      return result.toString ();
   }

   public String getConstantsDescription () {
      StringBuffer result = new StringBuffer ();
      result
               .append (lineSeparator)
               .append (lineSeparator)
               .append (constantPoolCount+" Constants").append (lineSeparator)
               .append ("=============").append (lineSeparator);
        for (int j = 1; j < constantPoolCount;) {
           try {
             ConstantPoolItem item = getConstantPoolItem(j);
             result.append(item.getPresentation(this));
             j += item.noOfSlots();
           } catch (Exception e) {
             result.append("getConstantPoolItem("+j+"): "+e);
             j++;
           }
           result.append (lineSeparator);
        }
      return result.toString ();
   }


   String getMemberDescription (ClassesEnvironment env, boolean doDissassemble) {

     getMethodNamesSignatures (env);
     StringBuffer result = new StringBuffer();
    //result.append (         getDescription());
      result.append (    getShortDescription());
      result.append (getConstantsDescription());
      result.append (getAttributeDescription());
      result.append (getVariablesDescription());
      result.append (  getMethodsDescription(env));
/***
      result.append (lineSeparator);
      result.append ("Variables").append (lineSeparator);
      result.append ("=========").append (lineSeparator);
      for (int i=0; i < variables.length; i++) {
         Variable v = variables[i];
         v.getDataType(env);
         result.append (v.getDescription ()).append (lineSeparator);
         //result.append (v.attributesContainer.getDescription());
      }
***/
      if (doDissassemble) {
        result.append (lineSeparator);
        for (int i=0; i < methods.length; i++) {
          Method m = methods[i];
          m.parseSignatureIfNeeded (env);
          result.append (m.getDescription ());
        }
      }
      if (innerClasses != null) {
          for (int i=0; i<innerClasses.length; i++) {
               result.append ("--------\n");
               result.append (innerClasses[i].getMemberDescription (env, doDissassemble));
          }
      }
      return result.toString();
   }


   /**
    * find a refinement with also exists in (interface) t, with
    * same signature but also with different return type
    */
   public Method findMethodWithSameSignatureButDifferentReturnTypesAsIn (CompilerEnvironment env, ClassType t)
                                                                  throws IOException, CompilerError {
        Method r = null;
        for (int i=0; i<methods.length; i++) {
            r = methods[i];
            String parameterSignature = r.getParameterSignature(env);
            HashMap<String, Object> signatures = getMethodNamesSignatures (env).get (r.name);
            if (signatures != null) {
                Method rf = (Method) signatures.get (parameterSignature);
                if (rf != null) {
                  if (rf.returnType != r.returnType)
                   {
                      return r;
                   }
                }
            }
        }
        return null;
   }

/***************
   void makeSureThisIsLoadedOrBeingCompiled (CompilerEnvironment env) throws IOException, CompilerError {
/******
if (className.equals("Exception"))
System.out.println ("makeSureThisIsLoadedOrBeingCompiled: "+getShortDescription()
+(sourceDeclaration == null?"  sourceDeclaration==null":"  source exists")
+"  hasBeenLoaded="+hasBeenLoaded()
);
*******
       if (!hasBeenCompiled()
       &&  !hasBeenLoaded  () {

           env.load (this, packageNameWithSlashes, packageNameWithDots(), className);

/********************
System.out.println ("makeSureThisIsLoadedOrBeingCompiled: "+getShortDescription()
+(sourceDeclaration == null?"  sourceDeclaration==null":"  source exists")
+"  hasBeenLoaded="+hasBeenLoaded()
+lineSeparator
+getDescription(env)
);
*********************
       }
       if (superclass1==null) {
           resolveUsedClasses (env, false);
       }
   }
***************/

   /** load the class file.
    */
   void load (ClassesEnvironment env) throws IOException, ByteCodingException {

      hasError        = true;
      sourceDeclaration = null;
      initializeTables();
      setLoaded(); // we'll attempt this only once

if (classFile==null)
System.out.println("load: classFile==null for"+lineSeparator+getDescription(env));

/*
      byte bytes[] = classFile.getBytes();
      DataInputStream stream 
            = new DataInputStream (new ByteArrayInputStream (bytes));
*/
      DataInputStream stream 
            = new DataInputStream (new BufferedInputStream(classFile.getResourceAsStream()));
            
      if (stream.readInt() != 0xCAFEBABE) throw new CompilerError ("illegal magic number");

      stream.readUnsignedShort(); // minor version

      int majorVersion = stream.readUnsignedShort();
      if (majorVersion != 45) {
//          throw new CompilerError ("illegal major version: "+majorVersion+"; expected 45");
      }

      readConstantPool  (env, stream);

      modifiers.modifierFlags = stream.readUnsignedShort();
      int thisClassIndex      = stream.readUnsignedShort();

      setNameWithSlashes (getConstantPoolItem (thisClassIndex).getName(this));

      // for superclass and interface, only the names are set now.
      // Resolve after this function call.
      int superclassIndex = stream.readUnsignedShort();
      if (superclassIndex > 0) {
         superclassName = getConstantPoolItem (superclassIndex).getName(this);
      }
      int interfaceCount = stream.readUnsignedShort();
      interfaceNames = new String [interfaceCount];
      for (int i = 0; i < interfaceCount; i++) {
           interfaceNames[i] = getConstantPoolItem (stream.readUnsignedShort()).getName(this);
      }

      readFields  (env, stream);
      readMethods (env, stream);
      attributesContainer = AttributesContainer.readFromStream (this, this, stream);

      DimensionsAttribute dimensionsAttribute = attributesContainer.getDimensions();
      if (dimensionsAttribute != null) {
          getDimensionsFrom (dimensionsAttribute);
      }
 //System.out.println(getDescription(env));
      hasError = false;
      stream.close();
   }

   void readConstantPool (ClassesEnvironment env, DataInputStream cs)
                                                            throws IOException, CompilerError {
		 	constantPoolCount = cs.readUnsignedShort();
      constantPool1 = new ConstantPoolItem[constantPoolCount];
		 	//	System.out.println ("Constant count: " + constantCount);
		 	//	System.out.println ("Constants:");
		 	for (int i=1; i<constantPoolCount; i++) {
    		int tag = cs.readUnsignedByte();
        ConstantPoolItem cpi = null;
        switch (tag) {
          case ConstantPoolUnicodeTag        : cpi = ConstantPoolUnicode                 .readFromStream (cs); break;
          case ConstantPoolIntegerTag        : cpi = ConstantPoolInteger                 .readFromStream (cs); break;
          case ConstantPoolFloatTag          : cpi = ConstantPoolFloat                   .readFromStream (cs); break;
          case ConstantPoolLongTag           : cpi = ConstantPoolLong                    .readFromStream (cs); break;
          case ConstantPoolDoubleTag         : cpi = ConstantPoolDouble                  .readFromStream (cs); break;
          case ConstantPoolClassTag          : cpi = ConstantPoolClass                   .readFromStream (cs); break;
          case ConstantPoolStringTag         : cpi = ConstantPoolString                  .readFromStream (cs); break;
          case ConstantPoolFieldTag          : cpi = ConstantPoolFieldReference          .readFromStream (cs); break;
          case ConstantPoolMethodTag         : cpi = ConstantPoolMethodReference         .readFromStream (cs); break;
          case ConstantPoolInterfaceMethodTag: cpi = ConstantPoolInterfaceMethodReference.readFromStream (cs); break;
          case ConstantPoolNameAndTypeTag    : cpi = ConstantPoolNameAndType             .readFromStream (cs); break;
          default:
             System.out.println("index: "+i+" unexpected tag: "+tag);
        }
        cpi.slot = i;
        constantPool1[i] = cpi;
        switch (tag) {
          case ConstantPoolLongTag           : 
          case ConstantPoolDoubleTag         : i++; break;
        }
      }
   }

   void readFields (ClassesEnvironment env, DataInputStream stream) throws IOException, ByteCodingException {
      int fieldCount = stream.readUnsignedShort ();

      variables     = new MemberVariable [fieldCount];
      variablesByName = new HashMap<String, MemberVariable>();

      for (int index = 0; index < fieldCount; index++) {
         MemberVariable memberVariable = new MemberVariable ();
         memberVariable.owner          = this;
         memberVariable.readFromStream (env, this, stream);
         variablesByName.put (memberVariable.name, memberVariable);
         variables[index] = memberVariable;
      }
   }

   void readMethods (ClassesEnvironment env, DataInputStream stream) throws IOException, ByteCodingException { 
      int methodCount = stream.readUnsignedShort ();
      methods = new Method [methodCount];
      for (int i = 0; i<methodCount; i++) {

         Method method = new Method();
         method.owner  = this;
         method.readFromStream (env, this, stream);
         methods [i]   = method;
      }
   }

   /** Answer a ClassType with the given name,
    *  for it was used in the given containingClass
    *  If the given ClassesEnvironment is null, or !loadAsWell
    *    no attempt is made to load the result from file
    */
   public static ClassType getFromNameWithSlashes (ClassesEnvironment env,
                                                   String             name,
                                                   ClassType          containingClass) {
       return getFromNameWithSlashes (env, name, containingClass, false);
   }
   public static ClassType getFromNameWithSlashes (ClassesEnvironment env,
                                                   String             name,
                                                   ClassType          containingClass,
                                                   boolean            loadAsWell ) {
     if (env != null) {
/**********8
System.out.println ("getFromNameWithSlashes: "+name
+"  loadAsWell="+loadAsWell
);
*******/
       ClassType result = env.resolveClassNameWithSlashes (name, loadAsWell);
       if (result==null) {
             String p = containingClass.sourceFile!=null
                      ? containingClass.sourceFile.getPath()
                      : containingClass.classFile.getPath();
             env.parserError (2, "Class " + name
                        + " cannot be resolved"
                        + (containingClass==null? "." : "; referred to by "
                          +containingClass.nameWithDots + " ["+p+"]"));
             result = new UnresolvedClassOrInterfaceType();
       }
       return result;
     }
     else {
       String clazz   = name;
       int i = name.lastIndexOf ('/');
       if (i >= 0) {
           clazz   = name.substring(i+1);
       }
/****************
System.out.println ("getFromNameWithSlashes: "+name+", "+clazz);
new Exception().printStackTrace();
*******************/
       return new ClassType (name, clazz);
     }
   }
}
