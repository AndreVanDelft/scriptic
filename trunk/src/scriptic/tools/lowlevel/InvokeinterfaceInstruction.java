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

public class InvokeinterfaceInstruction extends InvokeInstruction implements ClassFileConstants
{
  public InvokeinterfaceInstruction(ConstantPoolItem cpe, int nargs, int deltaStackSize) throws ByteCodingException {
    this (cpe,nargs,deltaStackSize,null);}
  public InvokeinterfaceInstruction(ConstantPoolItem cpe, int nargs, int deltaStackSize,InstructionOwner owner) throws ByteCodingException
  {
    this (new InvokeinterfaceOperand(cpe, nargs), owner);
    this.deltaStackSize = deltaStackSize;
  }
  public InvokeinterfaceInstruction(InvokeinterfaceOperand operand) throws ByteCodingException
  {super(INSTRUCTION_invokeinterface,operand, null);}
  public InvokeinterfaceInstruction(InvokeinterfaceOperand operand, InstructionOwner owner) throws ByteCodingException
  {super(INSTRUCTION_invokeinterface,operand, owner);}
}
