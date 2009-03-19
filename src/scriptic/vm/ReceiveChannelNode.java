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

final class ReceiveChannelNode extends CommNode {

	ReceiveChannelNode (CommFrame frame, Node parent) {
	    super (frame, parent);
	}

	/** 
	 * Answer whether the i'th parameter is out
	 */
	final public boolean isOut (int i)
	{
	    if (partners == null) return false;
            return ((ScriptCallNode)partners[0].parent).isActualParameterOut(i);
	}

	/** 
	 * Answer whether the i'th parameter is forced
	 */
	final public boolean isForced (int i)
	{
	    if (partners == null) {
if(true)if(doTrace)trace("isForced("+i+"): false ");
               return false;
	    }
            //return ((ScriptCallNode)partners[0].parent).isActualParameterForcing(i);
            if (((ScriptCallNode)partners[0].parent).isActualParameterForcing(i)) {
if(true)if(doTrace)trace("isForced("+i+"): true ");
               return true;
	    }
if(true)if(doTrace)trace("isForced("+i+"): false ");
               return false;
	}

	/**
	 * Test wether the formal parameter at the given 
	 * index equals the actual forcing value
	 */
	public Boolean testParam (int index) {
	    if (partners == null) return Boolean.FALSE;
	    return partners[0].testParam(index);
	}

	/**
	 * Test wether all formal parameters equal the actual forcing values
	 */
	public Boolean testParams () {
	    if (partners == null) return Boolean.FALSE;
	    return partners[0].testParams();
	}

	/** 
	 * find partners and try them out on the given code fragment; bind if applicable
	 */
	final Boolean findAndTryPartnersFor (TryableNode tryableNode) {

	    partners = new CommRequestNode[1];

	    ReceivePartnerFrame partnerFrame = (ReceivePartnerFrame) frame._partners[0];
//ERROR?    for (ReceiveRequestNode r = (ReceiveRequestNode) partnerFrame.sendRequests.first; r!=null;
	    for (ReceiveRequestNode r = (ReceiveRequestNode) partnerFrame._requests.first; r!=null;
		 r = (ReceiveRequestNode)r.nextReq)
	    {
		partners[0] = r;
if(doTrace)trace("findAndTryPartnersFor("+tryableNode+") partner: "+r+"   ");
		if (tryableNode.tryOutInBoundMode(/*wasUnbound=*/ true)) {
		    bindToPartners();
		    return Boolean.TRUE;
		}
	    }
	    partners = null;
	    return Boolean.FALSE;
	}
}
