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

import java.util.ArrayList;
import java.util.List;

/**
 * This class is for the root node in the runtime tree.
        Last change:  AVD  24 Feb 96    1:43 am
 */
final class RootNode extends Node
                     implements CallerNodeInterface
                                    {
/*
        public StringBuffer infoNested(int level) {
            StringBuffer result = super.infoNested(level);
            result.append(RootNode.rootOfOperands.infoNested());
            result.append('\n');
            if (!             unboundCFs.isEmpty()) result.append(             unboundCFs).append('\n');
            if (!            unboundCFDs.isEmpty()) result.append(            unboundCFDs).append('\n');
            if (!  requestsToBeScheduled.isEmpty()) result.append(  requestsToBeScheduled).append('\n');
            if (!requestsToBeDescheduled.isEmpty()) result.append(requestsToBeDescheduled).append('\n');
            if (!              unsureCFs.isEmpty()) result.append(              unsureCFs).append('\n');
            if (!        newSubRootNodes.isEmpty()) result.append(        newSubRootNodes).append('\n');
            if (!       busySubRootNodes.isEmpty()) result.append(       busySubRootNodes).append('\n');
            for (SubRootNode s=(SubRootNode)busySubRootNodes.first; s!=null; 
                             s=(SubRootNode)s.nextReq)
                   result.append(s.infoNested());
            return result;
        }
*/
        StringBuffer infoNested (int nestingLevel)
        {   if (!traceOK) return new StringBuffer();
            StringBuffer result = super.infoNested(nestingLevel);
            result.append("Deactivate:");
            for (Node n: nodesToBeDeactivated) {
                result.append(n.instanceNr)
                      .append(" ");
            }
            result.append('\n');
            if (!    pendingCFs.isEmpty()) result.append(    pendingCFs).append("\n");
            if (!   pendingCFDs.isEmpty()) result.append(   pendingCFDs).append("\n");
            if (!       busyCFs.isEmpty()) result.append(       busyCFs).append("\n");
            if (!      busyCFDs.isEmpty()) result.append(      busyCFDs).append("\n");
            if (!     successfulCFs.isEmpty()) result.append(     successfulCFs).append("\n");
            if (!successfulCFDs.isEmpty()) result.append(successfulCFDs).append("\n");
            if (! ehcfSuccesses.isEmpty()) result.append( ehcfSuccesses).append("\n");

            result.append("Priority  :   ").append(priority).append("\n")
                  .append("Duration  :   ").append(duration).append("\n");
            return result;
        }
        
        FrameLookup frameLookup = new FrameLookup(this);

        double duration;
    	public final double  getDuration()        {return     duration  ;}
	    public final void    setDuration(double d){           duration=d;}
        int waitingForMutex;
        synchronized void doNotify() {notify();}
        synchronized void doWait  () {
          try {wait();} catch (InterruptedException e) {
              trace("doWait InterruptedException "+e+"\nat ");
          }
        }
        public synchronized void enterMutex(Node who) {
            if (waitingForMutex++ > 0) {
                 if (doTrace) {
                     traceOutput("  WAIT START at: "+getName()+" by: "+who.getName()+" waiting:"+waitingForMutex);
                 }
                 try {
                     wait();
                 } catch (InterruptedException e) {
                     trace("mutex.enter InterruptedException "+e+"\nby "+who+" at ");
                 }
                 if (doTrace) {
                     traceOutput("  WAIT END   at: "+getName()+" by: "+who.getName()+" waiting:"+waitingForMutex);
                 }
            }
            else if (doTrace) {
                     traceOutput("     NO WAIT at: "+getName()+" by: "+who.getName());
                 }
        }
        public synchronized void exitMutex(Node who) {
            if (--waitingForMutex>0) {
                 if (doTrace) {
                     traceOutput("NOTIFY START at: "+getName()+" by: "+who.getName()+" waiting:"+waitingForMutex);
                 }
                 notify();
            }
            else if (doTrace) {
                     traceOutput("   NO NOTIFY at: "+getName()+" by: "+who.getName());
                 }
        }

	/**
	 * Answer wether the actual parameter at the given 
	 * index is of forcing type ('!' or: '?!' and ... see language definition)
	 */
	public boolean isActualParameterForcing(int index) {return false;}
	/**
	 * Answer wether the actual parameter at the given 
	 * index is of out type ('?' or '?!' and ... see language definition)
	 */
	public boolean isActualParameterOut(int index) {return false;}

        boolean disallowedToRun = false;

        RootNode () {
            super(NodeTemplate.getRootTemplate());
            parOpAncestor = new RootOfOperands(this);
            rootNode = this;
        }

        /** 
         * add a child node: 
         * set the parent variable; adjust the active count.
         * insert the child into the semi-closed double linked list of siblings
         * ensure next==null
         */
        void addChild (Node child)
        {
            super.addChild (child);
            child.parOpAncestor  = new RootOperand(child);
            child.subRootNode = child;
        }

        DurationList pendingCFDs           = new DurationList(this, "pendingCFDs");
        DurationList busyCFDs              = new DurationList(this, "busyCFDs"); // {*duration=...:...*}
        DurationList successfulCFDs        = new DurationList(this, "successfulCFDs");
        RequestList  pendingCFs            = new RequestList (this, "pendingCFs");
        RequestList  busyCFs               = new RequestList (this, "busyCFs"); // {*...*}
        RequestList  successfulCFs             = new RequestList (this, "successes");
        RequestList  ehcfSuccesses         = new RequestList (this, "ehcfSuccesses");
        RequestList  succeededRequests     = new RequestList (this, "succeededRequests");
        ArrayList<Node> nodesToBeDeactivated  = new ArrayList<Node>(); // to be emptied completely every time

        /** mark node for deactivation, so that it will be disposed shortly */
        final void markForDeactivation(Node node) {
if (doTrace) node.trace("markForDeactivation: ");
            nodesToBeDeactivated.add(node);}

        final void handleNodesToBeDeactivated() {
            while (!nodesToBeDeactivated.isEmpty()) {
            	ArrayList<Node> currentNodesToBeDeactivated = nodesToBeDeactivated;
                nodesToBeDeactivated = new ArrayList<Node>();
                for (Node n: currentNodesToBeDeactivated) {
                    n.deactivate(true);
                }
            }
        }

        void addToEHCFSuccesses(EventHandlingCodeFragmentNode r) {
            synchronized(ehcfSuccesses) {
                r.addToRequestList(ehcfSuccesses);
            }
        }

        /** handle successful EventHandlingCodeFragmentNodes
        /* NOT for (r = ehcfSuccesses.first; r != null; r = r.nextReq) ...
        /* within a synchronized(ehcfSuccesses) section,
        /* because that would cause a deadlock, if this would call addToEHCFSuccesses()
         */
        void handleEHCFSuccesses() {
            RequestNode r = null; //otherwise error: variable might not have been initialized
            for (;;)
            {
                synchronized(ehcfSuccesses) {
                    r=ehcfSuccesses.first;
                }
                if (r == null) {
                    break;
                }
                r.removeFromRequestList();
 
                if (((EventHandlingCodeFragmentNode)r).hasBeenExcluded)
                {
                  r.deactivate(true);
                } else {
                  // the next lines also appear in class EventHandlingCodeFragmentNode;
                  // it closely resembles as well the section 'handle successes' below:
                  if (!r.isIteration||r.pass==0)
                  {   // otherwise atomicActionHappens has already been called...
                      r.atomicActionHappens();
                  }
                  // NOT scheduleSuccess(); because then priorities would mess things up a bit.
                  // instead, do now like subRootNodes handling scheduled Successes:
                  if (doTrace) r.trace("s>");
                  r.parOpAncestor.firstReq = null;  //faster than removeFromParOpList()
                  r.succeed();
                  r.addToRequestList(succeededRequests);
                }
            }
        }

        void handleSucceededRequests() {
            RequestNode r = null; //otherwise error: variable might not have been initialized
            for (;;)
            {
                synchronized(succeededRequests) {
                    r=succeededRequests.first;
                }
                if (r == null) {
                    break;
                }
                r.removeFromRequestList();
                r.hadSucceeded();
                r.deactivate(true);
            }
        }

        void tick (int thePriority, double theDuration)
        {
            duration -= theDuration;
            tick(thePriority, theDuration, busyCFDs);
            tick(thePriority, theDuration, pendingCFDs);
            tick(thePriority, theDuration, successfulCFDs);
        }
        void tick (int thePriority, double theDuration, DurationList durationList)
        {
        	List<CodeFragmentNode> suspendedRequests = new ArrayList<CodeFragmentNode>();
        	CodeFragmentNode next; // needed because suspended requests will be taken out
            for (CodeFragmentNode r=(CodeFragmentNode)durationList.first; r!=null; r=next) 
            {
            	next = (CodeFragmentNode)r.nextReq; 
                if (r.priority < thePriority) break;
                if (r.isSuspended()) {
                	r.removeFromRequestList(); //temporary removal to be followed by addition,
                	// so that correct ordering by priority and duration will be restored
                	suspendedRequests.add(r);
                }
                else
                {
                  r.duration -= theDuration;
                }
            }
            for (CodeFragmentNode r: suspendedRequests)
            {
            	// add in correct duration order
            	r.addToDurationList(durationList);
            }
        }

        public void traceIt() {debugOutput(infoNested().toString());}

        StringBuffer infoNested() {
           return super.infoNested().append(rootOfOperands.infoNested());
        }

//      static Node rootScriptCall() {
//          return rootNode.firstChild.next; // firstChild is allways commRootNode
//      }


        /** code fragments in communication that still may be unbound */
        RequestList unboundCFs  = new RequestList(this, "unboundCFs");

        /** code fragments in communication with duration that still may be unbound */
        DurationList unboundCFDs  = new DurationList(this, "unboundCFDs");

        /** unsure code fragments in communication that still may be unbound */
        RequestList  unsureCFs  = new RequestList(this, "unsureCFs");

        /** if for a real script main(args), then System.exit when done */
        boolean isForMainScript;

        /** whether the main loop is not to be called by startScript */
        boolean isWithoutMainLoop;

        /** whether time has not been put forward recently due to unsure code fragments */
        boolean hasWaitedForUnsureCFs;

        int activeEventHandlingCodeFragmentNodes;

        void stopActivity() {
            while (next != null) next.parOpAncestor.excludeRequests();
            parOpAncestor.excludeRequests();
        }

        /**
         * Called from another script, in a given frame and without parameters
         */
        final public void startScript (Object owner, NodeTemplate t) {
        	startScript ( owner, t, null);}
        /**
         * Called from another script, in a given frame and with given data.
         * It also may be called through FromJava.main;
         * the commRootNode should start running through FromJava.mainloop...
         */
        // OLD: public final void startScript (ScriptFrame frame, ScriptParams params) {
        public final void startScript (Object owner, NodeTemplate t, Object params[]) {
            if (doTrace) trace("CommRootNode.startScript: "
                             + (owner==null? "  owner null": "  owner: " + owner));
            t.activateFrom (this, owner, params);
            if (!isWithoutMainLoop) {
                FromJava.mainLoop(this);
                if (isForMainScript) System.exit(hadRecentSuccess()? 0: 1);
            }
        }

        final public void run() {
            enterMutex(this);   // for future subthreads
                     //AND for the other SubRootNode that registers as a busy subRootNode
            if (doTrace) trace("running: ");
            // the mainLoop will enter&exit this' critical section all the time
            FromJava.mainLoop(this);
            exitMutex(this);  
            if (doTrace) trace("stopping: ");
            if (isForMainScript) System.exit(hadRecentSuccess()? 0: 1);
        }

void t(int i) {System.out.print(""+i+" ");System.out.flush();}

        /**
         * try to do something at own requests and allow other subrootnodes the same
         * and return whether anything remains to be done:
         *
         * enter the critical section
         * if there is nothing more to do, exit
         * determine the priority to run this round
         *   take the highest of the other thread's priorities
         *   then try the private fragments with at least the same priority;
         *   if such a priority is higher, then adjust the round's priority
         * allow child threads for a short while...
         * determine the time to elaps
         * FromJava.tick(this);
         * let time elapse for all other threads
         * let time elapse for own requests
         * let successes with timeToGo=0 succeed
         * allow child threads for a short while...
         * exit the critical section
         * return true: there is more work to do
         *
         * outside the critical section, the other threads get the chance to try code fragments
         * and to let successes succeed.
         */
        boolean hasActivity()
        {
            CodeFragmentNode f, g;
            RequestNode      r, nextR;
            //SubRootNode      s, nextS;

            enterMutex(this);

            if (FromJava.doCallbackMainloop()) {
                FromJava.debugger.callbackMainloop (this);
            }

            if (doTrace) {
              traceOutput("********************************************");
              traceOutput(infoNested());
              traceOutput("++++++++++++++++++++++++++++++++++++++++++++");
            }

            handleEHCFSuccesses();  // this may add successes

            // determine the priority for this round
            priority = MIN_PRIORITY;
            r =       busyCFs .first; if (r!=null && priority<r.priority) priority=r.priority;
            r =       busyCFDs.first; if (r!=null && priority<r.priority) priority=r.priority;
            r =      successfulCFs.first; if (r!=null && priority<r.priority) priority=r.priority;
            r = successfulCFDs.first; if (r!=null && priority<r.priority) priority=r.priority;
            r =    pendingCFs .first; if (r!=null && priority<r.priority) priority=r.priority;
            r =    pendingCFDs.first; if (r!=null && priority<r.priority) priority=r.priority;

            // the round priority may actually change when trying out fragments
            // in case of A & .. & B etc.
            // Maybe the language definition will tell to ignore such rare cases

            /* 960513: PROBLEMS WITH THE TRYING-OUT THE NEXT CODE FRAGMENT:
             * When try-out succeeds, what's the next request to take?
             * Taking nextReq on beforehand is dangerous; it may be
             * excluded by the succeeding code fragment.
             * The request lists now have a 'current' member; this
             * is updated in the tryOut function
             */

            // try the unsure and unbound fragments with at least the same priority;
            // if such a priority is higher, then adjust the round's priority
            // beware: this is tricky code. It merges the three request lists!
            unboundCFs.current = (CodeFragmentNode) unboundCFs.first;
           unboundCFDs.current = (CodeFragmentNode)unboundCFDs.first;
             unsureCFs.current = (CodeFragmentNode)  unsureCFs.first;

            for(;;) {

                boolean tookUnboundCF  = false;
                boolean tookUnboundCFD = false;
                f =  unsureCFs .current;
                g = unboundCFs .current;if(g!=null&&(f==null||g.priority>f.priority)) {f=g;tookUnboundCF =true;}
                g = unboundCFDs.current;if(g!=null&&(f==null||g.priority>f.priority)) {f=g;tookUnboundCFD=true;}
                if (f==null||f.priority < priority) break;
                if (doTrace) f.trace("UT>");

                if (!f.isSuspended() 
                &&   f.tryOut()==Boolean.TRUE)
                {
                    if (doTrace) f.trace("UT OK>");
                    success  = Boolean.TRUE;
                    priority = f.priority;
                    if (doTrace) trace("round priority now: "+priority+"  ");
                }
                else {
                    if (doTrace && !f.isSuspended()) f.trace("UT FAIL>");
                    //NOTE: both tookUnboundCF and tookUnboundCFD may be true!!!
                         if (tookUnboundCFD)unboundCFDs.current = (CodeFragmentNode) f.nextReq;
                    else if (tookUnboundCF ) unboundCFs.current = (CodeFragmentNode) f.nextReq;
                    else                      unsureCFs.current = (CodeFragmentNode) f.nextReq;
                }
            }

            // 'try' the pending fragments with at least the same priority;
            pendingCFs.current = (CodeFragmentNode) pendingCFs.first;
            for(;;) {

                f = pendingCFs.current;
                while (f!=null && f.isSuspended()) {
                	f = (CodeFragmentNode) f.nextReq;
                }
                if (f==null || f.priority < priority) break;
                if (doTrace) f.trace("T>");
                if (f.tryOut()==Boolean.TRUE) {
                	success = Boolean.TRUE; 
                	if (doTrace)f.trace("T OK>");
                    if (doTrace&&priority!=f.priority) trace("round priority now: "+f.priority+"  ");
                    priority = f.priority; // should have no effect...
                }
                else            {f.trace("ERROR: UNEXPECTED T FAIL>");}
            }
            pendingCFDs.current = (CodeFragmentNode) pendingCFDs.first;
            for(;;) {
                f = pendingCFDs.current;
                while (f!=null && f.isSuspended()) {
                	f = (CodeFragmentNode) f.nextReq;
                }
                if (f==null || f.priority < priority) break;
                if (doTrace) f.trace("T>");
                if (f.tryOut()==Boolean.TRUE) {
                    success = Boolean.TRUE;
                    if (doTrace)f.trace("T OK>");
                    if (doTrace&&priority!=f.priority) trace("round priority now: "+f.priority+"  ");
                    priority = f.priority; // should have no effect...
                }
                else            {
                   f.trace("ERROR: UNEXPECTED T FAIL>");
                   (new Exception()).printStackTrace();
                }
            }

            // allow child threads for a short while
            // they can only be started if communication is already bound
            // otherwise (*...*) is ignored...
             exitMutex(this); 
            enterMutex(this);

             duration = NO_DURATION;

             synchronized(this) { // needed because of threaded code fragments

                if (priority > MIN_PRIORITY) {
    
                    /* determine the time to proceed
                     * by inspecting private busyCFs, successes, busySubRootNodes
                     * 2 strategies are possible IN OLD SITUATION WHEN THERE
                     * WERE NO SEPARATE LISTS FOR UNSURE AND UNBOUND CODE FRAGMENTS
                     * A: also include pendingCFs, then time may as well proceed
                     *    while there are pending code fragments; these get executed
                     *    somewhere after their start time, but before their end-time
                     * B: do not proceed time while there are pending code fragments
                     *    except for unsuccessful unbound unsure fragments
                     * Strategy A is hasty: time proceeds as fast as possible;
                     * will by useful when simulating on multiprocessor systems
                     * Strategy B is more careful; it is needed if communications
                     * are allowed to start with a code fragment with duration>0
                     * Namely, suppose time proceeds while the fragment remains pending;
                     * In strategy A, only later the fragment may get bound and only then a new
                     * unbound instance may be created, but this instance could have
                     * been needed earlier. Example:
                     *   C<<>>(int i) = {duration=1}
                     *   C<<(1)&C<<(2)&C>>(1)&C>>(2)
                     * Here the 1's will communicate from 0 to 1 and the 2's from 1 to 2,
                     * with strategy A.
                     *
                     * Things improved a bit with unbound and unsure code fragments
                     * in their own list...the unsure ones can be discarded here;
                     * the unbound ones are discarded as well...
                     * Things improved a bit with unbound code fragments in their own list...
                     */
        
                    // FromJava.tickDependsOnUnboundCodeFragments enables strategy A
                    // then ... UNBOUND FRAGMENTS DO NO HARM ...

                    determineDuration: {
                      r =     pendingCFs.firstNonSuspended(); if (r!=null &&  priority == r.priority) {duration=NO_DURATION; break determineDuration;}
                      r =        busyCFs.firstNonSuspended(); if (r!=null &&  priority == r.priority) {duration=NO_DURATION; break determineDuration;}
                      r =  successfulCFs.firstNonSuspended(); if (r!=null &&  priority == r.priority) {duration=NO_DURATION; break determineDuration;}
                      r =    unboundCFDs.firstNonSuspended(); if (r!=null &&  priority == r.priority) {duration=r.duration;}
 
                      r =    pendingCFDs.firstNonSuspended(); if (r!=null &&  priority == r.priority
                                                                          &&  duration  > r.duration) duration=r.duration;
                      r =       busyCFDs.firstNonSuspended(); if (r!=null &&  priority == r.priority
                                                                          &&  duration  > r.duration) duration=r.duration;
                      r = successfulCFDs.firstNonSuspended(); if (r!=null &&  priority == r.priority
                                                                          &&  duration  > r.duration) duration=r.duration;

                      // unsure code fragments get intermittendly a chance to set the time to elapse to 0
                      if (hasWaitedForUnsureCFs) {
                          hasWaitedForUnsureCFs = false;
                      } else {
                        if (duration!=NO_DURATION) {
                           r =  unsureCFs.first;
                           if (r!=null &&  priority == r.priority) {duration=NO_DURATION;hasWaitedForUnsureCFs=true;}
                        }
                      }
                    }
                    FromJava.shouldWaitForUserInput &= successfulCFs       .first == null
                                                    && successfulCFDs  .first == null
                                                    && unboundCFs      .first == null
                                                    && unboundCFDs     .first == null
                                                    &&  unsureCFs      .first == null
                                                    &&  busyCFDs       .first == null
                                                    &&  busyCFs        .first == null
                                                    && pendingCFs      .first == null
                                                    && pendingCFDs     .first == null;
    
    
                }

                // time ticks away under two more conditions:
                if (duration == NO_DURATION
                ||  FromJava.ticksSuspended) {duration = 0.0;
                                              if (doTrace) trace("tick; priority: "+priority+"  ");}
                else                          if (doTrace) trace("tick; priority: "+priority
                                                                +     " duration: "+duration+"  ");
    
                // duration is now the time to elapse, though FromJava may adjust it...
                FromJava.tick(this);
    
                // let time elapse for pending and busy code fragments, and for successes
                tick(priority, duration);
    
                // handle successes
                for (r=successfulCFs.first; r != null; r = nextR) {
                    nextR = r.nextReq;
                    if (r.priority < priority) 
                        break;
                    if (r.isSuspended())
                    {
                    	continue;
                    }
                    if (doTrace) r.trace("s>");
                    r.removeFromRequestList(); 
                    r.parOpAncestor.firstReq = null;  // faster than removeFromParOpList()
                    r.succeed();
                    r.addToRequestList(succeededRequests);
                }
                // let duration successes succeed if their time has come
                for (r=successfulCFDs.first; r != null; r = nextR) {
                    nextR = r.nextReq;
                    if (r.priority < priority
                    ||  r.duration > 0) 
                        break;
                    if (r.isSuspended())
                    {
                    	continue;
                    }
                    if (doTrace) r.trace("s>");
                    r.removeFromRequestList(); 
                    r.parOpAncestor.firstReq = null;  // faster than removeFromParOpList()
                    r.succeed();
                    r.addToRequestList(succeededRequests);
                }
                handleSucceededRequests();
                handleNodesToBeDeactivated();

                pass++;

           }  // synchronized(this)

            //trace("exitMutexes by: ");
            exitMutex(this);

            // return whether there is still work to do
            return activeEventHandlingCodeFragmentNodes > 0
                ||            pendingCFs.first != null
                ||           pendingCFDs.first != null
                ||            unboundCFs.first != null
                ||           unboundCFDs.first != null
                ||             unsureCFs.first != null
                ||               busyCFs.first != null
                ||              busyCFDs.first != null
                ||        successfulCFDs.first != null
                ||         successfulCFs.first != null;
        }
}