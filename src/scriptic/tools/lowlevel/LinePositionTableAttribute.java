/* This file is part of Sawa, the Scriptic-Java compiler
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class LinePositionTableAttribute extends ClassFileAttribute
{
  public static String name = "scriptic.LinePositionTable";
  public String getName () {return name;}
  int nPrimaryEntries;
  int   nScopeEntries;
  int nOverflows;
  int  primary_start_positions[];
  int    primary_end_positions[];
  int                scope_pcs[];
  int scope_start_line_offsets[];
  int   scope_end_line_offsets[];
  int    scope_start_positions[];
  int      scope_end_positions[];

  public LinePositionTableAttribute()
  {
     primary_start_positions = new int[2];
       primary_end_positions = new int[2];
                   scope_pcs = new int[1];
    scope_start_line_offsets = new int[1];
      scope_end_line_offsets = new int[1];
       scope_start_positions = new int[1];
         scope_end_positions = new int[1];
  }

  public void addEntry(int pc,
                       int primaryLine,
                       int primaryStartPosition,
                       int primaryEndPosition,
                       int scopeStartLine,
                       int scopeEndLine,
                       int scopeStartPosition,
                       int scopeEndPosition) {
    if (nPrimaryEntries >= primary_start_positions.length) {
        int old[]=primary_start_positions;
                  primary_start_positions=new int[primary_start_positions.length*2];
        System.arraycopy(old,0,  primary_start_positions,0,old.length);
            old  =  primary_end_positions;
                    primary_end_positions=new int[  primary_end_positions.length*2];
        System.arraycopy(old,0,primary_end_positions,0,old.length);
    }
    primary_start_positions [nPrimaryEntries] = primaryStartPosition;
      primary_end_positions [nPrimaryEntries] = primaryEndPosition;
    if (primaryStartPosition >= 255) nOverflows++;
    if (primaryEndPosition   >= 255) nOverflows++;
    nPrimaryEntries++;

    if (scopeStartPosition != primaryStartPosition
    ||  scopeEndPosition   != primaryEndPosition
    ||  scopeStartLine     != primaryLine
    ||  scopeEndLine       != primaryLine) {

      if (nScopeEntries >= scope_pcs.length) {
         int old[] = scope_pcs;
                     scope_pcs=new int[  scope_pcs.length*2];
         System.arraycopy(old,0,scope_pcs,0,old.length);
             old   = scope_start_positions;
                   scope_start_positions=new int[scope_start_positions.length*2];
         System.arraycopy (old,0,scope_start_positions,0,old.length);
             old   = scope_end_positions;
                     scope_end_positions=new int[  scope_end_positions.length*2];
         System.arraycopy(old,0,scope_end_positions,0,old.length);
             old   = scope_start_line_offsets;
                     scope_start_line_offsets=new int[  scope_start_line_offsets.length*2];
         System.arraycopy(old,0,scope_start_line_offsets,0,old.length);
             old   = scope_end_line_offsets;
                     scope_end_line_offsets=new int[  scope_end_line_offsets.length*2];
         System.arraycopy(old,0,scope_end_line_offsets,0,old.length); 
      }
      int startLineOffset =  primaryLine - scopeStartLine;
      int   endLineOffset = scopeEndLine -    primaryLine;
      scope_pcs                [nScopeEntries] = pc;
      scope_start_positions    [nScopeEntries] = scopeStartPosition;
      scope_end_positions      [nScopeEntries] = scopeEndPosition;
      scope_start_line_offsets [nScopeEntries] = startLineOffset;
      scope_end_line_offsets   [nScopeEntries] =   endLineOffset;
      if (scopeStartPosition >= 255) nOverflows++;
      if (scopeEndPosition   >= 255) nOverflows++;
      if (startLineOffset    >= 255) nOverflows++;
      if (  endLineOffset    >= 255) nOverflows++;
      nScopeEntries++;
    }
  }

  int size()
  { return 2                    // name_idx
         + 4  	                // attr_len
         + 2  		        // primary table len spec
         + 2*nPrimaryEntries 	// primary table
         + 2  		        // scope table len spec
         + 6*nScopeEntries 	// scope table
         + 2*nOverflows;	// overflow short values
  }

  public void writeU13(DataOutputStream out, int value) throws IOException
  {
      if (value < 255) {
         out.writeByte(value);
      }
      else {
         out.writeByte (255);
         out.writeShort(value);
      }
  }

  public int readU13(DataInputStream in) throws IOException
  {
      int result =  in.readUnsignedByte();
      if (result == 255) {
          result =  in.readUnsignedShort();
      }
      return result;
  }

  static final boolean doTrace = false;

  public void write(ClassEnvironment e, AttributeOwner owner, DataOutputStream out)
    throws IOException, ByteCodingException
  {
    out.writeShort(e.resolveUnicode(getName()).slot);
    out.writeInt( 2 + 2*nPrimaryEntries
                + 2 + 6*  nScopeEntries
                +     2*  nOverflows);
    if (doTrace) System.out.println("nPrimaryEntries: "+nPrimaryEntries);
    out.writeShort (nPrimaryEntries);
    for (int i=0; i<nPrimaryEntries; i++) {
        if (doTrace) System.out.println("PrimaryEntries["+i+"] = "+primary_start_positions [i]+","+primary_end_positions[i]);
        writeU13 (out, primary_start_positions [i]);
        writeU13 (out,   primary_end_positions [i]);
    }
    out.writeShort (nScopeEntries);
    if (doTrace) System.out.println("nScopeEntries: "+nScopeEntries);
    for (int i=0; i<nScopeEntries; i++) {
if (doTrace) System.out.println("ScopeEntries["+i+"] = "
+scope_pcs [i]+","
+scope_start_line_offsets [i]+","
+scope_end_line_offsets[i]+","
+scope_start_positions[i]+","
+scope_end_positions[i]);
        out.writeShort (scope_pcs [i]);
        writeU13 (out, scope_start_line_offsets[i]);
        writeU13 (out, scope_end_line_offsets  [i]);
        writeU13 (out, scope_start_positions   [i]);
        writeU13 (out, scope_end_positions     [i]);
    }
  }

  public void decode (ClassEnvironment e) throws IOException {
    if (bytes == null) return;
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

    nPrimaryEntries = in.readUnsignedShort ();
     primary_start_positions = new int[nPrimaryEntries];
       primary_end_positions = new int[nPrimaryEntries];

    if (doTrace) System.out.println("nPrimaryEntries: "+nPrimaryEntries);
    for (int i=0; i<nPrimaryEntries; i++) {
        primary_start_positions [i] = readU13(in);
          primary_end_positions [i] = readU13(in);
        if (doTrace) System.out.println("PrimaryEntries["+i+"] = "+primary_start_positions [i]+","+primary_end_positions[i]);
    }
    nScopeEntries = in.readUnsignedShort ();
                   scope_pcs = new int[nScopeEntries];
    scope_start_line_offsets = new int[nScopeEntries];
      scope_end_line_offsets = new int[nScopeEntries];
       scope_start_positions = new int[nScopeEntries];
         scope_end_positions = new int[nScopeEntries];
    if (doTrace) System.out.println("nScopeEntries: "+nScopeEntries);
    for (int i=0; i<nScopeEntries; i++) {
                       scope_pcs [i] = in.readUnsignedShort();
        scope_start_line_offsets [i] = readU13(in);
        scope_end_line_offsets   [i] = readU13(in);
        scope_start_positions    [i] = readU13(in);
        scope_end_positions      [i] = readU13(in);
if (doTrace) System.out.println("ScopeEntries["+i+"] = "
+scope_pcs [i]+","
+scope_start_line_offsets [i]+","
+scope_end_line_offsets[i]+","
+scope_start_positions[i]+","
+scope_end_positions[i]);
    }
  }

/*************
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    try {
      int n = in.readShort();  // vars.size()
      for (int i=0; i<n; i++) {
          in.readShort(); // line.addElement (in.readShort())
          in.readShort();;
      }
    } catch (Exception exc) {
      System.out.println("LineNumberTableAttribute: decoding error");
      exc.printStackTrace();
    }
********/
}
