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

public class LookupswitchInstruction extends Instruction implements ClassFileConstants
{
  public LookupswitchInstruction(LabelInstruction def, int match[], LabelInstruction target[]) throws ByteCodingException
  {
      this (def, match, target, null);
  }
  public LookupswitchInstruction(LabelInstruction def, int match[], LabelInstruction target[], InstructionOwner owner) throws ByteCodingException
  {
    this (owner);
    setContents (def, match, target);
  }
  public LookupswitchInstruction(InstructionOwner owner)
  {
    super (owner);
    opc = INSTRUCTION_lookupswitch;
  }

  public LookupswitchInstruction(LookupswitchOperand operand) throws ByteCodingException
  {super(INSTRUCTION_lookupswitch, operand);}

  public void setContents (LabelInstruction def, int match[], LabelInstruction target[]) throws ByteCodingException {
    operand = new LookupswitchOperand(this, def, match, target);
  }


  public int deltaStackSize() {return -1;}
}
