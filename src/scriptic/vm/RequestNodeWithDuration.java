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

abstract class RequestNodeWithDuration extends RequestNode {

	RequestNodeWithDuration(NodeTemplate template) {this (template, null);}
	RequestNodeWithDuration(NodeTemplate template, Node parent) {
	    super (template, parent);
	}

/**
         * Add this to a given request list, which is sorted on priority and duration.
         */
	synchronized void addToDurationList(DurationList listOwner) {
            if (doTrace)traceOutput("RA>"+getName()+" "+instanceNr+"@"+listOwner);

  	    this.listOwner = listOwner;

	    for (prevReq=null,    nextReq=listOwner.first; nextReq!=null
                                                       && (nextReq.priority >priority
                                                         ||nextReq.priority==priority
                                                         &&nextReq.duration <duration);
	         prevReq=nextReq, nextReq=nextReq.nextReq) {
	    }
            if (nextReq != null) nextReq.prevReq = this;
	    if (prevReq != null) prevReq.nextReq = this;
            else                 listOwner.first = this;
	}

	public String toString() {
            if (!traceOK) return "";
            StringBuffer result = new StringBuffer(super.toString());
            if (duration != NO_DURATION) result.append(" duration:" + duration);
            return result.toString();
	}
}
