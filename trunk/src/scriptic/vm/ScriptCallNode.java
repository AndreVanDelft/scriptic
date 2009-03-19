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
 * This class serves represventing a script call in the runtime tree,
 * i.e. the call of a regular script, a communication or 
 *      send to/receive from a channel
 */
public class ScriptCallNode extends Node implements CallerNodeInterface {


	ScriptCallNode (NodeTemplate template, Node parent) {
	                                super (template, parent);}

	ScriptCallNode () {super ();}

	/** A child deactivates from the run-time tree */
	void childDeactivates (Node theChild) {}

	/** save area between activate() and start() */
	int activateResult;

	/**
	 * Called from another script, in a given frame and without parameters
	 */
	final public void startScript (Object owner, NodeTemplate t) {
		startScript (owner, t, null);}

	/**
	 * Called from another script, in a given frame and with given parameters 
	 */
	// OLD: public final void startScript (ScriptFrame frame, ScriptParams params) {
	public final void startScript (Object owner, NodeTemplate t, Object params[]) {

if (doTrace)
trace("ScriptCall.startScript: " 
	+ (owner==null? "  owner null": "  owner: " + owner));

	    activateResult = t.activateFrom (this, owner, params);
		    if ((activateResult & SuccessBit) != 0) {
		        setRecentSuccess();
		    }
	}

	int activate() { 
if (doTrace){
trace("ScriptCall.activate: ");
}
        doAnchorCodeIfPresent();
	    doCode(); 
	    return activateResult;
	}

	public boolean isActualParameterForcing(int index) {
	    if ( template.isActualParameterForcing (index)) return true;
	    if (!template.isActualParameterAdapting(index)) return false;
	    if (refinementAncestor==null)                   return false;
            int otherIndex = template.actualOutForcingParameterIndexes [index];
	    return refinementAncestor.isForced(otherIndex);
	}
	public boolean isActualParameterOut(int index) {
	    if ( template.isActualParameterOut     (index)) return true;
	    if (!template.isActualParameterAdapting(index)) return false;
	    if (refinementAncestor==null)                     return false;
            int otherIndex = template.actualOutForcingParameterIndexes [index];
	    return refinementAncestor.isOut(otherIndex);
	}
            //if (refinementAncestor instanceof CommNode) {
            //    CommRequestNode partners[] = ((CommNode)refinementAncestor).partners;
            //    Point p = partnerIndexOfParameterAt(partners, index);
            //    return ((ScriptCallNode)partners[p.x].parent).isActualParameterForcing(p.y);
            //} 


	/** Child succeeds. Transfer parameters */
	int childSucceeds (Node n) {
if (doTrace)trace("ScriptCall.Transfer #"+template.nrIndexesAndParameters+"   ");
	    int offset = 0; // at 0 is the script call itself!
	    for (short i=0; i<template.nrIndexesAndParameters; i++) {
		if (template.isActualParameterOut     (i)
		||  template.isActualParameterAdapting(i)) offset++;
		if (         isActualParameterOut     (i)) {
if (doTrace)trace("ScriptCall.Transfer("+i+")@"+offset+">> "+(codeIndex()+offset)+"   ");
		    doCode (codeIndex() + offset);
		}
	    }
	    return 0;
	}
}
