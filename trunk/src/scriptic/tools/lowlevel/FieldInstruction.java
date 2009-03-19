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

public class FieldInstruction extends Instruction {

  boolean isBig;

  public FieldInstruction(int opc, ConstantPoolFieldReference arg, boolean isBig) throws ByteCodingException {
               this (opc,arg,isBig,null);}
  public FieldInstruction(int opc, ConstantPoolFieldReference arg, boolean isBig,InstructionOwner owner)
    throws ByteCodingException
  {
    this.owner = owner;
    this.opc   = opc;
    this.isBig = isBig;

    switch(opc)
      {
      case INSTRUCTION_getstatic:
      case INSTRUCTION_putstatic:
      case INSTRUCTION_getfield:
      case INSTRUCTION_putfield:  operand = new CPOperand(arg); break;
      default: throw new ByteCodingException
          (INSTRUCTION_String[opc] + " is not a FieldInstruction");
      }
  }

  public int deltaStackSize() {
    switch(opc)
      {
      case INSTRUCTION_getstatic: return isBig?  2:  1;
      case INSTRUCTION_putstatic: return isBig? -2: -1;
      case INSTRUCTION_getfield : return isBig?  1:  0;
      case INSTRUCTION_putfield : return isBig? -3: -2;
      }
    return 666; // Error...
  }
}
