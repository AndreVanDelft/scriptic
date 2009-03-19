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

public class InvokeInstruction extends Instruction
{
  public InvokeInstruction(int opc, ConstantPoolItem arg, int deltaStackSize) throws ByteCodingException {this (opc,arg,deltaStackSize,null);}
  public InvokeInstruction(int opc, ConstantPoolItem arg, int deltaStackSize,InstructionOwner owner)
    throws ByteCodingException {
    this.owner = owner;
    this.opc   = opc;
    this.deltaStackSize   = deltaStackSize;
    if (opc < 0) throw new ByteCodingException ("Instruction opcode: "+opc);
    switch(opc)
    {
      case INSTRUCTION_invokespecial:
      case INSTRUCTION_invokestatic:
      case INSTRUCTION_invokevirtual: operand = new CPOperand(arg);        break;

      default: throw new ByteCodingException
          (INSTRUCTION_String[opc] + " does not take a ConstantPoolItem item as an argument");
    }
  }
  public InvokeInstruction(int opc, InstructionOperand operand, InstructionOwner owner) throws ByteCodingException
  {super(opc,operand,owner);}

  int deltaStackSize;
  public int deltaStackSize() {return deltaStackSize;}
}
