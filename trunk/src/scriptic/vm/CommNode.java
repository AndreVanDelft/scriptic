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

/** Communication node in the run-time tree 
 */
class CommNode extends Node 
implements RefinementNodeInterface, TryableNode  {

	CommFrame frame;
	
    CommNode (CommFrame frame, Node parent) {
        super (frame.template, parent);
        this.owner = frame.owner;
        this.frame = frame;
        refinementAncestor = this; // cannot through refinementAncestor4Child
    }

/** allow binding to parameters in case of reporting success at activation */
public Boolean tryOutInBoundMode(boolean wasUnbound) {return Boolean.TRUE;}

    /** Partner request nodes */
    CommRequestNode partners[];

    /**
     * answer a child's nearest ancestor that is reactive stuff, i.e. '-'
     * mostly the same as this' reacAncestor; sometimes this, however
     */
    Node reacAncestor4Child() { return this; }

    /** Answer parameter data: none of itself */
    public ValueHolder[] paramDataPrim() {return null;}

    /**
     * Return the parameters supplied by a communicating partner
     * @parm i: index for partner.
     * If not yet bound, then the first of the waiting
     * requests is taken (one of those that had this' commNode activated)
         */
    public ValueHolder[] paramDataPrim(int i)
    {
        CommRequestNode p = partner(i);
        if (p==null)
        {
          p = (CommRequestNode) ((CommFrame)frame).
                _partners[i]._requests.first;
        }
        if (p==null) // still possible...
                  return null;
        return p.paramData();
    }

    /**
     * Return the old parameters supplied by a communicating partner
     * If not yet bound, then the first of the waiting
     * requests is taken (one of those that had this' commNode activated)
     * @parm i: index for partner.
     */
    public Object[] oldParamDataPrim(int i)
    {
        CommRequestNode p = partner(i);
        if (p==null)
        {
          p = (CommRequestNode) ((CommFrame)frame).
                _partners[i]._requests.first;
        }
        if (p==null) // still possible...
                  return null;
        return p.oldParamData();
    }


    /** Answer the i'th partner in communication */
    public CommRequestNode partner(int i) {
      if (partners == null) {
    	  return null;
      }
      if (partners.length <= i) {
       return null;
      }    
      return partners[i];
    }

    /** 
     * a success: propagate to partners
     * partners should be present; early successes are handled by activate...
     */
    int childSucceeds (Node child) {
        if (partners==null) return 0;
        if (hadRecentSuccess()
        || testParams()==Boolean.FALSE ) return 0;
        setRecentSuccess();
        int result = 0;
        for (int i=0; i<partners.length; i++) {
        result |= partners[i].succeed();
        }
        return result;
    }

    /** 
     * a child hadSucceeded: propagate to partners
     */
    void childHadSucceeded (Node child) {
        if (partners==null) return;
        for (int i=0; i<partners.length; i++) {
            partners[i].hadSucceeded();
        }
        return;
    }

    /** 
     * answer a child's refinement ancestor: this selve
     */
    final RefinementNodeInterface refinementAncestor4Child() { return this; }

    /** 
     * Answer whether the i'th parameter is out
     */
    public boolean isOut (int i)
    {
        if (partners == null) return false;
            int[] partnerIndexAndPartnersIndex = template.partnerIndexOfParameterAt(partners, i);
            return ((ScriptCallNode)partners[partnerIndexAndPartnersIndex[0]].parent)
            		   .isActualParameterOut(partnerIndexAndPartnersIndex[1]);
    }

    /** 
     * Answer whether the i'th parameter is forced
     */
    public boolean isForced (int i)
    {
        if (partners == null) {
               return false;
        }
        int[] partnerIndexAndPartnersIndex = template.partnerIndexOfParameterAt(partners, i);
            //return ((ScriptCallNode)partners[p.x].parent).isActualParameterForcing(p.y);
            if (((ScriptCallNode)partners[partnerIndexAndPartnersIndex[0]].parent)
                .isActualParameterForcing(partnerIndexAndPartnersIndex[1])) {
              return true;
            }
            return false;
    }

    /**
     * Test wether the formal parameter at the given 
     * index equals the actual forcing value
     */
    public Boolean testParam (int index) {
        if (partners == null) return false;
        short nrParameters = 0;
        for (int i=0; i<partners.length; i++) {
            nrParameters = partners[i].template.nrIndexesAndParameters;
            if (index < nrParameters) return partners[i].testParam(index);
        index = (short) (index - nrParameters);
        //index -= nrParameters; gives compiler error
        }
        return Boolean.TRUE;
    }

    /**
     * Test wether all formal parameters equal the actual forcing values
     */
    public Boolean testParams () {
        if (partners == null) return Boolean.FALSE;
        for (int i=0; i<partners.length; i++) {
            if (partners[i].testParams()==Boolean.FALSE) return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /** exclude all partners except the given node */
    void excludeAllExcept (CommRequestNode node) {
        CommRequestNode partner;
        for (int i=0; i<partners.length; i++) {
        partner = partners[i];
        if (partner!=node) {
            partner.commNode = null;
            partner.exclude();
        }
        }
        partners = null;
        parOpAncestor.excludeRequests();
    }

    /** some local request hasSuccess. Propagate to partners */
    void hasSuccess()
    {
        if (partners!=null) {    // else error!
          for (int i=0; i<partners.length; i++)
          {
             partners[i].hasSuccess();
          }
        }
        else if(doTrace)trace("H> Unexpected: hasSuccess, partners==null ");
    }
    
    /**
     * this was unbound; now bind to the partners that were tried out 
     * and reactivate trialtree if applicable
     */
    void bindToPartners () {
        ((CommFrame)frame)._trialTree = null;
        for (int i=0; i<partners.length; i++) {
            CommRequestNode partner = partners[i];
            partner.deschedule();
            partner.commNode = this;
        }
        ((CommFrame)frame).activateWhenAllPartnerActive();
    }

    /** child n is about to deactivate: deactivate partners */
    void childDeactivates(Node n, boolean isActively) {
        if (isActively 
        &&  partners != null) // sometimes needed; unknown why...
        {
        for (int i=0; i<partners.length; i++) {
            partners[i].removeFromParOpList();
            partners[i].deactivate(true);
        }
        }
        //partners = null; NO, so launched expressions may still use parameters...
    }

    /** 
     * find partners and try them out on the given code fragment; bind if applicable
     * Note: the trick of finding possible partners is in the following rule:
     *   Two requests are in parallel, and thus possible partners,
     *   iff their traces upwards in the mixed trees of SpecificOperands
     *   and the parent nodes thereof, meet for the first time
     *   in such a parent node, so NOT in a SpecificOperands.
     * To check for this condition, these traces will be marked by a stamp value.
     */
    Boolean findAndTryPartnersFor (TryableNode tryableNode) {

        partners = new CommRequestNode[((CommFrame)frame)._partners.length];
        if (findAndTryRemainingPartnersFor (tryableNode, 0)) {

          currentStamp += ((CommFrame)frame)._partners.length;
          return Boolean.TRUE;
        }
        partners = null;
        return Boolean.FALSE;
    }

    /** 
     * find remaining partners and try them out on the 
     * given code fragment; bind if applicable.
     * @n Already established number of partners
     */
    final Boolean findAndTryRemainingPartnersFor (TryableNode tryableNode, int n) 
    {
        CommPartnerFrame partnerFrame = ((CommFrame)frame)._partners[n];
        for (CommRequestNode c = (CommRequestNode) partnerFrame._requests.first; c!=null;
         c = (CommRequestNode)c.nextReq)
        {
	        if (n>0 && !checkStamps(c,n))   continue; // must not exclude one another
	        partners[n] = c;
	        if (n < partners.length-1) {
	            placeStamps(c,n);
	            if (findAndTryRemainingPartnersFor (tryableNode, n+1)==Boolean.TRUE) {
	            return Boolean.TRUE;
	            }
	            removeStamps(c,n);
	        } else {
	            if (tryableNode.tryOutInBoundMode(/*wasUnbound=*/ true)==Boolean.TRUE) {
	            bindToPartners();
	            return Boolean.TRUE;
	            }
	        }
        }
        return Boolean.FALSE;
    }

    /** current value for stamping from communication requests upwards */
    long currentStamp;

    /**
     * place stamps currentStamp+n on all upwards SpecificOperands
     * and related parallel operators that have no stamps from the
     * current quest for partners
     */
    final void placeStamps(CommRequestNode c, int n) {
        Node p; SpecificOperand s;

        if (c.parOpAncestor==null&&traceOK) {
			debugOutput("Error: c.parOpAncestor==null... c=" + c);
			debugOutput(rootNode.infoNested().toString());
			(new Exception()).printStackTrace();
        }
		if (c.parOpAncestor.node==null&&traceOK) {
			debugOutput("Error: c.parOpAncestor.node==null... c=" + c);
			debugOutput(rootNode.infoNested().toString());
			(new Exception()).printStackTrace();
		}
        for (s=c.parOpAncestor, p=s.node.parent; p!=null; 
         s=s.parent       , p=s.node.parent) {
	        if (s.stamp > currentStamp) break; s.stamp = currentStamp+n;
	        if (p.stamp > currentStamp) break; p.stamp = currentStamp+n;
	        if (p instanceof CommNode) {
	            CommRequestNode r[] = ((CommNode)p).partners;
	            if (r==null) break; // error! should have been checked
	            for (int i=0; i<r.length; i++) {
	            placeStamps (r[i], n);
	            }
	        }
			if (c.parOpAncestor.node==null) {
				if (doTrace)debugOutput("Error: c.parOpAncestor.node==null... c=" + c);
				if (doTrace)debugOutput(rootNode.infoNested().toString());
				if (doTrace)(new Exception()).printStackTrace();
			}
        }
    }

    /**
     * remove stamps currentStamp+n on all upwards SpecificOperands
     * and related parallel operators
     */
    final void removeStamps(CommRequestNode c, int n) {
        Node p; SpecificOperand s;
		if (c.parOpAncestor==null) {
		if (doTrace)debugOutput("Error: c.parOpAncestor==null... c=" + c);
		if (doTrace)debugOutput(rootNode.infoNested().toString());
		if (doTrace)(new Exception()).printStackTrace();
		}
        for (s=c.parOpAncestor, p=s.node.parent; p!=null; 
         s=s.parent       , p=s.node.parent) {
	        if (s.stamp < currentStamp+n) break; s.stamp = 0;
	        if (p.stamp < currentStamp+n) break; p.stamp = 0;
	        if (p instanceof CommNode) {
	            CommRequestNode r[] = ((CommNode)p).partners;
	            if (r==null) break; // error! should have been checked
	            for (int i=0; i<r.length; i++) {
	            removeStamps (r[i], n);
	            }
	        }
        }
    }

    /**
     * check stamps on all upwards SpecificOperands
     * and related parallel operators up to the first one with a stamp from the
     * current quest for partners. 
     * If this first was a SpecificOperand   then return false
     * If this first was a parallel operator then return true
     * If an unbound CommNode is encountered then return false
     * If a    bound CommNode is encountered then return whether the check
     *                  returns true for each of its partners
     */
    final boolean checkStamps(CommRequestNode c, int n) {
        Node p; SpecificOperand s;
        for (s=c.parOpAncestor, p=s.node.parent; p!=null; 
         s=s.parent       , p=s.node.parent) {
        if (s.stamp > currentStamp) return false;
        if (p.stamp > currentStamp) return true;
        if (p instanceof CommNode) {
            CommRequestNode r[] = ((CommNode)p).partners;
            if (r==null) break; // error! should have been checked
            for (int i=0; i<r.length; i++) {
            if (!checkStamps (r[i], n)) return false;
            }
            return true;
        }
        }
        return true;
    }
}
