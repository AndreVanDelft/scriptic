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
 * Frames for communication channels
 */

public class ChannelFrame extends CommFrame {
    /**
     * answer a new frame with the given owner and template
     */
	public ChannelFrame (Object owner, NodeTemplate template, ValueHolder[] indexValues, RootNode rootNode) {
	    super (owner,template, indexValues, rootNode);
	}

    /**
     * in case the number of active partners equals 2, activate the script body.
     * To be called when the number of active partners may just have become 2
     */
	void activateWhenAllPartnerActive() {
	    if (_activePartners==2) {
		_trialTree = new ChannelNode(this, rootNode);
_trialTree.trace("A> (activateWhenAllPartnerActive) ");
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
}



