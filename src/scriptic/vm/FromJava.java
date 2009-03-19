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
import java.util.HashMap;

/**
 * This class is the interface for event handling from Java
 */
public class FromJava {

public static void main(String args[]) {}

     public static int CALLBACK_MAINLOOP     = 0x0001;
     public static int CALLBACK_ACTIVATE     = 0x0002;
     public static int CALLBACK_DEACTIVATE   = 0x0004;
     public static int CALLBACK_HAS_SUCCESS  = 0x0008;
     public static int CALLBACK_EXECUTE_CODE = 0x0010;
     public static int CALLBACK_SUCCEED      = 0x0020;

    /** hook for a debugger for registration:
     *  call this function if a debugger should be called at
     *  typical moments of execution; use bit fields in the
     *  integer parameter to state when (0=never):
     *  CALLBACK_MAINLOOP     - each pass in the main loop
     *  CALLBACK_ACTIVATE     - activation
     *  CALLBACK_DEACTIVATE   - deactivation
     *  CALLBACK_HAS_SUCCESS  - a code fragment has success
     *  CALLBACK_EXECUTE_CODE - a code fragment gets executed
     *  CALLBACK_SUCCEED      - a code fragment succeeded
     *
     */
    public void registerDebugger (DebuggerInterface aDebugger, int aDebuggerBits) {
        debugger     = aDebugger;
        debuggerBits = aDebuggerBits;
    }

    static DebuggerInterface debugger;
    static int               debuggerBits;
    static boolean doCallbackMainloop   () {return (debuggerBits & CALLBACK_MAINLOOP    ) != 0;}
    static boolean doCallbackActivate   () {return (debuggerBits & CALLBACK_ACTIVATE    ) != 0;}
    static boolean doCallbackDeactivate () {return (debuggerBits & CALLBACK_DEACTIVATE  ) != 0;}
    static boolean doCallbackHasSuccess () {return (debuggerBits & CALLBACK_HAS_SUCCESS ) != 0;}
    static boolean doCallbackExecuteCode() {return (debuggerBits & CALLBACK_EXECUTE_CODE) != 0;}
    static boolean doCallbackSucceed    () {return (debuggerBits & CALLBACK_SUCCEED     ) != 0;}

        public final static double  NO_DURATION = AttributesInterface.NO_DURATION;

               static Object  theSynchronizer = new Object();
        public static double  currentElapsedTime;
               static boolean halted, ticksSuspended;
        public static boolean stopped;
               static boolean shouldWaitForUserInput, waitingForUserInput;
               static ArrayList<TickListenerInterface>  tickListeners = new ArrayList<TickListenerInterface>();
        // public static int     tickDependsOnUnboundCodeFragments = 1|  4|8|16;

        private static boolean codeHasBeenExecutedThisRound;
        private static boolean backgroundCodeExecutionHasEndedThisRound;
        public static void codeIsExecuted() {codeHasBeenExecutedThisRound=true;}
        public static void threadedCodeExecutionHasEnded() {backgroundCodeExecutionHasEndedThisRound=true;}
    	public static void    asyncCodeExecutionHasEnded() {backgroundCodeExecutionHasEndedThisRound=true;}
        
        public static void    addTickListener (TickListenerInterface ts) {
            tickListeners.add (ts);
        }
        public static void removeTickListener (TickListenerInterface ts) {
            tickListeners.remove (ts);
        }
        public static void     suspendTicks () {ticksSuspended =  true;}
        public static void      resumeTicks () {ticksSuspended = false;}
        public boolean isHalted             () {return halted;}
        public boolean isStopped            () {return stopped;}
        public boolean areTicksSuspended    () {return ticksSuspended;}
        public boolean isWaitingForUserInput() {return waitingForUserInput;}

