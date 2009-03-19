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
 * Frames for communication scripts
 */

public class CommFrame extends ScriptFrame  {
	RootNode rootNode;
void trace() {trace("");}
void trace(String sp) {Node.traceOutput(sp+info());}

String info() {
  String result = "CommFrame:"+this+"  partners:";
  for(int i=0;i<_partners.length;i++) result += (i==0?" ":",")+_partners[i];
  return result;
}

	public CommFrame (Object owner, NodeTemplate template, ValueHolder[] indexValues, RootNode rootNode) {
	    super (owner, template, indexValues);
	    this.rootNode = rootNode;
	}

    /** the run-time tree from which code fragments can be tried out
     *  in order to bind to partners
     */
	CommNode         _trialTree;
	
    /** the partner frames */
	CommPartnerFrame _partners[];
	short            _activePartners;

    /** a partner got activated. Activate the trial tree when applicable */
	void partnerActivated() {
	    _activePartners++;
	    activateWhenAllPartnerActive();
	}

    /**
     * in case the number of active partners equals the number of partner frames,
     * activate the script body.
     * To be called when the number of active partners may just have become 2
     */
	void activateWhenAllPartnerActive() {
	    if (_activePartners==_partners.length) {
		_trialTree = new CommNode(this, rootNode);
	        _trialTree.activate();
            }
            /* the following had been deactivated because of errors:
              // allow binding to parameters in case of reporting success in activation
              // not tested out well, yet...
              if (trialTree!=null && trialTree.hadRecentSuccess()) {
                if (trialTree.findAndTryPartnersFor (trialTree)) {
                    trialTree.succeed();
                }
              }
            */
	}

    /**
     * in case the number of active partners equals the number of partner frames - 1,
     * and if there is a trialTree, deactivate it.
     * To be called when the number of active partners just diminished
     */
	void partnerDeactivated() {

	    _activePartners--;
	    if (_trialTree != null // could be null because communication just started
	    && _activePartners==_partners.length-1) {
	    	// trialTree.deactivate(false); is wrong; the entire tree should go
            _trialTree.parOpAncestor.excludeRequests();
		    _trialTree = null;
	    }
	}
}

