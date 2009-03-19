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
 * An or operator node in the runtime tree,
 */
final class OrNode extends OperatorNode {

	OrNode (NodeTemplate template, Node parent) {super (template, parent);}
	boolean isOrLikeOptr() {return true;}

	int activate() {
		    boolean hadSuccessfulChilds  = false;
	    boolean codeActivated        = false;
	    boolean codeActivatedInCycle = false;
	    int activateResult = 0;
	  outerLoop:
            for (int i=0; ;i=0) {
		for (; i< template.childs.length; i++) {
	            activateResult =  template.childs[i].activateFrom (this);

		    if ((activateResult & ExitBit) != 0) {isIteration=false; break outerLoop;}
		    hadSuccessfulChilds |= ((activateResult&SuccessBit)!=0);
		    if ((activateResult & SomeBit) != 0)
		    {
		    	if (!codeActivatedInCycle && cycle > 0)
		           break outerLoop;
		        codeActivatedInCycle = false;
			cycle++;
		    }
		    else 
		    {
		        codeActivatedInCycle |= ((activateResult&CodeBit)!=0);
		        codeActivated        |= codeActivatedInCycle;
		    }
	    	}
		if (!isIteration) break;
		pass++;
	    }

	    if (codeActivatedInCycle) activateResult |= CodeBit;
	    if (hadSuccessfulChilds ) {
	        activateResult |= SuccessBit; setRecentSuccess();
	    }
	    if (activeChilds==0) markForDeactivation();
	    return activateResult & (CodeBit|SuccessBit);
	}
}

