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


final class SendChannelNode extends CommNode {

	SendChannelNode (CommFrame frame, Node parent) {
	    super (frame, parent);
	}

	/** 
	 * Answer whether the i'th parameter is out
	 */
	final public boolean isOut (int i)
	{
	    return false;
	}

	/** 
	 * Answer whether the i'th parameter is forced
	 */
	final public boolean isForced (int i)
	{
if(true)if(doTrace)trace("isForced("+i+"): false ");
	    return false;
	}

	/**
	 * Test wether the formal parameter at the given 
	 * index equals the actual forcing value
	 */
	public Boolean testParam (int index) {
	    return Boolean.FALSE;
	}

	/**
	 * Test wether all formal parameters equal the actual forcing values
	 */
	public Boolean testParams () {
	    return Boolean.FALSE;
	}

	/** 
	 * find partners and try them out on the given code fragment; bind if applicable
	 */
	final Boolean findAndTryPartnersFor (TryableNode tryableNode) {

	    partners = new CommRequestNode[1];

	    SendPartnerFrame partnerFrame = (SendPartnerFrame) frame._partners[0];
	    for (SendRequestNode s = (SendRequestNode) partnerFrame._requests.first; s!=null;
		 s = (SendRequestNode)s.nextReq)
	    {
		partners[0] = s;
		if (tryableNode.tryOutInBoundMode(/*wasUnbound=*/ true)) {
		    bindToPartners();
		    return Boolean.TRUE;
		}
	    }
	    partners = null;
	    return Boolean.FALSE;
	}
}
