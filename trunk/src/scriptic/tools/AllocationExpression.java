/* This file is part of Sawa, the Scriptic-Java compiler
 * Copyright (C) 2009 Andre van Delft
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

package scriptic.tools;

import java.util.ArrayList;
import scriptic.tools.lowlevel.*;

class AllocationExpression extends MethodOrConstructorCallExpression {
   public AllocationExpression() {setSpecialMode();}
   public JavaExpression             enclosingInstance;
   public AnonymousTypeDeclaration   anonymousTypeDeclaration;
   public ArrayInitializer           arrayInitializer;
   public DataTypeDeclaration        dataTypeDeclaration;
   public ArrayList<JavaExpression> sizeExpressions = new ArrayList<JavaExpression>();
   public int    extraBracketPairs = 0;
   public int languageConstructCode() {return AllocationExpressionCode;}
   public void  setDimensionSignature(String s) {dimensionSignature=s;}

   Instruction newInstruction(ClassType constantPoolOwner) throws ByteCodingException {
       return new Instruction(INSTRUCTION_new, dataType.classRef(constantPoolOwner), this);}
}

