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
 * This class serves representing event handling code fragment nodes in the runtime tree.
 */
public final class EventHandlingCodeFragmentNode extends CodeFragmentNode {
public String toString() {return super.toString()+((isIteration&&!mustBreakFromLoop)? " iter":"");}
        EventHandlingCodeFragmentNode (NodeTemplate template, Node parent,
                                       int typeCode) {
            super(template, parent);
            isIteration = typeCode != EventHandlingCodeFragmentCode;
            hasOptionalExit = isIteration && typeCode != EventHandlingManyCodeFragmentCode;
            if (isIteration) pass = 0;
        }

        boolean isIteration, hasOptionalExit, hasBeenExcluded, mustBreakFromLoop;

        /**
         * Cancel the iteration, if any.
         */
        public final void breakIteration() {mustBreakFromLoop=true;}

        public final Boolean tryOut() {
        	if (isSuspended())
        	{
        		return null;
        	}
            if (subRootNode instanceof CommNode) {
                CommNode commNode = (CommNode) subRootNode;
                if (commNode.partners == null) {
                    trace("tryOut.findAndTryPartnersFor on: "+commNode+" ");
                    rootNode.enterMutex(this);  // obtain mutex lock
                    Boolean result = commNode.findAndTryPartnersFor (this);
                    trace("tryOut.findAndTryPartnersFor: "+result+" ");
                    rootNode.exitMutex(this);  // release mutex lock
                    return result;
                }
            } 
            return tryOutInBoundMode(/*wasUnbound=*/ false);
        }

        /** 
         * set the boolean hasBeenExcluded before proceeding, because
         * otherwise this node may still succeed
         */
        synchronized void exclude()
        {
            hasBeenExcluded = true;
            super.exclude();
        }

        synchronized final public Boolean tryOutInBoundMode(boolean wasUnbound) {
trace("tryOutInBoundMode: ");
            doCode();
            if (success==Boolean.TRUE) {

                if (isIteration && !mustBreakFromLoop) {
                    if (wasUnbound
                    && pass==0) // this test is not really needed
                                    // since wasUnbound would otherwise be false...
                    {   // subRootNode's lock already in possession!
                        atomicActionHappens(); // will bind to the partners...
                    }
                    if (hasOptionalExit) {
                        if (!parent.isWithOrLikeOptr()  )
                    	{
                           	succeed();
                    	}
                    }
                    pass++;
                 
                } else {

                    //synchronized(anchor) 
                    {
                      removeFromRequestList(); 
                      RequestList eventHandlerList = FromJava.mapAnchor2RequestList.get(anchor);
                      if (eventHandlerList==null||eventHandlerList.isEmpty())
                      {
                    	  FromJava.mapAnchor2RequestList.remove(anchor);
                      }
                    }
                    rootNode.activeEventHandlingCodeFragmentNodes--;

 //Note: needs more synchronization...

                    if (wasUnbound) {// subRootNode's lock already in possession!
                        if (!isIteration||mustBreakFromLoop||pass==0)
                        {   // otherwise atomicActionHappens has already been called...
                            atomicActionHappens();
                        }
                        // NOT scheduleSuccess(); because then priorities would mess things up a bit.
                        // instead, do now like subRootNode's handling scheduled Successes:
                        trace("s>");
                        parOpAncestor.firstReq = null;  // faster than removeFromParOpList()
                        succeed();
                        addToRequestList(rootNode.succeededRequests);
                    } else {
                        // subRootNode's lock not in possession, so add to ehcfSuccesses
                        // so that atomicActionHappens and success handling will be done later
                        rootNode.addToEHCFSuccesses(this);
                    }
                    FromJava.doNotify(); // in case it was waiting for input
                }
            }
            else if (success==Boolean.FALSE) {
                  //synchronized(anchor) 
                  {
                    removeFromRequestList(); 
                  }
                  rootNode.activeEventHandlingCodeFragmentNodes--;
                  FromJava.doNotify(); // in case it was waiting for input
            }

            return success;
        }
        final void scheduleRequest() {
            //synchronized(anchor) 
            {
              rootNode.activeEventHandlingCodeFragmentNodes++;
            }
            RequestList requestList = FromJava.mapAnchor2RequestList.get(anchor);
            if (requestList==null)
            {
            	requestList = new RequestList(anchor, "Requests@"+anchor);
            	FromJava.mapAnchor2RequestList.put(anchor, requestList);
            }
            addToRequestList(requestList);
        }
        final void deschedule() {
            //synchronized(anchor)
            {
//System.out.println("deschedule@"+this+": "+anchor);
              RequestList oldListOwner = listOwner;
if (listOwner==null||listOwner.first==null){
String s="Error in deschedule@"+this+": "+listOwner+"\n\n"+rootNode.infoNested();
if (doTrace)debugOutput(s);
else        System.out.println(s);
(new Exception()).printStackTrace();
rootNode.traceIt();
}
              removeFromRequestList();
              RequestList requestList = FromJava.mapAnchor2RequestList.get(anchor);
              if (requestList==null)
              {
            	  // may occur...error?
              }
              if (requestList.isEmpty())
              {
            	  FromJava.mapAnchor2RequestList.remove(anchor);
              }
            }
            rootNode.activeEventHandlingCodeFragmentNodes--;
        }
}



