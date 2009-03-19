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
 * Parallel operator &
 */
class ParAndNode extends NotBreakParallelNode {

	public String toString() {
	    return super.toString() + " started:" + startedChilds;
	}

	int               startedChilds = 0;
	/* report childSuccess if
	 *   startedChilds == immediatelySuccessfulChilds + recentlySuccessfulChilds
	 * ignore Exit
         * to simplify computations, immediatelySuccessfulChilds has been left out,
	 * and startedChilds-- is done instead of immediatelySuccessfulChilds++
	 */

	ParAndNode (NodeTemplate template, Node parent) {super (template, parent);}
	boolean isOrLikeOptr() {return false;}

	int activate(short firstChildTemplateIndex) {
	    NodeTemplate childTemplate;
	    int activateResult = 0, result = 0;
	  outerLoop:
            for (int i=firstChildTemplateIndex; ;i=0) {
		for (; i< template.childs.length; i++) {
		    childTemplate  = (NodeTemplate) template.childs[i];
	            activateResult = childTemplate.activateFrom (this);

		    if ((activateResult &    ExitBit) != 0) {isIteration=false; break outerLoop;}
		    startedChilds++;
		    if ((activateResult & SuccessBit) != 0) startedChilds--;
		    if ((activateResult &    SomeBit) != 0)
		    {
		    	if (pass > 0 || firstChildTemplateIndex > 0)
                        {
                           indexOfActivatedOptionalExit = i;
		           break outerLoop;
                        }
			cycle++;
		    }
		    else 
		    {
		        result |= activateResult & CodeBit;
		    }
	    	}
		if (!isIteration) break;
		pass++;
	    }

	    if (startedChilds == recentlySuccessfulChilds) {
	        result |= SuccessBit; setRecentSuccess();
	    }
	    if (activeChilds==0) markForDeactivation();
	    return result;
	}
}
