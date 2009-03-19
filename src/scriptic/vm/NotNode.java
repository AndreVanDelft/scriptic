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
 * This class serves representing a Not node in the runtime tree,
 */
class NotNode extends OperatorNode {

	NotNode (NodeTemplate template, Node parent) {super (template, parent);
       operatorAncestor    = parent.  operatorAncestor;
	}
	final boolean isOrLikeOptr() {return false;}

	void childDeactivates (Node child, boolean isActively)
	{ if (isActively && !child.hadRecentSuccess()) succeed();
	}

	int activate() {
	    int result = ((NodeTemplate)template.childs[0]).activateFrom(this);
	     
	         if ((result & SuccessBit) != 0)  result &= ~SuccessBit;
	    else if ((result &    CodeBit) == 0) {result |=  SuccessBit; setRecentSuccess();}
	    return result;
        }
}
