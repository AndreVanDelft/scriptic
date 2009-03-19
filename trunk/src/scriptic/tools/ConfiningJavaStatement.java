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

import scriptic.tools.lowlevel.LabelInstruction;

/** Statement that confines variable declarations.
 * May also be abused in other cases, e.g.,
 * to establish start label and end label in a labeled statement,
 * or to serve as a superclass for TestStatement.
 * No problem; the variable confinement is effectuated by
 * applicable ScripticCompilerPass7.process*Statement functions
 */
abstract class  ConfiningJavaStatement extends JavaStatement {
   LabelInstruction startLabel; 
   LabelInstruction   endLabel; // so that local variables may see where their scope end
}
