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

/** A binary or ternary test node in the run-time tree: x?y or x?y:z */

class BiTernaryTestNode extends Node {
	BiTernaryTestNode (NodeTemplate template, Node parent) {
	    super (template, parent);
	}

    /** activate: propagate to first operand. If this returns immediate success,
     * activate second operand as well. Else, if no child got activated at all,
     * activate third operand, if present.
     */
	int activate() {
	    int result = ((NodeTemplate)template.childs[0]).activateFrom(this);
	    if ((result & SuccessBit) != 0) {
	        result = ((NodeTemplate)template.childs[1]).activateFrom(this);
		    if ((result & SuccessBit) != 0) {
		       setRecentSuccess();
		    }
        }
        else if (activeChilds == 0
        && template.childs.length == 3) {
            result = ((NodeTemplate)template.childs[2]).activateFrom(this);
        }
	    return result;
    }

	/** one of the childs reports a success. Activate the next one, if applicable */
	int childSucceeds (Node child) {
	    if (child.template.indexAsChild==0) {
	        int result = template.childs[1].activateFrom(this);
if(doTrace)trace("?: : template.childs[1].activateFrom(this)="+result
      +((result & SuccessBit) != 0? "success!": "failure"));
                if((result & SuccessBit) != 0) {
                    result |= succeed();
                }
                return result;
	    }
	    else {
                return succeed();
	    }
	}
}
