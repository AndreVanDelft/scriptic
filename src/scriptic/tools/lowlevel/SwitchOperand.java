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

abstract class SwitchOperand extends InstructionOperand
{
  LabelInstruction dflt;
  LabelInstruction jmp[];
  Instruction source;

  LabelInstruction[] getTargetLabels() {
    if (dflt==null) return jmp; // cannot happen?
    LabelInstruction result[] = new LabelInstruction[jmp.length+1];
    System.arraycopy (jmp, 0, result, 0, jmp.length);
    result [jmp.length] = dflt;
    return result;
  }

}

