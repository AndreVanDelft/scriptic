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
 * This class is for launched script expressions within the same thread
 * in a certain RootNode
 */
class LaunchedNode extends Node implements CallerNodeInterface {

	LaunchedNode (NodeTemplate template, Node launcher) {
	    super(template, launcher.rootNode);
	    //launcher.subRootNode().addChild (this);
            pass  = launcher.pass;
            owner = launcher.owner;
     		localData = launcher.localData;
     		refinementAncestor = launcher.refinementAncestor;
	}

	/**
	 * Called from another script, in a given frame and without parameters
	 */
	final public void startScript (Object owner, NodeTemplate t) {
		startScript (owner, t, null);}

	/**
	 * Called from another script, in a given frame and with given data 
	 * It may not be called through FromJava.main;
         * the commRootNode should already be running
	 */
	// OLD: public final void startScript (ScriptFrame frame, ScriptParams params) {
	public final void startScript (Object owner, NodeTemplate t, Object params[]) {
if (doTrace) traceOutput("LaunchedNode.startScript: " + this 
	+ (owner==null? "  owner null": "  owner: " + owner));

	    t.activateFrom (this, owner, params);
	}

	/**
	 * Answer wether the actual parameter at the given 
	 * index is of forcing type ('!' or '?!' and ...) ALLWAYS FALSE
	 */
	final public boolean isActualParameterForcing(int index) {return false;}

	/**
	 * Answer wether the actual parameter at the given 
	 * index is of out type ('!' or '?!' and ...) ALLWAYS FALSE
	 */
	final public boolean isActualParameterOut(int index) {return false;}

	/** 
	 * add a child node: 
         * set the parent variable; adjust the active count.
	 * insert the child into the semi-closed double linked list of siblings
	 * ensure next==null
	 */
	void addChild (Node theChild)
	{
	    super.addChild (theChild);
	    theChild.parOpAncestor = new ParallelOperand(theChild);
	}
}
