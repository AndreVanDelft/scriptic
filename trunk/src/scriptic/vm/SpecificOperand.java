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
 * This class serves storing requests belonging to operands of parallel operators
 */
abstract class SpecificOperand {
	String getName    () {return node==null? "????": node.getName();}
	StringBuffer info (int nestingLevel) {
	    int posToken = nestingLevel*2;
	    if (posToken >= Node.lengthCol0) posToken = Node.lengthCol0;
            StringBuffer result = new StringBuffer();
            String str = getName();
	    int i;
	    for (i=0; i<Node.lengthCol0; i++) {
	        if      (i <posToken)      result.append(i%4==2? ' ': ' ');
	        else if (i==posToken)      result.append(getName());
	        else if (i > posToken
                           + str.length()) result.append(' ');
	    }
	    result.append(node==null? -1: node.instanceNr)
                  .append(": ")     .append(requestsString())
                  .append("       ").append(getName())
                  .append(" next:") .append(next!=null?""+next.node.instanceNr:"-")
                  .append(" prev:") .append(prev!=null?""+prev.node.instanceNr:"-")
                  .append('\n');
            return result;
        }
	StringBuffer infoNested () {return infoNested(0);}
	StringBuffer infoNested (int nestingLevel)
	{
            StringBuffer result = info(nestingLevel);
	    for (SpecificOperand c=firstChild; c!=null; c=c.next)
              result.append(c.infoNested(nestingLevel+1));
            return result;
	}	

        String requestsString() {
            String s="";
	    for (RequestNode r=firstReq; r!=null; r=r.nextInParOp) {
		s+=(r==firstReq?"":",")+r.instanceNr;
	    }return s;
        }

	long        passOfSuccess;
	long        ticks;
	long        stamp; // for determining whether requests can be partners
	RequestNode firstReq;
	Node        node;

	SpecificOperand firstChild = null, parent = null, next = null, prev = null;

	SpecificOperand (Node node) {
	    this.node   = node;
	    if  (null  != node.parent) // else rootOfOperands
            {
                 parent = node.parent.parOpAncestor;
	         if  (parent.firstChild != null) 
		      parent.firstChild.prev = (prev=parent.firstChild.prev).next = this;
	         else parent.firstChild = prev = this;
            }
	}

	void remove() {
if (node  ==null)Node.debugOutput("REMOVE-ERROR: node null@"+info(0));
if (parent==null)Node.debugOutput("REMOVE-ERROR: parent null@"+info(0));
if (false) Node.traceOutput(""+parent.infoNested(0)+"REMOVE: "+info(0));
	    if (next!=null)                           next.prev = prev;
	    else                         parent.firstChild.prev = prev;
	    if (this==parent.firstChild) parent.firstChild      = next;
	    else                                      prev.next = next;
if (false) Node.traceOutput(parent.infoNested(0)+"DONE");
	    node   = null;
	    parent = null;
	}

	/** 
	 * all subthreads and (then) all requests in this and (grand)childs 
	 * are to be descheduled and deactivated,
	 * probably because an exclusive code fragment succeeded
	 */
	final void excludeRequests () {
	    RequestNode theNext;
	    //for (ThreadNode t=firstThread; t!=null; t=t.nextThread) {
		//t.stop();
	    //}
	    for (RequestNode r=firstReq; r!=null; r=theNext) {
	        theNext = r.nextInParOp;
		r.exclude();
	    }
	    for (SpecificOperand s=firstChild; s!=null; s=s.next) {
		s.excludeRequests();
	    }
	}
}
