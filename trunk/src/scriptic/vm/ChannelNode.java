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

/** channel node in the run-time tree */
final class ChannelNode extends CommNode {

	ChannelNode (CommFrame frame, Node parent) {
	    super (frame, parent);
	}

	/**
     * Answer the parameters supplied by a communicating partner
	 * If not yet bound, then the first of the waiting
	 * requests is taken (one of those that had this' commNode activated)
	 * @return the parameters supplied by a communicating partner
	 * @parm i: index for partner; 0: sender 1: receiver
         */
	public ValueHolder[] paramDataPrim(int i)
	{
	    CommRequestNode p = partner(i);
	    if (p==null)
	    {
    		ChannelPartnerFrame framePartner =
                 (ChannelPartnerFrame) frame._partners[i];
	    	p = (CommRequestNode) framePartner._requests.first;
	    }
	    if (p==null) // still possible...
                  return null;
	    return p.paramData();
	}

	/**
     * Answer the old parameters supplied by a communicating partner
	 * If not yet bound, then the first of the waiting
	 * requests is taken (one of those that had this' commNode activated)
	 * @return the old parameters supplied by a communicating partner
	 * @parm i: index for partner; 0: sender 1: receiver
     */
	public Object[] oldParamDataPrim(int i)
	{
	    CommRequestNode p = partner(i);
	    if (p==null)
	    {
    		ChannelPartnerFrame framePartner =
                (ChannelPartnerFrame) frame._partners[i];
	    	p = (CommRequestNode) framePartner._requests.first;
	    }
	    if (p==null) // still possible...
                  return null;
	    return p.oldParamData();
	}

	/** 
	 * Answer whether the i'th parameter is out
	 */
	public boolean isOut (int i)
	{
	    if (partners == null) return false;
            return ((ScriptCallNode)partners[1].parent).isActualParameterOut(i);
	}

	/** 
	 * Answer whether the i'th parameter is forced
	 */
	public boolean isForced (int i)
	{
	    if (partners == null) {
if(true)if(doTrace)trace("isForced("+i+"): false ");
               return false;
	    }
            //return ((ScriptCallNode)partners[1].parent).isActualParameterForcing(i);
            if (((ScriptCallNode)partners[1].parent).isActualParameterForcing(i)) {
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
	    if (!isForced(index)) {
               return Boolean.TRUE;
            }
	    if (partners[0].   params[index].hasEqualValue (
                partners[1].oldParams[index])) {
               return Boolean.TRUE;
	    }
        return Boolean.FALSE;
	}

	/**
	 * Test wether all formal parameters equal the actual forcing values
	 */
	public Boolean testParams () {
	    if (partners == null) return Boolean.FALSE;
	    for (int i=0; i<partners[0].template.nrIndexesAndParameters; i++) {
              if (!testParam(i)) return Boolean.FALSE;
	    }
            return Boolean.TRUE;
	}


	/** 
	 * find partners and try them out on the given code fragment; bind if applicable
	 * Note: the trick of finding possible partners is in the following rule:
	 *   Two requests are in parallel, and thus possible partners,
	 *   iff their traces upwards in the mixed trees of SpecificOperands
	 *   and the parent nodes thereof, meet for the first time
	 *   in such a parent node, so NOT in a SpecificOperands.
	 * To check for this condition, these traces will be marked by a stamp value.
	 */
	Boolean findAndTryPartnersFor (TryableNode tryableNode) {

	    partners = new CommRequestNode[2];

	    SendPartnerFrame sendPartnerFrame = (SendPartnerFrame) ((CommFrame)frame)._partners[0];
	    for (SendRequestNode s = (SendRequestNode) sendPartnerFrame._requests.first; s!=null;
		 s = (SendRequestNode)s.nextReq)
	    {
		partners[0] = s;
		placeStamps(s,0);

	    ReceivePartnerFrame receivePartnerFrame = (ReceivePartnerFrame) ((CommFrame)frame)._partners[1];
		for (ReceiveRequestNode r = (ReceiveRequestNode) receivePartnerFrame._requests.first; r!=null;
			r = (ReceiveRequestNode)r.nextReq)
		{
		    partners[1] = r;
		    if (tryableNode.tryOutInBoundMode(/*wasUnbound=*/ true)==Boolean.TRUE) {
			bindToPartners();
			return Boolean.TRUE;
		    }
		}
		removeStamps(s,0);
	    }
	    partners = null;
	    return Boolean.FALSE;
	}
}
