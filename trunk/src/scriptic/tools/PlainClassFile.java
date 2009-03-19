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

class PlainClassFile extends ClassFile {
    public File file;
    public InputStream getResourceAsStream() 
    {
      try {
          return new FileInputStream (file);
      } catch (IOException e) { return null; }
    }
/*    
    public byte[] getBytes() {

      FileInputStream fileInputStream = null;
      try {
           fileInputStream = new FileInputStream (file);
           byte bytes[] = new byte [fileInputStream.available ()];
           fileInputStream.read (bytes, 0, bytes.length);
           return bytes;
      } catch (IOException e) {return null;
      } finally {
           try {fileInputStream.close();} catch (Exception e) {}
      }
    }
    */
    public PlainClassFile(String name) {file = new File(name);}
    public PlainClassFile(File file)   {this.file = file;}
    public boolean   isFile () {return file.     isFile ();}
    public boolean   canRead() {return file.     canRead();}
    public long lastModified() {return file.lastModified();}
    public String    getPath() {return file.getPath();}
    public void  freeMembers() {/*file = null; NO; file may remain needed for path etc*/}
}

