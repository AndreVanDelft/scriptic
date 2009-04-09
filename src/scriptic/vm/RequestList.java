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
 * This class is for representing a request list,
 * i.e. a pointer to a (chain of) RequestNode(s),
 * that are to be executed, to be coupled to a partner, 
 * or to report success
 */

class RequestList {
    Object owner;
    String name;
    RequestNode first = null;
    RequestNode firstNonSuspended() {
    	for (RequestNode r=first; r!=null; r=r.nextReq)
    	{
    		if (!r.isSuspended())
    		{
    			return r;
    		}
    	}
    	return null;
    }
    CodeFragmentNode current; // used when trying code fragments
    RequestList(Object owner,String s) {this.owner=owner; name=s;}
    boolean isEmpty() {return first==null;}
    public String toString() {
        if (!Node.traceOK) return "";
        String result = name+" "+(owner instanceof Node? (""+((Node)owner).instanceNr): owner)+":";
        for (RequestNode prev=null,node=first; node!=null;
                         prev=node,node=node.nextReq) {
            if (prev==null
            ||  prev.priority != node.priority)
            {
                result+="["+node.priority+ "]";
            }
            result+=node.instanceNr+" ";
        }
	return result;
    }
}

