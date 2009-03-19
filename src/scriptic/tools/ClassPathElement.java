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
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;


class ClassPathElement extends LanguageConstruct {
   public ArrayList<ClassPathPackage> classPackageVector = new ArrayList<ClassPathPackage>();
   public HashMap<String, HashMap<String, Object>>   sourceFileNamesByPackage 
    = new HashMap<String, HashMap<String, Object>>(); // source files found by package key
   public HashMap<String, HashMap<String, Object>>    classFileNamesByPackage 
    = new HashMap<String, HashMap<String, Object>>(); //  class files found by package key
   public HashSet<String>          knownPackageNames = new HashSet<String>(); // known   package names
   public HashSet<String>        illegalPackageNames = new HashSet<String>(); // illegal package names
   public HashSet<String> packagesWithoutSourceFiles = new HashSet<String>(); // package names where no .java  files exist
   public HashSet<String> packagesWithoutClassFiles  = new HashSet<String>(); // package names where no .class files exist
   ClassesEnvironment classesEnvironment;

static void putOne(String name, int n, String h) {
/*********
  if(n%1000==0) {
     System.out.println("["+n+"] ClassPathElement.puts "+h+": "+name);
     new Exception("TRACE").printStackTrace();
  }
**********/
}

static void putClassPackages             (String name) {putOne (name, ++nPutsClassPackages             , "ClassPackages"             );}
static void putClassFiles                (String name) {putOne (name, ++nPutsClassFiles                , "ClassFiles"                );}
static void putKnownPackageNames         (String name) {putOne (name, ++nPutsKnownPackageNames         , "KnownPackageNames"         );}
static void putIllegalPackageNames       (String name) {putOne (name, ++nPutsIllegalPackageNames       , "IllegalPackageNames"       );}
static void putSourceFileNamesByPackage  (String name) {putOne (name, ++nPutsSourceFileNamesByPackage  , "SourceFileNamesByPackage"  );}
static void putClassFileNamesByPackage   (String name) {putOne (name, ++nPutsClassFileNamesByPackage   , "ClassFileNamesByPackage"   );}
static void putPackagesWithoutSourceFiles(String name) {putOne (name, ++nPutsPackagesWithoutSourceFiles, "PackagesWithoutSourceFiles");}
static void putPackagesWithoutClassFiles (String name) {putOne (name, ++nPutsPackagesWithoutClassFiles , "PackagesWithoutClassFiles" );}
static void putHashDirectory             (String name) {putOne (name, ++nPutsHashDirectory             , "HashDirectory"             );}
static void putFileNamesInPackage        (String name) {putOne (name, ++nPutsFileNamesInPackage        , "FileNamesInPackage"        );}
static int nPutsClassPackages; // no. of puts in hashtable...
static int nPutsClassFiles;
static int nPutsKnownPackageNames;
static int nPutsIllegalPackageNames;
static int nPutsSourceFileNamesByPackage;
static int nPutsClassFileNamesByPackage;
static int nPutsPackagesWithoutSourceFiles;
static int nPutsPackagesWithoutClassFiles;
static int nPutsHashDirectory;
static int nPutsFileNamesInPackage;

   public ClassPathElement (ClassesEnvironment classesEnvironment, String name) {
      this.name               = name;
      this.classesEnvironment = classesEnvironment;
   }
   public boolean isKnownPackageName (String name) { // unqualified name
     if (illegalPackageNames.contains (name)) return false;
     if (  knownPackageNames.contains (name)) return  true;
     if (     getClassFileNamesInPackage (name) != null
     ||      getSourceFileNamesInPackage (name) != null) {
             knownPackageNames.add (name);                   putKnownPackageNames(name);
         return true;
     }
     illegalPackageNames.add (name);                         putIllegalPackageNames(name);
     return false;
   }

   /* find class names in package "java.lang, as far as not yet done */
   public void findPackageJavaLangClassNames () {

	 HashMap<String, Object> names = getClassFileNamesInPackage ("java.lang");
     if (names==null) return;
     for (Object s: names.values()) {
        classesEnvironment.addJavaLangClassName ((String)s);
     }
   }

   /* find the name of a source file for a given package/class
    */
   public String findSourceFileName (String pakkage, String clazz)
   {
     HashMap<String, Object> sourceFileNamesInPackage = getSourceFileNamesInPackage (pakkage);
     if (sourceFileNamesInPackage == null) {
         return null;
     }
     return (String) sourceFileNamesInPackage.get(clazz);
   }


