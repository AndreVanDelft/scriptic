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
 * Parallel break operator node in the runtime tree,
 */
final class ParBreakNode extends ParallelNode {

	ParBreakNode (NodeTemplate template, Node parent) {super (template, parent);}
	boolean isOrLikeOptr() {return true;}

	/**
	 * answer a child's nearest ancestor that is reactive stuff, i.e. '-'
	 * mostly the same as this' reacAncestor; sometimes this, however
	 */
	Node reacAncestor4Child() { return this; }

	/** 
	 * add a child node: 
         * set the parent variable; adjust the active count.
	 * insert the child into the semi-closed double linked list of siblings
	 * ensure next==null
	 */
	void addChild (Node theChild)
	{
	    super.addChild (theChild);
	    theChild.parOpAncestor = new ParBreakOperand(theChild);
	}
}

