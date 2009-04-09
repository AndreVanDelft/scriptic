/*
 * Created on 8 apr 2009
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package scriptic.vm;

public class SuspendResumeNode extends ParAndNode {
    /*
     * SuspendResume is implemented using the flag isSuspended on RequestNodes.
     * If that flag is set, the request's code should not be executed, or,
     * if it was already busy executing, it should not succeed for the time being.
     * If the request is a communication request, the flag setting should be propagated
     * to the requests in the communication body. 
     * Since there are multiple requests related to a single communication, the flag is essentially 
     * a counter, so that setting it n times requires resetting it n times, in order to resume.
     * Incrementing and decrementing the counter is by the method suspendOrResumeRequests(boolean suspend)
     * in class SpecificOperand
     * 
     * When is this method called? Let c be a child of a SuspendResumeNode
     * (or better: its SpecificOperand). c has a boolean flag hasSuspended
     * 
     * - when an atomic action under c happens
     *      for all right-hand siblings rhs of c:
     *         if (rhs.ticks>0) rhs.exclude()
     *         
     *   reason: must be a case such as x # y;(-+z)    
     *     rhs is here y;(-+z)  
     *     since ticks>0 y's atomic actions have happened
     *     since atomic action under x happens, x must have resumed because of -'s success
     *     then rest of rhs (i.e. z) must be excluded  
     *         
     * - when an atomic action under c happens, and if (!c.hasSuspended) 
     *      suspendOrResumeRequests(true) is called on c's left-hand siblings and c.hasSuspended=true;
     * - when c.succeed() or c deactivates, and if (c.hasSuspended)
     *      suspendOrResumeRequests(false) is called on c's left-hand siblings and c.hasSuspended=false;
     * 
     * This all is done using methods suspendOrResumeLeftSiblings(boolean suspend) and excludeRightSiblingsWithTick()
     */
	SuspendResumeNode(NodeTemplate template, Node parent) {
		super(template, parent);
	}
}
