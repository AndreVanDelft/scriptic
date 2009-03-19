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

class ReceiveCallNode extends ScriptCallNode {

	ReceiveCallNode (NodeTemplate template, Node parent) {
	                                super (template, parent);}

	/**
	 * Return the callee's paraneters for transfer;
         * actually, if possible, return the partner's parameters
         * DEACTIVATED
	final public ScriptParams calleeParams()
	{
            CommRequestNode child = (CommRequestNode)firstChild;
            if (child.commNode!=null
            &&  child.commNode.partners.length == 2) {
                return child.commNode.partners[0].paramData();
            }
	    return child.paramData();
	}
      **/

	/** Child succeeds. Transfer parameters */
	int childSucceeds (Node n) {
if (doTrace)trace("ReceiveCallNode.Transfer #"+template.nrIndexesAndParameters+"   ");

            CommRequestNode child = (CommRequestNode)n;
            if (child.commNode!=null
            &&  child.commNode.partners.length == 2) {
              ValueHolder targetParameters[] = child.commNode.partners[1].paramData();
              if (targetParameters!=null) {
                ValueHolder sourceParameters[] = child.commNode.partners[0].paramData();
                for (int i=0; i<targetParameters.length; i++) {
if (doTrace)trace("ReceiveCallNode.Transfer("+i+"): "+targetParameters.length+"<>"+sourceParameters.length);
                    targetParameters[i].setValue (sourceParameters[i]);
                }
              }
            }
            super.childSucceeds(n);
	    return 0;
	}

}
