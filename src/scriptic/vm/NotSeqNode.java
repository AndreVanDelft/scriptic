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
 * A not-sequence operator node in the runtime tree,
 */
final class NotSeqNode extends OperatorNode {

	short lastActivatedTemplateIndex;
	NotSeqNode (NodeTemplate template, Node parent) {super (template, parent);}
	boolean isOrLikeOptr() {return true;}
        boolean isSequence  () {return true;}

	/** default activation: propagate to first template child */
	int activate() {return activate((short)0);}

	/** general activation: start at the given template */
	int activate(short childTemplateIndex) {
	    int result = 0;
	    int localResult;
	    while (true) {
	        lastActivatedTemplateIndex = childTemplateIndex;
		localResult = template.childs[childTemplateIndex].activateFrom(this);
	        if ((localResult &    CodeBit) != 0) {result |=    CodeBit; break;}
	        if ((localResult &    ExitBit) != 0) {result |= SuccessBit; break;}
	        if ((localResult &    SomeBit) != 0) {result |= SuccessBit;}
	        if ((localResult & SuccessBit) != 0) {break;}
	        if (childTemplateIndex < template.childs.length -1) {
		    childTemplateIndex++;
		} else if (isIteration) {
		    childTemplateIndex = 0;
		    pass++;
		} else {result |= SuccessBit; setRecentSuccess(); break;}
	    }
	    if (activeChilds==0) markForDeactivation();
	    return result;
        }

	/** 
	 * specific action when child deactivates: when without success, proceed with next
	 */
	void childDeactivates (Node theChild, boolean isActively)
	{
	    if (!isActively || hadRecentSuccess()) return;
            short childIndex = (short) (theChild.template.indexAsChild + 1);
	    if (childIndex == template.childs.length) {
	        if (!isIteration) return;
		childIndex = 0;
		pass++;
	    }
	    cycle++;
	    activate(childIndex);
	}
}
