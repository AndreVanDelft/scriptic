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
import java.io.FileInputStream;
import java.util.HashMap;


class UncompressedZipClassPathElement extends ClassPathElement {

   File zipfile;
   HashMap<String, ZipDirectoryClassFileEntry> hashDirectory = new HashMap<String, ZipDirectoryClassFileEntry>(); // temporary...
   boolean loadAllPackageClassNames = true;
   byte    directoryBytes[]         = null;
   short countCentralDirectoryEntries;
   int centralDirectorySize;
   int centralDirectoryOffset;

   UncompressedZipClassPathElement (ClassesEnvironment classesEnvironment, String name, File zipfile) {
      super (classesEnvironment, name);
      this.zipfile = zipfile;
      loadDirectory();
   }

   /* find a source file for a given package/class
    */
   public    String    findSourceFileName          (String pakkage, String clazz) {return null;}
   protected HashMap<String, Object> getSourceFileNamesInPackage (String pakkage) {return null;}


   /* find a class file for a given package/class
    */
   public ClassFile findClassFile (String pakkage, String clazz)
   {
     if (loadAllPackageClassNames) {
       ZipDirectoryClassFileEntry z = (ZipDirectoryClassFileEntry)
                                               hashDirectory.get(pakkage+'.'+clazz);
       if (z == null) return null;
       return new UncompressedZipClassFile (zipfile, z);
     }

     HashMap<String, Object> classFileNamesInPackage = getClassFileNamesInPackage (pakkage);
     if (classFileNamesInPackage == null) {
         return null;
     }
     ZipDirectoryClassFileEntry z = (ZipDirectoryClassFileEntry) // a bit of abuse...
                                               classFileNamesInPackage.get(pakkage+'.'+clazz);
     if (z == null) return null;
     return new UncompressedZipClassFile (zipfile, z);
   }

   final int  LREC_SIZE = 26;    /* length of local file headers        */
   final int  CREC_SIZE = 42;    /* length of central directory headers */
   final int ECREC_SIZE = 18;    /* length of end-of-central-dir record */

   final int OFFSET_COMPRESSED_SIZE          = 20;
   final int OFFSET_UNCOMPRESSED_SIZE        = 24;
   final int OFFSET_FILENAME_LENGTH          = 28;
   final int RELATIVE_OFFSET_LOCAL_HEADER    = 42;

   final int NUMBER_THIS_DISK                =  4;
   final int NUM_DISK_WITH_START_CENTRAL_DIR =  6;
   final int NUM_ENTRIES_CENTRL_DIR_THS_DISK =  8;
   final int TOTAL_ENTRIES_CENTRAL_DIR       = 10;
   final int SIZE_CENTRAL_DIRECTORY          = 12;
   final int OFFSET_START_CENTRAL_DIRECTORY  = 16;
   final int ZIPFILE_COMMENT_LENGTH          = 20;

   final int       localFileHeaderSignature  = 0x04034b50;
   final int endOfCentralDirectorySignature  = 0x06054b50;

