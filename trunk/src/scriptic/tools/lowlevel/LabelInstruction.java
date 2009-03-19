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

import java.io.*;

public class LabelInstruction extends Instruction implements ClassFileConstants
{
  public static final int TRY_START_TYPE = 1;
  public static final int   TRY_END_TYPE = 2;
  public static final int     CATCH_TYPE = 3;
  String id;
  int type;
  private int referencesBySwitchTablesAndCatchHandlers;
  private int referenceCount;
  final   int getReferenceCount() {return referenceCount;}
  final void incReferenceCount(Instruction source) throws ByteCodingException {
    if (referenceCount==0) currentStackSize=source.currentStackSize;
    else if (currentStackSize!=source.currentStackSize) {
       throw new ByteCodingException ("label sources with inconsistent stack sizes: "+currentStackSize
                          + " and " + source.currentStackSize);
    }
    referenceCount++;
  }
  final void decReferenceCount() {referenceCount--;}
  final void incReferencesBySwitchTablesAndCatchHandlers()
               {referencesBySwitchTablesAndCatchHandlers++;}
  public final boolean isUnusedLabel() {return opc==INSTRUCTION_label && referenceCount <= 0
                        && referencesBySwitchTablesAndCatchHandlers <= 0;}
  public boolean isTryStartLabel() {return type==TRY_START_TYPE;}
  public boolean   isTryEndLabel() {return type==TRY_END_TYPE;}
  public boolean    isCatchLabel() {return type==CATCH_TYPE;}

  public LabelInstruction(String tag) {this(tag,null);}
  public LabelInstruction(String tag, InstructionOwner owner)
  {
    id      = tag.intern();
    opc     = INSTRUCTION_label;
    operand = null;
    this.owner = owner;
  }
  public LabelInstruction(String tag, InstructionOwner owner, int type) {
      this (tag, owner);
      this.type = type;
  }
  public void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
  { return; }
                                // and the size method appropriately
  int size(ClassEnvironment e, CodeAttribute ce)
  { return 0; }
                                // This is called from the LabelOperand
  void writeOffset(ClassEnvironment e, CodeAttribute ce, Instruction source, DataOutputStream out)
    throws ByteCodingException, IOException
  {                             // write the offset (as a short)
                                // of source
    int tpc;
    if (source==null) tpc = 0;
    else              tpc = source.pc;
    short offset = (short) (pc - tpc);
    out.writeShort(offset);
  }
  void writeWideOffset(ClassEnvironment e, CodeAttribute ce, Instruction source, DataOutputStream out)
     throws IOException, ByteCodingException
  {
    int tpc;
    if (source==null) tpc = 0;
    else              tpc = source.pc;
    out.writeInt(pc - tpc);
  }
  public String getPresentation(ClassEnvironment e) {
    return id+'['+referenceCount+"]:"
         + (opc==INSTRUCTION_deleted?" DELETED":"");}
}

