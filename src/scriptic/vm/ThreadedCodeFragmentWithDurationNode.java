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
 * threaded code fragment node with duration
 */
final class ThreadedCodeFragmentWithDurationNode extends ThreadedCodeFragmentNode {
	ThreadedCodeFragmentWithDurationNode (NodeTemplate template, Node parent) {
	                             super (template, parent);}
	void scheduleRequest() {
            old_duration = duration;
            if ((subRootNode instanceof CommNode)
            && ((CommNode) subRootNode).partners == null) 
                 addToDurationList (rootNode.unboundCFDs);
            else addToDurationList (rootNode.pendingCFDs);
	}
	void scheduleSuccess() {addToDurationList(rootNode.successfulCFDs);}

	public Boolean tryOutInBoundMode(boolean wasUnbound) {
	    // note: hasSuccess must come first. Then the listOwner.current
	    // may be set to the nextReq for the subRootNode.hasActivity loop;
	    // only then deschedule is possible
	    hasSuccess();
	    listOwner.current = (CodeFragmentNode) nextReq;
	    deschedule(); 
            //subRootNode must know priority and duration!
            //So must be present in requests or successes or busy nodes
            //presence in successes dangerous; not yet to succeed();
            //  only when busy became false:
	    addToDurationList (rootNode.busyCFDs);
		codeInvoker().invokeInThread(this); // thread with doCode(); scheduleSuccess(); release lock...
	    return Boolean.TRUE;
	}
}
