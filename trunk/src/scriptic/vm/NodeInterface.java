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

/** Interface for a node in the run-time tree, for use by debuggers
 *  To enumerate the node hierarchy, start processing with rootNode();
 *
 *  process a node depth first =
 *    do whatever you want with this node
 *    if (firstChild()!=null) process it
 *    if (next      ()!=null) process it
 *
 *    
 */
public interface NodeInterface extends AttributesInterface {

       String getClassName();
       String getName();

	/** the unique root node */
	NodeInterface rootNode();

	/** number of instances created */
	int instances();

	/** instance creation index */
	int instanceNr();

	/** if true then this is an iteration; otherwise unknown */
	boolean isIteration();

	/** parent in main run-time tree */
	NodeInterface parent();

	/** previous sibling in main run-time tree */
	NodeInterface prev();

	/** next sibling in main run-time tree */
	NodeInterface next();

	/** first child */
	NodeInterface firstChild();

	/** active childs count */
	int activeChilds();

	/** associated template */
	NodeTemplateInterface template();

	/** associated frame */
	//ScriptFrameInterface frame();

	/** nearest ancestor that is an operand of a parallel operator */
	//SpecificOperand parOpAncestor();

	/** the cycle count at the parent's operatorAncestor at the time of this' creation */
	long cycleAtCreation();

	/** the tick count at the parOpAncestor at the time of this' last success */
	long ticksAtLastSuccess();

	/** ancestor in run-time tree that is a kind of script */
	NodeInterface refinementAncestor();

	/** ancestor in run-time tree that is reactive, i.e. '-' or '%' */
	NodeInterface reacAncestor();

	/** ancestor in run-time tree (or this) that is a script operator,
         *  e.g., *&+|/ etc.
         *  if not null: operatorAncestor.operatorAncestor == operatorAncestor
         */
	NodeInterface operatorAncestor();

	/** the unique initiator of this, below the rootNode */
	NodeInterface subRootNode();


	/** activation sequence number:
	 *  for parallel operators: incremented like loop passes
	 *  for sequence operators: incremented for each succesful child
	 */
	int cycle();

	/** code index associated with this (template) */
	int codeIndex();

	/** type code associated with this (template) */
	int typeCode();
}
