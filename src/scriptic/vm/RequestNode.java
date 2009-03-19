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
 * Most features are implemented in
 * This class is for representing a request node in the runtime tree,
 * i.e. a code fragment or a communicating partner.
 * subclasses.
 *
 * @version 	1.0, 11/30/95
 * @author	Andre van Delft
 */
abstract class RequestNode extends Node {

//boolean doTrace = true;

        public       double     duration = NO_DURATION;
        public       double old_duration = NO_DURATION;
	public       double  oldDuration()        {return old_duration  ;}
	public final double  getDuration()        {return     duration  ;}
	public final void    setDuration(double d){           duration=d;}

final boolean CHECK_LISTS=true;
void checkList(RequestList listOwner) {
   RequestNode n;
   for (n = listOwner.first; n!=null; n=n.nextReq) {
      if ( (n==listOwner.first) != (n.prevReq==null)
      ||    n.prevReq!=null     &&  n.prevReq.nextReq!=n)
      {
          debugOutput("checkList ERROR@"+listOwner              +'\n'
                     +"this:                 "+this             +'\n'
                     +"listOwner.first:      "+listOwner.first  +'\n'
                     +"node:                 "+n                +'\n'
                     +"node.prevReq:         "+n.prevReq        );
          if (n.prevReq!=null)
          debugOutput("node.prevReq.nextReq: "+n.prevReq.nextReq);
          (new Exception()).printStackTrace();
          return;
      }
   }
}

	/** previous in parallel-operand list */
	RequestNode prevInParOp;
	/** next in parallel-operand list */
	RequestNode nextInParOp;

	/** previous in waiting list */
	RequestNode prevReq;
	/** next in waiting list */
	public RequestNode nextReq;

	/** owner of waiting list */
	RequestList listOwner;

	RequestNode (NodeTemplate template) {this (template, null);}
	RequestNode (NodeTemplate template, Node parent) {
	                             super (template, parent);}

	int activate() {
if (CHECK_LISTS) {
 if (parOpAncestor==null)
  debugOutput("parOpAncestor==null in activate: "+this+" parent: "+parent);
}
	    scheduleRequest();
	    addToParOpList ();
	    return CodeBit;
	}

	abstract void scheduleRequest();
	abstract void deschedule();

	void hadSucceeded() {
	    Node n, p;
	    for (n=this; (p=n.reacAncestor) != null; n=p) {
                if(doTrace)n.trace("hadSucceeded>");

		switch (p.template.typeCode) {
		case ReactiveNotOperatorCode: if (!n.firstChild.hadRecentSuccess()
                                              &&  !n.           hadRecentSuccess())
                                              {    n.succeed();
                                                   n.setRecentSuccess();
                                              }
                                              break;
		case ParBreakOperatorCode:
			if (!p.hadRecentSuccess())
            {  
			   ParBreakNode pbn = (ParBreakNode) p;
			   int templateChildIndex = 0;
			   for (templateChildIndex=0; templateChildIndex<p.template.childs.length; templateChildIndex++) {
				   if (n.template==p.template.childs[templateChildIndex]) {
					   break;
				   }
			   }
			   boolean succeeds = ((ParallelNode)p).recentlySuccessfulChilds > 0
                   || pbn.passOfLatestOperandWithSuccessOnActivation > n.pass
                   || pbn.passOfLatestOperandWithSuccessOnActivation == n.pass
                   && pbn.templateChildIndexOfLatestOperandWithSuccessOnActivation > templateChildIndex;
			   if (succeeds) 
               {   p.succeed();
                   p.setRecentSuccess();
               }
            }
            break;
		case  ParOrOperatorCode:
		case    ParOr2OperatorCode: if (!p.hadRecentSuccess())
                                           {  if (((ParallelNode)p).recentlySuccessfulChilds > 0) 
                                              {   p.succeed();
                                                  p.setRecentSuccess();
                                              }
			                         }
                                           break;
		case  CommunicationDeclarationCode:
		case        ChannelDeclarationCode:
		case    SendChannelDeclarationCode:
		case ReceiveChannelDeclarationCode: p.childHadSucceeded(n); break;
		}
	    }
        }

	void addToParOpList() {
	    nextInParOp = parOpAncestor.firstReq;
	    if (nextInParOp != null) nextInParOp.prevInParOp = this;
	    prevInParOp = null;
	    parOpAncestor.firstReq = this;
if (doTrace)traceOutput("PA>"+getName()+" "+instanceNr+"@"+parOpAncestor.requestsString());
	}

	void removeFromParOpList() {
if (doTrace)traceOutput("PR>"+getName()+" "+instanceNr+"@"+(parOpAncestor!=null
                                          ?parOpAncestor.requestsString()
                                          :"parOpAncestor null"));

	    if  (prevInParOp != null) prevInParOp.nextInParOp = nextInParOp;
	    if  (nextInParOp != null) nextInParOp.prevInParOp = prevInParOp;
if (parOpAncestor==null){
if (doTrace)debugOutput("Warning: removeFromParOpList while parOpAncestor null... ");
//if (doTrace)debugOutput(commRootNode.infoNested().toString());
if (doTrace)debugOutput(toString());
if (doTrace)(new Exception()).printStackTrace();
// it seems this is not an error. This situation may occur for "a<<()||a>>()"
  return;
}
	    if  (parOpAncestor.firstReq == this)
                 parOpAncestor.firstReq = nextInParOp;
	}

