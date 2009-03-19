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

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


class ZipClassPathElement extends ClassPathElement {

   ZipFile zipFile;

   ZipClassPathElement (ClassesEnvironment classesEnvironment, String name, File file) {
      super (classesEnvironment, name);
      try                 {this.zipFile = new ZipFile (file);}
      catch (Exception e) {throw new RuntimeException (e.getMessage());} 
      loadDirectory();
   }

   /* find a source file for a given package/class
    */
   public    String    findSourceFileName          (String pakkage, String clazz) {return null;}
   protected HashMap<String, Object> getSourceFileNamesInPackage (String pakkage)               {return null;}


   /* find a class file for a given package/class
    */
   public ClassFile findClassFile (String pakkage, String clazz)
   {
       ZipEntry z = zipFile.getEntry (pakkage.replace('.','/')+'/'+clazz+".class");
       if (z == null) return null;
       return new ZipClassFile (zipFile, z);
   }

   protected void loadDirectory () {

      for (Enumeration<? extends ZipEntry> enu = zipFile.entries(); enu.hasMoreElements(); ) {

         ZipEntry ze = enu.nextElement();
         String   zn = ze.getName();
         if (zn.endsWith (".class")
         && !ze.isDirectory()) {

               String packageClass = zn.substring(0,zn.length()-6).replace('/','.');
               String packageName  = "";
               String className    = packageClass;
               int index = packageClass.lastIndexOf('.');
               if (index > 0) {
                   packageName = packageClass.substring(0,index);
                   className   = packageClass.substring(index+1);
               }

               if (packageName.equals("java.lang")) {
                   classesEnvironment.addJavaLangClassName (className);
                   //System.out.println("addJavaLangClassName: " + packageClass + "("+className+")");
               }
               if (!knownPackageNames.contains(packageName)) {
                    knownPackageNames.add (packageName);                   //     putKnownPackageNames(packageName);
               }
         }
      }
   }


   /** Answer the class file names in the given pakkage; null if there are none
    ****************
   protected Hashtable getFileNamesInPackage (String pakkage, String extension) {

     Hashtable result = null;

     int currentOffset = 0;
     int extensionLength = extension.length();

     for (int i = 0; i < countCentralDirectoryEntries; i++)
     {
           int uncompressedSize = getZipInt   (directoryBytes, currentOffset+OFFSET_UNCOMPRESSED_SIZE);
           short filenameLength = getZipShort (directoryBytes, currentOffset+OFFSET_FILENAME_LENGTH);

           if (filenameLength > extensionLength) {
             int    fileNameOffset    = currentOffset + CREC_SIZE + 4;
             String fileNameExtension = new String (directoryBytes, 0, fileNameOffset+filenameLength-extensionLength, extensionLength);

             if (fileNameExtension.equals (extension)) {

               int fileStart = getZipInt (directoryBytes, currentOffset+RELATIVE_OFFSET_LOCAL_HEADER)
                             + (LREC_SIZE+4)
                             + filenameLength;

               String packageClass = new String (directoryBytes, 0, fileNameOffset, filenameLength-extensionLength)
                                   .replace('/','.');
               String className    = null;

               int index = packageClass.lastIndexOf('.');
               if (index > 0) {
                   if (!pakkage.equals (packageClass.substring(0,index)))
                          continue;
                   className   = packageClass.substring(index+1);
               }
               else {
                   if (pakkage.length() != 0)
                          continue;
                   className = packageClass;
               }

               // now insert a new ZipDirectoryClassFileEntry

               if (result==null) {
                   result = new Hashtable();
               }
               result.put(className, 
                          new ZipDirectoryClassFileEntry (pakkage, className, zipfile,
                                                          fileStart,   uncompressedSize));      //      putHashDirectory(packageName);
             }
           }
           currentOffset     += CREC_SIZE + 4 + filenameLength;

           if ( currentOffset > centralDirectoryOffset + centralDirectorySize) {
               classesEnvironment.resolverError("Illegal zip file: "+zipfile.getName()+"; unexpected end of central directory");
               return null;
           }
       }
       return result;
   }
************/
}

