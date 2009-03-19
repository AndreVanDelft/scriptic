/* This file is part of the Scriptic Virtual Machine
 * Copyright (C) 2009 Andre van Delft
 *
 * The Scriptic Virtual Machine is free software: 
 * you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package scriptic.vm;

/**
 * This class serves representing an operator node in the runtime tree,
 * i.e. an operator as +|;&*,/ etc.
 * Most features are implemented in subclasses.
 *
 * @version 	1.0, 11/30/95
 * @author	Andre van Delft
 */
abstract class OperatorNode extends Node {

	/** whether this is an iteration */
	boolean isIteration;

	OperatorNode (NodeTemplate template, Node parent) {
	    super (template, parent);
            operatorAncestor = this;
            isIteration      = template.isIteration();
            pass             = 0;
        }

        /** whether this is one of | / % -; + * supply default answer */
        boolean isOrLikeOptr() {return true;}

        /** whether this is one of -; ; supply default answer */
        boolean isSequence() {return false;}
}
