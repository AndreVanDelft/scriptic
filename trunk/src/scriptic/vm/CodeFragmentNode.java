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
 * Code fragment node in the runtime tree.
 */
abstract class CodeFragmentNode extends RequestNodeWithDuration implements TryableNode  {

	CodeFragmentNode (NodeTemplate template, Node parent) {
	                             super (template, parent);}

    /** schedule this request in the appropriate list for try-out
     * i.e. commRootNode.unboundCFs or subRootNode().pendingCFs 
     */
	void scheduleRequest() {
            if ((subRootNode instanceof CommNode)
            && ((CommNode) subRootNode).partners == null) 
                 addToRequestList (rootNode.unboundCFs);
            else addToRequestList (rootNode.pendingCFs);
	}
	/**
	 * schedule this request in the successes list
	 */
	void scheduleSuccess() {addToRequestList(rootNode.successfulCFs);}
	/**
	 * remove this request from its request list
	 */
	void deschedule()      {removeFromRequestList();}
	
	/** 
	 * try out with the possibility that the subSubRootNode is an unbound
	 * communication script, with no established partners.
	 * Then that node should search for partners, and try those
	 * using tryOutInBoundMode.
	 * Otherwise tryOutInBoundMode will be called directly
	 */
	Boolean tryOut() {
	    if (subRootNode instanceof CommNode) {
		CommNode commNode = (CommNode) subRootNode;
		if (commNode.partners == null) {
if (doTrace) trace("tryOut.findAndTryPartnersFor on: "+commNode+" ");
		    Boolean result = commNode.findAndTryPartnersFor (this);
if (doTrace) trace("tryOut.findAndTryPartnersFor: "+result+" ");

		    return result;
		}
	    } 
	    return tryOutInBoundMode(/*wasUnbound=*/ false);
	}
}