        public static void tick (AttributesInterface a) {
            for (TickListenerInterface ts: tickListeners) {
                ts.tickInit(a);
            }
            currentElapsedTime += a.getDuration();
            for (TickListenerInterface ts: tickListeners) {
                ts.tick(a);
            }
            if (Node.doTrace)Node.traceOutput("tick("+a.getDuration()+"):"+currentElapsedTime);
        }
        public static double  elapsedTime ()         {return currentElapsedTime;}
        public static double  elapsedTime (double t) {return currentElapsedTime=t;}

        public static void doNotify() {
          if (waitingForUserInput) {
            synchronized(theSynchronizer) {
              if (Node.doTrace)System.out.println("doNotify@"+java.lang.Thread.currentThread());
              theSynchronizer.notify();
            }
          }
        }
        public static void doWait() {
            synchronized(theSynchronizer) {
              waitingForUserInput = true;
              // if (Node.doTrace)debugOutput("wait start@"+java.lang.Thread.currentThread());
              try {
                theSynchronizer.wait();
              } catch(InterruptedException e) {}
              waitingForUserInput = false;
              // if (Node.doTrace)debugOutput("wait   end@"+java.lang.Thread.currentThread());
            }
        }
	public static void stop(RootNode rootNode) {stopped=true; rootNode.stopActivity();}
	
	
        public static void mainLoop(RootNode rootNode) {
            while(true) {
                if (halted) {stop(rootNode); break;}
                if (stopped) doWait();

                if (!rootNode.hasActivity()
                &&  !backgroundCodeExecutionHasEndedThisRound)
                {
                        break;
                }
                if (!codeHasBeenExecutedThisRound
                &&  !backgroundCodeExecutionHasEndedThisRound) {
                	doWait();
                }
                else
                {
                	codeHasBeenExecutedThisRound = false;
                	backgroundCodeExecutionHasEndedThisRound = false;
                }
            }
            stopped = true;
        } 

//        private static void debugOutput(String s) {System.out.println(s);}

	/**
	 * Called from Java to start a main script
	 */
	public static Node main () {
            return new RootNode();}

	/**
	 * Called from Java to start a main script
         * "public static script main(String args[])"
         * this one will automatically System.exit(...) when done
         * exit code: 0 - success, 1 - otherwise
	 */
	public static Node mainscript () {
		RootNode result = new RootNode();
		result.isForMainScript = true;
        return result;
    }

	/**
	 * Called from Java to start a main script
         * Caller will also call FromJava.mainloop,
         * so commRootNode.startScript will not do mainLoop itself
	 */
	public static Node mainWithoutMainLoop () {
		RootNode result = new RootNode();
		result.isWithoutMainLoop = true;
        return result;
	}
	
	/**
	 * Called from Java to launch a script within the (first) RootScriptCall
	 */
	public static Node launch (Node n) {
	    Node parent = n.rootNode;
	    //if (parent==null) {
		//return launchThread();
	    //}
	    return new LaunchedNode (NodeTemplate.launchTemplate, parent);
	}

	/* *
	 * Called from Java to launch a script within a new thread
	 */
	//public static Node launchThread () {
	//    return new LaunchedThreadNode (NodeTemplate.launchTreadTemplate, RootNode.rootNode);
	//}

    static HashMap<Object, RequestList> mapAnchor2RequestList 
     = new HashMap<Object, RequestList>();
   
	
	public static boolean doCodeAtAnchor(Object anchor)
	{
		return doCodeAtAnchor(anchor, true);
	}
	public static boolean doCodeAtAnchor(Object anchor, boolean stopAtFirstSuccess)
	{
		RequestList requestList = mapAnchor2RequestList.get(anchor);
		if (requestList==null) {
			return false;
		}
		codeHasBeenExecutedThisRound = true;
		boolean result = false;
        synchronized (anchor) {
	      for (RequestNode n = requestList.first; n!=null; n = n.nextReq)
		  {
	    	  EventHandlingCodeFragmentNode ehcfn = (EventHandlingCodeFragmentNode) n;
             if (ehcfn.tryOut()==Boolean.TRUE) {
		        doNotify();
		        result = true;
		        if (stopAtFirstSuccess)
		        {
		        	break;
		        }
             }
	      }
		}
        return result;
	}
}

