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
 * Threaded code fragment node in the runtime tree.
 */
class ThreadedCodeFragmentNode extends CodeFragmentNode implements Runnable {
	ThreadedCodeFragmentNode (NodeTemplate template, Node parent) {
	                             super (template, parent);}

	public String toString() {return super.toString()+" myThread: "+myThread;}

	public Boolean tryOutInBoundMode(boolean wasUnbound) {
	    // note: hasSuccess must come first. Then the listOwner.current
	    // may be set to the nextReq for the subRootNode.hasActivity loop;
	    // only then deschedule is possible
	    hasSuccess();
	    listOwner.current = (CodeFragmentNode) nextReq;
	    deschedule(); 
            //subRootNode must know priority!
            //So must be present in requests or successes or busy nodes
            //presence in successes dangerous; not yet to succeed();
            //  only when busy became false:
	    addToRequestList (rootNode.busyCFs);
	    start(); // thread with doCode(); scheduleSuccess(); release lock...
	    return Boolean.TRUE;
	}
	
	/** 
	 * this is to be stopped, descheduled and deactivated,
	 * probably because an exclusing code fragment succeeded
	 */
	void exclude()
	{
	    stop();
	    super.exclude();
	}

	ScripticThread myThread = null;
	public void init () {}
	public void start() {
	    if (myThread == null) {
	        myThread = new ScripticThread(this, "Script thread");
                myThread.setPriority(Thread.NORM_PRIORITY);
	        myThread.start();
	    }
	}
	public void stop() {
            if (myThread!=null
            &&  myThread.isAlive())
            {
	          //myThread.stop();
	          myThread.setStop(true);
	          myThread.waitStop(60);
                synchronized(rootNode) {
                  if (myThread != null) {
                      myThread  = null;
                      deschedule();
                      rootNode.markForDeactivation(this);
                  }
                }
	        //subRootNode().exitMutex(this); ...
            }
	}
	public void run() {
	    doCode();
	    FromJava.threadedCodeExecutionHasEnded();
	    //rootNode.enterMutex(this); 
            if (myThread != null
            &&  !myThread.mustStop1()) {
                 myThread.setStopped(true);
    	        myThread  = null;
                synchronized(rootNode) {
                  deschedule();
                  scheduleSuccess();
                }
            }
            FromJava.shouldWaitForUserInput = false;
            FromJava.doNotify(); // in case it had been waitingForUserInput...
	    //rootNode.exitMutex(this); 
	}
}
