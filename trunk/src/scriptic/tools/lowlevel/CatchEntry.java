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

public class CatchEntry implements ClassFileConstants
{
  LabelInstruction start_pc, end_pc, handler_pc;
  ConstantPoolItem catch_cpe;

  public
  CatchEntry(LabelInstruction start, LabelInstruction end, LabelInstruction handler, ConstantPoolClass cat)
  {
    start_pc   = start;
    end_pc     = end;
    handler_pc = handler;
    catch_cpe  = cat;
  }

   public String getDescription () {
      StringBuffer result = new StringBuffer();
      result.append (  start_pc.getPresentation()).append ("|");
      result.append (    end_pc.getPresentation()).append ("|");
      result.append (handler_pc.getPresentation()).append ("|");
      return result.append (lineSeparator).toString ();
   }

  void resolve(ClassEnvironment e)
  { }

  void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException
  {
      start_pc.writeOffset(e, ce, null, out);
        end_pc.writeOffset(e, ce, null, out);
    handler_pc.writeOffset(e, ce, null, out);
    if (catch_cpe != null) { out.writeShort(catch_cpe.slot); }
    else                   { out.writeShort(0); }
  }
  static CatchEntry readFrom (ClassEnvironment e, DataInputStream in)
    throws IOException, ByteCodingException
  {
    LabelInstruction start_pc=null, end_pc=null, handler_pc=null;
    ConstantPoolClass catch_cpe=null;
      start_pc = new LabelInstruction (""+in.readShort(), null);
        end_pc = new LabelInstruction (""+in.readShort(), null);
    handler_pc = new LabelInstruction (""+in.readShort(), null);
    e.getConstantPoolItem (in.readShort());
    CatchEntry result = new CatchEntry(start_pc, end_pc, handler_pc, catch_cpe);
    return result;
  }

}
