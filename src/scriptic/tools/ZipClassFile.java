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
import java.util.zip.*;

public class ZipClassFile extends ClassFile {
    public ZipFile  zipFile;
    public ZipEntry zipEntry;
    
    public InputStream getResourceAsStream() {
       try { // look for name in zipfile, return null if something goes wrong.
         return (zipEntry==null)?null:zipFile.getInputStream(zipEntry);
       } catch (UnsatisfiedLinkError e) {
        //System.err.println("UNSATISFIED LINK ERROR: "+getName());
        return null;
       } catch (IOException e) { return null; }
     }

    public ZipClassFile(ZipFile f, ZipEntry e) {zipFile = f; zipEntry = e;}
    public boolean   isFile () {return !zipEntry.isDirectory ();}
    public long lastModified() {return zipEntry .     getTime();}
    public boolean   canRead() {return true;}
    public String    getPath() {return zipEntry.getName();}
    public void  freeMembers() {zipFile = null; zipEntry = null;}
}

