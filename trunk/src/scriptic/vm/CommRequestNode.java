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
 * A communication request node, i.e. a communicating partner.
 */

class CommRequestNode extends RequestNode implements CalleeNodeInterface {

	CommNode commNode = null;
	ValueHolder params[];
	Object   oldParams[];
	
	/** associated frame */
	CommPartnerFrame frame;

	CommRequestNode (NodeTemplate template, Node parent, 
			         Object owner, Object oldParams[]) {
	    super (template, parent);
	    this.owner = owner;
	    this.oldParams = oldParams;
	    refinementAncestor = this; // cannot through refinementAncestor4Child
            params = template.makeParameterHolders (oldParams);
	}

	/** 
	 * answer a child's refinement ancestor: this selve
	 */
	final RefinementNodeInterface refinementAncestor4Child() { return this; }

	/**
	 * Return the paraneters
         */
	final public ValueHolder[] paramDataPrim()
	{
	    return params;
	}

	/**
	 * Return the old paraneters
         */
	final public Object[] oldParamDataPrim()
	{
	    return oldParams;
	}

	/** answer the index in the communicating array. Currently 0 */
	short index() {return 0;}

	void scheduleRequest() {
        rootNode.frameLookup.addCommRequestNode(this);
	}

	void deschedule() {
        rootNode.frameLookup.removeCommRequestNode(this);
	}

	/** 
	 * this is to be descheduled and deactivated,
	 * probably because an exclusive code fragment succeeded
	 */
	void exclude()
	{
	    if   (commNode!=null) {commNode.excludeAllExcept(this); commNode=null;}
	    else if (listOwner!=null) deschedule(); // from request list; success list N.A.
	    deactivate(false); // upwards in tree
	}
	/** 
	 * this is to be descheduled and deactivated,
	 * probably because an exclusive code fragment succeeded
	 */
    public void incSuspendedCount(boolean increment)
	{
		super.incSuspendedCount(increment);
	    if   (commNode!=null) {
	    	commNode.parOpAncestor.suspendOrResumeRequests(increment);
	    }
	}

	/**
	 * Test wether the formal parameter at the given 
	 * index equals the actual forcing value
	 */
	public Boolean testParam (int index) {
	    if (!((CallerNodeInterface)parent).isActualParameterForcing(index)) return Boolean.TRUE;
	    doCode(index);
	    return success;
	}

	/**
	 * Test wether all formal parameters equal the actual forcing values
	 */
	public Boolean testParams () {
	    for (short i=0; i<template.nrIndexesAndParameters; i++) {
	        if (testParam(i)==Boolean.FALSE) return Boolean.FALSE;
	    }
	    return Boolean.TRUE;
	}
}
