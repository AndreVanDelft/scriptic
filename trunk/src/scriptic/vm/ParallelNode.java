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
 * A parallel operator node in the runtime tree, i.e. an operator as +&*,/. Most
 * features are implemented in subclasses.
 * 
 * @version 1.0, 11/30/95
 * @author Andre van Delft
 */
abstract class ParallelNode extends OperatorNode {

	int indexOfActivatedOptionalExit = -1;

	// needed for ParBreakNode:
	int passOfLatestOperandWithSuccessOnActivation = -1;
	int templateChildIndexOfLatestOperandWithSuccessOnActivation = -1;

	public String toString() {
		return super.toString() + " rsuc:" + recentlySuccessfulChilds;
	}

	int recentlySuccessfulChilds = 0;

	ParallelNode(NodeTemplate template, Node parent) {
		super(template, parent);
	}

	int activate() {
		return activate((short) 0);
	}

	int activate(short firstChildTemplateIndex) {
		// assumes or-like behaviour
		boolean hadSuccessfulChilds = false;
		int activateResult = 0;
		int result = 0;
		outerLoop: for (int i = firstChildTemplateIndex;; i = 0) {
			for (; i < template.childs.length; i++) {
				activateResult = template.childs[i].activateFrom(this);

				if ((activateResult & ExitBit) != 0) {
					isIteration = false;
					break outerLoop;
				}
				boolean activationHasSuccess = (activateResult & SuccessBit) != 0;
				hadSuccessfulChilds |= activationHasSuccess;
				
				if (activationHasSuccess && (activateResult & CodeBit) == 0) {
					passOfLatestOperandWithSuccessOnActivation = pass;
					templateChildIndexOfLatestOperandWithSuccessOnActivation = i;
				}
				if ((activateResult & SomeBit) != 0) {
					if (pass > 0 || firstChildTemplateIndex > 0 || i > 0)
					// ..|x gets another chance at the first pass
					{
						indexOfActivatedOptionalExit = i;
						break outerLoop;
					}
					cycle++;
				} else {
					result |= activateResult & CodeBit;
				}
			}
			if (!isIteration)
				break;
			pass++;
		}
		if (hadSuccessfulChilds) {
			result |= SuccessBit;
			setRecentSuccess();
		}
		if (activeChilds == 0)
			markForDeactivation();
		return result;
	}

	void atomicActionHappensInChild(Node theChild) {
		if (theChild.hadRecentSuccess())
			recentlySuccessfulChilds--;
		if (theChild.cycleAtCreation == cycle && isIteration) // continue .. iteration
		{
			cycle++;
			int index = (indexOfActivatedOptionalExit + 1)
					% template.childs.length;
			indexOfActivatedOptionalExit = -1;
			activate((short) index);
		}
	}
}
