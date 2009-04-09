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
 * plain code fragment node
 */
class PlainCodeFragmentNode extends CodeFragmentNode {
	PlainCodeFragmentNode (NodeTemplate template, Node parent) {
	                             super (template, parent);}

	public Boolean tryOutInBoundMode(boolean wasUnbound) {
            // note: atomicActionHappens must come first. Then the listOwner.current
            // may be set to the nextReq for the subRootNode.hasActivity loop;
            // only then deschedule is possible
	    atomicActionHappens();
        listOwner.current = (CodeFragmentNode) nextReq;
	    deschedule(); 

		if ((template.codeBits & TemplateCodes.AsyncFlag)!=0)
		{
			// much like ThreadedCodeFragmentNode.tryOutInBoundMode()
		    addToRequestList (rootNode.busyCFs);
            doCode(); 
		}
		else
		{
			scheduleSuccess();
			if ((template.codeBits & TemplateCodes.CodeFlag)>0) {
              rootNode.exitMutex(this); // safe???
	          doCode();
	          rootNode.enterMutex(this);
			}
		}
	    return Boolean.TRUE;
	}
}
