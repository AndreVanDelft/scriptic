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

final class SeqNode extends OperatorNode {
	short lastActivatedTemplateIndex;
	int successfulChilds;

	SeqNode (NodeTemplate template, Node parent) {super (template, parent);}
	boolean isOrLikeOptr() {return false;}
        boolean isSequence  () {return true;}

	/** default activation: propagate to first template child */
	int activate() {return activate((short)0);}

	/** general activation: start at the given template */
	int activate(short childTemplateIndex) {
	    int result = 0;
	    int localResult = 0;
	    short i=childTemplateIndex;
            while (true) {
	        lastActivatedTemplateIndex = i;
	        NodeTemplate childTemplate = template.childs[i];
		localResult = childTemplate.activateFrom(this);
	        if ((localResult & CodeBit) != 0) {result |= CodeBit;}
	        if ((localResult & ExitBit) != 0) {result |= SuccessBit; break;}
	        if ((localResult & SomeBit) != 0) {result |= SuccessBit;}
                                  // not applicable: setRecentSuccess()? also in line before?

                if ((i+1)%template.childs.length == childTemplateIndex
                &&  (result & CodeBit) == 0
                &&  isIteration )  // prevent hanging "while(true);-"
                {
                    if ((localResult & SuccessBit) != 0)
                             result |= SuccessBit; 
                    break; 
                }
	        if ((localResult & SuccessBit) != 0) {
if (doTrace)trace("successfully activated; isIteration? " + isIteration + "...");
		    i++;
		    if (i == template.childs.length) {
		      if (isIteration) {
		        i = 0;
		        pass++;
		      } else {result |= SuccessBit; break;}
                                  // not applicable: setRecentSuccess()?
		    }
		} else break;
	    }
	    if (activeChilds==0) {
//trace("activate(i): markForDeactivation: ");
                markForDeactivation();
	    }
	    return result;
        }

	/** one of the childs reports a success.
         *  Activate the next child, if applicable
         *  return succeed() if this succeeds
         *  @return applicable bitset out of CodeBit, SuccessBit
         */
	int childSucceeds (Node child) {
	    short childIndex = (short) (child.template.indexAsChild + 1);
/***********
trace("childSucceeds: childIndex="+childIndex
     +" template.childs.length="+template.childs.length
     +" isIteration="+isIteration);
************/
	    if (childIndex== template.childs.length) {
	        if (!isIteration) return succeed();
		childIndex = 0;
		if (lastActivatedTemplateIndex == child.template.indexAsChild)
		    pass++;
	    }
	    else if (lastActivatedTemplateIndex < child.template.indexAsChild)
		     pass--;
	    cycle++;
	    int result = activate(childIndex);
            if ((result & SuccessBit) != 0) {
                int localResult = succeed();
                if ((localResult & CodeBit) != 0) result |= CodeBit;
            }
            return result;
	}

/* messed-up version:
	int childSucceeds (Node child) {
	    int result      = 0;
	    int localResult = 0;
            for (int childIndex = child.template.indexAsChild + 1; ; childIndex++)
            {
	        if (childIndex== template.childs.length)
                {
	            if (!isIteration) return succeed();
		    childIndex = 0;
		    if (lastActivatedTemplateIndex == child.template.indexAsChild)
		        pass++;
	        }
	        else if (lastActivatedTemplateIndex < child.template.indexAsChild)
	 	         pass--;
	        cycle++;
if(doTrace)trace("seqNode childSucceeds; activating: "+childIndex);
	        localResult = activate((short)childIndex);
                if ((localResult &    CodeBit) != 0) {result |=    CodeBit;}
	        if ((localResult &    ExitBit) != 0) {result |= SuccessBit; return succeed();}
	        if ((localResult &    SomeBit) != 0) {result |= SuccessBit; succeed();}
                if ((localResult & SuccessBit) == 0) break;
                if (      result &    CodeBit) == 0)
                && childIndex == child.template.indexAsChild)
                    // no code activated; one round activated: stop
                    break;
	    }
	    return result;
	}
*/
}
