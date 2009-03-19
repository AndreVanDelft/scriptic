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
 * A communication partner frames
 */

public class CommPartnerFrame extends ScriptFrame implements Cloneable {
void trace() {trace("");}
void trace(String sp) {
  String s = sp+this+"  sharedFrames:\n";
  for(int i=0;i<_sharedFrames.length;i++) 
	s+=(i==0?" ":",")+_sharedFrames[i];
  Node.debugOutput(s);
}
	public CommPartnerFrame (Object owner, NodeTemplate template, ValueHolder[] indexValues) {
	    super (owner, template, indexValues);
	}

    /** the sharedFrames related to this partner frame.
     *  e.g. with communication defined for a,b=... a,c=... and d,a=...
     *  partner frame a gets sharedFrames [a_b, a_c, d_a].
     */
	CommFrame _sharedFrames[];
	//short   indexesInShared[];
	RequestList _requests = new RequestList(this,"Requests@"+template.getName());

    /** add a node to the request list.
     * if this were empty before, notify all sharedFrames with partnerActivated
     */
	final void addRequest (RequestNode node)
	{
	    boolean wasEmpty = _requests.isEmpty();
	    node.addToRequestList(_requests);
	    if (wasEmpty)
	    {
	        for (int i=0; i<_sharedFrames.length; i++) {
		       _sharedFrames[i].partnerActivated();
  	    	}
	    }
	}

    /** remove a node from the request list.
     * if this becomes empty afterwards,
     * notify all sharedFrames with partnerDeactivated
     */
	final int removeRequest (RequestNode node)
	{
	    node.removeFromRequestList();
	    if (_requests.isEmpty())
	    {
	        for (int i=0; i<_sharedFrames.length; i++) {
    		  _sharedFrames[i].partnerDeactivated();
    		}
	        return 0;
		}
	    return 1;
	}
}
