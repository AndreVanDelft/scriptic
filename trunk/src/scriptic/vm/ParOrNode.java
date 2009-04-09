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
 * Dual-parallel operator node |
 */
class ParOrNode extends NotBreakParallelNode {
	ParOrNode (NodeTemplate template, Node parent) {super (template, parent);}
	boolean isOrLikeOptr() {return true;}
	
	void atomicActionHappensInChild(Node theChild) {
		if (passOfLatestOperandWithSuccessOnActivation>=0)
		{
			if (!hadRecentSuccess()) {
				succeed();
				setRecentSuccess();
			}
		}
		super.atomicActionHappensInChild(theChild);
	}
	
}