   /** Answer the source file names in the given pakkage; null if there are none
    */
   protected HashMap<String, Object> getSourceFileNamesInPackage (String pakkage) {

     if (packagesWithoutSourceFiles.contains (pakkage)) {
         return null;
     }
     HashMap<String, Object> result = sourceFileNamesByPackage.get(pakkage);
     if (result==null) {
         result = getFileNamesInPackage (pakkage, ".java");

         if (result != null) {
            sourceFileNamesByPackage.put(pakkage, result);                        putSourceFileNamesByPackage(pakkage);
         }
         else {
             packagesWithoutSourceFiles.add (pakkage);                        putPackagesWithoutSourceFiles(pakkage);
         }
     }
     return result;
   }

   /** Answer the class file names in the given pakkage; null if there are none
    */
   protected HashMap<String, Object> getClassFileNamesInPackage (String pakkage) {

     if (packagesWithoutClassFiles.contains (pakkage)) {
         return null;
     }
     HashMap<String, Object> result = classFileNamesByPackage.get(pakkage);
     if (result==null) {
         result = getFileNamesInPackage (pakkage, ".class");

         if (result != null) {
            classFileNamesByPackage.put(pakkage, result);                        putClassFileNamesByPackage(pakkage);
         }
         else {
             packagesWithoutClassFiles.add (pakkage);                        putPackagesWithoutClassFiles(pakkage);
         }
     }
     //System.out.println("\nCPEntry: " + name
     //                  +" at: "       + pakkage
     //                  +"\n--------------------------------------");
     //for (Enumeration e = classFileNamesInPackage.keys(); e.hasMoreElements();) {
     //    String s = (String) e.nextElement();
     //    System.out.println(s);
     //}
     return result;
   }

   /** Answer the class file names in the given pakkage; null if there are none
    */
   protected HashMap<String, Object> getFileNamesInPackage (String pakkage, String extension) {

	 HashMap<String, Object> result = null;

     StringBuffer directoryName = new StringBuffer(name);
     if (name.charAt(directoryName.length()-1)!=File.separatorChar)
     {
         directoryName.append(File.separatorChar);
     }
     directoryName.append(pakkage.replace('.',File.separatorChar));
     File directory = new File (directoryName.toString());
     //System.out.println("Making cache for: " + pakkage
     //              +" at: "+directoryName);

     if (directory.isDirectory()) {
         String [ ] dirEntries = directory.list ();
         for (int i = 0; i < dirEntries.length; i++) {
             String name = (String) dirEntries [i];
             if (name.length()<=extension.length()
             || !name.endsWith (extension)) {
                 continue;
             }
             if (result==null) {
                 result = new HashMap<String, Object>();
             }
             StringBuffer fullName = new StringBuffer (directoryName.toString())
                                     .append(File.separatorChar).append(name);
             String className = name.substring (0, name.length()-extension.length());
             result.put(className, fullName.toString());                      putFileNamesInPackage(name);

             // System.out.println("caching .class file: "
             //                   + name.substring (0, name.length() - extension.length)
             //                   +" at: "+fullName);
         }
     }
     return result;
   }


   /* find a class file for a given package/class
    */
   public ClassFile findClassFile (String pakkage, String clazz)
   {
     HashMap<String, Object> classFileNamesInPackage = getClassFileNamesInPackage (pakkage);
     if (classFileNamesInPackage == null) {
         return null;
     }
     String classFileName = (String) classFileNamesInPackage.get(clazz);
     if (classFileName==null) return null;

     ClassFile file = new PlainClassFile (classFileName);
     if (!file.isFile ()
     ||  !file.canRead()) return null;

     return file;
   }


  public void addToClassPathPackage (String pakkage, String clazz, ClassType c) {
     ClassPathPackage classPathPackage = new ClassPathPackage (pakkage);
     classPackageVector.add(classPathPackage);

     ClassPackage classPackage = classesEnvironment.classPackages.get (pakkage);
     if (classPackage == null) {
         classPackage = new ClassPackage (pakkage);
         classesEnvironment.classPackages.put (pakkage, classPackage);                        putClassPackages(pakkage);
     }
     c.classPackage     = classPackage;
     c.classPathPackage = classPathPackage;
     classPathPackage.classFileVector.add (c);
     classPackage    .classFiles     .put (clazz, c);
     putClassFiles(clazz);
  }
}
