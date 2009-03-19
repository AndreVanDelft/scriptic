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

 package scriptic.tools.lowlevel;

import java.io.*;
import java.util.*;

public class Catchtable implements ClassFileConstants
{
  ArrayList<CatchEntry> entries;

  public Catchtable() { entries = new ArrayList<CatchEntry>(); }

  /**
   * add an entry to the catch table
   */

  public void addEntry(CatchEntry entry) { entries.add(entry); }

  /**
   * add an entry to the catch table
   * @param start LabelInstruction marking the beginning of the area
   *       where the catch table is active.
   * @param end LabelInstruction marking the end of the area where the
   *       table is active.
   * @param handler LabelInstruction marking the entrypoint into the
   *  exception handling routine.
   * @param cat (usually a classCP) informing the VM to direct
   * any exceptions of this (or its subclasses) to the handler.
   */

  public void
  addEntry(LabelInstruction start, LabelInstruction end, LabelInstruction handler, ConstantPoolClass cat)
  { addEntry(new CatchEntry(start, end, handler, cat)); }

   public String getDescription () {
      StringBuffer result = new StringBuffer();
      result.append ("Catch table").append (lineSeparator);
      for (CatchEntry ce: entries)
      {
        result.append (ce.getDescription());
      }
      return result.append (lineSeparator).toString ();
   }


  void resolve(ClassEnvironment e)
  {
      for (CatchEntry ce: entries)
      {
        ce.resolve(e);
      }
  }

  int size()
  { return (8*entries.size()); } // each entry is 8 bytes

  void compact() {
      // remove entries starting with a deleted TryStartLabel
    for (int i=0; i<entries.size(); ) {
        CatchEntry ce = entries.get(i);
        if (ce.start_pc.opc==INSTRUCTION_deleted) {
             entries.remove(i);
        }
        else {
           i++;
        }
    }
  }
  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort(entries.size());
    for (CatchEntry entry: entries)
       {
        entry.write(e, ce, out);
      }
  }
  static Catchtable readFrom (ClassEnvironment e, DataInputStream in, int size)
    throws IOException, ByteCodingException
  {
    Catchtable result = new Catchtable();
    for (int i=0; i<size; i++)
      {
        CatchEntry entry = CatchEntry.readFrom (e, in);
        result.entries.add(entry);
      }
    return result;
  }
}