	/**
         * Add this to a given request list, which is sorted on priority
         */
	synchronized void addToRequestList(RequestList listOwner) {
            if (doTrace)traceOutput("RA>"+getName()+" "+instanceNr+"@"+listOwner);

  	    this.listOwner = listOwner;

	    for (prevReq=null,    nextReq=listOwner.first; nextReq!=null && nextReq.priority>priority;
	         prevReq=nextReq, nextReq=nextReq.nextReq) {
	    }
            if (nextReq != null) nextReq.prevReq = this;
	    if (prevReq != null) prevReq.nextReq = this;
            else                 listOwner.first = this;
//checkList(listOwner);
	}

	/**
         * Remove this from the request list that it belongs to (listOwner)
         * The list is sorted on priority and duration.
         * When in list, the duration attribute only reflects the difference
         * in duration with the predecessor. The first in the list
         * keeps its duration.
         */
	synchronized void removeFromRequestList() {

	    if (listOwner==null) return; /*for Snd,Rec,Com that already communicate*/

            if (doTrace)traceOutput("RQ1>"+getName()+" "+instanceNr+"@"+listOwner);
            if (CHECK_LISTS) checkList(listOwner);

	    if (nextReq!=null)         nextReq.prevReq = prevReq;
	    if (this!=listOwner.first) prevReq.nextReq = nextReq;
	    else                       listOwner.first = nextReq;
	    if (this==listOwner.current)listOwner.current = (CodeFragmentNode) nextReq;
	    
	    nextReq = prevReq = null;  // clean up references

            if(doTrace)traceOutput("RQ2>"+getName()+" "+instanceNr+"@"+listOwner);
            if (CHECK_LISTS) checkList(listOwner);

	    listOwner   = null;
	}

	/** 
	 * this is to be descheduled and deactivated,
	 * probably because an exclusiding code fragment succeeded
	 */
	void exclude()
	{
if (doTrace)trace("X>");
	    deschedule(); // from request list or success list
	    deactivate(false); // upwards in tree
	}

        // Work to do:
        //ThreadNode threadAncestor() {return null;}

	/**
	 * this just had/will have success.
	 * Make sure appropriate requests will be excluded,
	 * special care for parallel operators, and propagate through partners
	 */
	void hasSuccess() {

if (doTrace)trace("H>");
if (doTrace)parOpAncestor.node.trace("P>");

	    SpecificOperand s1, parentOp;
	    RequestNode n, nextN;
if (CHECK_LISTS)
if (parOpAncestor==null) debugOutput("hasSuccess: parOpAncestor==null @"+this);

            if (FromJava.doCallbackHasSuccess()) {
                FromJava.debugger.callbackHasSuccess (this);
            }

	    parOpAncestor.passOfSuccess = rootNode.pass;

	    // stop sibbling threads
	    //ThreadNode thr = threadAncestor();
	    //for (ThreadNode t=parOpAncestor.firstThread; t!=null; t=t.nextThread) {
		//if (t!=thr) t.stop();
	    //}

	    // exclude sibbling requests
	    for (n=parOpAncestor.firstReq; n!=null; n=nextN) {
		nextN=n.nextInParOp;
		if (n!=this) {
                    n.exclude();
		}
	    }
	    parOpAncestor.firstReq=this;
	    nextInParOp = prevInParOp = null;

	    // exclude cousin requests
	    for (s1 = parOpAncestor.firstChild; s1 != null; s1=s1.next)
		 s1.excludeRequests();

	    // go up in ancestor line
	    for (s1 = parOpAncestor; (parentOp=s1.parent)!=null; s1=parentOp) {
                if (doTrace)s1.node.trace("H>^");
		switch (s1.node.template.typeCode) {
		case  CommunicationDeclarationCode:
		case        ChannelDeclarationCode:
		case    SendChannelDeclarationCode:
		case ReceiveChannelDeclarationCode:
if (doTrace)s1.node.trace("H>@CommNode: ");
                                  ((CommNode)s1.node).hasSuccess(); // propagate to partners
				  break;
		default: switch (s1.node.parent.template.typeCode) {
		  case ParBreakOperatorCode: ((ParBreakOperand)s1).excludeLeftSibblings(); // NO break;
		  case      ParAndOperatorCode:     
		  case  ParOrOperatorCode:     
		  case   ParAnd2OperatorCode:     
		  case    ParOr2OperatorCode:    // set cycle, recentSuccesses; continue ".."
		                    ((ParallelNode)s1.node.parent).successOccursInChild(s1.node);
		                    break;
		  }
		}
		// stop this extensive treatment if the parent is already handled this main pass
		if (parentOp.passOfSuccess == rootNode.pass) break;
		parentOp    .passOfSuccess  = rootNode.pass;
		s1.ticks++;

		//  exclude requests in parentOp
		for(n=parentOp.firstReq,parentOp.firstReq=null;n!=null;n=nextN) {
		    nextN=n.nextInParOp;
		    n.exclude();
		}

		// exclude those sibblings of s1 that have different the parent NODES
		// then we're sure that those sibblings are not in parallel !!!!
		if (s1.node.parent.template.typeCode != RootCode)
		    for (SpecificOperand s2=parentOp.firstChild; s2!=null; s2=s2.next) 
			if(s1.node.parent != s2.node.parent) s2.excludeRequests();
	    }
	    for (; s1!=null; s1=parentOp) { parentOp=s1.parent;
		s1.ticks++;
		if (parentOp!=null
		&&  s1.node.parent.template.typeCode == ParBreakOperatorCode) 
			        ((ParBreakOperand)s1).excludeLeftSibblings();
	    }
	}
	/**
	 * Return the paraneters: dummy to avoid subclass abstractness consequences
   */
	public ValueHolder[] paramDataPrim()
	{
	    return null;
	}

	/**
	 * Return the paraneters: dummy to avoid subclass abstractness consequences
   */
	public Object[] oldParamDataPrim()
	{
	    return null;
	}

}
