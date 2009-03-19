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
 * This class serves storing send-channel frames
 *
 * @version 	1.0, 11/30/95
 * @author	Andre van Delft
 */

public class SendChannelFrame extends CommFrame {
	public SendChannelFrame (Object owner, NodeTemplate template, ValueHolder[] indexValues, RootNode rootNode) {
	    super (owner, template, indexValues, rootNode);
	}
	void activateWhenAllPartnerActive() {
	    if (_activePartners==1) {
		_trialTree = new SendChannelNode(this, rootNode);
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


