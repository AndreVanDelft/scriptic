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

public class TableswitchInstruction extends Instruction implements ClassFileConstants
{
  public TableswitchInstruction(int min, int max, LabelInstruction def, LabelInstruction j[]) throws ByteCodingException
  {
      this (min, max, def, j, null);
  }
  public TableswitchInstruction(int min, int max, LabelInstruction def, LabelInstruction j[], InstructionOwner owner) throws ByteCodingException
  {
    this (owner);
    setContents (min, max, def, j);
  }

  public TableswitchInstruction(InstructionOwner owner)
  {
    super (owner);
    opc = INSTRUCTION_tableswitch;
  }
  public TableswitchInstruction(TableswitchOperand operand) throws ByteCodingException
  {super(INSTRUCTION_tableswitch, operand);}

  public void setContents (int min, int max, LabelInstruction def, LabelInstruction j[]) throws ByteCodingException {
    operand = new TableswitchOperand(this, min, max, def, j);
  }

  public int deltaStackSize() {return -1;}
}
