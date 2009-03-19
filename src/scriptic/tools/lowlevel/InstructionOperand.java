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

abstract class InstructionOperand implements ClassFileConstants
{
  abstract void write(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException;
  abstract int size(ClassEnvironment e, CodeAttribute code) throws ByteCodingException;
  void resolve(ClassEnvironment e) {}
  void writePrefix(ClassEnvironment e, CodeAttribute ce, DataOutputStream out)
    throws IOException, ByteCodingException  {}
  public String getFullPresentation (Instruction owner) {return getPresentation(owner)+' '+getCommentString(null);}
  public abstract String getPresentation (Instruction owner);
  public          String getCommentString     (ClassEnvironment e) {return null;}
                  void   incTargetReferenceCount() throws ByteCodingException {}  // only for LabelOperands
                  void   decTargetReferenceCount() {}  // only for LabelOperands
  LabelInstruction[] getTargetLabels() {return null;}
  InstructionOperand copyFor (Instruction source) {return this;} // only for LabelOperand
  public LabelInstruction getTarget() {return null;}
}