   private short getZipShort(byte[] bytes, int offset) {
       return (short) ( ((bytes[offset+0]&0xff)      )
                      + ((bytes[offset+1]&0xff) <<  8));
   }
   private int getZipInt(byte[] bytes, int offset) {
       return (int)   ( ((bytes[offset+0]&0xff)      )
                      + ((bytes[offset+1]&0xff) <<  8)
                      + ((bytes[offset+2]&0xff) << 16)
                      + ((bytes[offset+3]&0xff) << 24));
   }
   protected void loadDirectory () {

      FileInputStream fileInputStream = null;
      int             noOfBytes;

      try {
         fileInputStream = new FileInputStream (zipfile);
         noOfBytes       = fileInputStream.available ();
         if (noOfBytes < ECREC_SIZE+4) {
             throw new Exception("Illegal zip file: "+zipfile.getName()+"; length too short");
         }
         int lengthToRead   = noOfBytes;
         //int lengthToRead = noOfBytes < 0xffff+ECREC_SIZE? noOfBytes : 0xffff+ECREC_SIZE;
         byte bytes[]       = new byte [lengthToRead];

         fileInputStream.skip (noOfBytes-lengthToRead);
         fileInputStream.read (bytes, 0, lengthToRead);
         fileInputStream.close();

         int i = 0;
         // search endOfCentralDirectorySignature backwards
         for (i = lengthToRead-ECREC_SIZE; i>=0; i--) {
             if (bytes[i+3] == ((endOfCentralDirectorySignature >> 24) & 0xff)
             &&  bytes[i+2] == ((endOfCentralDirectorySignature >> 16) & 0xff)
             &&  bytes[i+1] == ((endOfCentralDirectorySignature >>  8) & 0xff)
             &&  bytes[i+0] == ((endOfCentralDirectorySignature      ) & 0xff))
                 break;
         }
         if (i<0) {
             throw new Exception("Illegal zip file: endOfCentralDirectorySignature not found");
         }
         countCentralDirectoryEntries = getZipShort (bytes, i+TOTAL_ENTRIES_CENTRAL_DIR);
         centralDirectorySize         = getZipInt   (bytes, i+SIZE_CENTRAL_DIRECTORY);
         centralDirectoryOffset       = getZipInt   (bytes, i+OFFSET_START_CENTRAL_DIRECTORY);
                                  // == noOfBytes - lengthToRead + i - centralDirectorySize;

/*
         System.out.println ("noOfBytes = " + noOfBytes);
         System.out.println ("endOfCentralDirectoryOffset     = " + i);
         System.out.println ("number_this_disk                = " + getZipShort (bytes, i+NUMBER_THIS_DISK));
         System.out.println ("num_disk_with_start_central_dir = " + getZipShort (bytes, i+NUM_DISK_WITH_START_CENTRAL_DIR));
         System.out.println ("num_entries_centrl_dir_ths_disk = " + getZipShort (bytes, i+NUM_ENTRIES_CENTRL_DIR_THS_DISK));
         System.out.println ("total_entries_central_dir       = " + countCentralDirectoryEntries);
         System.out.println ("size_central_directory          = " + centralDirectorySize);
         System.out.println ("offset_start_central_directory ?=?" + centralDirectoryOffset);
         System.out.println ("offset_start_central_directory  = " + getZipInt   (bytes, i+OFFSET_START_CENTRAL_DIRECTORY));
         System.out.println ("zipfile_comment_length          = " + getZipShort (bytes, i+ZIPFILE_COMMENT_LENGTH));
*/
         bytes = null;
         directoryBytes = new byte[centralDirectorySize];
         fileInputStream = new FileInputStream (zipfile);
         fileInputStream.skip (centralDirectoryOffset);
         fileInputStream.read (directoryBytes, 0, centralDirectorySize);
         fileInputStream.close();

         if (!loadAllPackageClassNames) return;

         String packageName = null;
         int currentOffset = 0;
         for (i = 0; i < countCentralDirectoryEntries; i++)
         {
           int uncompressedSize = getZipInt   (directoryBytes, currentOffset+OFFSET_UNCOMPRESSED_SIZE);
           short filenameLength = getZipShort (directoryBytes, currentOffset+OFFSET_FILENAME_LENGTH);
/****
if (currentOffset>65000) System.out.println(
"currentOffset: "+currentOffset+"\n"+
"currentOffset + CREC_SIZE + 4: "+currentOffset + CREC_SIZE + 4+"\n"+
"filenameLength: "+filenameLength+"\n"+
"bytes.length: "+bytes.length+"\n"+
"fileName: "+fileName+"\n"
);
*****/
           if (filenameLength > 6) {
             int    fileNameOffset    = currentOffset + CREC_SIZE + 4;
             String fileNameExtension = new String (directoryBytes, fileNameOffset+filenameLength-6, 6);

             if (fileNameExtension.equals (".class")) {

               int fileStart = getZipInt (directoryBytes, currentOffset+RELATIVE_OFFSET_LOCAL_HEADER)
                             + (LREC_SIZE+4)
                             + filenameLength;

               String packageClass = new String (directoryBytes, fileNameOffset, filenameLength-6)
                                   .replace('/','.');
               String className    = null;

               int index = packageClass.lastIndexOf('.');
               if (index > 0) {
                   packageName = packageClass.substring(0,index);
                   className   = packageClass.substring(index+1);
               }
               else {
                   packageName = "";
                   className   = packageClass;
               }

               // now insert a new ZipDirectoryClassFileEntry

               //System.out.println(""+i+": " + fileName + "("+packageName+"."+className+") fileStart: "+fileStart);
               hashDirectory.put (packageClass,
                                  new ZipDirectoryClassFileEntry (packageName, className, zipfile,
                                                                  fileStart,   uncompressedSize));      //      putHashDirectory(packageName);
               if (packageName.equals("java.lang"))
                   classesEnvironment.addJavaLangClassName (className);

               if (!knownPackageNames.contains(packageName)) {
                    knownPackageNames.add (packageName);                   //     putKnownPackageNames(packageName);
               }
             }
           }
           currentOffset     += CREC_SIZE + 4 + filenameLength;

           if ( currentOffset > centralDirectoryOffset + centralDirectorySize) {
               classesEnvironment.resolverError("Illegal zip file: "+zipfile.getName()+"; unexpected end of central directory");
               return;
           }
         }
      } catch (Exception e) {
         e.printStackTrace();
         return;
      } finally {
         try { fileInputStream.close(); } catch (Exception e) { }
      }
   }


   /** Answer the class file names in the given pakkage; null if there are none
    */
   protected HashMap<String, Object> getFileNamesInPackage (String pakkage, String extension) {

	   HashMap<String, Object> result = null;

     int currentOffset = 0;
     int extensionLength = extension.length();

     for (int i = 0; i < countCentralDirectoryEntries; i++)
     {
           int uncompressedSize = getZipInt   (directoryBytes, currentOffset+OFFSET_UNCOMPRESSED_SIZE);
           short filenameLength = getZipShort (directoryBytes, currentOffset+OFFSET_FILENAME_LENGTH);

           if (filenameLength > extensionLength) {
             int    fileNameOffset    = currentOffset + CREC_SIZE + 4;
             String fileNameExtension = new String (directoryBytes, fileNameOffset+filenameLength-extensionLength, extensionLength);

             if (fileNameExtension.equals (extension)) {

               int fileStart = getZipInt (directoryBytes, currentOffset+RELATIVE_OFFSET_LOCAL_HEADER)
                             + (LREC_SIZE+4)
                             + filenameLength;

               String packageClass = new String (directoryBytes, fileNameOffset, filenameLength-extensionLength)
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
                   result = new HashMap<String, Object>();
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

}

