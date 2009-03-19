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

final class UnsureCodeFragmentNode extends CodeFragmentNode {
	UnsureCodeFragmentNode (NodeTemplate template, Node parent,
                                       boolean isRetrying) {
	    super (template, parent);
            this.isRetrying = isRetrying;
        }
        boolean isRetrying;

	final void scheduleRequest() {
	    if ((subRootNode instanceof CommNode)
            && ((CommNode) subRootNode).partners == null) 
	         addToRequestList (rootNode.unsureCFs);
	    else addToRequestList (rootNode.pendingCFs);
	}

	final public Boolean tryOutInBoundMode(boolean wasUnbound) {
if (doTrace) trace("tryOutInBoundMode: ");
	    doCode();
	    if (success==Boolean.TRUE) {
		    // note: hasSuccess must come first. Then the listOwner.current
		    // may be set to the nextReq for the subRootNode.hasActivity loop;
		    // only then deschedule is possible
		    hasSuccess();
		    listOwner.current = (CodeFragmentNode) nextReq;
		    deschedule(); 
		    scheduleSuccess();
	    }
	    else if (success==Boolean.FALSE) {
			if (!wasUnbound && !isRetrying ) {
			    listOwner.current = (CodeFragmentNode) nextReq;
			    deschedule();
			    markForDeactivation();
			}
	    }
		return success;
	}
}
