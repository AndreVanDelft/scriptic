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

/**
 * Node hierarchy:
 *
 * Node
 *	Branch
 *		Script 
 *		Comm
 *			Channel
 *				SndChannel, RcvChannel
 *
 * LocalData  > Script, Communication, Channel, SndChannel, RcvChannel
 * Callee     > Script
 * Callee     > CommRequest > SendRequest, ReceiveRequest
 */

package scriptic.vm;

/**
 * This class serves representing a script node in the runtime tree,
 * i.e. the root of a regular script, a communication or a channel
 *
 * @version 	1.0, 11/30/95
 * @author	Andre van Delft
 */

class ScriptNode extends Node implements RefinementNodeInterface, CalleeNodeInterface {

	ScriptNode (NodeTemplate template, Node parent, 
	                 Object owner, Object oldParams[]) {
	    super (template, parent);
	    this.owner         = owner;
	    this.oldParams     = oldParams;
	    this.localData     = null;
	    refinementAncestor = this; // cannot through refinementAncestor4Child
            params = template.makeParameterHolders (oldParams);
	}

	/** Parameter data */
	ValueHolder params[];
	Object   oldParams[];
	
	/** Answer parameter data */
	public ValueHolder[] paramDataPrim() {return params;}

	/** Answer old parameter data */
	public Object[] oldParamDataPrim() {return oldParams;}

	/** Answer the i'th partner in communication */
	public CommRequestNode partner(int i) {return null;}

	/** 
	 * answer a child's refinement ancestor: this selve
	 */
	final RefinementNodeInterface refinementAncestor4Child() { return this; }

	/** 
	 * Answer whether the i'th parameter is out
	 */
	final public boolean isOut (int i)
	{
	    return ((CallerNodeInterface)parent).isActualParameterOut(i);
	}

	/** 
	 * Answer whether the i'th parameter is forced
	 */
	final public boolean isForced (int i)
	{
      if (! (parent instanceof CallerNodeInterface)) {
                  return false; // for root script call...
      }
	    //return ((CallerNodeInterface)parent).isActualParameterForcing(i);
      if (((CallerNodeInterface)parent).isActualParameterForcing(i)) {
        return true;
      }
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
	    // return params[index].equals (oldParams[index]);
	    if (params[index].hasEqualValue (oldParams[index])) {
               return Boolean.TRUE;
	    }
        return Boolean.FALSE;
	}

	/**
	 * Test wether all formal parameters equal the actual forcing values
	 */
	public Boolean testParams () {
	    for (short i=0; i<template.nrIndexesAndParameters; i++) {
	        if (!testParam(i)) return Boolean.FALSE;
	    }
	    return Boolean.TRUE;
	}
}

